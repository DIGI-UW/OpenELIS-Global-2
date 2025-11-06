package org.openelisglobal.analyzer.form;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.WellInfo;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.LabelValuePair;
import org.openelisglobal.patient.action.bean.PatientSearch;

public class AnalyzerSetupForm extends BaseForm {

    private PatientSearch patientSearch;

    private String filename;

    private Map<String, String> wellValues;

    private List<LabelValuePair> previousRuns;

    private List<LabelValuePair> analyzers;

    private Map<String, List<LabelValuePair>> analyzersTests;

    private Map<String, WellInfo> analyzersWellInfo;

    private String analyzerId;

    private Integer previousRun;

    public PatientSearch getPatientSearch() {
        return patientSearch;
    }

    public void setPatientSearch(PatientSearch patientSearch) {
        this.patientSearch = patientSearch;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Map<String, String> getWellValues() {
        return wellValues;
    }

    public void setWellValues(Map<String, String> wellValues) {
        this.wellValues = wellValues;
    }

    public List<LabelValuePair> getPreviousRuns() {
        return previousRuns;
    }

    public void setPreviousRuns(List<LabelValuePair> previousRuns) {
        this.previousRuns = previousRuns;
    }

    public Integer getPreviousRun() {
        return previousRun;
    }

    public void setPreviousRun(Integer previousRun) {
        this.previousRun = previousRun;
    }

    public List<LabelValuePair> getAnalyzers() {
        return analyzers;
    }

    public void setAnalyzers(List<LabelValuePair> analyzers) {
        this.analyzers = analyzers;
    }

    public String getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(String analyzerId) {
        this.analyzerId = analyzerId;
    }

    public Map<String, List<LabelValuePair>> getAnalyzersTests() {
        return analyzersTests;
    }

    public void setAnalyzersTests(Map<String, List<LabelValuePair>> analyzersTests) {
        this.analyzersTests = analyzersTests;
    }

    public Map<String, WellInfo> getAnalyzersWellInfo() {
        return analyzersWellInfo;
    }

    public void setAnalyzersWellInfo(Map<String, WellInfo> analyzersWellInfo) {
        this.analyzersWellInfo = analyzersWellInfo;
    }
}
