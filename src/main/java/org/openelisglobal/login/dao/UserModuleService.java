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
package org.openelisglobal.login.dao;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us)
 */
// this should not be a dao, and should instead be a service layer
public interface UserModuleService {

    public boolean isSessionExpired(HttpServletRequest request) throws LIMSRuntimeException;

    public boolean isUserModuleFound(HttpServletRequest request) throws LIMSRuntimeException;

    public boolean isVerifyUserModule(HttpServletRequest request) throws LIMSRuntimeException;

    public boolean isAccountLocked(HttpServletRequest request) throws LIMSRuntimeException;

    public boolean isAccountDisabled(HttpServletRequest request) throws LIMSRuntimeException;

    public boolean isPasswordExpired(HttpServletRequest request) throws LIMSRuntimeException;

    public boolean isUserAdmin(HttpServletRequest request) throws LIMSRuntimeException;

    public void setupUserSessionTimeOut(HttpServletRequest request) throws LIMSRuntimeException;

    public void enabledAdminButtons(HttpServletRequest request) throws LIMSRuntimeException;
}
