package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

@Embeddable
public class AnalyzerMappingResultPK implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "mapping_id")
    @Convert(converter = StringToIntegerConverter.class)
    private String mappingId;

    @Column(name = "source_row_key", length = 255)
    private String sourceRowKey;

    @Column(name = "raw_value", length = 255)
    private String rawValue;

    public AnalyzerMappingResultPK() {
    }

    public AnalyzerMappingResultPK(String mappingId, String sourceRowKey, String rawValue) {
        this.mappingId = mappingId;
        this.sourceRowKey = sourceRowKey;
        this.rawValue = rawValue;
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

    public String getRawValue() {
        return rawValue;
    }

    public void setRawValue(String rawValue) {
        this.rawValue = rawValue;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnalyzerMappingResultPK that)) {
            return false;
        }
        return Objects.equals(mappingId, that.mappingId) && Objects.equals(sourceRowKey, that.sourceRowKey)
                && Objects.equals(rawValue, that.rawValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mappingId, sourceRowKey, rawValue);
    }
}
