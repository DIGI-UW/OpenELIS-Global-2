package org.openelisglobal.tb.valueholder;

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
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.tb.valueholder.TbEnums.IdentificationMethod;
import org.openelisglobal.tb.valueholder.TbEnums.IdentificationResult;

/**
 * TB species identification results. Records identification of Mycobacterium
 * species from positive cultures. Results: MTB (M. tuberculosis complex), NTM
 * (Non-tuberculous Mycobacteria), Negative, Contaminated Methods: Smear
 * morphology, BHI/BA, Rapid test kit
 */
@Entity
@Table(name = "tb_identification_result")
public class TbIdentificationResult extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tb_identification_result_generator")
    @SequenceGenerator(name = "tb_identification_result_generator", sequenceName = "tb_identification_result_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id", nullable = false)
    private SampleItem sampleItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 50)
    private IdentificationResult result;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 50)
    private IdentificationMethod method;

    @Column(name = "result_date", nullable = false)
    private Timestamp resultDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tested_by")
    private SystemUser testedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private SystemUser reviewedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Getters and setters
    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
    }

    public String getSampleItemId() {
        return sampleItem != null ? sampleItem.getId() : null;
    }

    public IdentificationResult getResult() {
        return result;
    }

    public void setResult(IdentificationResult result) {
        this.result = result;
    }

    public IdentificationMethod getMethod() {
        return method;
    }

    public void setMethod(IdentificationMethod method) {
        this.method = method;
    }

    public Timestamp getResultDate() {
        return resultDate;
    }

    public void setResultDate(Timestamp resultDate) {
        this.resultDate = resultDate;
    }

    public SystemUser getTestedBy() {
        return testedBy;
    }

    public void setTestedBy(SystemUser testedBy) {
        this.testedBy = testedBy;
    }

    public SystemUser getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(SystemUser reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Returns true if the identification confirms M. tuberculosis complex.
     */
    public boolean isMtbPositive() {
        return result == IdentificationResult.MTB;
    }

    /**
     * Returns true if the identification shows non-tuberculous Mycobacteria.
     */
    public boolean isNtm() {
        return result == IdentificationResult.NTM;
    }

    /**
     * Returns true if the sample was contaminated during identification.
     */
    public boolean isContaminated() {
        return result == IdentificationResult.CONTAMINATED;
    }
}
