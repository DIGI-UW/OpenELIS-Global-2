package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import java.sql.Timestamp;
import java.time.Instant;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "analyzer_activation_candidate", schema = "clinlims")
@Immutable
public class AnalyzerActivationCandidate extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "analyzer_activation_candidate_seq_gen")
    @GenericGenerator(name = "analyzer_activation_candidate_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "analyzer_activation_candidate_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analyzer_id", nullable = false, updatable = false)
    private Analyzer analyzer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_binding_revision_id", nullable = false, updatable = false)
    private AnalyzerSiteBindingRevision siteBindingRevision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_confirmation_id", nullable = false, updatable = false)
    private AnalyzerSiteBindingConfirmation verificationConfirmation;

    @Column(name = "candidate_document_json", columnDefinition = "TEXT", nullable = false, updatable = false)
    private String candidateDocumentJson;

    @Column(name = "bridge_registration_json", columnDefinition = "TEXT", nullable = false, updatable = false)
    private String bridgeRegistrationJson;

    @Pattern(regexp = "^sha256:[0-9a-f]{64}$")
    @Column(name = "desired_state_fingerprint", length = 71, nullable = false, updatable = false)
    private String desiredStateFingerprint;

    @Column(name = "created_by", length = 36, nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @PrePersist
    protected void prepareForInsert() {
        if (createdAt == null) {
            createdAt = Timestamp.from(Instant.now());
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

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public AnalyzerSiteBindingRevision getSiteBindingRevision() {
        return siteBindingRevision;
    }

    public void setSiteBindingRevision(AnalyzerSiteBindingRevision siteBindingRevision) {
        this.siteBindingRevision = siteBindingRevision;
    }

    public AnalyzerSiteBindingConfirmation getVerificationConfirmation() {
        return verificationConfirmation;
    }

    public void setVerificationConfirmation(AnalyzerSiteBindingConfirmation verificationConfirmation) {
        this.verificationConfirmation = verificationConfirmation;
    }

    public String getCandidateDocumentJson() {
        return candidateDocumentJson;
    }

    public void setCandidateDocumentJson(String candidateDocumentJson) {
        this.candidateDocumentJson = candidateDocumentJson;
    }

    public String getBridgeRegistrationJson() {
        return bridgeRegistrationJson;
    }

    public void setBridgeRegistrationJson(String bridgeRegistrationJson) {
        this.bridgeRegistrationJson = bridgeRegistrationJson;
    }

    public String getDesiredStateFingerprint() {
        return desiredStateFingerprint;
    }

    public void setDesiredStateFingerprint(String desiredStateFingerprint) {
        this.desiredStateFingerprint = desiredStateFingerprint;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
