package org.openelisglobal.labunit.dto;

import java.util.List;

/**
 * Data Transfer Object for Lab Unit Import data structure. Represents the JSON
 * structure expected for lab unit imports.
 */
public class LabUnitImportData {

    private String exportVersion;
    private String exportDate;
    private List<LabUnitImportItem> labUnits;

    public LabUnitImportData() {
        this.labUnits = new java.util.ArrayList<>();
    }

    public String getExportVersion() {
        return exportVersion;
    }

    public void setExportVersion(String exportVersion) {
        this.exportVersion = exportVersion;
    }

    public String getExportDate() {
        return exportDate;
    }

    public void setExportDate(String exportDate) {
        this.exportDate = exportDate;
    }

    public List<LabUnitImportItem> getLabUnits() {
        return labUnits;
    }

    public void setLabUnits(List<LabUnitImportItem> labUnits) {
        this.labUnits = labUnits;
    }

    /**
     * Represents a single lab unit item in import data
     */
    public static class LabUnitImportItem {
        private String code;
        private String name;
        private String description;
        private Integer displayOrder;
        private Boolean isActive;
        private String externalId;
        private List<AssignmentItem> tests;
        private List<AssignmentItem> panels;
        private List<AssignmentItem> programs;
        private List<AssignmentItem> projects;
        private List<WorkflowAssignmentItem> workflows;

        public LabUnitImportItem() {
            this.tests = new java.util.ArrayList<>();
            this.panels = new java.util.ArrayList<>();
            this.programs = new java.util.ArrayList<>();
            this.projects = new java.util.ArrayList<>();
            this.workflows = new java.util.ArrayList<>();
        }

        // Getters and setters
        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }

        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }

        public String getExternalId() {
            return externalId;
        }

        public void setExternalId(String externalId) {
            this.externalId = externalId;
        }

        public List<AssignmentItem> getTests() {
            return tests;
        }

        public void setTests(List<AssignmentItem> tests) {
            this.tests = tests;
        }

        public List<AssignmentItem> getPanels() {
            return panels;
        }

        public void setPanels(List<AssignmentItem> panels) {
            this.panels = panels;
        }

        public List<AssignmentItem> getPrograms() {
            return programs;
        }

        public void setPrograms(List<AssignmentItem> programs) {
            this.programs = programs;
        }

        public List<AssignmentItem> getProjects() {
            return projects;
        }

        public void setProjects(List<AssignmentItem> projects) {
            this.projects = projects;
        }

        public List<WorkflowAssignmentItem> getWorkflows() {
            return workflows;
        }

        public void setWorkflows(List<WorkflowAssignmentItem> workflows) {
            this.workflows = workflows;
        }
    }

    /**
     * Represents a basic assignment item (test, panel, program, project)
     */
    public static class AssignmentItem {
        private String code;
        private String name;
        private String loincCode; // For tests
        private Boolean isPrimary; // For tests

        public AssignmentItem() {
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLoincCode() {
            return loincCode;
        }

        public void setLoincCode(String loincCode) {
            this.loincCode = loincCode;
        }

        public Boolean getIsPrimary() {
            return isPrimary;
        }

        public void setIsPrimary(Boolean isPrimary) {
            this.isPrimary = isPrimary;
        }
    }

    /**
     * Represents a workflow assignment with default flag
     */
    public static class WorkflowAssignmentItem {
        private String code;
        private String name;
        private Boolean isDefault;

        public WorkflowAssignmentItem() {
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Boolean getIsDefault() {
            return isDefault;
        }

        public void setIsDefault(Boolean isDefault) {
            this.isDefault = isDefault;
        }
    }
}