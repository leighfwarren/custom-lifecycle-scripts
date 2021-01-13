package com.atex.onecms.scripting.api;

import com.atex.onecms.scripting.ContextMap;
import com.atex.onecms.scripting.ImmutableException;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ScriptEngineContext wraps a ContextMap and makes it more usable in the JS environment.
 * The class exposes the get and put methods of the ContentMap to the user, but ensures immutability
 * on those properties. Also,
 */
public class ScriptEngineContext extends BaseJSObject<ContextMap> {

    /**
     * Create a new ScriptEngineContext from a ContextMap.
     *
     * @param baseObject The ContextMap to wrap.
     */
    public ScriptEngineContext(final ContextMap baseObject) {
        super(baseObject, true);
    }

    /**
     * Create an empty ScriptEngineContext.
     */
    public ScriptEngineContext() {
        super(new ContextMap(), true);
    }

    /**
     * Create an empty ScriptEngineContext.
     */
    public ScriptEngineContext(final Object... args) {
        super(new ContextMap(args), true);
    }

    @Override
    public Object newObject(final Object... args) {
        ContextMap baseContext = new ContextMap(args);
        return new ScriptEngineContext(baseContext);
    }

    /**
     * Expose the necessary get and put methods to the user in JS-land.
     * @param name The name of the context item.
     * @return The context item.
     */
    @Override
    public Object getMember(final String name) {
        if ("get".equals(name)) {
            return (Function<String, Object>) baseObject::get;
        } else if ("put".equals(name)) {
            return (BiFunction<String, Object, Object>) baseObject::put;
        } else if ("remove".equals(name)) {
            return (Function<Object, Object>) baseObject::remove;
        } else if ("getBaseObject".equals(name)) {
            return (Supplier<ContextMap>) super::getBaseObject;
        }
        return baseObject.get(name);
    }

    @Override
    public void setMember(final String name, final Object value) {
        if ("get".equals(name) || "put".equals(name)) {
            throw new ImmutableException("get");
        }
        baseObject.put(name, value);
    }
}
