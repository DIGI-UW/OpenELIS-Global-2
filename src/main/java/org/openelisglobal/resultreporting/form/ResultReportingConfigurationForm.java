package org.openelisglobal.resultreporting.form;

import jakarta.validation.Valid;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.resultreporting.beans.ReportingConfiguration;

public class ResultReportingConfigurationForm extends BaseForm {

    public interface ResultReportConfig {
    }

    @Valid
    private List<ReportingConfiguration> reports;

    // for display
    private List<IdValuePair> hourList;

    // for display
    private List<IdValuePair> minList;

    public ResultReportingConfigurationForm() {
        setFormName("ResultReportingConfigurationForm");
    }

    public List<ReportingConfiguration> getReports() {
        return reports;
    }

    public void setReports(List<ReportingConfiguration> reports) {
        this.reports = reports;
    }

    public List<IdValuePair> getHourList() {
        return hourList;
    }

    public void setHourList(List<IdValuePair> hourList) {
        this.hourList = hourList;
    }

    public List<IdValuePair> getMinList() {
        return minList;
    }

    public void setMinList(List<IdValuePair> minList) {
        this.minList = minList;
    }
}
