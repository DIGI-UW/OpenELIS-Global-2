package org.openelisglobal.security;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Resolves and caches the daemon system user at startup. The daemon user is
 * created by Liquibase migration and its ID is stored in site_information. This
 * configuration exposes the daemon SystemUser and its ID as Spring beans.
 */
@Configuration
public class DaemonUserConfig {

    private static final String DAEMON_LOGIN_NAME = "daemon";
    private static final String SITE_INFO_KEY = "daemon_system_user_id";
    private static final String FALLBACK_USER_ID = "1";

    @Autowired(required = false)
    private SiteInformationService siteInformationService;

    @Autowired(required = false)
    private SystemUserService systemUserService;

    @Bean("daemonSystemUser")
    @DependsOn("liquibase")
    public SystemUser daemonSystemUser() {
        // Strategy 1: Look up by login_name directly
        if (systemUserService != null) {
            try {
                SystemUser daemonUser = systemUserService.getDataForLoginUser(DAEMON_LOGIN_NAME);
                if (daemonUser != null && daemonUser.getId() != null) {
                    LogEvent.logInfo(DaemonUserConfig.class.getSimpleName(), "daemonSystemUser",
                            "Daemon system user resolved: id=" + daemonUser.getId());
                    return daemonUser;
                }
            } catch (Exception e) {
                LogEvent.logWarn(DaemonUserConfig.class.getSimpleName(), "daemonSystemUser",
                        "Could not look up daemon user by login_name: " + e.getMessage());
            }
        }

        // Strategy 2: Look up by ID from site_information
        if (siteInformationService != null && systemUserService != null) {
            try {
                SiteInformation siteInfo = siteInformationService.getSiteInformationByName(SITE_INFO_KEY);
                if (siteInfo != null && siteInfo.getValue() != null && !siteInfo.getValue().isEmpty()) {
                    SystemUser daemonUser = systemUserService.getUserById(siteInfo.getValue());
                    if (daemonUser != null) {
                        LogEvent.logInfo(DaemonUserConfig.class.getSimpleName(), "daemonSystemUser",
                                "Daemon system user resolved from site_information: id=" + daemonUser.getId());
                        return daemonUser;
                    }
                }
            } catch (Exception e) {
                LogEvent.logWarn(DaemonUserConfig.class.getSimpleName(), "daemonSystemUser",
                        "Could not resolve daemon user from site_information: " + e.getMessage());
            }
        }

        // Fallback: build a minimal SystemUser with admin ID for backward compat
        LogEvent.logWarn(DaemonUserConfig.class.getSimpleName(), "daemonSystemUser",
                "Daemon system user not found. Falling back to admin user ID '" + FALLBACK_USER_ID
                        + "'. Run Liquibase migrations to create the daemon user.");
        SystemUser fallback = new SystemUser();
        fallback.setId(FALLBACK_USER_ID);
        fallback.setLoginName("admin");
        fallback.setFirstName("Admin");
        fallback.setLastName("Fallback");
        return fallback;
    }

    @Bean("daemonSysUserId")
    public String daemonSysUserId(SystemUser daemonSystemUser) {
        return daemonSystemUser.getId();
    }
}
