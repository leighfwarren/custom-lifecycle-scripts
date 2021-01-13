/**
 * Java Object imports
 */
var ContentId = Java.type('com.atex.onecms.content.ContentId');
var ScriptEngineContext = Java.type('com.atex.onecms.scripting.api.ScriptEngineContext');

/**
 * Resolve an externalId to a ContentVersionId using ContentManager.
 * @param externalId The externalId to resolve.
 * @returns {ContentVersionId} The ContentVersionId of the Content that externalId refers to.
 */
function resolve(externalId) {
    var Subject = Java.type('com.atex.onecms.content.Subject');
    return contentManager.resolve(externalId, Subject.NOBODY_CALLER);
}

/**
 *  Set the partition of some content.
 * @param {Content} content The content to set the partition on.
 * @param {String} partitionName The name of the partition to set.
 */
function setPartition(content, partitionName) {
    var PartitionUtils = Java.type('com.atex.workflow.PartitionUtils');
    PartitionUtils.changePartition(content, partitionName);
}

/**
 * Set the WorkFlow (Print) status of a content.
 * @param {Content} content The content to set the status on.
 * @param {string} statusId The statusID to set.
 */
function setWFStatus(content, statusId) {
    var WFStatusUtils = Java.type('com.atex.onecms.app.dam.workflow.WFStatusUtils');
    var statusUtils = new WFStatusUtils(contentManager);
    var statusBean = statusUtils.getStatusById(statusId);
    if (statusBean === null) {
        throw new Error('No status with the ID "' + statusId + '"');
    }
    var contentStatusBean = content.getAspect(Java.type('com.atex.onecms.app.dam.workflow.WFContentStatusAspectBean').ASPECT_NAME);
    contentStatusBean.setStatus(statusBean);
}

/**
 * Set the Web Status of a content.
 * @param {Content} content The Content to set the status on.
 * @param {string} statusId The ID to set the status to.
 */
function setWebStatus(content, statusId) {
    var WebStatusUtils = Java.type('com.atex.workflow.WebStatusUtils');
    var wsUtils = new WebStatusUtils(contentManager);
    wsUtils.setWebStatus(content, statusId);
}
/**
 * @typedef {Object} ScriptEngineContext
 * @property {function(contextKey:{string}): Object} get - Get the context item linked to this key.
 * @property {function(contextKey:{string}, contextItem:{Object}): void} put - Put an item into the context under a unique key.
 * @property {function(contextKey:{string}): boolean} containsKey - Returns true if the context contains an item with this key.
 */
/**
 * Run a script with a given ID and await it's completion. The function returns a deep-copy of the context passed to
 * it with any changes made in the called script.
 * @param {string} scriptId The ID of the script to run.
 * @param {ScriptEngineContext} context The context to provide the script.
 * @returns {ScriptEngineContext} An updated {@link ScriptEngineContext} object.
 */
function run(scriptId, context) {
    var engine = (Java.type('com.atex.onecms.scripting.LifecycleScriptingEngine')).getInstance(contentManager);
    if (context) {
        return engine.run(scriptId, context.getBaseObject());
    } else {
        var ContextMap = Java.type('com.atex.onecms.scripting.ContextMap');
        return engine.run(scriptId, new ContextMap());
    }
}

/**
 * Invoke a script with an ID. The script is run in a different thread and context to this
 * one, meaning changes to this context will not effect it.
 * @param {string} scriptId The ID of the script to run.
 * @param {ScriptEngineContext} context The context to provide the script.
 */
function runDetached(scriptId, context) {
    var engine = (Java.type('com.atex.onecms.scripting.LifecycleScriptingEngine')).getInstance(contentManager);
    if (context) {
        engine.runDetached(scriptId, context.getBaseObject());
    } else {
        var ContextMap = Java.type('com.atex.onecms.scripting.ContextMap');
        engine.runDetached(scriptId, new ContextMap());
    }
}

/**
 * Get a scripts exports object.
 * @param path
 * @returns {*|void}
 */
function require(path) {
    var engine = (Java.type('com.atex.onecms.scripting.LifecycleScriptingEngine')).getInstance(contentManager);
    return engine.require(path);
}

/**
 * @typedef {Object} EmailAuth - Authentication information for an email provider.
 * @property {string} user - The username (email address) to connect to the host with.
 * @property {string} password - The password to use to connect to the host.
 */
