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
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * One laboratory on one cycle's roster (FR-V2.5-02, the wizard's participant
 * step).
 *
 * <p>
 * This is deliberately not the same thing as an {@link EQAProgramEnrollment}:
 * an enrollment says a lab takes part in a scheme, this says a lab is in *this
 * run* of it. The distinction is what the prep gate needs — a lab that enrolls
 * after the panel was aliquoted must not silently raise the number of aliquots
 * the cycle already needed.
 */
@Getter
@Setter
@Entity
@Table(name = "eqa_cycle_participant", uniqueConstraints = @UniqueConstraint(name = "uq_eqa_cycle_participant_cycle_org", columnNames = {
        "cycle_id", "organization_id" }))
public class EQACycleParticipant extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_cycle_participant_generator")
    @SequenceGenerator(name = "eqa_cycle_participant_generator", sequenceName = "eqa_cycle_participant_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private EQACycle cycle;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EQACycleParticipantStatus status = EQACycleParticipantStatus.ACTIVE;

    @Column(name = "enrolled_at", nullable = false)
    private Timestamp enrolledAt;

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
        if (enrolledAt == null) {
            enrolledAt = new Timestamp(System.currentTimeMillis());
        }
    }
}
