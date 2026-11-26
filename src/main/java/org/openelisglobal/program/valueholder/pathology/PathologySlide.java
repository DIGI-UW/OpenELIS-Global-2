package org.openelisglobal.program.valueholder.pathology;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * A slide cut from a block, carrying the identity written on its label and
 * deactivated rather than deleted.
 */
@Entity
@Table(name = "pathology_slide")
public class PathologySlide extends BaseObject<Integer> {

    /** Width of the level column. */
    public static final int LEVEL_MAX_LENGTH = 16;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pathology_slide_generator")
    @SequenceGenerator(name = "pathology_slide_generator", sequenceName = "pathology_slide_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "slide_number")
    private Integer slideNumber;

    /** Block this slide was cut from; null when no parent block is recorded. */
    @Column(name = "block_id")
    private Integer blockId;

    /** Identifier written on the slide label, unique within its block. */
    @Column(name = "designation", length = 32)
    private String designation;

    /** Machine-readable form of the designation, scanned to resolve this slide. */
    @Column(name = "barcode", length = 64)
    private String barcode;

    /** Depth into the block the section was taken at, such as L1. */
    @Column(name = "level", length = LEVEL_MAX_LENGTH)
    private String level;

    /** Dictionary id of the stain applied to this section. */
    @Column(name = "stain_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String stainId;

    /** What staining found for this slide; null while it has not been stained. */
    @Enumerated(EnumType.STRING)
    @Column(name = "stain_status", length = 24)
    private StainStatus stainStatus;

    /** When the stain status was last recorded, by the server clock. */
    @Column(name = "stain_status_at")
    private Timestamp stainStatusAt;

    /**
     * The signed-in user who last recorded the stain status, as a system user id.
     */
    @Column(name = "stain_status_by", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String stainStatusBy;

    /**
     * The coverslip quality check of this slide; null while it has not been
     * checked.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "coverslip_qc", length = 8)
    private CoverslipQc coverslipQc;

    /** When the coverslip check was recorded, by the server clock. */
    @Column(name = "coverslip_qc_at")
    private Timestamp coverslipQcAt;

    /** The signed-in user who recorded the coverslip check, as a system user id. */
    @Column(name = "coverslip_qc_by", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String coverslipQcBy;

    /** False once the row has been deactivated; rows are never deleted. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Type(type = "org.hibernate.type.BinaryType")
    private byte[] image;

    @Column(name = "file_type")
    private String fileType;

    private String location;

    /**
     * When the slide's printed label was checked against its block, by the server
     * clock; null means it never was.
     */
    @Column(name = "label_verified_at")
    private Timestamp labelVerifiedAt;

    /**
     * The signed-in user who checked the label, as a system user id; null means it
     * was never checked.
     */
    @Column(name = "label_verified_by", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String labelVerifiedBy;

    /**
     * The identifier written on the slide, falling back to its legacy number and
     * then to its row id.
     */
    public String displayIdentifier() {
        if (StringUtils.isNotBlank(designation)) {
            return designation.trim();
        }
        if (slideNumber != null) {
            return String.valueOf(slideNumber);
        }
        return id == null ? "" : "S" + id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSlideNumber() {
        return slideNumber;
    }

    public void setSlideNumber(Integer slideNumber) {
        this.slideNumber = slideNumber;
    }

    public Integer getBlockId() {
        return blockId;
    }

    public void setBlockId(Integer blockId) {
        this.blockId = blockId;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getStainId() {
        return stainId;
    }

    public void setStainId(String stainId) {
        this.stainId = stainId;
    }

    public StainStatus getStainStatus() {
        return stainStatus;
    }

    public void setStainStatus(StainStatus stainStatus) {
        this.stainStatus = stainStatus;
    }

    public Timestamp getStainStatusAt() {
        return stainStatusAt;
    }

    public void setStainStatusAt(Timestamp stainStatusAt) {
        this.stainStatusAt = stainStatusAt;
    }

    public String getStainStatusBy() {
        return stainStatusBy;
    }

    public void setStainStatusBy(String stainStatusBy) {
        this.stainStatusBy = stainStatusBy;
    }

    public CoverslipQc getCoverslipQc() {
        return coverslipQc;
    }

    public void setCoverslipQc(CoverslipQc coverslipQc) {
        this.coverslipQc = coverslipQc;
    }

    public Timestamp getCoverslipQcAt() {
        return coverslipQcAt;
    }

    public void setCoverslipQcAt(Timestamp coverslipQcAt) {
        this.coverslipQcAt = coverslipQcAt;
    }

    public String getCoverslipQcBy() {
        return coverslipQcBy;
    }

    public void setCoverslipQcBy(String coverslipQcBy) {
        this.coverslipQcBy = coverslipQcBy;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Timestamp getLabelVerifiedAt() {
        return labelVerifiedAt;
    }

    public void setLabelVerifiedAt(Timestamp labelVerifiedAt) {
        this.labelVerifiedAt = labelVerifiedAt;
    }

    public String getLabelVerifiedBy() {
        return labelVerifiedBy;
    }

    public void setLabelVerifiedBy(String labelVerifiedBy) {
        this.labelVerifiedBy = labelVerifiedBy;
    }
}
