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
@Table(name = "analyzer_mapping_test", schema = "clinlims")
public class AnalyzerMappingTest extends BaseObject<AnalyzerMappingTestPK> {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private AnalyzerMappingTestPK id;

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

    @Column(name = "test_id")
    @Convert(converter = StringToIntegerConverter.class)
    private String testId;

    @Column(name = "component_id", length = 36)
    private String componentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "unresolved_reason", length = 12)
    private AnalyzerUnresolvedReason unresolvedReason;

    @Override
    public AnalyzerMappingTestPK getId() {
        return id;
    }

    @Override
    public void setId(AnalyzerMappingTestPK id) {
        this.id = id;
    }

    @Override
    public String getStringId() {
        return id == null ? null : id.getMappingId() + ":" + id.getSourceRowKey();
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

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public AnalyzerUnresolvedReason getUnresolvedReason() {
        return unresolvedReason;
    }

    public void setUnresolvedReason(AnalyzerUnresolvedReason unresolvedReason) {
        this.unresolvedReason = unresolvedReason;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }
}
