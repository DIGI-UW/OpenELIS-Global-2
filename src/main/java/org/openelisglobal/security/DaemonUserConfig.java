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
 * Resolves the daemon system user at startup. The daemon user is created by
 * Liquibase migration ({@code 003-daemon-system-user.xml}) and its ID is also
 * stored in site_information. This configuration exposes the daemon SystemUser
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
    private static final String SITE_INFO_KEY = "daemon_system_user_id";

    @Autowired(required = false)
    private SiteInformationService siteInformationService;

    @Autowired(required = false)
    private SystemUserService systemUserService;

    @Bean("daemonSystemUser")
    @DependsOn("liquibase")
    public SystemUser daemonSystemUser() {
        if (systemUserService == null) {
            throw new IllegalStateException("SystemUserService not available — cannot resolve daemon system user.");
        }

        // Strategy 1: Look up by login_name directly
        SystemUser daemonUser = systemUserService.getDataForLoginUser(DAEMON_LOGIN_NAME);
        if (daemonUser != null && daemonUser.getId() != null) {
            LogEvent.logInfo(DaemonUserConfig.class.getSimpleName(), "daemonSystemUser",
                    "Daemon system user resolved: id=" + daemonUser.getId());
            return daemonUser;
        }

        // Strategy 2: Look up by ID from site_information
        if (siteInformationService != null) {
            SiteInformation siteInfo = siteInformationService.getSiteInformationByName(SITE_INFO_KEY);
            if (siteInfo != null && siteInfo.getValue() != null && !siteInfo.getValue().isEmpty()) {
                SystemUser fromSiteInfo = systemUserService.getUserById(siteInfo.getValue());
                if (fromSiteInfo != null) {
                    LogEvent.logInfo(DaemonUserConfig.class.getSimpleName(), "daemonSystemUser",
                            "Daemon system user resolved from site_information: id=" + fromSiteInfo.getId());
                    return fromSiteInfo;
                }
            }
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
