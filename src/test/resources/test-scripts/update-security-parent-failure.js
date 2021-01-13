/**
 * If the content status is "review" then we need to update the security parent on the new content
 * to "NEW VALUE" and then change the status on the new content to indicate it has been updated automatically.
 */

// var currentStatus = getByPath(oldContent, 'aspects/atex.WFContentStatus/data/status/statusID');
// if (currentStatus === 'review') {
//     newContent.aspects['p.InsertionInfo'].data.securityParentId  = new ContentId('contentid', 'policy:22.333');
// }
content.aspects['p.InsertionInfo'].data.securityParentId  = new ContentId('contentid', 'policy:22.333');
print('contentid changed');
deliberateError(newContent, 'aspects/p.InsertionInfo/data/securityParentId', 'policy:22.333');
print('didnt reach this point');

