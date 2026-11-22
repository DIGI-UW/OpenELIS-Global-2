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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Molecular methodology detail. One-to-one with
 * {@link VectorSpecimenIdentification}; populated only when method = MOLECULAR
 * or BOTH.
 */
@Getter
@Setter
@Entity
@Table(name = "vector_molecular_record", schema = "clinlims")
@DynamicUpdate
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class VectorMolecularRecord extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vector_molecular_record_seq_gen")
    @SequenceGenerator(name = "vector_molecular_record_seq_gen", sequenceName = "vector_molecular_record_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "identification_id", nullable = false)
    private Long identificationId;

    @Column(name = "target_gene", length = 100)
    private String targetGene;

    @Column(name = "assay_name", length = 200)
    private String assayName;

    @Column(name = "genbank_accession", length = 50)
    private String genbankAccession;

    @Column(name = "linked_result_id")
    private Long linkedResultId;

    @Override
    protected String getDefaultLocalizedName() {
        return targetGene != null ? targetGene : assayName;
    }
}
