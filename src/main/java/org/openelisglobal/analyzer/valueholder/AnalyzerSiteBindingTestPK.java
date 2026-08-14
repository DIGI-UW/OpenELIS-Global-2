package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AnalyzerSiteBindingTestPK implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "site_binding_revision_id", length = 36, nullable = false)
    private String siteBindingRevisionId;

    @Column(name = "source_row_key", length = 255, nullable = false)
    private String sourceRowKey;

    public AnalyzerSiteBindingTestPK() {
    }

    public AnalyzerSiteBindingTestPK(String siteBindingRevisionId, String sourceRowKey) {
        this.siteBindingRevisionId = siteBindingRevisionId;
        this.sourceRowKey = sourceRowKey;
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

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnalyzerSiteBindingTestPK that)) {
            return false;
        }
        return Objects.equals(siteBindingRevisionId, that.siteBindingRevisionId)
                && Objects.equals(sourceRowKey, that.sourceRowKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteBindingRevisionId, sourceRowKey);
    }
}
