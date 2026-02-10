package org.openelisglobal.eqa.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Getter
@Setter
@Entity
@Table(name = "eqa_result", uniqueConstraints = @UniqueConstraint(name = "uk_eqa_result_dist_org_test", columnNames = {
        "eqa_distribution_id", "participant_organization_id", "test_id" }))
public class EQAResult extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_result_generator")
    @SequenceGenerator(name = "eqa_result_generator", sequenceName = "eqa_result_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eqa_distribution_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQADistribution eqaDistribution;

    @Column(name = "participant_organization_id", nullable = false)
    private Long participantOrganizationId;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "result_value", precision = 15, scale = 5)
    private BigDecimal resultValue;

    @Column(name = "target_value", precision = 15, scale = 5)
    private BigDecimal targetValue;

    @Column(name = "z_score", precision = 10, scale = 5)
    private BigDecimal zScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_method", nullable = false, length = 20)
    private EQASubmissionMethod submissionMethod;

    @Column(name = "submission_date", nullable = false)
    private Timestamp submissionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "performance_status", length = 20)
    private EQAPerformanceStatus performanceStatus;

    @Column(name = "is_late_submission", nullable = false)
    private Boolean isLateSubmission = false;

    @Column(name = "late_submission_justification", columnDefinition = "TEXT")
    private String lateSubmissionJustification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private SystemUser approvedBy;

    // Audit trail fields for duplicate result overwrite (T116)
    @Column(name = "previous_result_value", precision = 15, scale = 5)
    private BigDecimal previousResultValue;

    @Column(name = "previous_submission_date")
    private Timestamp previousSubmissionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_submission_method", length = 20)
    private EQASubmissionMethod previousSubmissionMethod;

    @Column(name = "modified_by_user_id")
    private Long modifiedByUserId;

    @Column(name = "sys_user_id", length = 36, nullable = false)
    private String sysUserId;

    @Override
    public String getSysUserId() {
        return sysUserId;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    @PrePersist
    public void prePersist() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }
}
