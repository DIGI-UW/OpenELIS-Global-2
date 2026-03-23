package org.openelisglobal.alert.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notification.dao.NotificationConfigOptionDAO;
import org.openelisglobal.notification.valueholder.NotificationConfigOption;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationMethod;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("unused")
public class AlertNotificationConfigServiceImpl implements AlertNotificationConfigService {

    private final NotificationConfigOptionDAO notificationConfigOptionDAO;
    private final SiteInformationService siteInformationService;

    private static final String SITE_INFO_ESCALATION_ENABLED = "alert.escalation.enabled";
    private static final String SITE_INFO_ESCALATION_DELAY_MINUTES = "alert.escalation.delayMinutes";
    private static final String SITE_INFO_SUPERVISOR_EMAIL = "alert.supervisor.email";

    public AlertNotificationConfigServiceImpl(NotificationConfigOptionDAO notificationConfigOptionDAO,
            SiteInformationService siteInformationService) {
        this.notificationConfigOptionDAO = notificationConfigOptionDAO;
        this.siteInformationService = siteInformationService;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAlertNotificationConfig() {
        Map<String, Object> config = new HashMap<>();
        List<NotificationConfigOption> allAlertConfigs = notificationConfigOptionDAO.getAllAlertConfigs();

        Map<String, Map<String, Boolean>> alertConfigs = new HashMap<>();
        NotificationNature[] alertNatures = { NotificationNature.FREEZER_TEMPERATURE_ALERT,
                NotificationNature.EQUIPMENT_ALERT, NotificationNature.INVENTORY_ALERT };

        for (NotificationNature nature : alertNatures) {
            Map<String, Boolean> methods = new HashMap<>();
            methods.put("email", false);
            methods.put("sms", false);
            alertConfigs.put(nature.toString(), methods);
        }

        for (NotificationConfigOption opt : allAlertConfigs) {
            String natureKey = opt.getNotificationNature().toString();
            if (alertConfigs.containsKey(natureKey)) {
                if (opt.getNotificationMethod() == NotificationMethod.EMAIL) {
                    alertConfigs.get(natureKey).put("email", opt.getActive());
                } else if (opt.getNotificationMethod() == NotificationMethod.SMS) {
                    alertConfigs.get(natureKey).put("sms", opt.getActive());
                }
            }
        }

        config.put("alertConfigs", alertConfigs);

        SiteInformation escalationEnabled = siteInformationService
                .getSiteInformationByName(SITE_INFO_ESCALATION_ENABLED);
        SiteInformation escalationDelay = siteInformationService
                .getSiteInformationByName(SITE_INFO_ESCALATION_DELAY_MINUTES);
        SiteInformation supervisorEmail = siteInformationService.getSiteInformationByName(SITE_INFO_SUPERVISOR_EMAIL);

        config.put("escalationEnabled",
                escalationEnabled != null ? Boolean.parseBoolean(escalationEnabled.getValue()) : false);
        config.put("escalationDelayMinutes",
                escalationDelay != null ? Integer.parseInt(escalationDelay.getValue()) : 15);
        config.put("supervisorEmail", supervisorEmail != null ? supervisorEmail.getValue() : "");
        return config;
    }

    @Override
    @Transactional
    public void saveAlertNotificationConfig(Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Boolean>> alertConfigs = (Map<String, Map<String, Boolean>>) config.get("alertConfigs");
        Boolean escalationEnabled = (Boolean) config.get("escalationEnabled");
        Integer escalationDelayMinutes = (Integer) config.get("escalationDelayMinutes");
        String supervisorEmail = (String) config.get("supervisorEmail");

        if (alertConfigs != null) {
            for (Map.Entry<String, Map<String, Boolean>> entry : alertConfigs.entrySet()) {
                String natureStr = entry.getKey();
                Map<String, Boolean> methods = entry.getValue();

                try {
                    NotificationNature nature = NotificationNature.valueOf(natureStr);
                    Boolean emailEnabled = methods.get("email");
                    Boolean smsEnabled = methods.get("sms");

                    if (emailEnabled != null) {
                        updateAlertNotificationConfig(nature, NotificationMethod.EMAIL, emailEnabled);
                    }

                    if (smsEnabled != null) {
                        updateAlertNotificationConfig(nature, NotificationMethod.SMS, smsEnabled);
                    }
                } catch (IllegalArgumentException e) {
                    // Skip invalid nature values
                }
            }
        }

        saveSiteInformation(SITE_INFO_ESCALATION_ENABLED,
                escalationEnabled != null ? escalationEnabled.toString() : "false", "boolean");
        saveSiteInformation(SITE_INFO_ESCALATION_DELAY_MINUTES,
                escalationDelayMinutes != null ? escalationDelayMinutes.toString() : "15", "text");
        saveSiteInformation(SITE_INFO_SUPERVISOR_EMAIL, supervisorEmail != null ? supervisorEmail : "", "text");
    }

    private void updateAlertNotificationConfig(NotificationNature nature, NotificationMethod method, boolean active) {
        List<NotificationConfigOption> existing = notificationConfigOptionDAO.getByNature(nature);

        NotificationConfigOption config = existing.stream().filter(opt -> opt.getNotificationMethod() == method)
                .findFirst().orElse(null);

        if (config == null) {
            config = new NotificationConfigOption();
            config.setNotificationNature(nature);
            config.setNotificationMethod(method);
            config.setNotificationPersonType(NotificationConfigOption.NotificationPersonType.PROVIDER);
            config.setActive(active);
            notificationConfigOptionDAO.insert(config);
        } else {
            config.setActive(active);
            notificationConfigOptionDAO.update(config);
        }
    }

    private void saveSiteInformation(String name, String value, String valueType) {
        SiteInformation siteInfo = siteInformationService.getSiteInformationByName(name);
        if (siteInfo == null) {
            siteInfo = new SiteInformation();
            siteInfo.setName(name);
            siteInfo.setValueType(valueType);
        }
        siteInfo.setValue(value);
        siteInformationService.save(siteInfo);
    }
}
