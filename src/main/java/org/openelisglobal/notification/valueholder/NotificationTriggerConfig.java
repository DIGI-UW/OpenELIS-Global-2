package org.openelisglobal.notification.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * One row per notification event type (e.g. REFERRAL_OUT). The runtime fires
 * the trigger when its event happens; the row dictates whether anything is
 * sent, on which channels, and to which recipient roles. Channel/recipient
 * selections are independent multi-selects — the dispatcher takes their
 * cross-product at fire time.
 */
@Entity
@Table(name = "notification_trigger_config")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationTriggerConfig extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_trigger_config_generator")
    @SequenceGenerator(name = "notification_trigger_config_generator", sequenceName = "notification_trigger_config_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "event_code", unique = true, nullable = false, length = 64)
    private String eventCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_trigger_channel", joinColumns = @JoinColumn(name = "trigger_config_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 32, nullable = false)
    private Set<NotificationChannel> channels = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_trigger_recipient", joinColumns = @JoinColumn(name = "trigger_config_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", length = 32, nullable = false)
    private Set<NotificationRecipientType> recipientTypes = new HashSet<>();

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.DETACH,
            CascadeType.REFRESH }, fetch = FetchType.EAGER)
    @JoinColumn(name = "payload_template_id", referencedColumnName = "id")
    @JsonIgnore
    private NotificationPayloadTemplate payloadTemplate;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<NotificationChannel> getChannels() {
        if (channels == null) {
            channels = new HashSet<>();
        }
        return channels;
    }

    public void setChannels(Set<NotificationChannel> channels) {
        this.channels = channels == null ? new HashSet<>() : channels;
    }

    public Set<NotificationRecipientType> getRecipientTypes() {
        if (recipientTypes == null) {
            recipientTypes = new HashSet<>();
        }
        return recipientTypes;
    }

    public void setRecipientTypes(Set<NotificationRecipientType> recipientTypes) {
        this.recipientTypes = recipientTypes == null ? new HashSet<>() : recipientTypes;
    }

    public NotificationPayloadTemplate getPayloadTemplate() {
        return payloadTemplate;
    }

    public void setPayloadTemplate(NotificationPayloadTemplate payloadTemplate) {
        this.payloadTemplate = payloadTemplate;
    }
}
