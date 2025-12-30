package org.openelisglobal.notebook.form;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.openelisglobal.notebook.valueholder.NoteBookFile;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.validation.annotations.SafeHtml;

public class NoteBookForm {
    private Integer id;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String title;
    private Integer type;
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String objective;
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String protocol;

    // Project metadata fields
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String principalInvestigator;
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String fundingSource;
    private BigDecimal budget;
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String projectTimeline;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String content;
    private Integer technicianId;
    private Integer systemUserId;
    private NoteBookStatus status;
    private List<Integer> sampleIds;
    private List<String> tags;
    private List<NoteBookPage> pages;
    private List<NoteBookFileForm> files;
    private List<NoteBookCommentForm> comments;
    private List<Integer> analyzerIds;
    private List<Long> inventoryInstrumentIds;
    private Integer templateId;
    private Boolean isTemplate;
    private java.util.UUID questionnaireFhirUuid;
    private Set<String> organizationIds;
    private Set<String> departmentIds;
    private Set<String> allowedRoles;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPrincipalInvestigator() {
        return principalInvestigator;
    }

    public void setPrincipalInvestigator(String principalInvestigator) {
        this.principalInvestigator = principalInvestigator;
    }

    public String getFundingSource() {
        return fundingSource;
    }

    public void setFundingSource(String fundingSource) {
        this.fundingSource = fundingSource;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public String getProjectTimeline() {
        return projectTimeline;
    }

    public void setProjectTimeline(String projectTimeline) {
        this.projectTimeline = projectTimeline;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Integer technicianId) {
        this.technicianId = technicianId;
    }

    public List<Integer> getSampleIds() {
        return sampleIds;
    }

    public void setSampleIds(List<Integer> sampleIds) {
        this.sampleIds = sampleIds;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<NoteBookFileForm> getFiles() {
        return files;
    }

    public void setFiles(List<NoteBookFileForm> files) {
        this.files = files;
    }

    public List<NoteBookPage> getPages() {
        return pages;
    }

    public void setPages(List<NoteBookPage> pages) {
        this.pages = pages;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<Integer> getAnalyzerIds() {
        return analyzerIds;
    }

    public void setAnalyzerIds(List<Integer> analyzerIds) {
        this.analyzerIds = analyzerIds;
    }

    public List<Long> getInventoryInstrumentIds() {
        return inventoryInstrumentIds;
    }

    public void setInventoryInstrumentIds(List<Long> inventoryInstrumentIds) {
        this.inventoryInstrumentIds = inventoryInstrumentIds;
    }

    public NoteBookStatus getStatus() {
        return status;
    }

    public void setStatus(NoteBookStatus status) {
        this.status = status;
    }

    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    public Boolean getIsTemplate() {
        return isTemplate;
    }

    public void setIsTemplate(Boolean isTemplate) {
        this.isTemplate = isTemplate;
    }

    public List<NoteBookCommentForm> getComments() {
        return comments;
    }

    public void setComments(List<NoteBookCommentForm> comments) {
        this.comments = comments;
    }

    public java.util.UUID getQuestionnaireFhirUuid() {
        return questionnaireFhirUuid;
    }

    public void setQuestionnaireFhirUuid(java.util.UUID questionnaireFhirUuid) {
        this.questionnaireFhirUuid = questionnaireFhirUuid;
    }

    public Set<String> getOrganizationIds() {
        return organizationIds;
    }

    public void setOrganizationIds(Set<String> organizationIds) {
        this.organizationIds = organizationIds;
    }

    public Set<String> getDepartmentIds() {
        return departmentIds;
    }

    public void setDepartmentIds(Set<String> departmentIds) {
        this.departmentIds = departmentIds;
    }

    public Set<String> getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(Set<String> allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public static class NoteBookFileForm extends NoteBookFile {

        private static final long serialVersionUID = 3142138533368581327L;

        @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
        private String base64File;

        @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
        public String getBase64File() {
            return base64File;
        }

        public void setBase64File(String base64File) {
            this.base64File = base64File;
            String[] imageInfo = base64File.split(";base64,", 2);

            setFileType(imageInfo[0]);
            setFileData(Base64.getDecoder().decode(imageInfo[1]));
        }
    }

    public static class NoteBookCommentForm {
        private Integer id;
        @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
        private String text;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

}