/**
 * emailHostSMTP returns a function that can be used to send mail to a recipient
 * using the same credentials / smtp host server. Any content can by passed in the message body, however
 * if it's content type is anything other than text/plain a valid MIME content type should passed also.
 *
 *@example
 * // Get the sendMail function for host smtp.office365.com, username user@domain.com and password 'password'.
 * var sendMail = emailHost('smtp.office365.com', {auth: 'user@domain.com', password: 'password'});
 *
 * // Send emails to 'recipient@domain.com' from the account 'user@domain.com'.
 * sendMail('recipient@domain.com', 'Email subject', 'Email Body');
 *
 * // Send an email containing HTML content with the content type text/html.
 * var htmlContent = '&lt;h1&gt;This is an email&lt;/h1&gt;';
 * sendMail('recipient@domain.com', 'Email subject', htmlContent, 'text/html');
 *
 * @param {string} host The SMTP host server address. E.g smtp.office365.com
 * @param  {EmailAuth} auth The authentication details to send emails with.
 * @returns {function(to:{string}, subject:{string}, body:{any}, contentType:{string}): void}
 *          A function that can be used to send emails to the host SMTP server using the auth
 *          credentials already provided.
 */
function emailHostSMTP(host, auth) {
    var System = Java.type('java.lang.System');
    var Authenticator = Java.type('javax.mail.Authenticator');
    var AuthenticatorImpl = Java.extend(Authenticator);
    var PasswordAuthentication = Java.type('javax.mail.PasswordAuthentication');
    var Session = Java.type('javax.mail.Session');

    var properties = System.getProperties();
    properties.setProperty("mail.smtp.host", host);
    var session;
    if (auth) {
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.starttls.enable", "true");
        var authenticator = new AuthenticatorImpl() {
            getPasswordAuthentication: function() {
                return new PasswordAuthentication(auth.user, auth.password);
            }
        };
        session = Session.getDefaultInstance(properties, authenticator);
    } else {
        session = Session.getDefaultInstance(properties);
    }

    var send =  function (to, subject, body, contentType) {
        var MimeMessage = Java.type('javax.mail.internet.MimeMessage');
        var InternetAddress = Java.type('javax.mail.internet.InternetAddress');
        var Message = Java.type('javax.mail.Message');
        var Transport = Java.type('javax.mail.Transport');

        var message = new MimeMessage(session);
        message.setFrom(new InternetAddress(auth.user));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setContent(body, contentType ? contentType : 'text/plain');
        Transport.send(message);
    };
    return {
        send: send
    };
}

/**
 * Taken from https://github.com/atex-polopoly/onecms-lib/blob/d8520aff93a6d82055310ba96d7f503b928e9323/src/main/javascript/service-lib/core/common.js
 * and references to underscoreJS removed (to avoid extra overhead in the engine).
 *
 * Returns the data found in the given object at the given path.
 *
 * Returns undefined if the given path doesn't exist in the given object.
 *
 * Returns the input object if the path is empty.
 * Returns the input object if either the input object or the path is null or undefined.
 *
 * NOTE: addressing a path where individual path segments actually contain the '/' character
 * is only possible by using the array path syntax.
 *
 * Examples:
 *
 * - getByPath(object, 'first')
 * - getByPath(object, 'first/second')
 * - getByPath(object, ['first'])
 * - getByPath(object, ['first', 'second'])
 * - getByPath(object, ['first', 'second/third'])
 *
 * @example
 * var myObject = {
 *   'field': 'value',
 *
 *   'sub.Object': {
 *     'sub.Field': 'subValue'
 *   }
 * };
 *
 * // Accessing the values of myObject:
 * var fieldValue = getByPath(myObject, ['field']); // --> 'value'
 *
 * var subObject = getByPath(myObject, ['sub.Object']); // --> { 'sub.Field': 'subValue' }
 * var subFieldValue = getByPath(myObject, ['sub.Object', 'sub.Field']); // --> 'subValue'
 *
 * @param {Object} object The object to look into for the requested data.
 * @param {String|Array} path The path into the object at which location to find the requested data.
 * Should be either a string or an array of strings. If the path is a string, the '/' character
 * functions as a separator for path segments.
 * @return {*}
 */
function getByPath(object, path) {
    if (object == null || path == null || path.length === 0) {
        return object;
    }

    var objectPath = (typeof path === 'string') ? path.split('/') : path;

    var pathSegments = objectPath.slice(0);
    var chain = pathSegments.slice(0, -1);

    var valueObject = object;

    for (var i = 0; i < chain.length; i++) {
        var pathSegment = chain[i];
        if (!valueObject || !valueObject[pathSegment]) {
            return undefined;
        }
        valueObject = valueObject[pathSegment];
    }

    return valueObject[pathSegments.pop()];
}