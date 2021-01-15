package com.atex.onecms.scripting;

import com.atex.onecms.content.ContentManager;
import com.atex.onecms.content.ContentResult;
import com.atex.onecms.content.ContentVersionId;
import com.atex.onecms.content.Status;
import com.atex.onecms.content.Subject;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.polopoly.cm.ExternalContentId;
import com.rits.cloning.Cloner;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The LifecycleScriptingEngine stores and executes user scripts. The engine runs on top of the
 * Nashorn JavaScript engine.
 */
@SuppressWarnings("UnstableApiUsage")
public final class LifecycleScriptingEngine {

    /**
     * The library script that provides util functions for users.
     */
    private CompiledScript libraryScript;

    protected static final Logger LOGGER = Logger.getLogger(LifecycleScriptingEngine.class.getName());

    private final NashornScriptEngine engine;

    private final Executor detachedScriptExecutor;

    private final LoadingCache<String, CompilableScript> scriptCache;

    private final LoadingCache<String, ScriptList> scriptListCache;

    private final ContentManager contentManager;

    private static volatile LifecycleScriptingEngine INSTANCE;

    private static final Object LOCK = new Object();

    private LifecycleScriptingEngine(final ContentManager cm) {
        detachedScriptExecutor = Executors.newCachedThreadPool();
        engine = (NashornScriptEngine) new NashornScriptEngineFactory().getScriptEngine();
        contentManager = cm;
        scriptCache = CacheBuilder.newBuilder()
                                .maximumSize(1000)
                                .expireAfterAccess(5, TimeUnit.SECONDS)
                                .build(new CacheLoader<String, CompilableScript>() {
                                    @Override
                                    @ParametersAreNonnullByDefault
                                    public CompilableScript load(final String externalId) throws Exception {
                                        final ContentVersionId versionId =  cm.resolve(externalId, Subject.NOBODY_CALLER);
                                        if (versionId != null) {
                                            ContentResult<LifecycleScript> result = cm.get(versionId,
                                                    LifecycleScript.class,
                                                    Subject.NOBODY_CALLER);
                                            if (result.getStatus().equals(Status.OK)) {
                                                return new CompilableScript(result.getContent().getContentData());
                                            }
                                        }
                                        throw new Exception("Cannot find " + externalId);
                                    }
                                });

        scriptListCache = CacheBuilder.newBuilder()
                .maximumSize(1)
                .refreshAfterWrite(5, TimeUnit.SECONDS)
                .build(new CacheLoader<String, ScriptList>() {
                    @Override
                    @ParametersAreNonnullByDefault
                    public ScriptList load(final String contentId) throws Exception {
                        final ContentVersionId versionId =  cm.resolve(contentId, Subject.NOBODY_CALLER);
                        if (versionId != null) {
                            ContentResult<ScriptList> result = cm.get(versionId, ScriptList.class, Subject.NOBODY_CALLER);
                            if (result.getStatus().equals(Status.OK)) {
                                return result.getContent().getContentData();
                            }
                        }
                        throw new Exception("Cannot find " + contentId);
                    }
                });
    }

