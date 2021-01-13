
// require a specific function from a util script.
var sendMail = require('mailUtil');


// mailutil
function send1() {

}

function send2() {

}



// OR
// look for the same function name each time, eg 'run'
var sendMail = require('sendmail');

// so sendmail.js would look like:
function run(context) {

}

sendMail();

