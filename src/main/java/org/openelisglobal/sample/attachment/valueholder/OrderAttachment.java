package org.openelisglobal.sample.attachment.valueholder;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "order_attachment", schema = "clinlims")
public class OrderAttachment extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_attachment_seq_gen")
    @SequenceGenerator(name = "order_attachment_seq_gen", sequenceName = "order_attachment_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "sample_id", nullable = false)
    private Long sampleId;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_content", nullable = false, columnDefinition = "BYTEA")
    private byte[] fileContent;

    @Column(name = "uploaded_by")
    private Integer uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Timestamp uploadedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    public OrderAttachment() {
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

    public Long getSampleId() {
        return sampleId;
    }

    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    public Integer getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Integer uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OrderAttachment that = (OrderAttachment) o;
        return Objects.equals(id, that.id) && Objects.equals(sampleId, that.sampleId)
                && Objects.equals(originalFileName, that.originalFileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sampleId, originalFileName);
    }

    @Override
    public String toString() {
        return "OrderAttachment{" + "id=" + id + ", sampleId=" + sampleId + ", originalFileName='" + originalFileName
                + '\'' + ", fileSizeBytes=" + fileSizeBytes + ", isDeleted=" + isDeleted + '}';
    }
}
