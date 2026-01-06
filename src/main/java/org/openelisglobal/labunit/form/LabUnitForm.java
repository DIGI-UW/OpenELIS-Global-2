package org.openelisglobal.labunit.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.validation.annotations.SafeHtml;

public class LabUnitForm extends BaseForm {

    @NotBlank(message = "labunit.name.required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Size(max = 100, message = "labunit.name.maxsize")
    private String name;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Size(max = 50, message = "labunit.code.maxsize")
    private String code;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Size(max = 500, message = "labunit.description.maxsize")
    private String description;

    private String organizationId;

    private String parentLabUnitId;

    private Integer sortOrder;

    private Integer displayOrder;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Size(max = 50, message = "labunit.externalId.maxsize")
    private String externalId;

    private Boolean active = true;

    // For bulk operations
    private String[] labUnitIds;

    public LabUnitForm() {
        setFormName("labUnitForm");
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

    public String getParentLabUnitId() {
        return parentLabUnitId;
    }

    public void setParentLabUnitId(String parentLabUnitId) {
        this.parentLabUnitId = parentLabUnitId;
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

    public String[] getLabUnitIds() {
        return labUnitIds;
    }

    public void setLabUnitIds(String[] labUnitIds) {
        this.labUnitIds = labUnitIds;
    }
}