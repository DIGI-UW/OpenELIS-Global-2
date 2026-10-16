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
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * A cassette cut at grossing and the block it becomes at embedding, held as one
 * row that is deactivated rather than deleted.
 */
@Entity
@Table(name = "pathology_block")
public class PathologyBlock extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pathology_block_generator")
    @SequenceGenerator(name = "pathology_block_generator", sequenceName = "pathology_block_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "block_number")
    private Integer blockNumber;

    /** Identifier written on the cassette, unique within the case. */
    @Column(name = "designation", length = 32)
    private String designation;

    /** Machine-readable form of the designation, scanned to resolve this block. */
    @Column(name = "barcode", length = 64)
    private String barcode;

    /** Whether the row is still a cassette or has been embedded into a block. */
    @Enumerated(EnumType.STRING)
    @Column(name = "cassette_state", length = 16)
    private CassetteState cassetteState;

    /** Dictionary id of the tissue this cassette holds. */
    @Column(name = "tissue_type_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String tissueTypeId;

    /** Part of the specimen the tissue was taken from, such as A or B. */
    @Column(name = "part_designation", length = 16)
    private String partDesignation;

    /** False once the row has been deactivated; rows are never deleted. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    private String location;

    /**
     * The identifier written on the block, falling back to its legacy number and
     * then to its row id.
     */
    public String displayIdentifier() {
        if (StringUtils.isNotBlank(designation)) {
            return designation.trim();
        }
        if (blockNumber != null) {
            return String.valueOf(blockNumber);
        }
        return id == null ? "" : "B" + id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(Integer blockNumber) {
        this.blockNumber = blockNumber;
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

    public CassetteState getCassetteState() {
        return cassetteState;
    }

    public void setCassetteState(CassetteState cassetteState) {
        this.cassetteState = cassetteState;
    }

    public String getTissueTypeId() {
        return tissueTypeId;
    }

    public void setTissueTypeId(String tissueTypeId) {
        this.tissueTypeId = tissueTypeId;
    }

    public String getPartDesignation() {
        return partDesignation;
    }

    public void setPartDesignation(String partDesignation) {
        this.partDesignation = partDesignation;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
