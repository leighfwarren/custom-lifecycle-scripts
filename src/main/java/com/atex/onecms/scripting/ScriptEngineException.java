package com.atex.onecms.scripting;

public class ScriptEngineException extends Exception {
    public ScriptEngineException(final String s) {
        super(s);
    }

    protected ScriptEngineException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
