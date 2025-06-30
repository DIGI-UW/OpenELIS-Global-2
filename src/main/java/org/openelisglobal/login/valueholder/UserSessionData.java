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
package org.openelisglobal.login.valueholder;

import java.io.Serializable;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us)
 */
public class UserSessionData implements Serializable {

    /** */
    private static final long serialVersionUID = -2401594712506375645L;

    private String elisUserName;
    private int userTimeOut;
    private int systemUserId;
    private String loginName;
    private boolean isAdmin;
    private int loginLabUnit;

    public void setElisUserName(String elisUserName) {
        this.elisUserName = elisUserName;
    }

    public String getElisUserName() {
        return elisUserName;
    }

    public void setUserTimeOut(int userTimeOut) {
        this.userTimeOut = userTimeOut;
    }

    public int getUserTimeOut() {
        return userTimeOut;
    }

    public void setSytemUserId(int systemUserId) {
        this.systemUserId = systemUserId;
    }

    public int getSystemUserId() {
        return systemUserId;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getLoginName() {
        return loginName;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public int getLoginLabUnit() {
        return loginLabUnit;
    }

    public void setLoginLabUnit(int loginLabUnit) {
        this.loginLabUnit = loginLabUnit;
    }
}
