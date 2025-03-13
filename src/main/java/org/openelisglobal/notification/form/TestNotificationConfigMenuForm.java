package org.openelisglobal.notification.form;

import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.notification.valueholder.TestNotificationConfig;
import org.openelisglobal.test.valueholder.Test;

public class TestNotificationConfigMenuForm extends AdminOptionMenuForm<TestNotificationConfig> {

    private static final long serialVersionUID = 5050862916821209589L;

    private List<TestNotificationConfig> testNotificationConfigs;

    private List<Test> testList;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> selectedIDs;

    @Override
    public List<TestNotificationConfig> getMenuList() {
        return testNotificationConfigs;
    }

    @Override
    public void setMenuList(List<TestNotificationConfig> menuList) {
        testNotificationConfigs = menuList;
    }

    @Override
    public List<String> getSelectedIDs() {
        return selectedIDs;
    }

    @Override
    public void setSelectedIDs(List<String> selectedIDs) {
        this.selectedIDs = selectedIDs;
    }

    public void setTestList(List<Test> testList) {
        this.testList = testList;
    }

    public List<Test> getTestList() {
        return testList;
    }
}
