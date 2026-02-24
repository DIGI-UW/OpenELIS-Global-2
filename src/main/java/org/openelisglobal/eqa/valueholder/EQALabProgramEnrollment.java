package org.openelisglobal.eqa.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

@Getter
@Setter
@Entity
@Table(name = "eqa_lab_program_enrollment", schema = "clinlims")
public class EQALabProgramEnrollment extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_lab_enroll_generator")
    @SequenceGenerator(name = "eqa_lab_enroll_generator", sequenceName = "eqa_lab_enroll_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "program_name", nullable = false, length = 255)
    private String programName;

    @Column(name = "provider", nullable = false, length = 255)
    private String provider;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "last_modified")
    private Date lastModified;

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EQALabEnrollmentLabUnit> labUnits = new ArrayList<>();

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EQALabEnrollmentTestMap> testMaps = new ArrayList<>();

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
