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
package org.openelisglobal.panelitem.valueholder;

import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.test.valueholder.Test;

public class PanelItem extends EnumValueItemImpl {

    private static final long serialVersionUID = 78313088562219465L;

    private String id;

    private ValueHolderInterface panel;
    private ValueHolderInterface test;

    private String panelName;

    private String selectedPanelId;

    private String sortOrder;

    private String methodName;

    private String testName;

    public PanelItem() {
        super();
        this.panel = new ValueHolder();
        this.test = new ValueHolder();
    }

    @Override
    public String getId() {
        return this.id;
    }

    public Panel getPanel() {
        return (Panel) this.panel.getValue();
    }

    public void setPanel(Panel panel) {
        this.panel.setValue(panel);
    }

    protected ValueHolderInterface getPanelHolder() {
        return this.panel;
    }

    public String getPanelName() {
        return this.panelName;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    protected void setPanelHolder(ValueHolderInterface panel) {
        this.panel = panel;
    }

    public void setPanelName(String panelName) {
        this.panelName = panelName;
    }

    @Override
    public String getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getTestName() {
        return this.testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setSelectedPanelId(String selectedPanelId) {
        this.selectedPanelId = selectedPanelId;
    }

    public String getSelectedPanelId() {
        return this.selectedPanelId;
    }

    public Test getTest() {
        return (Test) test.getValue();
    }

    public void setTest(Test test) {
        this.test.setValue(test);
    }
}
