package org.openelisglobal.labunit.dto;

import java.time.LocalDateTime;
import java.util.List;

public class LabUnitResponse {
    private String id;
    private String name;
    private String code;
    private String description;
    private String organizationId;
    private String organizationName;
    private String parentLabUnitId;
    private String parentLabUnitName;
    private Integer sortOrder;
    private Integer displayOrder;
    private String externalId;
    private Boolean active;
    private String fhirUuid;
    private LocalDateTime createdDate;
    private LocalDateTime lastUpdated;
    private Long testCount;
    private Long panelCount;
    private Long programCount;
    private Long projectCount;
    private Long workflowCount;
    private List<LabUnitAssignmentResponse> tests;
    private List<LabUnitAssignmentResponse> panels;
    private List<LabUnitAssignmentResponse> programs;
    private List<LabUnitAssignmentResponse> projects;
    private List<LabUnitAssignmentResponse> workflows;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getParentLabUnitId() {
        return parentLabUnitId;
    }

    public void setParentLabUnitId(String parentLabUnitId) {
        this.parentLabUnitId = parentLabUnitId;
    }

    public String getParentLabUnitName() {
        return parentLabUnitName;
    }

    public void setParentLabUnitName(String parentLabUnitName) {
        this.parentLabUnitName = parentLabUnitName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(String fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Long getTestCount() {
        return testCount;
    }

    public void setTestCount(Long testCount) {
        this.testCount = testCount;
    }

    public Long getPanelCount() {
        return panelCount;
    }

    public void setPanelCount(Long panelCount) {
        this.panelCount = panelCount;
    }

    public Long getProgramCount() {
        return programCount;
    }

    public void setProgramCount(Long programCount) {
        this.programCount = programCount;
    }

    public Long getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(Long projectCount) {
        this.projectCount = projectCount;
    }

    public Long getWorkflowCount() {
        return workflowCount;
    }

    public void setWorkflowCount(Long workflowCount) {
        this.workflowCount = workflowCount;
    }

    public List<LabUnitAssignmentResponse> getTests() {
        return tests;
    }

    public void setTests(List<LabUnitAssignmentResponse> tests) {
        this.tests = tests;
    }

    public List<LabUnitAssignmentResponse> getPanels() {
        return panels;
    }

    public void setPanels(List<LabUnitAssignmentResponse> panels) {
        this.panels = panels;
    }

    public List<LabUnitAssignmentResponse> getPrograms() {
        return programs;
    }

    public void setPrograms(List<LabUnitAssignmentResponse> programs) {
        this.programs = programs;
    }

    public List<LabUnitAssignmentResponse> getProjects() {
        return projects;
    }

    public void setProjects(List<LabUnitAssignmentResponse> projects) {
        this.projects = projects;
    }

    public List<LabUnitAssignmentResponse> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(List<LabUnitAssignmentResponse> workflows) {
        this.workflows = workflows;
    }
}