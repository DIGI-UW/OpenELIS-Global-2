package org.openelisglobal.calendar.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "weekend_config", schema = "clinlims")
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class WeekendConfig extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "weekend_config_seq")
    @SequenceGenerator(name = "weekend_config_seq", sequenceName = "weekend_config_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @NotNull
    @Min(0)
    @Max(6)
    @Column(name = "day_of_week", nullable = false, unique = true)
    private Integer dayOfWeek;

    @Column(name = "is_weekend")
    private Boolean isWeekend = false;

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

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Boolean getIsWeekend() {
        return isWeekend;
    }

    public void setIsWeekend(Boolean isWeekend) {
        this.isWeekend = isWeekend;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }
}
