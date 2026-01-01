package org.openelisglobal.equipmentusage.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "equipment_usage_entry")
@Access(AccessType.FIELD)
public class EquipmentUsageEntry extends BaseObject<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "equipment_usage_entry_generator")
    @SequenceGenerator(name = "equipment_usage_entry_generator", sequenceName = "equipment_usage_entry_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipment_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_usage_equipment"))
    @NotNull
    private Equipment equipment;

    @Column(name = "operator_name", nullable = false, length = 255)
    @NotNull
    private String operatorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = true, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_usage_operator"))
    private SystemUser operator;

    @Column(name = "login_time", nullable = false)
    @NotNull
    private LocalDateTime loginTime;

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @Column(name = "activities_done", columnDefinition = "LONGTEXT")
    private String activitiesDone;

    @Column(name = "equipment_status", nullable = false, length = 50)
    @NotNull
    @Enumerated(EnumType.STRING)
    private EquipmentStatus equipmentStatus;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "entry_status", nullable = false, length = 50)
    @NotNull
    @Enumerated(EnumType.STRING)
    private EntryStatus entryStatus = EntryStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", nullable = true, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_usage_approved_by"))
    private SystemUser approvedBy;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "approval_signature", length = 255)
    private String approvalSignature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_usage_created_by"))
    private SystemUser createdBy;

    @Column(name = "created_date", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by", nullable = true, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_usage_modified_by"))
    private SystemUser modifiedBy;

    @Column(name = "modified_date", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime modifiedDate;


    public enum EquipmentStatus {
        FUNCTIONAL, UNDER_MAINTENANCE, FAULTY, CALIBRATION_REQUIRED
    }

    public enum EntryStatus {
        DRAFT, SUBMITTED, APPROVED
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }
}
