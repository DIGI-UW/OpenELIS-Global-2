package org.openelisglobal.qaevent.form;

import java.util.List;

/**
 * Form class for NCE creation responses.
 * Maps to the NCEResponse schema in the OpenAPI specification.
 */
public class NCEResponseForm {

    private Integer id;
    private String nceNumber;
    private String status;
    private String createdDate;
    private List<String> associatedResults;

    public NCEResponseForm() {
    }

    public NCEResponseForm(Integer id, String nceNumber, String status, String createdDate, List<String> associatedResults) {
        this.id = id;
        this.nceNumber = nceNumber;
        this.status = status;
        this.createdDate = createdDate;
        this.associatedResults = associatedResults;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNceNumber() {
        return nceNumber;
    }

    public void setNceNumber(String nceNumber) {
        this.nceNumber = nceNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public List<String> getAssociatedResults() {
        return associatedResults;
    }

    public void setAssociatedResults(List<String> associatedResults) {
        this.associatedResults = associatedResults;
    }

    @Override
    public String toString() {
        return "NCEResponseForm{" +
                "id=" + id +
                ", nceNumber='" + nceNumber + '\'' +
                ", status='" + status + '\'' +
                ", createdDate='" + createdDate + '\'' +
                ", associatedResults=" + associatedResults +
                '}';
    }
}