package org.openelisglobal.biorepository.valueholder;

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
import jakarta.validation.constraints.NotNull;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

/**
 * Configuration entity for biorepository-approved sample types. Implements
 * FR-MAN-007: Validate sample types against biorepository-approved list.
 *
 * <p>
 * This table defines which sample types from the master type_of_sample table
 * are approved for use in the biorepository module. It allows AHRI to configure
 * approved types without code changes.
 */
@Entity
@Table(name = "biorepository_approved_sample_type", schema = "clinlims")
public class BiorepositoryApprovedSampleType extends BaseObject<Integer> {

    /**
     * Sample type categories per specification.
     */
    public enum SampleCategory {
        BLOOD_DERIVED, // Serum, Plasma, Whole Blood
        NUCLEIC_ACIDS, // DNA, RNA
        TISSUE, // Biopsies, FFPE blocks
        CELLULAR, // Cell lines, PBMCs
        MICROBIOLOGICAL, // Isolates, cultures
        OTHER // Extensibility for future types
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "biorepository_approved_sample_type_generator")
    @SequenceGenerator(name = "biorepository_approved_sample_type_generator", sequenceName = "biorepository_approved_sample_type_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @NotNull(message = "Type of sample is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_of_sample_id", nullable = false)
    private TypeOfSample typeOfSample;

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private SampleCategory category;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "sys_user_id", nullable = false, length = 36)
    private String sysUserId;

    // Constructors

    public BiorepositoryApprovedSampleType() {
    }

    public BiorepositoryApprovedSampleType(TypeOfSample typeOfSample, SampleCategory category) {
        this.typeOfSample = typeOfSample;
        this.category = category;
    }

    // Getters and Setters

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public TypeOfSample getTypeOfSample() {
        return typeOfSample;
    }

    public void setTypeOfSample(TypeOfSample typeOfSample) {
        this.typeOfSample = typeOfSample;
    }

    public SampleCategory getCategory() {
        return category;
    }

    public void setCategory(SampleCategory category) {
        this.category = category;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * Convenience method to get the sample type name.
     */
    public String getSampleTypeName() {
        return typeOfSample != null ? typeOfSample.getDescription() : null;
    }

    /**
     * Convenience method to get the sample type ID.
     */
    public String getSampleTypeId() {
        return typeOfSample != null ? typeOfSample.getId() : null;
    }

    @Override
    public String getSysUserId() {
        return sysUserId;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }
}
