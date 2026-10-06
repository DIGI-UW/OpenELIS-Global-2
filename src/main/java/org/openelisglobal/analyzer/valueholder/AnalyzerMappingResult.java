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
import org.openelisglobal.analyzer.service.AnalyzerUnresolvedReason;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

@Entity
@Table(name = "analyzer_mapping_result", schema = "clinlims")
public class AnalyzerMappingResult extends BaseObject<AnalyzerMappingResultPK> {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private AnalyzerMappingResultPK id;

    @MapsId("mappingId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mapping_id", nullable = false, updatable = false)
    private AnalyzerMapping mapping;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_state", length = 20, nullable = false, updatable = false)
    private AnalyzerMappingState mappingState;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", length = 10, nullable = false, updatable = false)
    private AnalyzerMappingOrigin origin = AnalyzerMappingOrigin.DEFAULT;

    @Column(name = "test_result_id")
    @Convert(converter = StringToIntegerConverter.class)
    private String testResultId;

    @Enumerated(EnumType.STRING)
    @Column(name = "unresolved_reason", length = 12)
    private AnalyzerUnresolvedReason unresolvedReason;

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
        return id == null ? null : id.getMappingId() + ":" + id.getSourceRowKey() + ":" + id.getRawValue();
    }

    public AnalyzerMapping getMapping() {
        return mapping;
    }

    public void setMapping(AnalyzerMapping mapping) {
        this.mapping = mapping;
    }

    public AnalyzerMappingState getMappingState() {
        return mappingState;
    }

    public void setMappingState(AnalyzerMappingState mappingState) {
        this.mappingState = mappingState;
    }

    public AnalyzerMappingOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(AnalyzerMappingOrigin origin) {
        this.origin = origin;
    }

    public AnalyzerUnresolvedReason getUnresolvedReason() {
        return unresolvedReason;
    }

    public void setUnresolvedReason(AnalyzerUnresolvedReason unresolvedReason) {
        this.unresolvedReason = unresolvedReason;
    }

    public String getTestResultId() {
        return testResultId;
    }

    public void setTestResultId(String testResultId) {
        this.testResultId = testResultId;
    }
}
