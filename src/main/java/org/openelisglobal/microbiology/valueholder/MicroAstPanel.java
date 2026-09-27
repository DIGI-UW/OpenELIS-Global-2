package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_ast_panel", schema = "clinlims")
public class MicroAstPanel extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "workflow_type", nullable = false, length = 40)
    private String workflowType;

    @Column(name = "organism_group", length = 100)
    private String organismGroup;

    @Column(name = "specimen_type_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String specimenTypeId;

    @Column(name = "logical_key", nullable = false, length = 36)
    private String logicalKey = UUID.randomUUID().toString();

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "is_current", nullable = false, length = 2)
    private String isCurrent = "Y";

    @Column(name = "supersedes_panel_id", length = 36)
    private String supersedesPanelId;

    @Column(name = "published_at")
    private Timestamp publishedAt;

    @Column(name = "published_by", length = 20)
    private String publishedBy;

    @Column(name = "is_active", nullable = false, length = 2)
    private String isActive = "Y";

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorkflowType() {
        return workflowType;
    }

    public void setWorkflowType(String workflowType) {
        this.workflowType = workflowType;
    }

    public String getOrganismGroup() {
        return organismGroup;
    }

    public void setOrganismGroup(String organismGroup) {
        this.organismGroup = organismGroup;
    }

    public String getSpecimenTypeId() {
        return specimenTypeId;
    }

    public void setSpecimenTypeId(String specimenTypeId) {
        this.specimenTypeId = specimenTypeId;
    }

    public String getLogicalKey() {
        return logicalKey;
    }

    public void setLogicalKey(String logicalKey) {
        this.logicalKey = logicalKey;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getIsCurrent() {
        return isCurrent;
    }

    public void setIsCurrent(String isCurrent) {
        this.isCurrent = isCurrent;
    }

    public String getSupersedesPanelId() {
        return supersedesPanelId;
    }

    public void setSupersedesPanelId(String supersedesPanelId) {
        this.supersedesPanelId = supersedesPanelId;
    }

    public Timestamp getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Timestamp publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(String publishedBy) {
        this.publishedBy = publishedBy;
    }

    public String getIsActive() {
        return isActive;
    }

    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
}
