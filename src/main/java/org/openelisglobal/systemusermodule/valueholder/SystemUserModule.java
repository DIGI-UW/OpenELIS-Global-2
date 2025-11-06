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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.systemusermodule.valueholder;

import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us) bugzilla 2203 fix to
 *         LazyInitializer error 11/07/2007
 */
public class SystemUserModule extends PermissionModule {

    private static final long serialVersionUID = 1L;

    private String systemUserId;
    private ValueHolderInterface systemUser;

    public SystemUserModule() {
        super();
        this.systemUser = new ValueHolder();
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

    @Override
    public String getPermissionAgentId() {
        return getSystemUserId();
    }

    @Override
    public PermissionAgent getPermissionAgent() {
        return getSystemUser();
    }

    @Override
    public void setPermissionAgent(PermissionAgent agent) {
        setSystemUser((SystemUser) agent);
    }
}
