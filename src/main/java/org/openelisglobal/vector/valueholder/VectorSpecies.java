package org.openelisglobal.vector.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

@Entity
@Table(name = "vector_species", schema = "clinlims")
@DynamicUpdate
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class VectorSpecies extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vector_species_seq_gen")
    @SequenceGenerator(name = "vector_species_seq_gen", sequenceName = "vector_species_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "genus", length = 100, nullable = false)
    private String genus;

    @Column(name = "species", length = 100)
    private String species;

    @Column(name = "subspecies", length = 100)
    private String subspecies;

    @Column(name = "sample_type_id", nullable = false)
    private Long sampleTypeId;

    @Column(name = "pathogen_category_id")
    private Long pathogenCategoryId;

    @Column(name = "lifecycle_category_id")
    private Long lifecycleCategoryId;

    @Transient
    private TypeOfSample sampleType;

    @Transient
    private DictionaryCategory pathogenCategory;

    @Transient
    private DictionaryCategory lifecycleCategory;

    @Column(name = "active")
    private Boolean active;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getGenus() {
        return genus;
    }

    public void setGenus(String genus) {
        this.genus = genus;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getSubspecies() {
        return subspecies;
    }

    public void setSubspecies(String subspecies) {
        this.subspecies = subspecies;
    }

    public Long getSampleTypeId() {
        return sampleTypeId;
    }

    public void setSampleTypeId(Long sampleTypeId) {
        this.sampleTypeId = sampleTypeId;
    }

    public Long getPathogenCategoryId() {
        return pathogenCategoryId;
    }

    public void setPathogenCategoryId(Long pathogenCategoryId) {
        this.pathogenCategoryId = pathogenCategoryId;
    }

    public Long getLifecycleCategoryId() {
        return lifecycleCategoryId;
    }

    public void setLifecycleCategoryId(Long lifecycleCategoryId) {
        this.lifecycleCategoryId = lifecycleCategoryId;
    }

    public TypeOfSample getSampleType() {
        return sampleType;
    }

    public void setSampleType(TypeOfSample sampleType) {
        this.sampleType = sampleType;
        if (sampleType != null && sampleType.getId() != null) {
            this.sampleTypeId = Long.valueOf(sampleType.getId());
        }
    }

    public DictionaryCategory getPathogenCategory() {
        return pathogenCategory;
    }

    public void setPathogenCategory(DictionaryCategory pathogenCategory) {
        this.pathogenCategory = pathogenCategory;
        if (pathogenCategory != null && pathogenCategory.getId() != null) {
            this.pathogenCategoryId = Long.valueOf(pathogenCategory.getId());
        }
    }

    public DictionaryCategory getLifecycleCategory() {
        return lifecycleCategory;
    }

    public void setLifecycleCategory(DictionaryCategory lifecycleCategory) {
        this.lifecycleCategory = lifecycleCategory;
        if (lifecycleCategory != null && lifecycleCategory.getId() != null) {
            this.lifecycleCategoryId = Long.valueOf(lifecycleCategory.getId());
        }
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    protected String getDefaultLocalizedName() {
        return genus + (species != null ? " " + species : "");
    }
}
