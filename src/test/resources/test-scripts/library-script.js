exports = function(val) {
    if ((typeof val) === 'number') {
        return val += 1;
    }
    throw new TypeError('val must a number');
};