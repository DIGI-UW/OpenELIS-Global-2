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
import org.openelisglobal.tb.valueholder.TbEnums.AfbResult;
import org.openelisglobal.tb.valueholder.TbEnums.SmearMethod;

/**
 * TB smear microscopy results. Records AFB (Acid-Fast Bacilli) staining results
 * for TB samples. Methods: ZN, Concentrated, Fluorescent, Other AFB Results:
 * Negative, Scanty, 1+, 2+, 3+
 */
@Entity
@Table(name = "tb_smear_result")
public class TbSmearResult extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tb_smear_result_generator")
    @SequenceGenerator(name = "tb_smear_result_generator", sequenceName = "tb_smear_result_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id", nullable = false)
    private SampleItem sampleItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 50)
    private SmearMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "afb_result", nullable = false, length = 20)
    private AfbResult afbResult;

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

    public SmearMethod getMethod() {
        return method;
    }

    public void setMethod(SmearMethod method) {
        this.method = method;
    }

    public AfbResult getAfbResult() {
        return afbResult;
    }

    public void setAfbResult(AfbResult afbResult) {
        this.afbResult = afbResult;
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
     * Returns true if the smear result is positive (any AFB detected).
     */
    public boolean isPositive() {
        return afbResult != null && afbResult != AfbResult.NEGATIVE;
    }

    /**
     * Returns true if the smear shows high AFB load (2+ or 3+).
     */
    public boolean isHighLoad() {
        return afbResult == AfbResult.PLUS2 || afbResult == AfbResult.PLUS3;
    }
}
