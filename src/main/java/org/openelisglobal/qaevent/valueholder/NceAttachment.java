package org.openelisglobal.qaevent.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "nce_attachment", schema = "clinlims")
public class NceAttachment extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nce_attachment_seq_gen")
    @SequenceGenerator(name = "nce_attachment_seq_gen", sequenceName = "nce_attachment_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "nce_id", nullable = false)
    private Integer nceId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uploaded_by")
    private Integer uploadedBy;

    @Column(name = "uploaded_date", nullable = false)
    private Timestamp uploadedDate;

    public NceAttachment() {
        super();
    }

    @Override
    public String getId() {
        return id != null ? String.valueOf(id) : null;
    }

    @Override
    public void setId(String id) {
        this.id = id != null ? Integer.valueOf(id) : null;
    }

    public Integer getNceId() {
        return nceId;
    }

    public void setNceId(Integer nceId) {
        this.nceId = nceId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Integer uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Timestamp getUploadedDate() {
        return uploadedDate;
    }

    public void setUploadedDate(Timestamp uploadedDate) {
        this.uploadedDate = uploadedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NceAttachment that = (NceAttachment) o;
        return Objects.equals(id, that.id) && Objects.equals(nceId, that.nceId)
                && Objects.equals(fileName, that.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nceId, fileName);
    }

    @Override
    public String toString() {
        return "NceAttachment{" + "id='" + id + '\'' + ", nceId=" + nceId + ", fileName='" + fileName + '\''
                + ", fileType='" + fileType + '\'' + ", fileSize=" + fileSize + '}';
    }
}
