package org.openelisglobal.tb.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.sql.Date;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.tb.valueholder.TbEnums.MediaQcStatus;
import org.openelisglobal.tb.valueholder.TbEnums.MediaType;

/**
 * Culture media preparation batch tracking for TB laboratory workflow. Tracks
 * LJ slants and MGIT tubes prepared for sample inoculation.
 */
@Entity
@Table(name = "tb_media_preparation")
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
public class TbMediaPreparation extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tb_media_preparation_generator")
    @SequenceGenerator(name = "tb_media_preparation_generator", sequenceName = "tb_media_preparation_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "batch_id", nullable = false, unique = true, length = 50)
    private String batchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    private MediaType mediaType;

    @Column(name = "preparation_date", nullable = false)
    private Date preparationDate;

    @Column(name = "expiry_date", nullable = false)
    private Date expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "qc_status", nullable = false, length = 20)
    private MediaQcStatus qcStatus = MediaQcStatus.PENDING;

    @Column(name = "qc_notes", columnDefinition = "TEXT")
    private String qcNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prepared_by")
    private SystemUser preparedBy;

    @Column(name = "notebook_entry_id")
    private Integer notebookEntryId;

    // Getters and setters
    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public Date getPreparationDate() {
        return preparationDate;
    }

    public void setPreparationDate(Date preparationDate) {
        this.preparationDate = preparationDate;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public MediaQcStatus getQcStatus() {
        return qcStatus;
    }

    public void setQcStatus(MediaQcStatus qcStatus) {
        this.qcStatus = qcStatus != null ? qcStatus : MediaQcStatus.PENDING;
    }

    public String getQcNotes() {
        return qcNotes;
    }

    public void setQcNotes(String qcNotes) {
        this.qcNotes = qcNotes;
    }

    public SystemUser getPreparedBy() {
        return preparedBy;
    }

    public void setPreparedBy(SystemUser preparedBy) {
        this.preparedBy = preparedBy;
    }

    public Integer getNotebookEntryId() {
        return notebookEntryId;
    }

    public void setNotebookEntryId(Integer notebookEntryId) {
        this.notebookEntryId = notebookEntryId;
    }

    /**
     * Returns true if this media batch has passed QC and is usable.
     */
    public boolean isUsable() {
        return qcStatus == MediaQcStatus.PASSED;
    }

    /**
     * Returns true if this media batch has expired.
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.before(new Date(System.currentTimeMillis()));
    }

    /**
     * Returns true if this media batch is available for inoculation (passed QC and
     * not expired).
     */
    public boolean isAvailableForInoculation() {
        return isUsable() && !isExpired();
    }
}
