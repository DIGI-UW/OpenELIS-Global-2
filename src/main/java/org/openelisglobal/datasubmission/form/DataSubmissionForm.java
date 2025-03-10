package org.openelisglobal.datasubmission.form;

import jakarta.validation.Valid;
import java.util.List;
import org.hibernate.validator.constraints.Range;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.datasubmission.valueholder.DataIndicator;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;

public class DataSubmissionForm extends BaseForm {

    public interface DataSubmission {
    }

    @Valid
    private SiteInformation dataSubUrl;

    @Valid
    private List<DataIndicator> indicators;

    @Range(min = 1, max = 12, groups = { DataSubmission.class })
    private int month = DateUtil.getCurrentMonth();

    // in validator
    private int year = DateUtil.getCurrentYear();

    // in validator
    private String siteId;

    public DataSubmissionForm() {
        setFormName("DataSubmissionForm");
    }

    public SiteInformation getDataSubUrl() {
        return dataSubUrl;
    }

    public void setDataSubUrl(SiteInformation dataSubUrl) {
        this.dataSubUrl = dataSubUrl;
    }

    public List<DataIndicator> getIndicators() {
        return indicators;
    }

    public void setIndicators(List<DataIndicator> indicators) {
        this.indicators = indicators;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }
}
