package org.openelisglobal.analyzerimport.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.validation.annotations.SafeHtml;

public class AnalyzerTestNameForm extends BaseForm {

    // for display
    private List<Analyzer> analyzerList;

    // for display
    private List<Test> testList;

    @NotBlank
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String analyzerId;

    @NotBlank
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String testId;

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String analyzerTestName;

    private boolean newMapping = true;

    public AnalyzerTestNameForm() {
        setFormName("analyzerTestNameForm");
    }

    public List<Analyzer> getAnalyzerList() {
        return analyzerList;
    }

    public void setAnalyzerList(List<Analyzer> analyzerList) {
        this.analyzerList = analyzerList;
    }

    public String getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(String analyzerId) {
        this.analyzerId = analyzerId;
    }

    public List<Test> getTestList() {
        return testList;
    }

    public void setTestList(List<Test> testList) {
        this.testList = testList;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getAnalyzerTestName() {
        return analyzerTestName;
    }

    public void setAnalyzerTestName(String analyzerTestName) {
        this.analyzerTestName = analyzerTestName;
    }

    public boolean isNewMapping() {
        return newMapping;
    }

    public void setNewMapping(boolean newMapping) {
        this.newMapping = newMapping;
    }
}
