// Number is expected to be passed into the global scope by the caller.
print('contentId before: ', contentId);
contentId = new ContentId('updated', 'updated');
print('contentId after: ', contentId);