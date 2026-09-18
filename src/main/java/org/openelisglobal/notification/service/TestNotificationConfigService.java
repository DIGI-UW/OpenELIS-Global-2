package org.openelisglobal.notification.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notification.valueholder.TestNotificationConfig;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TestNotificationConfigService extends BaseObjectService<TestNotificationConfig, Integer> {

    @PreAuthorize("hasAuthority('PRIV_NOTIFICATION_VIEW')")
    Optional<TestNotificationConfig> getTestNotificationConfigForTestId(String testId);

    @PreAuthorize("hasAuthority('PRIV_NOTIFICATION_MANAGE')")
    TestNotificationConfig saveTestNotificationConfigActiveStatuses(TestNotificationConfig targetTestNotificationConfig,
            String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_NOTIFICATION_MANAGE')")
    void saveTestNotificationConfigsActiveStatuses(List<TestNotificationConfig> targetTestNotificationConfigs,
            String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_NOTIFICATION_MANAGE')")
    void removeEmptyPayloadTemplates(TestNotificationConfig testNotificationConfig, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_NOTIFICATION_MANAGE')")
    void updatePayloadTemplatesMessageAndSubject(TestNotificationConfig testNotificationConfig, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_NOTIFICATION_VIEW')")
    List<TestNotificationConfig> getTestNotificationConfigsForTestId(List<String> testIds);

    @PreAuthorize("hasAuthority('PRIV_NOTIFICATION_VIEW')")
    TestNotificationConfig getForConfigOption(Integer configOptionId);

    @PreAuthorize("hasAuthority('PRIV_NOTIFICATION_MANAGE')")
    void saveStatusAndMessages(TestNotificationConfig config, String sysUserId);
}
