var host = emailHostSMTP('smtp.office365.com', {user: 'user@domain.com', password: 'password'});
host.send('user@domain.com', 'This is a test email', 'email body');