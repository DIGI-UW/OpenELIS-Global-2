package org.openelisglobal.notification.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notification.valueholder.TestNotificationConfig;

public interface TestNotificationConfigDAO extends BaseDAO<TestNotificationConfig, Integer> {

    Optional<TestNotificationConfig> getTestNotificationConfigForTestId(String testId);

    List<TestNotificationConfig> getTestNotificationConfigsForTestIds(List<String> testIds);

    TestNotificationConfig getForConfigOption(Integer configOptionId);
}
