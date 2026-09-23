package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.sql.Timestamp;
import java.time.Instant;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

@Entity
@Table(name = "analyzer_site_binding_confirmation", schema = "clinlims")
@DynamicUpdate
public class AnalyzerSiteBindingConfirmation extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "analyzer_site_binding_confirmation_seq_gen")
    @GenericGenerator(name = "analyzer_site_binding_confirmation_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "analyzer_site_binding_confirmation_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_binding_revision_id", nullable = false, updatable = false)
    private AnalyzerSiteBindingRevision siteBindingRevision;

    @Column(name = "profile_id", length = 128, nullable = false, updatable = false)
    private String profileId;

    @Min(1)
    @Column(name = "profile_revision", nullable = false, updatable = false)
    private int profileRevision;

    @Pattern(regexp = "^sha256:[0-9a-f]{64}$")
    @Column(name = "profile_revision_fingerprint", length = 71, updatable = false)
    private String profileRevisionFingerprint;

    @Pattern(regexp = "^sha256:[0-9a-f]{64}$")
    @Column(name = "binding_fingerprint", length = 71, nullable = false, updatable = false)
    private String bindingFingerprint;

    @Pattern(regexp = "^sha256:[0-9a-f]{64}$")
    @Column(name = "recognition_fingerprint", length = 71, nullable = false, updatable = false)
    private String recognitionFingerprint;

    @Column(name = "confirmed_rows_json", columnDefinition = "TEXT", nullable = false, updatable = false)
    private String confirmedRowsJson;

    @Column(name = "excluded_rows_json", columnDefinition = "TEXT", nullable = false, updatable = false)
    private String excludedRowsJson;

    @Column(name = "confirmed_by", length = 36, nullable = false, updatable = false)
    private String confirmedBy;

    @Column(name = "confirmed_at", nullable = false, updatable = false)
    private Timestamp confirmedAt;

    @Column(name = "audit_event_id", precision = 10, scale = 0)
    @Convert(converter = StringToIntegerConverter.class)
    private String auditEventId;

    @PrePersist
    protected void prepareForInsert() {
        if (confirmedAt == null) {
            confirmedAt = Timestamp.from(Instant.now());
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public AnalyzerSiteBindingRevision getSiteBindingRevision() {
        return siteBindingRevision;
    }

    public void setSiteBindingRevision(AnalyzerSiteBindingRevision siteBindingRevision) {
        this.siteBindingRevision = siteBindingRevision;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public int getProfileRevision() {
        return profileRevision;
    }

    public void setProfileRevision(int profileRevision) {
        this.profileRevision = profileRevision;
    }

    public String getProfileRevisionFingerprint() {
        return profileRevisionFingerprint;
    }

    public void setProfileRevisionFingerprint(String profileRevisionFingerprint) {
        this.profileRevisionFingerprint = profileRevisionFingerprint;
    }

    public String getBindingFingerprint() {
        return bindingFingerprint;
    }

    public void setBindingFingerprint(String bindingFingerprint) {
        this.bindingFingerprint = bindingFingerprint;
    }

    public String getRecognitionFingerprint() {
        return recognitionFingerprint;
    }

    public void setRecognitionFingerprint(String recognitionFingerprint) {
        this.recognitionFingerprint = recognitionFingerprint;
    }

    public String getConfirmedRowsJson() {
        return confirmedRowsJson;
    }

    public void setConfirmedRowsJson(String confirmedRowsJson) {
        this.confirmedRowsJson = confirmedRowsJson;
    }

    public String getExcludedRowsJson() {
        return excludedRowsJson;
    }

    public void setExcludedRowsJson(String excludedRowsJson) {
        this.excludedRowsJson = excludedRowsJson;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public Timestamp getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Timestamp confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getAuditEventId() {
        return auditEventId;
    }

    public void setAuditEventId(String auditEventId) {
        this.auditEventId = auditEventId;
    }
}
