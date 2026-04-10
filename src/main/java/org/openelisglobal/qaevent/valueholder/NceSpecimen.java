package org.openelisglobal.qaevent.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "nce_specimen", schema = "clinlims")
public class NceSpecimen extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nce_specimen_seq_gen")
    @SequenceGenerator(name = "nce_specimen_seq_gen", sequenceName = "nce_specimen_id_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "nce_id")
    private Integer nceId;

    @Column(name = "sample_item_id")
    private Integer sampleItemId;

    @Column(name = "analysis_id")
    private Integer analysisId;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNceId() {
        return nceId;
    }

    public void setNceId(Integer nceId) {
        this.nceId = nceId;
    }

    public Integer getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(Integer sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public Integer getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Integer analysisId) {
        this.analysisId = analysisId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NceSpecimen that = (NceSpecimen) o;
        return Objects.equals(id, that.id) && Objects.equals(nceId, that.nceId)
                && Objects.equals(sampleItemId, that.sampleItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nceId, sampleItemId);
    }
}
