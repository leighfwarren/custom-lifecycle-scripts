package com.atex.onecms.scripting;

import com.atex.onecms.app.dam.standard.aspects.OneArticleBean;
import com.atex.onecms.app.dam.workflow.WFContentStatusAspectBean;
import com.atex.onecms.app.dam.workflow.WFStatusBean;
import com.atex.onecms.content.*;
import com.atex.onecms.content.aspects.Aspect;
import com.atex.onecms.scripting.api.ContentWriteFacade;
import com.atex.plugins.structured.text.StructuredText;
import com.polopoly.cm.ExternalContentId;
import com.polopoly.util.StringUtil;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This test class mocks the ContentManager using mockito. Because the LifecycleScriptingEngine is used for all tests is a static instance,
 * and so ContentManager to be injected is also a static instance, the @Mock or @RunWith(MockitoJUnitRunner.class) annotations
 * cannot be used as they will mock a new instance of the ContentManger for each test method that runs.
 */
public class LifecycleScriptTest {

    private static ContentManager cm;

    /**
     * The scriptList that ContentManager mock will defer to when searching for scripts.
     */
    private static final ScriptList scriptList = new ScriptList();


    private static int contentVersionCounter = 300;

    @BeforeClass
    public static void initContentManager() {
        scriptList.setScripts(new ArrayList<>());
        cm = mock(ContentManager.class);

        // create and add a mock stub for the ScriptList.
        ContentResultBuilder<ScriptList> builder = new ContentResultBuilder<>();
        builder.status(Status.OK);
        builder.mainAspectData(scriptList);
        ContentResult<ScriptList> result = builder.build();

        ContentVersionId scriptListId = setupResolve(createPolicyContentVersionId(2), ScriptList.EXTERNAL_ID);
        when(cm.get(eq(scriptListId), eq(ScriptList.class), eq(Subject.NOBODY_CALLER))).thenReturn(result);
    }



    private static ContentVersionId createPolicyContentVersionId(final int major) {
        return createContentVersionId("policy", major + ".");
    }

    private static ContentVersionId createContentVersionId(final String delegationId, final String prefix) {
        final String key;
        if (StringUtil.isEmpty(prefix)) {
            key = Integer.toString(contentVersionCounter++);
        } else {
            key = prefix + contentVersionCounter++;
        }
        if ("policy".equals(delegationId)) {
            return new ContentVersionId(delegationId, key, Long.toString(new Date().getTime()));
        } else {
            return new ContentVersionId(delegationId, key, UUID.randomUUID().toString());
        }
    }

    private static ContentVersionId setupResolve(final ContentVersionId id, final String externalId) {
        when(cm.resolve(externalId, Subject.NOBODY_CALLER)).thenReturn(id);
        return id;
    }

    public void setupScriptContentResolve(final LifecycleScript script) {
        String externalId = "com.atex.script." + script.getId();
        ContentVersionId id = setupResolve(createPolicyContentVersionId(2), externalId);
        scriptList.getScripts().add(new ExternalContentId(externalId));

        ContentResultBuilder<LifecycleScript> builder = new ContentResultBuilder<>();
        builder.mainAspectData(script);
        builder.status(Status.OK);
        ContentResult<LifecycleScript> result = builder.build();
        when(cm.get(id, LifecycleScript.class, Subject.NOBODY_CALLER)).thenReturn(result);
    }

    /**
     * Get a default ContentWrite based on an article that can be easily wrapped and passed to the engine context.
     * @return A new ContentWrite from a builder.
     */
    private ContentWrite<OneArticleBean> getContentWrite() {
        ContentWriteBuilder<OneArticleBean> builder = new ContentWriteBuilder<>();
        OneArticleBean article = new OneArticleBean();
        final String articleHeadline = "New Article";
        article.setHeadline(new StructuredText(articleHeadline));
        builder.mainAspectData(article).aspect(new Aspect<>(InsertionInfoAspectBean.ASPECT_NAME, new InsertionInfoAspectBean()));
        return builder.build();
    }

