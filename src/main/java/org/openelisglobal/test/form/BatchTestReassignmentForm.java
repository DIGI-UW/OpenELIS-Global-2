package org.openelisglobal.test.form;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;

public class BatchTestReassignmentForm extends BaseForm {
    // for display
    private List<IdValuePair> sampleList;

    // for display
    private String statusChangedSampleType = "";

    // for display
    private String statusChangedCurrentTest = "";

    // for display
    private String statusChangedNextTest = "";

    // for display
    private List statusChangedList;

    // additional in validator
    @NotBlank
    private String jsonWad = "";

    public BatchTestReassignmentForm() {
        setFormName("BatchTestReassignment");
    }

    public List<IdValuePair> getSampleList() {
        return sampleList;
    }

    public void setSampleList(List<IdValuePair> sampleList) {
        this.sampleList = sampleList;
    }

    public String getStatusChangedSampleType() {
        return statusChangedSampleType;
    }

    public void setStatusChangedSampleType(String statusChangedSampleType) {
        this.statusChangedSampleType = statusChangedSampleType;
    }

    public String getStatusChangedCurrentTest() {
        return statusChangedCurrentTest;
    }

    public void setStatusChangedCurrentTest(String statusChangedCurrentTest) {
        this.statusChangedCurrentTest = statusChangedCurrentTest;
    }

    public String getStatusChangedNextTest() {
        return statusChangedNextTest;
    }

    public void setStatusChangedNextTest(String statusChangedNextTest) {
        this.statusChangedNextTest = statusChangedNextTest;
    }

    public List getStatusChangedList() {
        return statusChangedList;
    }

    public void setStatusChangedList(List statusChangedList) {
        this.statusChangedList = statusChangedList;
    }

    public String getJsonWad() {
        return jsonWad;
    }

    public void setJsonWad(String jsonWad) {
        this.jsonWad = jsonWad;
    }
}
