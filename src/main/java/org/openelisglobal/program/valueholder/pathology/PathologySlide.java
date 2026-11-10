package org.openelisglobal.program.valueholder.pathology;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
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
    @Column(name = "level", length = 16)
    private String level;

    /** Dictionary id of the stain applied to this section. */
    @Column(name = "stain_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String stainId;

    /** How far the staining of this slide has got. */
    @Column(name = "stain_status", length = 24)
    private String stainStatus;

    /** False once the row has been deactivated; rows are never deleted. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Type(type = "org.hibernate.type.BinaryType")
    private byte[] image;

    @Column(name = "file_type")
    private String fileType;

    private String location;

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

    public String getStainStatus() {
        return stainStatus;
    }

    public void setStainStatus(String stainStatus) {
        this.stainStatus = stainStatus;
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
}
