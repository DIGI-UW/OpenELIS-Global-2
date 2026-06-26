package org.openelisglobal.common.rest.provider.bean.homedashboard;

public class AverageTimeDisplayBean {

    Long receptionToResult = 0L;

    Long resultToValidation = 0L;

    Long receptionToValidation = 0L;

    public Long getReceptionToResult() {
        return receptionToResult;
    }

    public void setReceptionToResult(Long receptionToResult) {
        this.receptionToResult = receptionToResult;
    }

    public Long getResultToValidation() {
        return resultToValidation;
    }

    public void setResultToValidation(Long resultToValidation) {
        this.resultToValidation = resultToValidation;
    }

    public Long getReceptionToValidation() {
        return receptionToValidation;
    }

    public void setReceptionToValidation(Long receptionToValidation) {
        this.receptionToValidation = receptionToValidation;
    }
}
