package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

@Entity
@Table(name = "analyzer_site_binding_result", schema = "clinlims")
public class AnalyzerMappingResult extends BaseObject<AnalyzerMappingResultPK> {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private AnalyzerMappingResultPK id;

    @MapsId("siteBindingRevisionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_binding_revision_id", nullable = false, updatable = false)
    private AnalyzerMapping siteBindingRevision;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_state", length = 20, nullable = false, updatable = false)
    private AnalyzerMappingState mappingState;

    @Column(name = "test_result_id")
    @Convert(converter = StringToIntegerConverter.class)
    private String testResultId;

    @Override
    public AnalyzerMappingResultPK getId() {
        return id;
    }

    @Override
    public void setId(AnalyzerMappingResultPK id) {
        this.id = id;
    }

    @Override
    public String getStringId() {
        return id == null ? null : id.getSiteBindingRevisionId() + ":" + id.getSourceRowKey() + ":" + id.getRawValue();
    }

    public AnalyzerMapping getSiteBindingRevision() {
        return siteBindingRevision;
    }

    public void setSiteBindingRevision(AnalyzerMapping siteBindingRevision) {
        this.siteBindingRevision = siteBindingRevision;
    }

    public AnalyzerMappingState getMappingState() {
        return mappingState;
    }

    public void setMappingState(AnalyzerMappingState mappingState) {
        this.mappingState = mappingState;
    }

    public String getTestResultId() {
        return testResultId;
    }

    public void setTestResultId(String testResultId) {
        this.testResultId = testResultId;
    }
}
