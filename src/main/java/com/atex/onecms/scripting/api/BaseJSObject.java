package com.atex.onecms.scripting.api;

import com.atex.onecms.scripting.ImmutableException;
import jdk.nashorn.api.scripting.AbstractJSObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * BaseJSObject is a wrapper class that can be used to make Nashorn treat any wrapped class as a script object. This class subclasses
 * {@link jdk.nashorn.api.scripting.AbstractJSObject} and provides some sensible defaults for objects such as a valueOf function, and
 * toString function.<br/>
 * Values in the properties map can be accessed in scripts using standard JavaScript syntax such as: <code>obj['property']</code> or
 * <code>obj.property</code>.<br/>
 * Subclasses of this class can be made immutable, ie assignment of properties or calls to <code>delete obj.property</code> will throw
 * an {@link com.atex.onecms.scripting.ImmutableException}
 * @param <T> The Type to be wrapped.
 */
public abstract class BaseJSObject<T> extends AbstractJSObject {

    /**
     * The object that this JSObject wraps. E.g. a ContentWrite or ContentResult.
     */
    protected final T baseObject;

    /**
     *  A list of properties available to scripts.
     */
    protected final Map<String, Object> properties = new HashMap<>();

    /**
     * Determine if the object should be immutable in script code.
     */
    private final boolean immutable;

    /**
     * Create a new BaseJSObject.
     * @param immutable true if the object should be immutable in script code, otherwise false.
     */
    public BaseJSObject(final T baseObject, final boolean immutable) {
        this.baseObject = baseObject;
        this.immutable = immutable;
        properties.put("toString", jsToString());
        properties.put("valueOf", jsValueOf());
    }

    /**
     * Called by Nashorn engine to set the value of a property when something like obj.propertyName = x;
     * or obj['propertyName'] = x; is called in JS code.
     * @param name The name of the property to set.
     * @param value The value to set the property to.
     */
    @Override
    public void setMember(final String name, final Object value) {
        if (immutable) {
            throw new ImmutableException(name);
        }
        properties.put(name, value);
    }

    /**
     * Called by the Nashorn engine to delete a property when delete obj.propertyName is called.
     * @param name The name of the property to remove.
     */
    @Override
    public void removeMember(final String name) {
        if (immutable) {
            throw new ImmutableException(name);
        }
        properties.remove(name);
    }

    /**
     * Called by the Nashorn engine when obj.propertyName or obj['propertyName'] is called.
     * @param name The name of the property to get.
     * @return The value of the property.
     */
    @Override
    public Object getMember(final String name) {
        if ("hasOwnProperty".equals(name)) {
            return (Predicate<String>) properties::containsKey;
        }
        if (properties.containsKey(name)) {
            return properties.get(name);
        }
        return super.getMember(name);
    }

    @Override
    public String getClassName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Get a AbstractJSObject that represents the toString function on the
     * ContentFacade object.
     * @return The callable AbstractJSObject.
     */
    private AbstractJSObject jsToString() {
        return new AbstractJSObject() {
            @Override
            public Object call(final Object jsObj, final Object... args) {
                return "[object" + getClassName() + "]";
            }

            @Override
            public boolean isFunction() {
                return true;
            }
        };
    }

    /**
     * Get a AbstractJSObject that represents the valueOf function on the
     * ContentFacade object.
     * @return The callable AbstractJSObject.
     */
    private AbstractJSObject jsValueOf() {
        return new AbstractJSObject() {
            @Override
            public Object call(final Object jsObj, final Object... args) {
                return jsObj;
            }

            @Override
            public boolean isFunction() {
                return true;
            }
        };
    }

    public T getBaseObject() {
        return baseObject;
    }
}
