package org.openelisglobal.security;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Resolves the daemon system user at startup. The daemon user is created by
 * Liquibase migration ({@code 003-daemon-system-user.xml}) with
 * {@code login_name='daemon'}. This configuration exposes the daemon SystemUser
 * and its ID as Spring beans.
 *
 * <p>
 * Fails fast at startup if the daemon user cannot be resolved — there is no
 * silent fallback. The whole point of the UserContext initiative is to
 * eliminate hardcoded admin attribution, so a missing daemon user must surface
 * as a startup error, not as silent miswriting of audit rows.
 */
@Configuration
public class DaemonUserConfig {

    private static final String DAEMON_LOGIN_NAME = "daemon";

    @Autowired(required = false)
    private SystemUserService systemUserService;

    @Bean("daemonSystemUser")
    @DependsOn("liquibase")
    public SystemUser daemonSystemUser() {
        if (systemUserService == null) {
            throw new IllegalStateException("SystemUserService not available — cannot resolve daemon system user.");
        }

        SystemUser daemonUser = systemUserService.getDataForLoginUser(DAEMON_LOGIN_NAME);
        if (daemonUser != null && daemonUser.getId() != null) {
            LogEvent.logInfo(DaemonUserConfig.class.getSimpleName(), "daemonSystemUser",
                    "Daemon system user resolved: id=" + daemonUser.getId());
            return daemonUser;
        }

        throw new IllegalStateException(
                "Daemon system user not found. Liquibase changeset 003-daemon-system-user.xml must run before "
                        + "application startup. Check the changelog history and re-run migrations.");
    }

    @Bean("daemonSysUserId")
    public String daemonSysUserId(SystemUser daemonSystemUser) {
        return daemonSystemUser.getId();
    }
}
