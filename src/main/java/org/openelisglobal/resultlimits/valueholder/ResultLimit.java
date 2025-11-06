package org.openelisglobal.resultlimits.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

public class ResultLimit extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;
    private String testId;
    private String resultTypeId;
    private String gender;
    private double minAge = 0;
    private double highCritical = Double.POSITIVE_INFINITY;
    private double lowCritical = Double.POSITIVE_INFINITY;
    private double maxAge = Double.POSITIVE_INFINITY;
    private double lowNormal = Double.NEGATIVE_INFINITY;
    private double highNormal = Double.POSITIVE_INFINITY;
    private double lowValid = Double.NEGATIVE_INFINITY;
    private double highValid = Double.POSITIVE_INFINITY;
    private double lowReportingRange = Double.NEGATIVE_INFINITY;
    private double highReportingRange = Double.POSITIVE_INFINITY;
    private String dictionaryNormalId;
    private boolean alwaysValidate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getResultTypeId() {
        return resultTypeId;
    }

    public void setResultTypeId(String resultTypeId) {
        this.resultTypeId = resultTypeId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public double getMinAge() {
        return minAge;
    }

    public void setMinAge(double minAge) {
        this.minAge = minAge;
    }

    public double getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(double maxAge) {
        this.maxAge = maxAge;
    }

    public double getLowNormal() {
        return lowNormal;
    }

    public void setLowNormal(double lowNormal) {
        this.lowNormal = lowNormal;
    }

    public double getHighNormal() {
        return highNormal;
    }

    public void setLowReportingRange(double lowReportingRange) {
        this.lowReportingRange = lowReportingRange;
    }

    public double getLowReportingRange() {
        return lowReportingRange;
    }

    public void setHighReportingRange(double highReportingRange) {
        this.highReportingRange = highReportingRange;
    }

    public double getHighReportingRange() {
        return highReportingRange;
    }

    public void setHighNormal(double highNormal) {
        this.highNormal = highNormal;
    }

    public double getLowValid() {
        return lowValid;
    }

    public void setLowValid(double lowValid) {
        this.lowValid = lowValid;
    }

    public double getHighValid() {
        return highValid;
    }

    public void setHighValid(double highValid) {
        this.highValid = highValid;
    }

    public boolean ageLimitsAreDefault() {
        return minAge == 0 && maxAge == Double.POSITIVE_INFINITY;
    }

    public String getDictionaryNormalId() {
        return dictionaryNormalId;
    }

    public void setDictionaryNormalId(String dictionaryNormalId) {
        this.dictionaryNormalId = dictionaryNormalId;
    }

    public boolean isAlwaysValidate() {
        return alwaysValidate;
    }

    public void setAlwaysValidate(boolean alwaysValidate) {
        this.alwaysValidate = alwaysValidate;
    }

    public void setLowCritical(double lowCritical) {
        this.lowCritical = lowCritical;
    }

    public double getLowCritical() {
        return lowCritical;
    }

    public void setHighCritical(double highCritical) {
        this.highCritical = highCritical;
    }

    public double getHighCritical() {
        return highCritical;
    }
}
