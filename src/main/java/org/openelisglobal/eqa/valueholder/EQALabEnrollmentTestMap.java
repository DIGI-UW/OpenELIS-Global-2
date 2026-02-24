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
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

@Getter
@Setter
@Entity
@Table(name = "eqa_lab_enrollment_test_map", schema = "clinlims")
public class EQALabEnrollmentTestMap extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_lab_test_map_generator")
    @SequenceGenerator(name = "eqa_lab_test_map_generator", sequenceName = "eqa_lab_test_map_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private EQALabProgramEnrollment enrollment;

    @Column(name = "test_id")
    private Long testId;

    @Column(name = "panel_id")
    private Long panelId;

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
