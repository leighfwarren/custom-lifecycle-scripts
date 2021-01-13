// Test the mutability of Script Contexts and that valid indexes can be changed.
var newContext = new ScriptEngineContext('first', 1);
var firstVal = newContext.get('first');
if (firstVal !== 1) {
    throw new Error('First val should === 1');
}

newContext['second'] = 2;
var secondVal = newContext['second'];
if (secondVal !== 2) {
    throw new Error('Second val should === 2');
}
