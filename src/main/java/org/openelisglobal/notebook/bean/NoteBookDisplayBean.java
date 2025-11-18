package org.openelisglobal.notebook.bean;

import java.util.List;
import java.util.UUID;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;

public class NoteBookDisplayBean {
    private Integer id;
    private String title;
    private Integer type;
    private String dateCreated;
    private List<String> tags;
    private String typeName;
    private NoteBookStatus status;
    private Boolean isTemplate;
    private Integer entriesCount;
    private UUID questionnaireFhirUuid;

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

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public NoteBookStatus getStatus() {
        return status;
    }

    public void setStatus(NoteBookStatus status) {
        this.status = status;
    }

    public Boolean getIsTemplate() {
        return isTemplate;
    }

    public void setIsTemplate(Boolean isTemplate) {
        this.isTemplate = isTemplate;
    }

    public Integer getEntriesCount() {
        return entriesCount;
    }

    public void setEntriesCount(Integer entriesCount) {
        this.entriesCount = entriesCount;
    }

    public UUID getQuestionnaireFhirUuid() {
        return questionnaireFhirUuid;
    }

    public void setQuestionnaireFhirUuid(UUID questionnaireFhirUuid) {
        this.questionnaireFhirUuid = questionnaireFhirUuid;
    }

}
