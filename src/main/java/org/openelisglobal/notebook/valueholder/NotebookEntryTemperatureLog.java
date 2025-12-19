package org.openelisglobal.notebook.valueholder;

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
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * NotebookEntryTemperatureLog - Stores environmental monitoring temperature
 * readings for MNTD and other laboratory workflows.
 *
 * Temperature logs track freezer/device temperature readings for compliance
 * with laboratory storage requirements.
 */
@Entity
@Table(name = "notebook_entry_temperature_log")
public class NotebookEntryTemperatureLog extends BaseObject<Integer> {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notebook_entry_temp_log_generator")
    @SequenceGenerator(name = "notebook_entry_temp_log_generator", sequenceName = "notebook_entry_temperature_log_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_entry_id", nullable = false)
    private NotebookEntry notebookEntry;

    @Column(name = "freezer_id", nullable = false, length = 100)
    private String freezerId;

    @Column(name = "check_time", length = 10)
    private String checkTime; // AM or PM

    @Column(name = "temperature_value", nullable = false)
    private Double temperatureValue;

    @Column(name = "temperature_unit", length = 5)
    private String temperatureUnit; // C or F

    @Column(name = "checked_by", length = 255)
    private String checkedBy;

    @Column(name = "checked_date_time")
    private Timestamp checkedDateTime;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "logged_by", length = 50)
    private String loggedBy;

    @Column(name = "logged_at")
    private Timestamp loggedAt;

    public NotebookEntryTemperatureLog() {
        this.loggedAt = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public NotebookEntry getNotebookEntry() {
        return notebookEntry;
    }

    public void setNotebookEntry(NotebookEntry notebookEntry) {
        this.notebookEntry = notebookEntry;
    }

    public String getFreezerId() {
        return freezerId;
    }

    public void setFreezerId(String freezerId) {
        this.freezerId = freezerId;
    }

    public String getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(String checkTime) {
        this.checkTime = checkTime;
    }

    public Double getTemperatureValue() {
        return temperatureValue;
    }

    public void setTemperatureValue(Double temperatureValue) {
        this.temperatureValue = temperatureValue;
    }

    public String getTemperatureUnit() {
        return temperatureUnit;
    }

    public void setTemperatureUnit(String temperatureUnit) {
        this.temperatureUnit = temperatureUnit;
    }

    public String getCheckedBy() {
        return checkedBy;
    }

    public void setCheckedBy(String checkedBy) {
        this.checkedBy = checkedBy;
    }

    public Timestamp getCheckedDateTime() {
        return checkedDateTime;
    }

    public void setCheckedDateTime(Timestamp checkedDateTime) {
        this.checkedDateTime = checkedDateTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLoggedBy() {
        return loggedBy;
    }

    public void setLoggedBy(String loggedBy) {
        this.loggedBy = loggedBy;
    }

    public Timestamp getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(Timestamp loggedAt) {
        this.loggedAt = loggedAt;
    }
}
