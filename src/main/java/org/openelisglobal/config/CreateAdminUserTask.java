package org.openelisglobal.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.Calendar;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.PasswordValidationFactory;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateAdminUserTask {

    private static String PASSWORD_FILEPATH = "adminPassword.txt";
    private static String PASSWORD_MARKER = LoginUserService.DEFAULT_ADMIN_USER_NAME + ":";

    @Autowired
    private LoginUserService loginService;

    @PostConstruct
    private void ensureAdminUserIsCreated() {
        if (!loginService.defaultAdminExists()) {
            if (!loginService.nonDefaultAdminExists()) {
                LoginUser login;
                try {
                    login = createAdminUser();
                    loginService.insert(login);
                } catch (LIMSException e) {
                    LogEvent.logError(e);
                }
            }
        }
    }

    private LoginUser createAdminUser() throws LIMSException {
        LoginUser login = new LoginUser();
        login.setSysUserId("1");
        login.setLoginName(LoginUserService.DEFAULT_ADMIN_USER_NAME);
        login.setPasswordExpiredDate(getExpiredDate());
        login.setAccountLocked(IActionConstants.NO);
        login.setAccountDisabled(IActionConstants.NO);
        login.setIsAdmin(IActionConstants.YES);
        login.setUserTimeOut("220");

        String password;
        try {
            byte[] adminUserInfo = getAdminUserInfo();
            if (adminUserInfo.length <= 0) {
                throw new LIMSException("could not create default admin user as password is 0 length");
            }
            password = new String(adminUserInfo, StandardCharsets.UTF_8);
            password = processPassword(password);

            if (!loginService.isHashedPassword(password)) {
                if (PasswordValidationFactory.getPasswordValidator().passwordValid(password)) {
                    loginService.hashPassword(login, password);
                } else {
                    throw new LIMSException("could not create default admin user as password was not complex enough");
                }
            } else {
                login.setPassword(password);
            }
        } catch (IOException | URISyntaxException e) {
            throw new LIMSException("could not create default admin user as password could not be read");
        }

        return login;
    }

    private String processPassword(String password) {
        // strip password marker at beginning of line and newline character at end
        if (password.startsWith(PASSWORD_MARKER)) {
            password = password.substring(PASSWORD_MARKER.length());
        }
        if (password.endsWith("\n")) {
            password = password.substring(0, password.length() - 1);
        }
        if (password.startsWith("$2y")) {
            password = password.replace("$2y", "$2a");
        }
        return password;
    }

    private Date getExpiredDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 5);
        return new Date(calendar.getTimeInMillis());
    }

    private byte[] getAdminUserInfo() throws IOException, URISyntaxException {
        return Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(PASSWORD_FILEPATH).toURI()));
    }
}
