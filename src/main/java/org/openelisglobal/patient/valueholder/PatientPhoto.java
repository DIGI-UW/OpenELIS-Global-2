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
@Table(name = "patient_photo")
public class PatientPhoto extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "patient_photo_generator")
    @SequenceGenerator(name = "patient_photo_generator", sequenceName = "patient_photo_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "patient_id", nullable = false, unique = true)
    private String patientId;

    @Column(name = "photo_data", columnDefinition = "LONGTEXT", nullable = false)
    private String photoData;

    @Column(name = "thumbnail_data", columnDefinition = "LONGTEXT", nullable = false)
    private String thumbnailData;

    @Column(name = "photo_type")
    private String photoType;

    public PatientPhoto() {
        super();
    }

    public PatientPhoto(String patientId) {
        this();
        this.patientId = patientId;
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

    public String getPhotoData() {
        return photoData;
    }

    public void setPhotoData(String photoData) {
        this.photoData = photoData;
    }

    public String getThumbnailData() {
        return thumbnailData;
    }

    public void setThumbnailData(String thumbnailData) {
        this.thumbnailData = thumbnailData;
    }

    public String getPhotoType() {
        return photoType;
    }

    public void setPhotoType(String photoType) {
        this.photoType = photoType;
    }
}
