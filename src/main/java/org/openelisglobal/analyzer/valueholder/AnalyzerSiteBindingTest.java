package org.openelisglobal.analyzer.valueholder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.type.JsonBinaryType;

@Entity
@Table(name = "analyzer_site_binding_test", schema = "clinlims")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class AnalyzerSiteBindingTest extends BaseObject<AnalyzerSiteBindingTestPK> {

    public enum MappingState {
        BOUND, UNRESOLVED, IGNORED
    }

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper JSON = new ObjectMapper();

    @EmbeddedId
    private AnalyzerSiteBindingTestPK id;

    @MapsId("siteBindingRevisionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_binding_revision_id", nullable = false, updatable = false)
    private AnalyzerSiteBindingRevision siteBindingRevision;

    @Column(name = "raw_analyzer_code", length = 255, nullable = false, updatable = false)
    private String rawAnalyzerCode;

    @Type(type = "jsonb")
    @Column(name = "aliases_json", columnDefinition = "jsonb", nullable = false, updatable = false)
    private String aliasesJson = "[]";

    @Column(name = "display_name", length = 255, updatable = false)
    private String displayName;

    @Column(name = "result_type", length = 30, updatable = false)
    private String resultType;

    @Column(name = "normalized_system", length = 255, updatable = false)
    private String normalizedSystem;

    @Column(name = "normalized_code", length = 255, updatable = false)
    private String normalizedCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_state", length = 20, nullable = false, updatable = false)
    private MappingState mappingState;

    @Column(name = "test_id", precision = 10, scale = 0, updatable = false)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @Column(name = "component_id", length = 36, updatable = false)
    private String componentId;

    @Override
    public AnalyzerSiteBindingTestPK getId() {
        return id;
    }

    @Override
    public void setId(AnalyzerSiteBindingTestPK id) {
        this.id = id;
    }

    @Override
    public String getStringId() {
        return id == null ? null : id.getSiteBindingRevisionId() + ":" + id.getSourceRowKey();
    }

    public AnalyzerSiteBindingRevision getSiteBindingRevision() {
        return siteBindingRevision;
    }

    public void setSiteBindingRevision(AnalyzerSiteBindingRevision siteBindingRevision) {
        this.siteBindingRevision = siteBindingRevision;
    }

    public String getRawAnalyzerCode() {
        return rawAnalyzerCode;
    }

    public void setRawAnalyzerCode(String rawAnalyzerCode) {
        this.rawAnalyzerCode = rawAnalyzerCode;
    }

    public List<String> getAliases() {
        try {
            return JSON.readValue(aliasesJson, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid analyzer site-binding aliases JSON", e);
        }
    }

    public void setAliases(List<String> aliases) {
        try {
            aliasesJson = JSON.writeValueAsString(aliases == null ? List.of() : aliases);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize analyzer site-binding aliases", e);
        }
    }

    public String getAliasesJson() {
        return aliasesJson;
    }

    public void setAliasesJson(String aliasesJson) {
        this.aliasesJson = aliasesJson;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getNormalizedSystem() {
        return normalizedSystem;
    }

    public void setNormalizedSystem(String normalizedSystem) {
        this.normalizedSystem = normalizedSystem;
    }

    public String getNormalizedCode() {
        return normalizedCode;
    }

    public void setNormalizedCode(String normalizedCode) {
        this.normalizedCode = normalizedCode;
    }

    public MappingState getMappingState() {
        return mappingState;
    }

    public void setMappingState(MappingState mappingState) {
        this.mappingState = mappingState;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }
}
