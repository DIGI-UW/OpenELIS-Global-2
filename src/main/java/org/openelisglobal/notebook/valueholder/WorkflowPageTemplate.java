package org.openelisglobal.notebook.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Entity representing a predefined workflow page template. Users can select
 * from these templates when adding pages to notebook templates. This is a
 * simple entity for static configuration data that doesn't require audit
 * tracking.
 */
@Entity
@Table(name = "workflow_page_template")
public class WorkflowPageTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "display_key")
    private String displayKey;

    @Column(name = "description")
    private String description;

    @Column(name = "default_instructions")
    private String defaultInstructions;

    @Column(name = "default_content")
    private String defaultContent;

    @Column(name = "page_type")
    private String pageType;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "workflow_category")
    private String workflowCategory;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayKey() {
        return displayKey;
    }

    public void setDisplayKey(String displayKey) {
        this.displayKey = displayKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultInstructions() {
        return defaultInstructions;
    }

    public void setDefaultInstructions(String defaultInstructions) {
        this.defaultInstructions = defaultInstructions;
    }

    public String getDefaultContent() {
        return defaultContent;
    }

    public void setDefaultContent(String defaultContent) {
        this.defaultContent = defaultContent;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
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

    public String getWorkflowCategory() {
        return workflowCategory;
    }

    public void setWorkflowCategory(String workflowCategory) {
        this.workflowCategory = workflowCategory;
    }
}
