package org.openelisglobal.eqa.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * One distribution event within a cycle (FR-V2.1-02). Status is a plain String
 * — the FRS never enumerates round states (EQAProgramEnrollment.status
 * precedent).
 */
@Getter
@Setter
@Entity
@Table(name = "eqa_round", uniqueConstraints = @UniqueConstraint(name = "uq_eqa_round_cycle_number", columnNames = {
        "cycle_id", "round_number" }))
public class EQARound extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_round_generator")
    @SequenceGenerator(name = "eqa_round_generator", sequenceName = "eqa_round_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQACycle cycle;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "distribution_date")
    private Timestamp distributionDate;

    @Column(name = "submission_deadline")
    private Timestamp submissionDeadline;

    @Column(name = "sample_count")
    private Integer sampleCount;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "sys_user_id", nullable = false)
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
