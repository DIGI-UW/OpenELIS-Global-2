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
import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * One run of a scheme (FR-V2.1-01). "Scheme" is the domain word for an
 * {@link EQAProgram} row (gate G1 kept the V1 table/class). The single status
 * column serves both the participant and provider state machines — see
 * {@link EQACycleStatus}.
 */
@Getter
@Setter
@Entity
@Table(name = "eqa_cycle", uniqueConstraints = @UniqueConstraint(name = "uq_eqa_cycle_scheme_number", columnNames = {
        "scheme_id", "cycle_number" }))
public class EQACycle extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_cycle_generator")
    @SequenceGenerator(name = "eqa_cycle_generator", sequenceName = "eqa_cycle_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "scheme_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQAProgram scheme;

    @Column(name = "cycle_number", nullable = false)
    private Integer cycleNumber;

    @Column(name = "cycle_name", length = 255)
    private String cycleName;

    @Column(name = "planned_start_date")
    private Date plannedStartDate;

    @Column(name = "planned_end_date")
    private Date plannedEndDate;

    @Column(name = "actual_start_date")
    private Date actualStartDate;

    @Column(name = "actual_end_date")
    private Date actualEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EQACycleStatus status = EQACycleStatus.PLANNED;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private SystemUser createdBy;

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
        if (createdAt == null) {
            createdAt = new Timestamp(System.currentTimeMillis());
        }
    }
}
