// number is expected to be passed into the global context from the calling test (so we can evaluate the result after).
var context = run('update-context', new ScriptEngineContext('contentId', new ContentId('content', 'id')));
contentId = context.get('contentId');
print('script finished');