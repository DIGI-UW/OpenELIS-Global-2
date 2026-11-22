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

    /**
     * The record's sub-identity under its code, in the vendor's HL7 sub-ID notation
     * (HIV-1&Ct); empty for the main result.
     */
    @Column(name = "sub_identity", length = 255, nullable = false)
    private String subIdentity = "";

    public AnalyzerMappingTestPK() {
    }

    public AnalyzerMappingTestPK(String mappingId, String sourceRowKey) {
        this(mappingId, sourceRowKey, "");
    }

    public AnalyzerMappingTestPK(String mappingId, String sourceRowKey, String subIdentity) {
        this.mappingId = mappingId;
        this.sourceRowKey = sourceRowKey;
        this.subIdentity = subIdentity == null ? "" : subIdentity;
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

    public String getSubIdentity() {
        return subIdentity;
    }

    public void setSubIdentity(String subIdentity) {
        this.subIdentity = subIdentity == null ? "" : subIdentity;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnalyzerMappingTestPK that)) {
            return false;
        }
        return Objects.equals(mappingId, that.mappingId) && Objects.equals(sourceRowKey, that.sourceRowKey)
                && Objects.equals(subIdentity, that.subIdentity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mappingId, sourceRowKey, subIdentity);
    }
}
