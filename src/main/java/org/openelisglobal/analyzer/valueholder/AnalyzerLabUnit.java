package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Many-to-many analyzer assignment to lab units. Per data-model.md
 * AnalyzerLabUnit entity. Uses composite PK (analyzer_id, lab_unit_id).
 */
@Entity
@Table(name = "analyzer_lab_unit")
@IdClass(AnalyzerLabUnitId.class)
public class AnalyzerLabUnit extends BaseObject<AnalyzerLabUnitId> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "analyzer_id", nullable = false)
    private Integer analyzerId;

    @Id
    @Column(name = "lab_unit_id", nullable = false, length = 36)
    private String labUnitId;

    @Override
    public AnalyzerLabUnitId getId() {
        return new AnalyzerLabUnitId(analyzerId, labUnitId);
    }

    @Override
    public void setId(AnalyzerLabUnitId id) {
        if (id != null) {
            this.analyzerId = id.getAnalyzerId();
            this.labUnitId = id.getLabUnitId();
        }
    }

    public Integer getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(Integer analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getLabUnitId() {
        return labUnitId;
    }

    public void setLabUnitId(String labUnitId) {
        this.labUnitId = labUnitId;
    }
}
