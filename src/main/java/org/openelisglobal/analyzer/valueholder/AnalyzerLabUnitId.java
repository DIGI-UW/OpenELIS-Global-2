package org.openelisglobal.analyzer.valueholder;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for AnalyzerLabUnit (analyzer_id, lab_unit_id).
 */
public class AnalyzerLabUnitId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer analyzerId;
    private String labUnitId;

    public AnalyzerLabUnitId() {
    }

    public AnalyzerLabUnitId(Integer analyzerId, String labUnitId) {
        this.analyzerId = analyzerId;
        this.labUnitId = labUnitId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AnalyzerLabUnitId that = (AnalyzerLabUnitId) o;
        return Objects.equals(analyzerId, that.analyzerId) && Objects.equals(labUnitId, that.labUnitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(analyzerId, labUnitId);
    }
}
