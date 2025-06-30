package org.openelisglobal.login.service;

import java.sql.Date;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.login.dao.LoginUserDAO;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.security.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUserServiceImpl extends AuditableBaseObjectServiceImpl<LoginUser, Integer>
        implements LoginUserService {

    @Autowired
    protected LoginUserDAO baseObjectDAO;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Pattern BCRYPT_PATTERN = Pattern.compile("\\A\\$2[ya]?\\$\\d\\d\\$[./0-9A-Za-z]{53}"); // make sure this
    // variable is
    // current

    LoginUserServiceImpl() {
        super(LoginUser.class);
    }

    @Override
    protected LoginUserDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserAdmin(LoginUser login) throws LIMSRuntimeException {
        return login.getIsAdmin().equalsIgnoreCase(IActionConstants.YES);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginUser get(Integer id) {
        LoginUser login = super.get(id);
        inferExtraData(login);
        return login;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LoginUser> getMatch(String propertyName, Object propertyValue) {
        Optional<LoginUser> login = super.getMatch(propertyName, propertyValue);
        if (login.isPresent()) {
            inferExtraData(login.get());
        }
        return login;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LoginUser> getMatch(Map<String, Object> propertyValues) {
        Optional<LoginUser> login = super.getMatch(propertyValues);
        if (login.isPresent()) {
            inferExtraData(login.get());
        }
        return login;
    }

    private void inferExtraData(LoginUser login) {
        login.setSystemUserId(baseObjectDAO.getSystemUserId(login));
        login.setPasswordExpiredDayNo(baseObjectDAO.getPasswordExpiredDayNo(login));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LoginUser> getValidatedLogin(String loginName, String password) {
        List<LoginUser> logins = baseObjectDAO.getAllMatching("loginName", loginName);

        if (logins.size() == 1) {
            LoginUser login = logins.get(0);
            if (PasswordUtil.checkPassword(password, login.getPassword())) {
                inferExtraData(login);
                login.setSysUserId(String.valueOf(login.getSystemUserId()));
                return Optional.of(login);
            }
        }
        return Optional.empty();
    }

    @Override
    public void hashPassword(LoginUser login, String newPassword) {
        login.setPassword(PasswordUtil.hashPassword(newPassword));
        Calendar passwordExpiredDate = Calendar.getInstance();
        passwordExpiredDate.add(Calendar.MONTH,
                Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("login.user.expired.month")));
        login.setPasswordExpiredDate(new Date(passwordExpiredDate.getTimeInMillis()));
        login.setPasswordExpiredDayNo(baseObjectDAO.getPasswordExpiredDayNo(login));
    }

    @Override
    public Integer insert(LoginUser login) {
        if (getBaseObjectDAO().duplicateLoginNameExists(login)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + login.getLoginName());
        }
        if (!isHashedPassword(login.getPassword())) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "insert",
                    "The password saved does not look like a valid hashed password."
                            + " Ensure that passwords are being hashed before they are stored"
                            + " because storing encrypted or plaintext passwords is disallowed");
        }
        return super.insert(login);
    }

    @Override
    public LoginUser save(LoginUser login) {
        if (getBaseObjectDAO().duplicateLoginNameExists(login)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + login.getLoginName());
        }
        if (!isHashedPassword(login.getPassword())) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "insert",
                    "The password saved does not look like a valid hashed password."
                            + " Ensure that passwords are being hashed before they are stored"
                            + " because storing encrypted or plaintext passwords is disallowed");
        }
        return super.save(login);
    }

    @Override
    public LoginUser update(LoginUser login) {
        if (getBaseObjectDAO().duplicateLoginNameExists(login)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + login.getLoginName());
        }
        if (!isHashedPassword(login.getPassword())) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "insert",
                    "The password saved does not look like a valid hashed password."
                            + " Ensure that passwords are being hashed before they are stored"
                            + " because storing encrypted or plaintext passwords is disallowed");
        }
        return super.update(login);
    }

    @Override
    @Transactional(readOnly = true)
    public int getPasswordExpiredDayNo(LoginUser login) {
        return getBaseObjectDAO().getPasswordExpiredDayNo(login);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginUser getUserProfile(String loginName) {
        return getMatch("loginName", loginName).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public int getSystemUserId(LoginUser login) {
        return getBaseObjectDAO().getSystemUserId(login);
    }

    @Override
    public boolean isHashedPassword(String password) {
        return BCryptPasswordEncoder.class.isInstance(passwordEncoder) && BCRYPT_PATTERN.matcher(password).matches();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean defaultAdminExists() {
        return getUserProfile(DEFAULT_ADMIN_USER_NAME) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean nonDefaultAdminExists() {
        List<LoginUser> logins = getAll();
        for (LoginUser login : logins) {
            if (login.getIsAdmin().equalsIgnoreCase(IActionConstants.YES)) {
                return true;
            }
        }
        return false;
    }
}
