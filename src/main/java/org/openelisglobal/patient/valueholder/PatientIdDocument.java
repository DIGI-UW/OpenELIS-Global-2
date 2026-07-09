package org.openelisglobal.patient.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "patient_id_document")
public class PatientIdDocument extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "patient_id_document_generator")
    @SequenceGenerator(name = "patient_id_document_generator", sequenceName = "patient_id_document_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "document_data", columnDefinition = "TEXT", nullable = false)
    private String documentData;

    @Column(name = "thumbnail_data", columnDefinition = "TEXT", nullable = false)
    private String thumbnailData;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "document_category", nullable = false)
    private String documentCategory;

    @Column(name = "description")
    private String description;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    public PatientIdDocument() {
        super();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getDocumentData() {
        return documentData;
    }

    public void setDocumentData(String documentData) {
        this.documentData = documentData;
    }

    public String getThumbnailData() {
        return thumbnailData;
    }

    public void setThumbnailData(String thumbnailData) {
        this.thumbnailData = thumbnailData;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentCategory() {
        return documentCategory;
    }

    public void setDocumentCategory(String documentCategory) {
        this.documentCategory = documentCategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
