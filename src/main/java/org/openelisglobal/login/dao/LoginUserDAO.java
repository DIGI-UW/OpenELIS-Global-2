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

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.login.valueholder.LoginUser;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us)
 */
public interface LoginUserDAO extends BaseDAO<LoginUser, Integer> {

    // public Login getValidateLogin(Login login) throws LIMSRuntimeException;

    // public void updatePassword(Login login) throws LIMSRuntimeException;

    public int getPasswordExpiredDayNo(LoginUser login) throws LIMSRuntimeException;

    public int getSystemUserId(LoginUser login) throws LIMSRuntimeException;

    // public Login getUserProfile(String loginName) throws LIMSRuntimeException;

    // public boolean insertData(Login login) throws LIMSRuntimeException;

    // public void deleteData(List login) throws LIMSRuntimeException;

    // public List getAllLoginUsers() throws LIMSRuntimeException;

    // public List getPageOfLoginUsers(int startingRecNo) throws
    // LIMSRuntimeException;

    // public void getData(Login login) throws LIMSRuntimeException;

    // public void updateData(Login login, boolean passwordUpdated) throws
    // LIMSRuntimeException;

    //

    // public Integer getTotalLoginUserCount() throws LIMSRuntimeException;

    // bugzilla 2286
    // public boolean lockAccount(Login login) throws LIMSRuntimeException;

    // public boolean unlockAccount(Login login) throws LIMSRuntimeException;

    boolean duplicateLoginNameExists(LoginUser login) throws LIMSRuntimeException;
}
