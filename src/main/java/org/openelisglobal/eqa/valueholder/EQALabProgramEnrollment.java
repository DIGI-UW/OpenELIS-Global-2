package org.openelisglobal.eqa.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

@Getter
@Setter
@Entity
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
@Table(name = "eqa_lab_program_enrollment", schema = "clinlims")
public class EQALabProgramEnrollment extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eqa_lab_enroll_generator")
    @SequenceGenerator(name = "eqa_lab_enroll_generator", sequenceName = "eqa_lab_enroll_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "eqa_program_id")
    private EQAProgram eqaProgram;

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

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<EQALabEnrollmentLabUnit> labUnits = new HashSet<>();

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<EQALabEnrollmentTestMap> testMaps = new HashSet<>();

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
