package org.openelisglobal.testsamplehandling.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-949 M8 / OGC-938 — per-test sample storage / handling / disposal config.
 * One row per test ({@code test_id} is UNIQUE), so it is a singleton edited via
 * upsert.
 *
 * <p>
 * {@code test_id} is a numeric FK (String via LIMSStringNumberUserType). The
 * audit {@code @Version} column ({@code last_updated}) comes from
 * {@link BaseObject}; the table's separate {@code lastupdated} (DEFAULT now())
 * is filled by the DB and not mapped.
 *
 * <p>
 * The {@code version} INTEGER column is an app-level config-version counter for
 * the v2 {@code test_sample_handling_history} audit trail (inert in v1) — it is
 * NOT the JPA optimistic-lock version (that role is already held by
 * BaseObject's {@code last_updated}; JPA allows only one {@code @Version}).
 */
@Entity
@Table(name = "test_sample_handling", schema = "clinlims")
public class TestSampleHandling extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "test_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @Column(name = "storage_condition", length = 50)
    private String storageCondition;

    @Column(name = "storage_condition_custom", length = 200)
    private String storageConditionCustom;

    @Column(name = "storage_duration")
    private Integer storageDuration;

    @Column(name = "storage_duration_unit", length = 20)
    private String storageDurationUnit;

    @Column(name = "stability_notes")
    private String stabilityNotes;

    @Column(name = "protect_from_light", nullable = false)
    private boolean protectFromLight = false;

    @Column(name = "do_not_freeze", nullable = false)
    private boolean doNotFreeze = false;

    @Column(name = "do_not_refrigerate", nullable = false)
    private boolean doNotRefrigerate = false;

    @Column(name = "disposal_method", length = 100)
    private String disposalMethod;

    @Column(name = "disposal_timeframe")
    private Integer disposalTimeframe;

    @Column(name = "disposal_unit", length = 20)
    private String disposalUnit;

    @Column(name = "special_instructions")
    private String specialInstructions;

    @Column(name = "override_restricted", nullable = false)
    private boolean overrideRestricted = false;

    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @Column(name = "is_active", nullable = false, length = 2)
    private String isActive = "Y";

    public TestSampleHandling() {
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

    public String getStorageCondition() {
        return storageCondition;
    }

    public void setStorageCondition(String storageCondition) {
        this.storageCondition = storageCondition;
    }

    public String getStorageConditionCustom() {
        return storageConditionCustom;
    }

    public void setStorageConditionCustom(String storageConditionCustom) {
        this.storageConditionCustom = storageConditionCustom;
    }

    public Integer getStorageDuration() {
        return storageDuration;
    }

    public void setStorageDuration(Integer storageDuration) {
        this.storageDuration = storageDuration;
    }

    public String getStorageDurationUnit() {
        return storageDurationUnit;
    }

    public void setStorageDurationUnit(String storageDurationUnit) {
        this.storageDurationUnit = storageDurationUnit;
    }

    public String getStabilityNotes() {
        return stabilityNotes;
    }

    public void setStabilityNotes(String stabilityNotes) {
        this.stabilityNotes = stabilityNotes;
    }

    public boolean getProtectFromLight() {
        return protectFromLight;
    }

    public void setProtectFromLight(boolean protectFromLight) {
        this.protectFromLight = protectFromLight;
    }

    public boolean getDoNotFreeze() {
        return doNotFreeze;
    }

    public void setDoNotFreeze(boolean doNotFreeze) {
        this.doNotFreeze = doNotFreeze;
    }

    public boolean getDoNotRefrigerate() {
        return doNotRefrigerate;
    }

    public void setDoNotRefrigerate(boolean doNotRefrigerate) {
        this.doNotRefrigerate = doNotRefrigerate;
    }

    public String getDisposalMethod() {
        return disposalMethod;
    }

    public void setDisposalMethod(String disposalMethod) {
        this.disposalMethod = disposalMethod;
    }

    public Integer getDisposalTimeframe() {
        return disposalTimeframe;
    }

    public void setDisposalTimeframe(Integer disposalTimeframe) {
        this.disposalTimeframe = disposalTimeframe;
    }

    public String getDisposalUnit() {
        return disposalUnit;
    }

    public void setDisposalUnit(String disposalUnit) {
        this.disposalUnit = disposalUnit;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }

    public boolean getOverrideRestricted() {
        return overrideRestricted;
    }

    public void setOverrideRestricted(boolean overrideRestricted) {
        this.overrideRestricted = overrideRestricted;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}
