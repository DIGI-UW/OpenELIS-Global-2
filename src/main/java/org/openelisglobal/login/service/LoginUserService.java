package org.openelisglobal.login.service;

import java.util.Optional;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.login.valueholder.LoginUser;

public interface LoginUserService extends BaseObjectService<LoginUser, Integer> {

    String DEFAULT_ADMIN_USER_NAME = "admin";

    boolean isUserAdmin(LoginUser login) throws LIMSRuntimeException;

    int getPasswordExpiredDayNo(LoginUser login);

    LoginUser getUserProfile(String loginName);

    int getSystemUserId(LoginUser login);

    void hashPassword(LoginUser login, String password);

    Optional<LoginUser> getValidatedLogin(String loginName, String password);

    boolean defaultAdminExists();

    boolean nonDefaultAdminExists();

    boolean isHashedPassword(String password);
}
