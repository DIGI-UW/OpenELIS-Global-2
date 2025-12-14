package org.openelisglobal.notebook.form;

import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;

/**
 * Form bean for bulk apply operations on notebook page samples. Used to apply
 * common values to multiple selected samples.
 */
public class BulkApplyForm {

    private Integer notebookPageId;
    private List<Integer> sampleItemIds;
    private Map<String, Object> dataToApply;
    private Status newStatus;

    public Integer getNotebookPageId() {
        return notebookPageId;
    }

    public void setNotebookPageId(Integer notebookPageId) {
        this.notebookPageId = notebookPageId;
    }

    public List<Integer> getSampleItemIds() {
        return sampleItemIds;
    }

    public void setSampleItemIds(List<Integer> sampleItemIds) {
        this.sampleItemIds = sampleItemIds;
    }

    public Map<String, Object> getDataToApply() {
        return dataToApply;
    }

    public void setDataToApply(Map<String, Object> dataToApply) {
        this.dataToApply = dataToApply;
    }

    public Status getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(Status newStatus) {
        this.newStatus = newStatus;
    }
}
