package org.openelisglobal.notebook.form;

import java.util.Map;
import java.util.UUID;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for NotebookPageSample operations.
 */
public class NotebookPageSampleForm {

    private Integer id;
    private Integer notebookPageId;
    private Integer sampleItemId;
    private Status status;
    private Map<String, Object> data;
    private UUID questionnaireResponseUuid;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String accessionNumber;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNotebookPageId() {
        return notebookPageId;
    }

    public void setNotebookPageId(Integer notebookPageId) {
        this.notebookPageId = notebookPageId;
    }

    public Integer getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(Integer sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public UUID getQuestionnaireResponseUuid() {
        return questionnaireResponseUuid;
    }

    public void setQuestionnaireResponseUuid(UUID questionnaireResponseUuid) {
        this.questionnaireResponseUuid = questionnaireResponseUuid;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }
}
