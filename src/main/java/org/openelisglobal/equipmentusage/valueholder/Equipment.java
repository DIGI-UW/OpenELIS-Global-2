package org.openelisglobal.equipmentusage.valueholder;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
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
@Table(name = "equipment")
@Access(AccessType.FIELD)
public class Equipment extends BaseObject<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "equipment_generator")
    @SequenceGenerator(name = "equipment_generator", sequenceName = "equipment_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    @NotNull
    @Size(min = 1, max = 255)
    private String name;

    @Column(name = "serial_number", nullable = false, unique = true, length = 100)
    @NotNull
    @Size(min = 1, max = 100)
    private String serialNumber;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    @Column(name = "model_number", length = 100)
    private String modelNumber;

    @Column(name = "purchase_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    @Column(name = "last_calibration_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate lastCalibrationDate;

    @Column(name = "next_calibration_due")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate nextCalibrationDue;

    @Column(name = "is_active", nullable = false, length = 1)
    private String isActive = "Y";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_equipment_created_by"))
    @JsonIgnore
    private SystemUser createdBy;

    @Column(name = "created_date", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by", nullable = true, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_equipment_modified_by"))
    @JsonIgnore
    private SystemUser modifiedBy;

    @Column(name = "modified_date", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime modifiedDate;

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

}
