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
 * Entity for tracking delivery of notebook results to recipients. Provides an
 * audit trail for regulatory compliance.
 */
@Entity
@Table(name = "notebook_delivery_record")
public class NotebookDeliveryRecord extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notebook_delivery_record_generator")
    @SequenceGenerator(name = "notebook_delivery_record_generator", sequenceName = "notebook_delivery_record_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id", nullable = false)
    private NoteBook notebook;

    @Column(name = "recipient_name", nullable = false, length = 255)
    private String recipientName;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "delivery_type", length = 50)
    private String deliveryType;

    @Column(name = "regulatory_body", length = 100)
    private String regulatoryBody;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "delivered_at", nullable = false)
    private Timestamp deliveredAt;

    @Column(name = "delivered_by", length = 100)
    private String deliveredBy;

    // Default constructor required by JPA
    public NotebookDeliveryRecord() {
    }

    // Constructor for convenience
    public NotebookDeliveryRecord(NoteBook notebook, String recipientName, String recipientEmail, String fileName,
            String deliveryType, String regulatoryBody, String notes, Timestamp deliveredAt, String deliveredBy) {
        this.notebook = notebook;
        this.recipientName = recipientName;
        this.recipientEmail = recipientEmail;
        this.fileName = fileName;
        this.deliveryType = deliveryType;
        this.regulatoryBody = regulatoryBody;
        this.notes = notes;
        this.deliveredAt = deliveredAt;
        this.deliveredBy = deliveredBy;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public NoteBook getNotebook() {
        return notebook;
    }

    public void setNotebook(NoteBook notebook) {
        this.notebook = notebook;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public String getRegulatoryBody() {
        return regulatoryBody;
    }

    public void setRegulatoryBody(String regulatoryBody) {
        this.regulatoryBody = regulatoryBody;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Timestamp deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getDeliveredBy() {
        return deliveredBy;
    }

    public void setDeliveredBy(String deliveredBy) {
        this.deliveredBy = deliveredBy;
    }
}
