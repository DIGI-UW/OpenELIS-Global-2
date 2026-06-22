package org.openelisglobal.notification.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "notification_payload_template")
public class NotificationPayloadTemplate extends BaseObject<Integer> {

    private static final long serialVersionUID = 3273600381468746329L;

    public enum NotificationPayloadType {
        TEST_RESULT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_payload_template_generator")
    @SequenceGenerator(name = "notification_payload_template_generator", sequenceName = "notification_payload_template_seq", allocationSize = 1)
    private Integer id;

    @Column(unique = true)
    @Enumerated(EnumType.STRING)
    private NotificationPayloadType type;

    @Column(name = "message_template")
    private String messageTemplate;

    @Column(name = "subject_template")
    private String subjectTemplate;

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public void setSubjectTemplate(String subjectTemplate) {
        this.subjectTemplate = subjectTemplate;
    }

    public NotificationPayloadType getType() {
        return type;
    }

    public void setType(NotificationPayloadType type) {
        this.type = type;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }
}
