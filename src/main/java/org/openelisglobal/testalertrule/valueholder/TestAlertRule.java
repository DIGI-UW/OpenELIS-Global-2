package org.openelisglobal.testalertrule.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-949 / OGC-763 — a per-test alert rule: when a result matches the trigger,
 * a notification is raised through the shipped Notification system.
 *
 * <p>
 * Per the v2.5 epic restructure this table authors the rule (trigger + channels
 * + recipients + {@code acknowledgmentRequired}) only — message templates and
 * actual delivery live in the Notification system, not here. {@code testId} and
 * {@code notifyRoleId} are {@code numeric} FKs mapped to String via
 * {@code LIMSStringNumberUserType} (the OpenELIS idiom). The audit
 * {@code @Version} column ({@code last_updated}) comes from {@link BaseObject};
 * the DB-filled {@code lastupdated} (DEFAULT now()) is not mapped here.
 */
@Entity
@Table(name = "test_alert_rule", schema = "clinlims")
public class TestAlertRule extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "test_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "trigger_type", nullable = false, length = 30)
    private String triggerType;

    @Column(name = "trigger_value", length = 100)
    private String triggerValue;

    @Column(name = "notify_sms", nullable = false)
    private Boolean notifySms = false;

    @Column(name = "notify_email", nullable = false)
    private Boolean notifyEmail = false;

    @Column(name = "notify_ordering_physician", nullable = false)
    private Boolean notifyOrderingPhysician = false;

    @Column(name = "notify_patient", nullable = false)
    private Boolean notifyPatient = false;

    @Column(name = "notify_referring_facility", nullable = false)
    private Boolean notifyReferringFacility = false;

    @Column(name = "notify_custom_phone", length = 20)
    private String notifyCustomPhone;

    @Column(name = "notify_custom_email", length = 100)
    private String notifyCustomEmail;

    @Column(name = "notify_role_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String notifyRoleId;

    @Column(name = "acknowledgment_required", nullable = false)
    private Boolean acknowledgmentRequired = false;

    public TestAlertRule() {
        super();
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerValue() {
        return triggerValue;
    }

    public void setTriggerValue(String triggerValue) {
        this.triggerValue = triggerValue;
    }

    public Boolean getNotifySms() {
        return notifySms;
    }

    public void setNotifySms(Boolean notifySms) {
        this.notifySms = notifySms;
    }

    public Boolean getNotifyEmail() {
        return notifyEmail;
    }

    public void setNotifyEmail(Boolean notifyEmail) {
        this.notifyEmail = notifyEmail;
    }

    public Boolean getNotifyOrderingPhysician() {
        return notifyOrderingPhysician;
    }

    public void setNotifyOrderingPhysician(Boolean notifyOrderingPhysician) {
        this.notifyOrderingPhysician = notifyOrderingPhysician;
    }

    public Boolean getNotifyPatient() {
        return notifyPatient;
    }

    public void setNotifyPatient(Boolean notifyPatient) {
        this.notifyPatient = notifyPatient;
    }

    public Boolean getNotifyReferringFacility() {
        return notifyReferringFacility;
    }

    public void setNotifyReferringFacility(Boolean notifyReferringFacility) {
        this.notifyReferringFacility = notifyReferringFacility;
    }

    public String getNotifyCustomPhone() {
        return notifyCustomPhone;
    }

    public void setNotifyCustomPhone(String notifyCustomPhone) {
        this.notifyCustomPhone = notifyCustomPhone;
    }

    public String getNotifyCustomEmail() {
        return notifyCustomEmail;
    }

    public void setNotifyCustomEmail(String notifyCustomEmail) {
        this.notifyCustomEmail = notifyCustomEmail;
    }

    public String getNotifyRoleId() {
        return notifyRoleId;
    }

    public void setNotifyRoleId(String notifyRoleId) {
        this.notifyRoleId = notifyRoleId;
    }

    public Boolean getAcknowledgmentRequired() {
        return acknowledgmentRequired;
    }

    public void setAcknowledgmentRequired(Boolean acknowledgmentRequired) {
        this.acknowledgmentRequired = acknowledgmentRequired;
    }
}
