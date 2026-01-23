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
 * NotebookEntryRoomEnvironmentLog - Stores room-level environmental monitoring
 * data for biorepository and other laboratory workflows.
 *
 * Room environment logs track O2 levels and humidity readings for storage rooms
 * containing cryogenic equipment or other environmentally sensitive materials.
 */
@Entity
@Table(name = "notebook_entry_room_environment_log")
public class NotebookEntryRoomEnvironmentLog extends BaseObject<Integer> {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notebook_entry_room_env_log_generator")
    @SequenceGenerator(name = "notebook_entry_room_env_log_generator", sequenceName = "notebook_entry_room_environment_log_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_entry_id", nullable = false)
    private NotebookEntry notebookEntry;

    @Column(name = "room_id", length = 100)
    private String roomId;

    @Column(name = "room_name", length = 255)
    private String roomName;

    @Column(name = "oxygen_level")
    private Double oxygenLevel; // Percentage, normal ~21%, alert if <19.5%

    @Column(name = "humidity")
    private Double humidity; // Percentage, optimal 30-60%

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

    public NotebookEntryRoomEnvironmentLog() {
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

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Double getOxygenLevel() {
        return oxygenLevel;
    }

    public void setOxygenLevel(Double oxygenLevel) {
        this.oxygenLevel = oxygenLevel;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
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
