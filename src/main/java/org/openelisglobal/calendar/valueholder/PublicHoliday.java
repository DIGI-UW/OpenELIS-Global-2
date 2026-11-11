package org.openelisglobal.calendar.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Date;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "public_holiday", schema = "clinlims")
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class PublicHoliday extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "public_holiday_seq")
    @SequenceGenerator(name = "public_holiday_seq", sequenceName = "public_holiday_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @NotNull
    @Column(name = "holiday_date", nullable = false)
    private Date holidayDate;

    @NotBlank
    @Size(max = 100)
    @Column(name = "holiday_name", nullable = false, length = 100)
    private String holidayName;

    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "sys_user_id", nullable = false)
    private Integer systemUserId;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Date getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(Date holidayDate) {
        this.holidayDate = holidayDate;
    }

    public String getHolidayName() {
        return holidayName;
    }

    public void setHolidayName(String holidayName) {
        this.holidayName = holidayName;
    }

    public Boolean getIsRecurring() {
        return isRecurring;
    }

    public void setIsRecurring(Boolean isRecurring) {
        this.isRecurring = isRecurring;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }
}
