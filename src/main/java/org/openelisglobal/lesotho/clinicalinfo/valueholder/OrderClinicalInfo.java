package org.openelisglobal.lesotho.clinicalinfo.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * LESOTHO: one saved version of the clinical information that came with an
 * order (a lab request), from the MOH request forms. Clinical details change
 * from one request to the next, so they belong to the order, not the patient.
 * Every change is a new row; the newest row is the order's current version.
 */
@Entity
@Table(name = "lesotho_order_clinical_info")
public class OrderClinicalInfo extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lesotho_order_clinical_info_generator")
    @SequenceGenerator(name = "lesotho_order_clinical_info_generator", sequenceName = "lesotho_order_clinical_info_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    /** The order's sample (clinlims.sample.id). */
    @Column(name = "sample_id", nullable = false)
    private String sampleId;

    /** The section's values as a JSON object. */
    @Column(name = "clinical_data", columnDefinition = "TEXT", nullable = false)
    private String clinicalData;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getClinicalData() {
        return clinicalData;
    }

    public void setClinicalData(String clinicalData) {
        this.clinicalData = clinicalData;
    }
}
