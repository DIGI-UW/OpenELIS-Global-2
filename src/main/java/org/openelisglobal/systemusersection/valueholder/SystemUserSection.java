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
package org.openelisglobal.systemusersection.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.valueholder.TestSection;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us) bugzilla 2203 fix to
 *         LazyInitializer error 11/07/2007
 */
public class SystemUserSection extends BaseObject<String> {

    private String id;
    private String hasView;
    private String hasAssign;
    private String hasComplete;
    private String hasRelease;
    private String hasCancel;
    private String systemUserId;
    private ValueHolderInterface systemUser;
    private String testSectionId;
    private TestSection testSection;

    public SystemUserSection() {
        super();
        this.systemUser = new ValueHolder();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setHasView(String hasView) {
        this.hasView = hasView;
    }

    public String getHasView() {
        return hasView;
    }

    public void setHasAssign(String hasAssign) {
        this.hasAssign = hasAssign;
    }

    public String getHasAssign() {
        return hasAssign;
    }

    public void setHasComplete(String hasComplete) {
        this.hasComplete = hasComplete;
    }

    public String getHasComplete() {
        return hasComplete;
    }

    public void setHasRelease(String hasRelease) {
        this.hasRelease = hasRelease;
    }

    public String getHasRelease() {
        return hasRelease;
    }

    public void setHasCancel(String hasCancel) {
        this.hasCancel = hasCancel;
    }

    public String getHasCancel() {
        return hasCancel;
    }

    protected void setSystemUserHolder(ValueHolderInterface systemUser) {
        this.systemUser = systemUser;
    }

    protected ValueHolderInterface getSystemUserHolder() {
        return this.systemUser;
    }

    public void setSystemUser(SystemUser systemUser) {
        this.systemUser.setValue(systemUser);
    }

    public SystemUser getSystemUser() {
        return (SystemUser) this.systemUser.getValue();
    }

    public void setSystemUserId(String systemUserId) {
        this.systemUserId = systemUserId;
    }

    public String getSystemUserId() {
        return systemUserId;
    }

    public TestSection getTestSection() {
        return testSection;
    }

    public void setTestSection(TestSection testSection) {
        this.testSection = testSection;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public String getTestSectionId() {
        return testSectionId;
    }
}
