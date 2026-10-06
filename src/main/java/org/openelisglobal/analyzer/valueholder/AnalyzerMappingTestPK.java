package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

@Embeddable
public class AnalyzerMappingTestPK implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "mapping_id")
    @Convert(converter = StringToIntegerConverter.class)
    private String mappingId;

    @Column(name = "source_row_key", length = 255)
    private String sourceRowKey;

    public AnalyzerMappingTestPK() {
    }

    public AnalyzerMappingTestPK(String mappingId, String sourceRowKey) {
        this.mappingId = mappingId;
        this.sourceRowKey = sourceRowKey;
    }

    public String getMappingId() {
        return mappingId;
    }

    public void setMappingId(String mappingId) {
        this.mappingId = mappingId;
    }

    public String getSourceRowKey() {
        return sourceRowKey;
    }

    public void setSourceRowKey(String sourceRowKey) {
        this.sourceRowKey = sourceRowKey;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnalyzerMappingTestPK that)) {
            return false;
        }
        return Objects.equals(mappingId, that.mappingId) && Objects.equals(sourceRowKey, that.sourceRowKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mappingId, sourceRowKey);
    }
}
