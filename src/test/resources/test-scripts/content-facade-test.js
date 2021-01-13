/**
 * Check ContentFacade implements getClassName correctly.
 */
print('Object.prototype.toString: ',  Object.prototype.toString(content));

/**
 * Check ContentFacade implements the defaultValue method, and the
 * toString and valueOf JSObject methods.
 */
print('calling content.toString: ', content);

print('JSON.stringify', JSON.stringify(content));

print('content.aspects: ', content.aspects.contentData.data);
delete content.aspects;
print(content);

/**
 * Check ContentFacade implements the getMember / hasMember methods correctly.
 */
print('content[\'aspects\']', content['aspects']);