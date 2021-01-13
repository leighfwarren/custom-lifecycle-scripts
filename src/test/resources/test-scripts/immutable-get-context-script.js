var newContext = new ScriptEngineContext();
newContext.get = function() {
    throw new Error('This function should not be allowed to be set');
}
newContext.get();