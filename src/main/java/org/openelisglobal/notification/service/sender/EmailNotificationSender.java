package org.openelisglobal.notification.service.sender;

import java.net.URI;
import java.util.Optional;
import java.util.Properties;
import org.openelisglobal.common.util.ConfigurationListener;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.externalconnections.service.BasicAuthenticationDataService;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.ProgrammedConnection;
import org.openelisglobal.notification.valueholder.EmailNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender implements ClientNotificationSender<EmailNotification>, ConfigurationListener {

    @Value("${org.openelisglobal.mail.bcc:}")
    private String bcc;

    @Value("${org.openelisglobal.mail.from:}")
    private String from;

    @Autowired
    private ExternalConnectionService externalConnectionService;

    @Autowired
    private BasicAuthenticationDataService basicAuthenticationDataService;

    @Override
    public Class<EmailNotification> forClass() {
        return EmailNotification.class;
    }

    /**
     * Throws {@link IllegalStateException} / {@link IllegalArgumentException} on
     * every "didn't actually send" path so callers (dispatcher retry, Resend) can
     * distinguish sent-vs-skipped. A silent return would produce false SUCCESS
     * notification_log rows.
     */
    @Override
    public void send(EmailNotification notification) {
        // SMTP host + creds live on the SMTP_SERVER external_connection row,
        // mirroring the WhatsApp setup. Read live so edits in the External
        // Connections admin UI take effect without a webapp restart.
        Optional<ExternalConnection> connection = externalConnectionService.getMatch("programmedConnection",
                ProgrammedConnection.SMTP_SERVER.name());
        if (connection.isEmpty() || !Boolean.TRUE.equals(connection.get().getActive())) {
            throw new IllegalStateException("SMTP_SERVER external connection is missing or inactive");
        }
        URI uri = connection.get().getUri();
        if (uri == null || GenericValidator.isBlankOrNull(uri.getHost()) || uri.getPort() <= 0) {
            throw new IllegalStateException(
                    "SMTP_SERVER external connection URI is missing or has no host/port: " + uri);
        }

        Optional<BasicAuthenticationData> basicAuth = basicAuthenticationDataService
                .getByExternalConnection(connection.get().getId());
        String username = basicAuth.map(BasicAuthenticationData::getUsername).orElse(null);
        String password = basicAuth.map(BasicAuthenticationData::getPassword).orElse(null);
        boolean hasAuth = !GenericValidator.isBlankOrNull(username) && !GenericValidator.isBlankOrNull(password);

        if (GenericValidator.isBlankOrNull(from)) {
            throw new IllegalStateException("Email 'from' address is not configured (org.openelisglobal.mail.from)");
        }
        if (GenericValidator.isBlankOrNull(notification.getRecipientEmailAddress())) {
            throw new IllegalArgumentException("Email notification missing recipient address");
        }

        JavaMailSender mailSender = buildMailSender(uri.getHost(), uri.getPort(), username, password, hasAuth);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(notification.getRecipientEmailAddress());
        if (notification.getBccs() != null && !notification.getBccs().isEmpty()) {
            message.setBcc(notification.getBccs().toArray(new String[0]));
        } else if (!GenericValidator.isBlankOrNull(bcc)) {
            message.setBcc(bcc);
        }
        message.setFrom(from);
        message.setSubject(notification.getSubject());
        message.setText(notification.getMessage());
        mailSender.send(message);
    }

    private JavaMailSender buildMailSender(String host, int port, String username, String password, boolean hasAuth) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.starttls.enable", "true");
        if (hasAuth) {
            mailSender.setUsername(username);
            mailSender.setPassword(password);
            props.put("mail.smtp.auth", "true");
        } else {
            props.put("mail.smtp.auth", "false");
        }
        return mailSender;
    }

    @Override
    public void refreshConfiguration() {
        // No-op: send() reads the SMTP_SERVER external_connection on every call.
    }
}
