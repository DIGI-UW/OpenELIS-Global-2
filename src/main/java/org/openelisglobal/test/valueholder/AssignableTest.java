/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.test.valueholder;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.test.service.TestServiceImpl;

/**
 * @author benzd1 bug 2293 this object can contain tests and panels (combination
 *         of tests) it is used for Assign Test popups
 */
public class AssignableTest extends EnumValueItemImpl implements IActionConstants {

    private String id;

    private String assignableTestName;

    private String description;

    private String displayValue;

    private String tooltipText;

    private String type;

    // panels have panel item tests
    private List listOfAssignableTests;

    public AssignableTest(Test aTest) {
        this.id = aTest.getId();
        this.assignableTestName = TestServiceImpl.getUserLocalizedTestName(aTest);
        this.description = TestServiceImpl.getLocalizedTestNameWithType(aTest);
        this.displayValue = aTest.getTestDisplayValue();
        this.type = ASSIGNABLE_TEST_TYPE_TEST;
        this.tooltipText = "";
        this.listOfAssignableTests = new ArrayList();
    }

    public AssignableTest(Panel panel) {
        this.id = panel.getId();
        this.assignableTestName = panel.getLocalizedName();
        this.description = panel.getDescription();
        this.displayValue = panel.getDescription();
        this.type = ASSIGNABLE_TEST_TYPE_PANEL;
    }

    public String getAssignableTestName() {
        return assignableTestName;
    }

    public void setAssignableTestName(String assignableTestName) {
        this.assignableTestName = assignableTestName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List getListOfAssignableTests() {
        return listOfAssignableTests;
    }

    public void setListOfAssignableTests(List listOfAssignableTests) {
        this.listOfAssignableTests = listOfAssignableTests;
    }

    public String getTooltipText() {
        return tooltipText;
    }

    public void setTooltipText(String tooltipText) {
        this.tooltipText = tooltipText;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
