package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.test.beanItems.TestResultItem;

public class MicroCaseTestForm {
    public String analysisId;
    public String testId;
    public String testName;
    public String placement;
    public String cultureId;
    public String status;
    public String version;
    public String enteredBy;
    public boolean canEdit;
    public boolean canValidate;
    public boolean selfValidationBlocked;
    public boolean testedElsewhere;
    public String performingLabId;
    public String performingUserId;
    public String performedAt;
    public String performedByDisplay;
    public List<TestResultItem> components = new ArrayList<>();
}
