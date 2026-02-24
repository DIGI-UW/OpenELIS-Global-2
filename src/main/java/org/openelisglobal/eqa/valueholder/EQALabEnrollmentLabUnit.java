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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

@Getter
@Setter
@Entity
@Table(name = "eqa_lab_enrollment_lab_unit", schema = "clinlims", uniqueConstraints = @UniqueConstraint(columnNames = {
        "enrollment_id", "test_section_id" }))
public class EQALabEnrollmentLabUnit extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_lab_unit_map_generator")
    @SequenceGenerator(name = "eqa_lab_unit_map_generator", sequenceName = "eqa_lab_unit_map_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private EQALabProgramEnrollment enrollment;

    @Column(name = "test_section_id", nullable = false)
    private Long testSectionId;

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
