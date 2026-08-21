package org.openelisglobal.vector.identification.valueholder;

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
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * One row per sample_item, UNIQUE(sample_item_id). Re-identification is an
 * UPDATE.
 *
 * @see VectorMolecularRecord one-to-one detail when method = MOLECULAR or BOTH
 */
@Getter
@Setter
@Entity
@Table(name = "vector_specimen_identification", schema = "clinlims")
@DynamicUpdate
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class VectorSpecimenIdentification extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    public static final String METHOD_MORPHOLOGICAL = "MORPHOLOGICAL";
    public static final String METHOD_MOLECULAR = "MOLECULAR";
    public static final String METHOD_BOTH = "BOTH";

    public static final String CONFIDENCE_CONFIRMED = "CONFIRMED";
    public static final String CONFIDENCE_PRESUMPTIVE = "PRESUMPTIVE";

    // Detinova age-grading (FRS v1.11).
    public static final String PHYS_UNFED = "UNFED";
    public static final String PHYS_BLOOD_FED = "BLOOD_FED";
    public static final String PHYS_HALF_GRAVID = "HALF_GRAVID";
    public static final String PHYS_GRAVID = "GRAVID";
    public static final String PHYS_UNKNOWN = "PHYS_UNKNOWN";

    public static final String LIFECYCLE_EGG = "EGG";
    public static final String LIFECYCLE_LARVA = "LARVA";
    public static final String LIFECYCLE_NYMPH = "NYMPH";
    public static final String LIFECYCLE_PUPA = "PUPA";
    public static final String LIFECYCLE_ADULT = "ADULT";
    public static final String LIFECYCLE_UNKNOWN = "UNKNOWN";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vector_specimen_identification_seq_gen")
    @SequenceGenerator(name = "vector_specimen_identification_seq_gen", sequenceName = "vector_specimen_identification_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "sample_item_id", nullable = false)
    private Long sampleItemId;

    @Column(name = "vector_species_id", nullable = false)
    private Long vectorSpeciesId;

    @Column(name = "identification_method", length = 30, nullable = false)
    private String identificationMethod;

    @Column(name = "confidence", length = 20, nullable = false)
    private String confidence;

    @Column(name = "identified_by_user_id", nullable = false)
    private Long identifiedByUserId;

    @Column(name = "identified_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Timestamp identifiedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    /** NULL when not assessed (males, non-mosquito, or lifecycleStage != ADULT). */
    @Column(name = "physiological_state", length = 20)
    private String physiologicalState;

    /** NULL inherits parent Sample.lifecycle_stage. */
    @Column(name = "lifecycle_stage", length = 20)
    private String lifecycleStage;

    @Transient
    private VectorMolecularRecord molecularRecord;

    @Override
    protected String getDefaultLocalizedName() {
        return vectorSpeciesId == null ? "" : String.valueOf(vectorSpeciesId);
    }
}
