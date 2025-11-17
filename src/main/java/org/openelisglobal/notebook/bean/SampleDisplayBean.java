package org.openelisglobal.notebook.bean;

import java.util.List;

public class SampleDisplayBean {

    private Integer id;
    private String sampleType;
    private String collectionDate;
    private String externalId;
    private Boolean voided;
    private String voidReason;
    private List<ResultDisplayBean> results;

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(String collectionDate) {
        this.collectionDate = collectionDate;
    }

    public List<ResultDisplayBean> getResults() {
        return results;
    }

    public void setResults(List<ResultDisplayBean> results) {
        this.results = results;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Boolean getVoided() {
        return voided;
    }

    public void setVoided(Boolean voided) {
        this.voided = voided;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void setVoidReason(String voidReason) {
        this.voidReason = voidReason;
    }

    public static class ResultDisplayBean {
        private String test;
        private String result;
        private String dateCreated;

        public String getTest() {
            return test;
        }

        public void setTest(String test) {
            this.test = test;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getDateCreated() {
            return dateCreated;
        }

        public void setDateCreated(String dateCreated) {
            this.dateCreated = dateCreated;
        }
    }

}
