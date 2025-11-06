package org.openelisglobal.notification.service.sender;

import org.openelisglobal.common.util.ConfigurationListener;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.notification.valueholder.EmailNotification;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender implements ClientNotificationSender<EmailNotification>, ConfigurationListener {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${org.openelisglobal.mail.bcc:safemauritius@govmu.org}")
    private String bcc;

    @Value("${org.openelisglobal.mail.from:ahl-lab@safemauritius.govmu.org}")
    private String from;

    @Override
    public Class<EmailNotification> forClass() {
        return EmailNotification.class;
    }

    @Override
    public void send(EmailNotification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(notification.getRecipientEmailAddress());
        if (notification.getBccs() != null && notification.getBccs().size() > 0) {
            message.setBcc(notification.getBccs().stream().toArray(String[]::new));
        } else {
            if (!GenericValidator.isBlankOrNull(bcc)) {
                message.setBcc(bcc);
            }
        }
        if (!GenericValidator.isBlankOrNull(from)) {
            message.setFrom(from);
        }
        message.setSubject(notification.getSubject());
        message.setText(notification.getMessage());
        javaMailSender.send(message);
    }

    @Override
    public void refreshConfiguration() {
        javaMailSender = SpringContext.getBean(JavaMailSender.class);
    }
}
