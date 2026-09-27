package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

@Embeddable
public class AnalyzerSiteBindingResultPK implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "site_binding_revision_id")
    @Convert(converter = StringToIntegerConverter.class)
    private String siteBindingRevisionId;

    @Column(name = "source_row_key", length = 255)
    private String sourceRowKey;

    @Column(name = "raw_value", length = 255)
    private String rawValue;

    public AnalyzerSiteBindingResultPK() {
    }

    public AnalyzerSiteBindingResultPK(String siteBindingRevisionId, String sourceRowKey, String rawValue) {
        this.siteBindingRevisionId = siteBindingRevisionId;
        this.sourceRowKey = sourceRowKey;
        this.rawValue = rawValue;
    }

    public String getSiteBindingRevisionId() {
        return siteBindingRevisionId;
    }

    public void setSiteBindingRevisionId(String siteBindingRevisionId) {
        this.siteBindingRevisionId = siteBindingRevisionId;
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
        if (!(other instanceof AnalyzerSiteBindingResultPK that)) {
            return false;
        }
        return Objects.equals(siteBindingRevisionId, that.siteBindingRevisionId)
                && Objects.equals(sourceRowKey, that.sourceRowKey) && Objects.equals(rawValue, that.rawValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteBindingRevisionId, sourceRowKey, rawValue);
    }
}
