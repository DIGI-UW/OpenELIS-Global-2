package org.openelisglobal.delivery.service.sender;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Properties;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.delivery.service.sender.DeliveryException.Code;
import org.openelisglobal.delivery.valueholder.DeliveryChannel;
import org.openelisglobal.delivery.valueholder.DeliveryChannel.Security;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * SMTP transport driven by the Email channel row: host, port, security mode,
 * credentials, sender address and name, reply-to. Errors come back in plain
 * language so the Channels page and Sent Messages can show them.
 */
@Component
public class EmailDeliverySender implements DeliverySender {

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int IO_TIMEOUT_MS = 30000;

    @Value("${org.openelisglobal.mail.from:}")
    private String fallbackFrom;

    @Value("${org.openelisglobal.mail.bcc:}")
    private String bcc;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean isSetUp(DeliveryChannel config) {
        return config != null && config.isActive() && !GenericValidator.isBlankOrNull(config.getHost())
                && config.getPort() != null && config.getPort() > 0
                && !GenericValidator.isBlankOrNull(senderAddress(config));
    }

    @Override
    public String send(DeliveryChannel config, DeliveryMessage message) {
        if (!isSetUp(config)) {
            throw new DeliveryException(Code.NOT_SET_UP, MessageUtil.getMessage("delivery.error.notSetUp",
                    MessageUtil.getMessage("delivery.channel.email")));
        }
        if (GenericValidator.isBlankOrNull(message.getAddress())) {
            throw new DeliveryException(Code.REJECTED, MessageUtil.getMessage("delivery.error.noAddress"));
        }
        JavaMailSenderImpl sender = mailSender(config);
        try {
            MimeMessage mime = sender.createMimeMessage();
            mime.setFrom(
                    GenericValidator.isBlankOrNull(config.getSenderName()) ? new InternetAddress(senderAddress(config))
                            : new InternetAddress(senderAddress(config), config.getSenderName(), "UTF-8"));
            if (!GenericValidator.isBlankOrNull(config.getReplyTo())) {
                mime.setReplyTo(InternetAddress.parse(config.getReplyTo()));
            }
            mime.setRecipients(jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(message.getAddress()));
            if (!GenericValidator.isBlankOrNull(bcc)) {
                mime.setRecipients(jakarta.mail.Message.RecipientType.BCC, InternetAddress.parse(bcc));
            }
            mime.setSubject(message.getSubject() == null ? "" : message.getSubject(), "UTF-8");
            mime.setText(message.getBody() == null ? "" : message.getBody(), "UTF-8");
            sender.send(mime);
            return null;
        } catch (MailAuthenticationException e) {
            throw new DeliveryException(Code.AUTHENTICATION, MessageUtil.getMessage("delivery.error.auth"), e);
        } catch (MailException e) {
            if (hasCause(e, ConnectException.class) || hasCause(e, UnknownHostException.class)
                    || hasCause(e, SocketTimeoutException.class)) {
                throw new DeliveryException(Code.UNREACHABLE, MessageUtil.getMessage("delivery.error.unreachable",
                        new Object[] { config.getHost(), String.valueOf(config.getPort()) }), e);
            }
            throw new DeliveryException(Code.OTHER, rootMessage(e), e);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new DeliveryException(Code.REJECTED, rootMessage(e), e);
        }
    }

    private JavaMailSenderImpl mailSender(DeliveryChannel config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.connectiontimeout", String.valueOf(CONNECT_TIMEOUT_MS));
        props.put("mail.smtp.timeout", String.valueOf(IO_TIMEOUT_MS));
        props.put("mail.smtp.writetimeout", String.valueOf(IO_TIMEOUT_MS));
        Security security = config.getSecurity() == null ? Security.STARTTLS : config.getSecurity();
        props.put("mail.smtp.starttls.enable", String.valueOf(security == Security.STARTTLS));
        props.put("mail.smtp.starttls.required", String.valueOf(security == Security.STARTTLS));
        props.put("mail.smtp.ssl.enable", String.valueOf(security == Security.SSL_TLS));
        boolean auth = !GenericValidator.isBlankOrNull(config.getUsername())
                && !GenericValidator.isBlankOrNull(config.getSecret());
        props.put("mail.smtp.auth", String.valueOf(auth));
        if (auth) {
            sender.setUsername(config.getUsername());
            sender.setPassword(config.getSecret());
        }
        return sender;
    }

    private String senderAddress(DeliveryChannel config) {
        return GenericValidator.isBlankOrNull(config.getSenderAddress()) ? fallbackFrom : config.getSenderAddress();
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        Throwable current = error;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            if (current instanceof MailException mail && !mail.getClass().equals(error.getClass())) {
                return hasCause(mail, type);
            }
            current = current.getCause();
        }
        return false;
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
