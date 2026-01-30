package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Represents the mapping configuration between an AnalyzerField and OpenELIS
 * field entries, including mapping type and activation state. Uses standard
 * BaseObject versioning and @ManyToOne relationships for analyzer and field
 * references.
 */
@Entity
@Table(name = "analyzer_field_mapping")
public class AnalyzerFieldMapping extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_field_id", nullable = false)
    private AnalyzerField analyzerField;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_id", nullable = false)
    private Analyzer analyzer;

    // OpenELIS field ID - polymorphic reference to Test, Panel, Result, etc.
    @Column(name = "openelis_field_id", length = 36, nullable = false)
    private String openelisFieldId;

    @Enumerated(EnumType.STRING)
    @Column(name = "openelis_field_type", nullable = false)
    private OpenELISFieldType openelisFieldType;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_type", nullable = false)
    private MappingType mappingType;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "specimen_type_constraint", length = 255)
    private String specimenTypeConstraint;

    @Column(name = "panel_constraint", length = 255)
    private String panelConstraint;

    // Version column exists in DB but not used for optimistic locking
    // (BaseObject's lastupdated handles versioning like all other entities)
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * Generates UUID for ID field if not set. Called automatically by
     * JPA @PrePersist lifecycle hook. Public access maintained for explicit
     * invocation if needed.
     */
    @PrePersist
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

    /**
     * Get the AnalyzerField relationship. Lazy-loaded by Hibernate.
     */
    public AnalyzerField getAnalyzerField() {
        return analyzerField;
    }

    /**
     * Set the AnalyzerField relationship.
     */
    public void setAnalyzerField(AnalyzerField analyzerField) {
        this.analyzerField = analyzerField;
    }

    /**
     * Get the Analyzer relationship. Lazy-loaded by Hibernate.
     */
    public Analyzer getAnalyzer() {
        return analyzer;
    }

    /**
     * Set the Analyzer relationship.
     */
    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Convenience getter for backward compatibility. Returns the ID from the
     * AnalyzerField relationship.
     */
    public String getAnalyzerFieldId() {
        return analyzerField != null ? analyzerField.getId() : null;
    }

    /**
     * Convenience getter for backward compatibility. Returns the ID from the
     * Analyzer relationship.
     */
    public String getAnalyzerId() {
        return analyzer != null ? analyzer.getId() : null;
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
