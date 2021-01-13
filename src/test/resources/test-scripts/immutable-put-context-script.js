var newContext = new ScriptEngineContext();
newContext.put = function() {
    throw new Error('This function should not be allowed to be set');
}
newContext.put();