    /**
     * Get the contents of a resource at the given path.
     * @param path The path to the resource.
     * @return The contents of the file as a string.
     * @throws IOException If there is an error copying the contents of the resource stream to a writer.
     */
    private String getFileContents(String path) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path)), writer);
        return writer.toString();
    }

    /**
     * Create a LifecycleScript from a .js script in a resource path.
     * @param path The path to the script file.
     * @return A new LifecycleScript that can be run on the engine.
     */
    private LifecycleScript getScript(String path) {
        LifecycleScript script = new LifecycleScript();
        // need a unique scriptType so when all tests are run, no scripts are conflicting
        final String scriptType = UUID.randomUUID().toString();
        script.setScriptType(scriptType);
        script.setEvent(ScriptType.PRE_STORE.toString());
        String scriptContent = "";
        try {
            scriptContent = getFileContents(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        script.setScript(scriptContent);
        script.setId(UUID.randomUUID().toString());
        return script;
    }

    /**
     * Get a new ContentWrite with a specific workflow status.
     * @param status The status ID to set the WorkflowStatus to.
     * @return A new ContentWrite with the given status.
     */
    private ContentWrite<OneArticleBean> getContentWriteWithStatus(final String status) {
        ContentWriteBuilder<OneArticleBean> builder = new ContentWriteBuilder<>();
        ContentId contentId = new ContentId("contentid", "policy:22.222");
        InsertionInfoAspectBean insertionInfoBean = new InsertionInfoAspectBean(contentId);

        WFStatusBean statusBean = new WFStatusBean();
        statusBean.setStatusID(status);
        WFContentStatusAspectBean contentStatus = new WFContentStatusAspectBean();
        contentStatus.setStatus(statusBean);

        builder.mainAspectData(new OneArticleBean())
                .aspects(new Aspect<>(InsertionInfoAspectBean.ASPECT_NAME, insertionInfoBean))
                .aspect(WFContentStatusAspectBean.ASPECT_NAME, contentStatus);
        return builder.build();
    }

    /**
     * Test the ContentFacade class handles all standard JS operations such as getting / setting
     * properties, it's default value etc.
     */
    @Test
    public void contentFacadeCanBeModified() {
        final String scriptType = "contentFacadeTest";
        LifecycleScript script = new LifecycleScript();
        script.setEvent(ScriptType.PRE_STORE.toString());
        script.setScriptType(scriptType);
        final String scriptId = "content-facade-test";
        script.setId(scriptId);
        try {
            script.setScript(getFileContents("test-scripts/content-facade-test.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        setupScriptContentResolve(script);

        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);
        ContentWrite<OneArticleBean> content = getContentWrite();
        try {
            engine.trigger(ScriptType.PRE_STORE, scriptType, new ContextMap("content", new ContentWriteFacade(content)));
        } catch (ScriptEngineException e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Test that modifying ContentWriteFacade with immutable flag set is not changed when deleting / modifying properties.
     */
    @Test(expected = ImmutableException.class)
    public void immutableContentFacadeCannotBeModified() throws ImmutableException {
        final String scriptType = "immutableContentFacadeTest";
        LifecycleScript script = new LifecycleScript();
        script.setEvent(ScriptType.PRE_STORE.toString());
        script.setScriptType(scriptType);
        script.setId(UUID.randomUUID().toString());
        try {
            script.setScript(getFileContents("test-scripts/content-facade-test.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        setupScriptContentResolve(script);
        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);

        ContentWrite<OneArticleBean> content = getContentWrite();
        ContentWriteFacade contentWriteFacade = new ContentWriteFacade(content, true);
        try {
            engine.trigger(ScriptType.PRE_STORE, scriptType, new ContextMap("content", contentWriteFacade));
        } catch (ScriptEngineException e) {
            // We're expecting the script to fail due to an ImmutableException, so fail if anything else goes wrong.
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Test that scripts can modify data of the objects passed to them.
     */
    @Test
    public void canModifyData() {
        // Prepare the data to pass to the engine.
        // Create a content write
        ContentWriteBuilder<OneArticleBean> builder = new ContentWriteBuilder<>();
        ContentId contentId = new ContentId("contentid", "policy:22.222");
        InsertionInfoAspectBean insertionInfoBean = new InsertionInfoAspectBean(contentId);

        WFStatusBean statusBean = new WFStatusBean();
        statusBean.setStatusID("review");
        WFContentStatusAspectBean contentStatus = new WFContentStatusAspectBean();
        contentStatus.setStatus(statusBean);

        builder.mainAspectData(new OneArticleBean())
                .aspects(new Aspect<>(InsertionInfoAspectBean.ASPECT_NAME, insertionInfoBean))
                .aspect(WFContentStatusAspectBean.ASPECT_NAME, contentStatus);
        ContentWrite<OneArticleBean> content = builder.build();

        // Get instance of the engine.
        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);

        // Create and register a script with the engine.
        LifecycleScript script = getScript("test-scripts/update-security-parent.js");
        script.setScriptType("canModifyData");

        setupScriptContentResolve(script);

        try {
            ContextMap context = new ContextMap("content", new ContentWriteFacade(content));
            ContextMap newContext = engine.trigger(ScriptType.valueOf(script.getEvent()), script.getScriptType(), context);
            ContentWrite<?> updatedContentWrite = (ContentWrite<?>) newContext.get("content");
            assertNotEquals(contentId, ((InsertionInfoAspectBean)updatedContentWrite.getAspect(InsertionInfoAspectBean.ASPECT_NAME)).getSecurityParentId());
            String status = ((WFContentStatusAspectBean)updatedContentWrite.getAspect("atex.WFContentStatus")).getStatus().getStatusID();
            assertEquals(status, "finished");
        } catch (ScriptEngineException e) {
            e.printStackTrace();fail();
        }
    }

    /**
     * Test that in the event of an error executing a script the state passed to the script it reverted.
     */
    @Test
    public void executionsAreAtomic() {
        // Prepare the data to pass to the engine.
        ContentWriteBuilder<OneArticleBean> builder = new ContentWriteBuilder<>();
        ContentId contentId = new ContentId("contentid", "policy:22.222");
        InsertionInfoAspectBean insertionInfoBean = new InsertionInfoAspectBean(contentId);

        WFStatusBean statusBean = new WFStatusBean();
        statusBean.setStatusID("review");
        WFContentStatusAspectBean contentStatus = new WFContentStatusAspectBean();
        contentStatus.setStatus(statusBean);

        builder.mainAspectData(new OneArticleBean())
                .aspects(new Aspect<>(InsertionInfoAspectBean.ASPECT_NAME, insertionInfoBean))
                .aspect(WFContentStatusAspectBean.ASPECT_NAME, contentStatus);
        ContentWrite<OneArticleBean> content = builder.build();

        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);

        // Create and register a script with the engine.
        LifecycleScript script = getScript("test-scripts/update-security-parent-failure.js");
        setupScriptContentResolve(script);

        ContextMap context = new ContextMap("content", new ContentWriteFacade(content));
        try {
            assertEquals(contentId, ((InsertionInfoAspectBean)content.getAspect(InsertionInfoAspectBean.ASPECT_NAME)).getSecurityParentId());
            engine.trigger(ScriptType.valueOf(script.getEvent()), script.getScriptType(), context);
        } catch (ScriptEngineException e) {
            assertEquals(contentId, ((InsertionInfoAspectBean)content.getAspect(InsertionInfoAspectBean.ASPECT_NAME)).getSecurityParentId());
        }
    }

    @Test
    public void threadTest() throws ScriptEngineException, InterruptedException {
        LifecycleScript script = new LifecycleScript();
        final String scriptType = "performanceTest";
        script.setScriptType(scriptType);
        script.setEvent(ScriptType.PRE_STORE.toString());
        script.setId(UUID.randomUUID().toString());
        try {
            script.setScript(getFileContents("test-scripts/update-security-parent.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(script);

        AtomicLong totalRunTime = new AtomicLong(0L);
        Object lock = new Object();


        LifecycleScriptingEngine taskEngine = LifecycleScriptingEngine.getInstance(cm);
        ContentWriteFacade content = new ContentWriteFacade(getContentWriteWithStatus("review"));
        taskEngine.trigger(ScriptType.PRE_STORE, scriptType, new ContextMap("content", content));
        Callable<Long> runScript = () -> {
            try {
                long startTime = System.nanoTime();
                taskEngine.trigger(ScriptType.PRE_STORE, scriptType, new ContextMap("content", content));
                long endTime = System.nanoTime();
                return endTime - startTime;
            } catch (ScriptEngineException e) {
                e.printStackTrace();
            }
            return 0L;
        };

        ExecutorService executor = Executors.newFixedThreadPool(1);
        List<Callable<Long>> scriptTasks = new ArrayList<>();
        final int scriptRuns = 100;
        for (int i = 0; i < scriptRuns; i++) {
            scriptTasks.add(runScript);
        }

        executor.invokeAll(scriptTasks).stream().map(
                future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return 0L;
                }
        ).forEach(runTime -> {
            synchronized (lock) {
                totalRunTime.addAndGet(runTime);
            }
        });
        System.out.println("Finished all scripts.");
        System.out.printf("Total runtime: %d nanoseconds\n", totalRunTime.longValue());
        long nanosecondRuntime = totalRunTime.longValue() / scriptRuns;
        long millisRuntimeWholePart = nanosecondRuntime / 1000000;
        long millisRuntimeFractionalPart = nanosecondRuntime % 1000000;
        System.out.printf("Mean runtime per script: %d nanoseconds (%d.%d milliseconds)", totalRunTime.longValue() / scriptRuns, millisRuntimeWholePart, millisRuntimeFractionalPart);
    }

    @Test
    public void performanceTest() throws ScriptEngineException {
        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);
        LifecycleScript script = new LifecycleScript();
        final String scriptType = "performanceTest";
        script.setScriptType(scriptType);
        script.setEvent(ScriptType.PRE_STORE.toString());
        script.setId(UUID.randomUUID().toString());
        try {
            script.setScript(getFileContents("test-scripts/update-security-parent.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(script);

        long totalTime = 0L;
        int maxRuns = 100; // 100million
        for (int i = 0; i < maxRuns; i++) {
            ContentWriteFacade content = new ContentWriteFacade(getContentWriteWithStatus("review"));
            long startTime = System.nanoTime();
            engine.trigger(ScriptType.PRE_STORE, scriptType, new ContextMap("content", content));
            long endTime = System.nanoTime();
            totalTime += endTime - startTime;
        }
        System.out.printf("Average time in nanoseconds over %d runs: %d", maxRuns, totalTime / maxRuns);
    }

    // This test requires credentials for an email host provider. To test it correctly update email-user.js
    // with some credentials (temporarily) add the test annotation to this method and run it.
    //@Test
    public void emailTest() {
        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);
        LifecycleScript script = new LifecycleScript();
        final String scriptType = "emailTest";
        script.setScriptType(scriptType);
        script.setEvent(ScriptType.PRE_STORE.toString());
        script.setId(UUID.randomUUID().toString());
        try {
            script.setScript(getFileContents("test-scripts/email-user.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(script);

        try {
            engine.trigger(ScriptType.PRE_STORE, script.getScriptType(), new ContextMap());
        } catch (ScriptEngineException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void runDetachedScript() {
        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);
        LifecycleScript originScript = new LifecycleScript();
        originScript.setScriptType("");
        originScript.setEvent(ScriptType.PRE_STORE.toString());
        originScript.setId("run-detached-script");
        try {
            originScript.setScript(getFileContents("test-scripts/run-detached-script.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(originScript);

        LifecycleScript secondaryScript = new LifecycleScript();
        secondaryScript.setScriptType("");
        secondaryScript.setEvent(ScriptType.PRE_STORE.toString());
        secondaryScript.setId("detached-script");
        try {
            secondaryScript.setScript(getFileContents("test-scripts/detached-script.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(secondaryScript);

        try {
            engine.run(originScript.getId(), new ContextMap());
        } catch (ScriptEngineException | ExecutionException e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Test that if a script called from another scripts fails, it doesn't take down the other scripts.
     */
    @Test
    public void detachedScriptErrorsDoNotBreakCaller() {
        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);
        LifecycleScript originScript = new LifecycleScript();
        originScript.setScriptType("");
        originScript.setEvent(ScriptType.PRE_STORE.toString());
        originScript.setId("run-detached-script-fail");
        try {
            originScript.setScript(getFileContents("test-scripts/run-detached-script-fail.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(originScript);

        LifecycleScript secondaryScript = new LifecycleScript();
        secondaryScript.setScriptType("");
        secondaryScript.setEvent(ScriptType.PRE_STORE.toString());
        secondaryScript.setId("detached-script-fail");
        try {
            secondaryScript.setScript(getFileContents("test-scripts/detached-script-fail.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(secondaryScript);

        try {
            engine.run(originScript.getId(), new ContextMap());
        } catch (ScriptEngineException | ExecutionException e) {
            // We should see output in the console from the detached script failing, but that
            // shouldn't be from this exception. If it is, the failure was caused by something else, and
            // the test should fail.
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void canPassContextToDetachedScript() {
        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);
        LifecycleScript originScript = new LifecycleScript();
        originScript.setScriptType("");
        originScript.setEvent(ScriptType.PRE_STORE.toString());
        originScript.setId("run-detached-script-with-context");
        try {
            originScript.setScript(getFileContents("test-scripts/run-detached-script-fail.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(originScript);

        LifecycleScript secondaryScript = new LifecycleScript();
        secondaryScript.setScriptType("");
        secondaryScript.setEvent(ScriptType.PRE_STORE.toString());
        secondaryScript.setId("detached-script-fail");
        try {
            secondaryScript.setScript(getFileContents("test-scripts/detached-script-fail.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(secondaryScript);

        try {
            engine.run(originScript.getId(), new ContextMap());
        } catch (ScriptEngineException | ExecutionException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void awaitRunScript() {
        LifecycleScript originScript = new LifecycleScript();
        originScript.setScriptType("");
        originScript.setEvent(ScriptType.PRE_STORE.toString());
        originScript.setId("await-called-script");
        try {
            originScript.setScript(getFileContents("test-scripts/await-called-script.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(originScript);

        LifecycleScript secondaryScript = new LifecycleScript();
        secondaryScript.setScriptType("");
        secondaryScript.setEvent(ScriptType.PRE_STORE.toString());
        secondaryScript.setId("update-context");
        try {
            secondaryScript.setScript(getFileContents("test-scripts/update-context.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(secondaryScript);

        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);
        try {
            ContextMap context = engine.run(originScript.getId(), new ContextMap("contentId", new ContentId("id", "key")));
            assertEquals("updated", ((ContentId)context.get("contentId")).getDelegationId());
        } catch (ScriptEngineException | ExecutionException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void canRequireLibraryExports() {
        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);
        LifecycleScript script = new LifecycleScript();
        script.setScriptType("");
        script.setEvent(ScriptType.PRE_STORE.toString());
        script.setId("require-script");
        try {
            script.setScript(getFileContents("test-scripts/require-script.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(script);

        LifecycleScript libraryScript = new LifecycleScript();
        libraryScript.setScriptType("");
        libraryScript.setEvent(ScriptType.PRE_STORE.toString());
        libraryScript.setId("library-script");
        try {
            libraryScript.setScript(getFileContents("test-scripts/library-script.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(libraryScript);

        try {
            ContextMap context = engine.run(script.getId(), new ContextMap("number", 1));
            // Any integer values coming from the JS engine are of type double (since JS number type is always double).
            assertEquals(2, ((Double)context.get("number")).intValue());
        } catch (ScriptEngineException | ExecutionException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void requiredLibraryDoesntEffectCallerScope() {
        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);
        LifecycleScript script = new LifecycleScript();
        script.setScriptType("");
        script.setEvent(ScriptType.PRE_STORE.toString());
        script.setId("caller-script");
        script.setScript("require('require-with-global');");
        setupScriptContentResolve(script);

        LifecycleScript libraryScript = new LifecycleScript();
        libraryScript.setScriptType("");
        libraryScript.setEvent(ScriptType.PRE_STORE.toString());
        libraryScript.setId("require-with-global");
        try {
            libraryScript.setScript(getFileContents("test-scripts/require-with-global.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(libraryScript);

        try {
            String globalValue = "Hello, World";
            ContextMap context = engine.run(script.getId(), new ContextMap("globalValue", globalValue));
            // script require-with-global sets globalValue to 1, however it should be running in it's own scope, and
            // calls to require should only return the value bound to the 'exports' variable, meaning globalValue
            // should be left alone·
            assertEquals(globalValue, context.get("globalValue"));
        } catch (ScriptEngineException | ExecutionException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void useScriptContextInJS() {
        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);
        LifecycleScript script = new LifecycleScript();
        script.setScriptType("");
        script.setEvent(ScriptType.PRE_STORE.toString());
        script.setId("context-script");
        try {
            script.setScript(getFileContents("test-scripts/context-script.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(script);

        try {
            engine.run(script.getId(), new ContextMap());
        } catch (ScriptEngineException | ExecutionException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test(expected = ImmutableException.class)
    public void scriptContextGetMethodIsImmutable() {
        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);
        LifecycleScript script = new LifecycleScript();
        script.setScriptType("");
        script.setEvent(ScriptType.PRE_STORE.toString());
        script.setId("immutable-get-context-script");
        try {
            script.setScript(getFileContents("test-scripts/immutable-get-context-script.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(script);

        try {
            engine.run(script.getId(), new ContextMap());
        } catch (ScriptEngineException | ExecutionException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test(expected = ImmutableException.class)
    public void scriptContextPutMethodIsImmutable() {
        LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);
        LifecycleScript script = new LifecycleScript();
        script.setScriptType("");
        script.setEvent(ScriptType.PRE_STORE.toString());
        script.setId("immutable-put-context-script");
        try {
            script.setScript(getFileContents("test-scripts/immutable-put-context-script.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupScriptContentResolve(script);

        try {
            engine.run(script.getId(), new ContextMap());
        } catch (ScriptEngineException | ExecutionException e) {
            e.printStackTrace();
            fail();
        }
    }
}
