package org.openelisglobal.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.Calendar;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSException;
import org.openelisglobal.common.provider.validation.PasswordValidationFactory;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.security.DaemonContextExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CreateAdminUserTask {

    @Value("${OE_INITIAL_ADMIN_PASSWORD_FILE:/run/secrets/initial-admin-password}")
    private String initialPasswordFile;

    @Autowired
    private LoginUserService loginService;

    @Autowired
    @Qualifier("daemonSysUserId")
    private String daemonSysUserId;

    @Autowired
    private DaemonContextExecutor daemonContextExecutor;

    @PostConstruct
    private void ensureAdminUserIsCreated() {
        daemonContextExecutor.executeAsDaemon(() -> {
            if (!loginService.defaultAdminExists()) {
                if (!loginService.nonDefaultAdminExists()) {
                    LoginUser login;
                    try {
                        login = createAdminUser();
                        loginService.insert(login);
                    } catch (LIMSException e) {
                        throw new IllegalStateException(
                                "Initial administrator could not be created: " + e.getMessage());
                    }
                }
            }
        });
    }

    private LoginUser createAdminUser() throws LIMSException {
        LoginUser login = new LoginUser();
        login.setSysUserId(daemonSysUserId);
        login.setLoginName(LoginUserService.DEFAULT_ADMIN_USER_NAME);
        login.setPasswordExpiredDate(getExpiredDate());
        login.setAccountLocked(IActionConstants.NO);
        login.setAccountDisabled(IActionConstants.NO);
        login.setIsAdmin(IActionConstants.YES);
        login.setUserTimeOut("220");

        String password;
        try {
            if (initialPasswordFile == null || initialPasswordFile.isBlank()
                    || Files.size(Paths.get(initialPasswordFile)) > 256) {
                throw new IOException("Invalid initial password file");
            }
            password = Files.readString(Paths.get(initialPasswordFile), StandardCharsets.UTF_8).replaceAll("[\\r\\n]+$",
                    "");
        } catch (IOException | RuntimeException e) {
            throw new LIMSException("Provide a readable initial administrator password file");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72
                || !PasswordValidationFactory.getPasswordValidator().passwordValid(password)) {
            throw new LIMSException("Initial administrator password does not meet the configured password policy");
        }
        loginService.hashPassword(login, password);
        return login;
    }

    private Date getExpiredDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 5);
        return new Date(calendar.getTimeInMillis());
    }

}
