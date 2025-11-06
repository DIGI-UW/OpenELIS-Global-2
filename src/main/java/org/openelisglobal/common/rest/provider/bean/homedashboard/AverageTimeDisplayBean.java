package org.openelisglobal.common.rest.provider.bean.homedashboard;

public class AverageTimeDisplayBean {

    Double receptionToResult = 0.0;

    Double resultToValidation = 0.0;

    Double receptionToValidation = 0.0;

    public Double getReceptionToResult() {
        return receptionToResult;
    }

    public void setReceptionToResult(Double receptionToResult) {
        this.receptionToResult = receptionToResult;
    }

    public Double getResultToValidation() {
        return resultToValidation;
    }

    public void setResultToValidation(Double resultToValidation) {
        this.resultToValidation = resultToValidation;
    }

    public Double getReceptionToValidation() {
        return receptionToValidation;
    }

    public void setReceptionToValidation(Double receptionToValidation) {
        this.receptionToValidation = receptionToValidation;
    }
}
