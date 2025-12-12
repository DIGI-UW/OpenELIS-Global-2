package org.openelisglobal.analyzer.valueholder;

import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * AnalyzerFieldMapping entity - Represents the mapping configuration between an
 * AnalyzerField and one or more OpenELIS field entries, including mapping type
 * and activation state.
 *
 * This entity is mapped via XML (AnalyzerFieldMapping.hbm.xml) to match the
 * legacy mapping approach used by other analyzer entities. The XML mapping
 * provides: - Custom version column for optimistic locking (instead of
 * BaseObject's) - LIMSStringNumberUserType for analyzerId (INTEGER in DB,
 * String in Java) - Proper column name mapping for all fields
 */
public class AnalyzerFieldMapping extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;

    private String analyzerFieldId;

    // analyzerId uses LIMSStringNumberUserType: stored as INTEGER in DB, String in
    // Java (matches legacy Analyzer entity ID type)
    private String analyzerId;

    // Transient fields for manually hydrated relationships (not persisted, set by
    // service layer)
    private transient AnalyzerField analyzerField;
    private transient Analyzer analyzer;

    private String openelisFieldId;

    private OpenELISFieldType openelisFieldType;

    private MappingType mappingType;

    private Boolean isRequired = false;

    private Boolean isActive = false;

    private String specimenTypeConstraint;

    private String panelConstraint;

    private Long version = 0L;

    /**
     * Generate UUID if not set. Called by DAO before insert since @PrePersist
     * doesn't work with XML-mapped entities.
     */
    public void generateIdIfNeeded() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        // Ensure last_updated is set before persist (required by database constraint)
        if (getLastupdated() == null) {
            setLastupdatedFields();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getAnalyzerFieldId() {
        return analyzerFieldId;
    }

    public void setAnalyzerFieldId(String analyzerFieldId) {
        this.analyzerFieldId = analyzerFieldId;
    }

    public String getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(String analyzerId) {
        this.analyzerId = analyzerId;
    }

    /**
     * Get the hydrated AnalyzerField entity (transient, set by service layer).
     * Returns null if not hydrated. Use this for read-only access after service
     * layer hydration.
     */
    public AnalyzerField getAnalyzerField() {
        return analyzerField;
    }

    /**
     * Set the hydrated AnalyzerField entity (transient, set by service layer). Also
     * updates analyzerFieldId and analyzerId to keep them in sync.
     */
    public void setAnalyzerField(AnalyzerField analyzerField) {
        this.analyzerField = analyzerField;
        if (analyzerField != null) {
            if (analyzerField.getId() != null) {
                this.analyzerFieldId = analyzerField.getId();
            }
            // Also set analyzerId if analyzer is available
            if (analyzerField.getAnalyzer() != null && analyzerField.getAnalyzer().getId() != null) {
                this.analyzerId = analyzerField.getAnalyzer().getId();
                this.analyzer = analyzerField.getAnalyzer();
            }
        }
    }

    /**
     * Get the hydrated Analyzer entity (transient, set by service layer). Returns
     * null if not hydrated. Use this for read-only access after service layer
     * hydration.
     */
    public Analyzer getAnalyzer() {
        return analyzer;
    }

    /**
     * Set the hydrated Analyzer entity (transient, set by service layer). Also
     * updates analyzerId to keep them in sync.
     */
    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
        if (analyzer != null && analyzer.getId() != null) {
            this.analyzerId = analyzer.getId();
        }
    }

    public String getOpenelisFieldId() {
        return openelisFieldId;
    }

    public void setOpenelisFieldId(String openelisFieldId) {
        this.openelisFieldId = openelisFieldId;
    }

    public OpenELISFieldType getOpenelisFieldType() {
        return openelisFieldType;
    }

    public void setOpenelisFieldType(OpenELISFieldType openelisFieldType) {
        this.openelisFieldType = openelisFieldType;
    }

    public MappingType getMappingType() {
        return mappingType;
    }

    public void setMappingType(MappingType mappingType) {
        this.mappingType = mappingType;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getSpecimenTypeConstraint() {
        return specimenTypeConstraint;
    }

    public void setSpecimenTypeConstraint(String specimenTypeConstraint) {
        this.specimenTypeConstraint = specimenTypeConstraint;
    }

    public String getPanelConstraint() {
        return panelConstraint;
    }

    public void setPanelConstraint(String panelConstraint) {
        this.panelConstraint = panelConstraint;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public enum OpenELISFieldType {
        TEST, PANEL, RESULT, ORDER, SAMPLE, QC, METADATA, UNIT
    }

    public enum MappingType {
        TEST_LEVEL, RESULT_LEVEL, METADATA
    }
}
