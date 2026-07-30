package org.openelisglobal.notification.service;

import java.util.List;
import java.util.Optional;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notification.dao.TestNotificationConfigDAO;
import org.openelisglobal.notification.valueholder.NotificationConfigOption;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;
import org.openelisglobal.notification.valueholder.TestNotificationConfig;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestNotificationConfigServiceImpl extends AuditableBaseObjectServiceImpl<TestNotificationConfig, Integer>
        implements TestNotificationConfigService {

    @Autowired
    private TestNotificationConfigDAO baseDAO;
    @Autowired
    private TestService testService;
    @Autowired
    private NotificationPayloadTemplateService notificationPayloadTemplateService;

    public TestNotificationConfigServiceImpl() {
        super(TestNotificationConfig.class);
        this.auditTrailLog = false;
    }

    @Override
    protected BaseDAO<TestNotificationConfig, Integer> getBaseObjectDAO() {
        return baseDAO;
    }

    @Override
    public Optional<TestNotificationConfig> getTestNotificationConfigForTestId(String testId) {
        return baseDAO.getTestNotificationConfigForTestId(testId);
    }

    @Override
    @Transactional
    public TestNotificationConfig saveTestNotificationConfigActiveStatuses(
            TestNotificationConfig targetTestNotificationConfig, String sysUserId) {
        TestNotificationConfig oldConfig;
        if (targetTestNotificationConfig.getId() != null) {
            oldConfig = get(targetTestNotificationConfig.getId());
        } else {
            oldConfig = new TestNotificationConfig();
            oldConfig.setTest(testService.get(targetTestNotificationConfig.getTest().getId()));
            NotificationPayloadTemplate incoming = targetTestNotificationConfig.getDefaultPayloadTemplate();
            if (incoming != null && incoming.getId() != null) {
                NotificationPayloadTemplate systemDefault = notificationPayloadTemplateService
                        .getSystemDefaultPayloadTemplateForType(NotificationPayloadType.TEST_RESULT);
                boolean isSystemDefault = systemDefault != null && incoming.getId().equals(systemDefault.getId());
                if (!isSystemDefault) {
                    NotificationPayloadTemplate existing = notificationPayloadTemplateService.get(incoming.getId());
                    if (existing != null) {
                        oldConfig.setDefaultPayloadTemplate(existing);
                    }
                }
            }
        }

        oldConfig.getPatientEmail().setActive(targetTestNotificationConfig.getPatientEmail().getActive());
        oldConfig.getPatientSMS().setActive(targetTestNotificationConfig.getPatientSMS().getActive());
        oldConfig.getProviderEmail().setActive(targetTestNotificationConfig.getProviderEmail().getActive());
        oldConfig.getProviderSMS().setActive(targetTestNotificationConfig.getProviderSMS().getActive());
        oldConfig.setSysUserId(sysUserId);
        return save(oldConfig);
    }

    @Override
    @Transactional
    public void saveTestNotificationConfigsActiveStatuses(List<TestNotificationConfig> targetTestNotificationConfigs,
            String sysUserId) {
        for (TestNotificationConfig targetTestNotificationConfig : targetTestNotificationConfigs) {
            saveTestNotificationConfigActiveStatuses(targetTestNotificationConfig, sysUserId);
        }
    }

    @Override
    @Transactional
    public void removeEmptyPayloadTemplates(TestNotificationConfig newTestNotificationConfig, String sysUserId) {
        TestNotificationConfig oldConfig;
        if (newTestNotificationConfig.getId() != null) {
            oldConfig = get(newTestNotificationConfig.getId());
            if (testDefaultEmpty(newTestNotificationConfig.getDefaultPayloadTemplate())) {
                oldConfig.setDefaultPayloadTemplate(null);
                oldConfig.setSysUserId(sysUserId);
            }
            for (NotificationConfigOption newOption : newTestNotificationConfig.getOptions()) {
                if (testDefaultEmpty(newOption.getPayloadTemplate())) {
                    NotificationConfigOption oldOption = oldConfig.getOptionFor(newOption.getNotificationNature(),
                            newOption.getNotificationMethod(), newOption.getNotificationPersonType());
                    oldOption.setPayloadTemplate(null);
                    oldOption.setSysUserId(sysUserId);
                }
            }
            save(oldConfig);
        }
    }

    private boolean testDefaultEmpty(NotificationPayloadTemplate notificationPayloadTemplate) {
        if (notificationPayloadTemplate == null
                || GenericValidator.isBlankOrNull(notificationPayloadTemplate.getMessageTemplate())) {
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void updatePayloadTemplatesMessageAndSubject(TestNotificationConfig newTestNotificationConfig,
            String sysUserId) {
        TestNotificationConfig oldConfig;

        if (newTestNotificationConfig.getId() != null) {
            oldConfig = get(newTestNotificationConfig.getId());

            NotificationPayloadTemplate systemDefault = notificationPayloadTemplateService
                    .getSystemDefaultPayloadTemplateForType(NotificationPayloadType.TEST_RESULT);
            Integer systemDefaultId = systemDefault == null ? null : systemDefault.getId();

            applyPerTestTemplate(oldConfig::setDefaultPayloadTemplate, oldConfig.getDefaultPayloadTemplate(),
                    newTestNotificationConfig.getDefaultPayloadTemplate(), sysUserId, systemDefaultId);

            for (NotificationConfigOption newOption : newTestNotificationConfig.getOptions()) {
                NotificationConfigOption oldOption = oldConfig.getOptionFor(newOption.getNotificationNature(),
                        newOption.getNotificationMethod(), newOption.getNotificationPersonType());
                applyPerTestTemplate(oldOption::setPayloadTemplate, oldOption.getPayloadTemplate(),
                        newOption.getPayloadTemplate(), sysUserId, systemDefaultId);
            }
        } else {
            oldConfig = newTestNotificationConfig;
            oldConfig.setTest(testService.get(newTestNotificationConfig.getTestId()));
        }
        save(oldConfig);
    }

    /**
     * Writes the incoming subject/message into a per-test template slot. If the
     * slot is empty or currently points at the system-default row (identified by
     * {@code systemDefaultId}), a fresh template is created via {@code setter};
     * otherwise the existing template is updated in place.
     */
    private void applyPerTestTemplate(java.util.function.Consumer<NotificationPayloadTemplate> setter,
            NotificationPayloadTemplate existing, NotificationPayloadTemplate incoming, String sysUserId,
            Integer systemDefaultId) {
        if (incoming == null) {
            return;
        }
        boolean sharesSystemDefault = existing != null && existing.getId() != null && systemDefaultId != null
                && existing.getId().equals(systemDefaultId);
        if (existing == null || sharesSystemDefault) {
            NotificationPayloadTemplate fresh = new NotificationPayloadTemplate();
            fresh.setSubjectTemplate(incoming.getSubjectTemplate());
            fresh.setMessageTemplate(incoming.getMessageTemplate());
            fresh.setType(NotificationPayloadType.TEST_RESULT);
            fresh.setSysUserId(sysUserId);
            setter.accept(fresh);
        } else {
            existing.setSubjectTemplate(incoming.getSubjectTemplate());
            existing.setMessageTemplate(incoming.getMessageTemplate());
            existing.setSysUserId(sysUserId);
        }
    }

    @Override
    public List<TestNotificationConfig> getTestNotificationConfigsForTestId(List<String> testIds) {
        return baseDAO.getTestNotificationConfigsForTestIds(testIds);
    }

    @Override
    public TestNotificationConfig getForConfigOption(Integer configOptionId) {
        return baseDAO.getForConfigOption(configOptionId);
    }

    @Override
    @Transactional
    public void saveStatusAndMessages(TestNotificationConfig config, String sysUserId) {
        TestNotificationConfig savedConfig = saveTestNotificationConfigActiveStatuses(config, sysUserId);
        config.setId(savedConfig.getId());
        updatePayloadTemplatesMessageAndSubject(config, sysUserId);
    }
}
