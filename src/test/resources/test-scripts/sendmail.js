/**
 * @typedef {Object} EmailAuth - Authentication information for an email provider.
 * @property {string} user - The username (email address) to connect to the host with.
 * @property {string} password - The password to use to connect to the host.
 */
/**
 * emailHost returns a function that can be used to send mail to a recipient
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

    return function (to, subject, body, contentType) {
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
}