    /**
     * Get the instance of the engine.
     * @param cm The ContentManager to use when loading scripts
     * @return The engine instance.
     */
    public static LifecycleScriptingEngine getInstance(final ContentManager cm) {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = new LifecycleScriptingEngine(cm);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Run a script in a detached state, in it's own context and thread. Once started, the caller has
     * no control over the execution of the script, and any errors will only be logged. This allows a caller
     * to continue execution and not wait for a script to complete and calling.
     * @param scriptId The ID of the script to execute.
     * @param context The context to execute the script in.
     */
    public void runDetached(final String scriptId, final ContextMap context) {
        detachedScriptExecutor.execute(() -> {
            try {
                run(scriptId, context);
            } catch (ScriptEngineException | ExecutionException e) {
                LOGGER.log(Level.SEVERE, "Error running detached script " + scriptId, e);
            }
        });
    }

    /**
     * Run a script by ID.
     * @param scriptId The ID of the script to execute.
     * @param contextData The context to run the script under.
     * @return A new updated context containing changes applied by the script.
     * @throws ScriptEngineException If there is an error compiling or running the script with the given ID.
     */
    public ContextMap run(final String scriptId, final ContextMap contextData) throws ScriptEngineException, ExecutionException {
        CompilableScript compilableScript = scriptCache.get("com.atex.script." + scriptId);
        if (compilableScript == null) {
            throw new ScriptEngineException("No such script: " + scriptId);
        }
        return executeScript(compilableScript, contextData);
    }

    /**
     * Run all scripts in the engine registered to a given event and content type.
     * @param scriptType The event type to execute scripts for.
     * @param contentType The content type to execute scripts for. e.g atex.onecms.article
     * @param contextData The context to run this script in.
     * @return A new ScriptEngineContext containing the changes made to the original by scripts.
     * @throws ScriptEngineException If there is an error running any scripts that are triggered.
     */
    public ContextMap trigger(final ScriptType scriptType,
                              final String contentType,
                              final ContextMap contextData) throws ScriptEngineException {
        ContextMap newContext = contextData;
        ScriptList scriptList = null;
        try {
            scriptList = scriptListCache.get(ScriptList.EXTERNAL_ID);
        } catch (ExecutionException e) {
            LOGGER.log(Level.WARNING, "Failed to get ScriptList: " + ScriptList.EXTERNAL_ID, e);
        }
        if (scriptList != null) {
            for (ExternalContentId id : scriptList.getScripts()) {
                CompilableScript compilableScript;
                try {
                    compilableScript = scriptCache.get(id.getExternalId());

                    if (ScriptType.valueOf(compilableScript.getEvent()) == scriptType
                            && compilableScript.getContentType().matches(contentType)) {
                        newContext = executeScript(compilableScript, newContext);
                    }
                } catch (ExecutionException e) {
                    LOGGER.log(Level.WARNING, "Error executing script: " + id.getExternalId(), e);
                }

            }
        }
        return newContext;
    }

    private CompiledScript getCompiledLibraryScript() throws ScriptEngineException {
        if (libraryScript == null) {
            final InputStream scriptUtilStream = getClass().getClassLoader()
                    .getResourceAsStream("com/atex/onecms/scripting/script-util.js");
            try {
                libraryScript = engine.compile(new InputStreamReader(Objects.requireNonNull(scriptUtilStream)));
            } catch (ScriptException e) {
                throw new ScriptEngineException("Error compiling Script Utils", e);
            }
        }
        return libraryScript;
    }


    /**
     * Load a script in a similar fashion to a CommonJS module. Here, the script is executed in it's own
     * context, and the value of the "exports" binding is returned.
     * @param scriptId The ID of the script to require.
     * @return The value of the "exports" binding.
     * @throws ScriptEngineException If there is an error requiring the module.
     */
    public Object require(final String scriptId) throws ScriptEngineException {
        try {
            CompiledScript script = getScript(scriptId);
            ScriptContext ctx = new SimpleScriptContext();
            ctx.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
            Bindings ctxBindings = ctx.getBindings(ScriptContext.ENGINE_SCOPE);

            script.eval(ctx);
            return ctxBindings.get("exports");
        } catch (ScriptEngineException | ScriptException e) {
            throw new ScriptEngineException("Error requiring script: " + scriptId, e);
        }
    }

    private ContextMap runCompiledScripts(final ContextMap context, final CompiledScript... scripts) throws ScriptException {
        ScriptContext ctx = new SimpleScriptContext();
        ctx.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        Bindings ctxBindings = ctx.getBindings(ScriptContext.ENGINE_SCOPE);

        // Bind key/values from data onto the engine.
        for (Map.Entry<String, Object> content : context.entrySet()) {
            ctxBindings.put(content.getKey(), content.getValue());
        }
        for (CompiledScript script : scripts) {
            script.eval(ctx);
        }
        context.replaceAll((k, v) -> ctxBindings.get(k));
        return context;
    }

    private CompiledScript getScript(final String scriptId) throws ScriptEngineException {
        CompilableScript compilableScript;
        try {
            compilableScript = scriptCache.get("com.atex.script." + scriptId);
        } catch (ExecutionException e) {
            throw new ScriptEngineException("Cannot get script: " + scriptId, e);
        }

        try {
            return compilableScript.getCompiledScript(engine);
        } catch (ScriptException e) {
            throw new ScriptEngineException("Cannot get compiled script: " + scriptId, e);
        }
    }

    /**
     * Execute a script under a specific context.
     * @param script The script to execute.
     * @param context The context to execute under.
     * @return A new updated context containing changes applied by the script.
     * @throws ScriptEngineException If there is an error compiling the script, or running the script.
     */
    private ContextMap executeScript(final CompilableScript script, final ContextMap context) throws ScriptEngineException {
        CompiledScript compiledScript = getScript(script.getId());
        CompiledScript library = getCompiledLibraryScript();
        context.put("contentManager", contentManager);
        Cloner cloner = new Cloner();
        cloner.dontClone(contentManager.getClass());
        ContextMap newContext = cloner.deepClone(context);
        try {
            newContext = runCompiledScripts(newContext, library, compiledScript);
        } catch (ScriptException e) {
            throw new ScriptEngineException("Error running script: " + script.getId(), e);
        }
        return newContext;
    }

    /**
     * A CompilableScript aggregates a LifecycleScript and it's corresponding CompiledScript. The CompiledScript
     * is generated lazily from the LifecycleScript, and won't be created until the getCompiledScript function
     * is called, which then only compiled the code in the LifecycleScript if necessary.
     */
    private static class CompilableScript {
        /**
         * The LifecycleScript to wrap.
         */
        private final LifecycleScript script;

        /**
         * The compiled script based on the lifecycle scripts code.
         */
        private CompiledScript compiledScript;

        /**
         * Create a new CompilableScript instance based off a given LifecycleScript.
         * @param script The LifecycleScript to base this CompilableScript from.
         */
        CompilableScript(final LifecycleScript script) {
            this.script = script;
        }

        /**
         * Get the evaluated and compiled script for this CompilableScript. If the
         * script hasn't yet been compiled, it will be compiled using the provided
         * engine.
         * @return A CompiledScript that can be executed on a ScriptEngine.
         * @throws ScriptException If there is an error compiling the script.
         */
        CompiledScript getCompiledScript(final ScriptEngine engine) throws ScriptException {
            if (compiledScript == null) {
                compiledScript = ((Compilable) engine).compile(script.getScript());
            }
            return compiledScript;
        }


        /**
         * Get the ID of the underlying LifecycleScript.
         * @return The script ID.
         */
        String getId() {
            return script.getId();
        }

        /**
         * Get the content type to match with the underlying LifecycleScript.
         * @return The scripts content type.
         */
        String getContentType() {
            return script.getScriptType();
        }

        /**
         * Get the event within the underlying LifecycleScript.
         * @return The event this script should run during.
         */
        String getEvent() {
            return script.getEvent();
        }
    }
}
