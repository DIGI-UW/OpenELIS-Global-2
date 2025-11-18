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
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * AnalyzerFieldMapping entity - Represents the mapping configuration between an
 * AnalyzerField and one or more OpenELIS field entries, including mapping type and
 * activation state.
 */
@Entity
@Table(name = "analyzer_field_mapping")
public class AnalyzerFieldMapping extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_field_id", nullable = false, referencedColumnName = "id")
    private AnalyzerField analyzerField;

    @Column(name = "openelis_field_id", nullable = false, length = 36)
    @NotNull
    private String openelisFieldId;

    @Column(name = "openelis_field_type", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private OpenELISFieldType openelisFieldType;

    @Column(name = "mapping_type", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private MappingType mappingType;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "specimen_type_constraint", length = 50)
    private String specimenTypeConstraint;

    @Column(name = "panel_constraint", length = 50)
    private String panelConstraint;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
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

    public AnalyzerField getAnalyzerField() {
        return analyzerField;
    }

    public void setAnalyzerField(AnalyzerField analyzerField) {
        this.analyzerField = analyzerField;
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

    public enum OpenELISFieldType {
        TEST, PANEL, RESULT, ORDER, SAMPLE, QC, METADATA, UNIT
    }

    public enum MappingType {
        TEST_LEVEL, RESULT_LEVEL, METADATA
    }
}

