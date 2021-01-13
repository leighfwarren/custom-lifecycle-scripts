package com.atex.onecms.scripting;

/**
 * ImmutableException represents an error during script runtime when an attempt has been made to modify an immutable object via
 * <code>delete obj.property</code> or <code>obj.property = someData;</code>.
 */
public class ImmutableException extends RuntimeException {
    public ImmutableException(final String propertyName) {
        super("Cannot remove or modify property '" + propertyName + "' as the object is immutable.");
    }
}
