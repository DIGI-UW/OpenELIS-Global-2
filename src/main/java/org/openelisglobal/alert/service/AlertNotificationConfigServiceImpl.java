package org.openelisglobal.alert.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.notification.dao.NotificationConfigOptionDAO;
import org.openelisglobal.notification.valueholder.NotificationConfigOption;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationPersonType;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@Transactional
public class AlertNotificationConfigServiceImpl implements AlertNotificationConfigService {

    @Autowired
    private NotificationConfigOptionDAO notificationConfigOptionDAO;

    @Autowired
    private SiteInformationService siteInformationService;

    @Autowired
    private LoginUserService loginUserService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAlertNotificationConfig() {
        Map<String, Object> config = new HashMap<>();

        // Get Nature-based Notification Config Options
        Map<String, Map<String, Boolean>> alertConfigs = new HashMap<>();
        for (NotificationNature nature : NotificationNature.values()) {
            if (nature == NotificationNature.FREEZER_TEMPERATURE_ALERT || nature == NotificationNature.EQUIPMENT_ALERT
                    || nature == NotificationNature.INVENTORY_ALERT) {

                Map<String, Boolean> methods = new HashMap<>();
                List<NotificationConfigOption> options = notificationConfigOptionDAO.getByNature(nature);
                for (NotificationConfigOption option : options) {
                    methods.put(option.getNotificationMethod().name(), option.getActive());
                }
                alertConfigs.put(nature.name(), methods);
            }
        }
        config.put("alertConfigs", alertConfigs);

        // Get Escalation Site Information
        SiteInformation enabled = siteInformationService.getSiteInformationByName("alert.escalation.enabled");
        config.put("escalationEnabled", enabled != null && Boolean.parseBoolean(enabled.getValue()));

        SiteInformation delay = siteInformationService.getSiteInformationByName("alert.escalation.delayMinutes");
        config.put("escalationDelayMinutes", delay != null ? Integer.parseInt(delay.getValue()) : 0);

        SiteInformation email = siteInformationService.getSiteInformationByName("alert.supervisor.email");
        config.put("supervisorEmail", email != null ? email.getValue() : "");

        return config;
    }

    @Override
    @Transactional
    public void saveAlertNotificationConfig(Map<String, Object> config) {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new LIMSRuntimeException(
                    "System User ID is required for auditing but was not found in session or security context.");
        }

        // Save Nature-based Notification Config Options
        Map<String, Map<String, Boolean>> alertConfigs = (Map<String, Map<String, Boolean>>) config.get("alertConfigs");
        if (alertConfigs != null) {
            for (Map.Entry<String, Map<String, Boolean>> entry : alertConfigs.entrySet()) {
                NotificationNature nature = NotificationNature.valueOf(entry.getKey());
                Map<String, Boolean> methods = entry.getValue();

                for (Map.Entry<String, Boolean> methodEntry : methods.entrySet()) {
                    NotificationConfigOption.NotificationMethod method = NotificationConfigOption.NotificationMethod
                            .valueOf(methodEntry.getKey().toUpperCase());
                    boolean active = methodEntry.getValue();

                    List<NotificationConfigOption> existingList = notificationConfigOptionDAO.getByNature(nature);
                    NotificationConfigOption option = existingList.stream()
                            .filter(opt -> opt.getNotificationMethod() == method).findFirst().orElse(null);

                    if (option != null) {
                        option.setActive(active);
                        option.setSysUserId(userId);
                        notificationConfigOptionDAO.update(option);
                    } else {
                        option = new NotificationConfigOption();
                        option.setNotificationNature(nature);
                        option.setNotificationMethod(method);
                        option.setNotificationPersonType(NotificationPersonType.PROVIDER);
                        option.setActive(active);
                        option.setSysUserId(userId);
                        notificationConfigOptionDAO.insert(option);
                    }
                }
            }
        }

        // Save Escalation Site Information
        if (config.containsKey("escalationEnabled")) {
            saveSiteInformation("alert.escalation.enabled", String.valueOf(config.get("escalationEnabled")), userId,
                    "BOOLEAN");
        }
        if (config.containsKey("escalationDelayMinutes")) {
            saveSiteInformation("alert.escalation.delayMinutes", String.valueOf(config.get("escalationDelayMinutes")),
                    userId, "NUMBER");
        }
        if (config.containsKey("supervisorEmail")) {
            saveSiteInformation("alert.supervisor.email", String.valueOf(config.get("supervisorEmail")), userId,
                    "TEXT");
        }
    }

    private void saveSiteInformation(String name, String value, String userId, String type) {
        SiteInformation siteInfo = siteInformationService.getSiteInformationByName(name);
        if (siteInfo != null) {
            siteInfo.setValue(value);
            siteInfo.setSysUserId(userId);
            siteInformationService.save(siteInfo);
        } else {
            siteInfo = new SiteInformation();
            siteInfo.setName(name);
            siteInfo.setValue(value);
            siteInfo.setValueType(type);
            siteInfo.setSysUserId(userId);
            siteInformationService.save(siteInfo);
        }
    }

    private String getCurrentUserId() {
        // Strategy 1: Active Web Request
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return ControllerUtills.getSysUserId(attributes.getRequest());
        }

        // Strategy 2: Resolve from Security Context via LoginUserService.
        // This correctly attributes writes to the real user for any authenticated
        // principal (e.g. @Async handlers) without hardcoding a fallback user ID.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            LoginUser login = loginUserService.getUserProfile(auth.getName());
            if (login != null) {
                return String.valueOf(loginUserService.getSystemUserId(login));
            }
        }

        return null;
    }
}
