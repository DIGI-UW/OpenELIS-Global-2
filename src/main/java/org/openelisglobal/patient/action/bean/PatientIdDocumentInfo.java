package org.openelisglobal.patient.action.bean;

import java.io.Serializable;

public class PatientIdDocumentInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String data;
    private String category;
    private String description;

    public PatientIdDocumentInfo() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
