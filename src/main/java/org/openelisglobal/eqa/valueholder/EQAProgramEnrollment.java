package org.openelisglobal.eqa.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

@Getter
@Setter
@Entity
@Table(name = "eqa_program_enrollment", schema = "clinlims")
public class EQAProgramEnrollment extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_enrollment_generator")
    @SequenceGenerator(name = "eqa_enrollment_generator", sequenceName = "eqa_enrollment_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eqa_program_id", nullable = false)
    private EQAProgram eqaProgram;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "enrollment_date", nullable = false)
    private Date enrollmentDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "Active";

    @Column(name = "status_changed_date")
    private Date statusChangedDate;

    @Column(name = "status_changed_by")
    private Long statusChangedBy;

    @Column(name = "withdrawal_reason", columnDefinition = "TEXT")
    private String withdrawalReason;

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
}
