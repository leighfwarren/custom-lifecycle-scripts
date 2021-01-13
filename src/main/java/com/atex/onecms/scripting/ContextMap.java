package com.atex.onecms.scripting;

import com.atex.onecms.scripting.api.BaseJSObject;

import java.util.HashMap;

/**
 * ContextMap is a Map type that stores Objects for use in scripts.
 * This type has handling for instances of BaseJSObject (such as ContentWriteFacade)
 * which will return the underlying value of the object to keep code clean for the user.
 */
public class ContextMap extends HashMap<String, Object> {
    private static final long serialVersionUID = 3401989712261567177L;

    /**
     * Create a new context with a given list of key value pairs.
     * @param contextData A list of keys and values that be added to the context. Keys should be Strings.
     *                    For example: new ContextMap("keyName", someObject, "otherKey", otherVal);
     */
    public ContextMap(final Object... contextData) {
        if (contextData != null) {
            if (contextData.length % 2 != 0) {
                throw new IllegalArgumentException("Context data should be a String key, followed by an Object value.");
            }
            for (int i = 0; i < contextData.length; i = i + 2) {
                this.put((String) contextData[i], contextData[i + 1]);
            }
        }
    }

    /**
     * Get a context object mapped to a certain key. If the object is an instance of
     * BaseJSObject, then the wrapped value is returned.
     * @param key The key to search the map for.
     * @return The Object corresponding to the in the map, or null if one doesn't exist.
     */
    public Object get(final String key) {
        Object value = super.get(key);
        if (value instanceof BaseJSObject) {
            return ((BaseJSObject<?>) value).getBaseObject();
        }
        return value;
    }
}
