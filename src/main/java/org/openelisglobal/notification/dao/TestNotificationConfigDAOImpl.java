package org.openelisglobal.notification.dao;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notification.valueholder.TestNotificationConfig;
import org.springframework.stereotype.Component;

@Component
public class TestNotificationConfigDAOImpl extends BaseDAOImpl<TestNotificationConfig, Integer>
        implements TestNotificationConfigDAO {

    public TestNotificationConfigDAOImpl() {
        super(TestNotificationConfig.class);
    }

    @Override
    public Optional<TestNotificationConfig> getTestNotificationConfigForTestId(String testId) {
        TestNotificationConfig data;
        try {
            String sql = "From TestNotificationConfig as tnc where tnc.test.id = :testId";
            Query<TestNotificationConfig> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TestNotificationConfig.class);
            query.setParameter("testId", Integer.parseInt(testId));
            data = query.uniqueResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(
                    "Error in TestNotificationConfigDAOImpl getTestNotificationConfigForTestId()", e);
        }

        return Optional.ofNullable(data);
    }

    @Override
    public List<TestNotificationConfig> getTestNotificationConfigsForTestIds(List<String> testIds) {
        List<TestNotificationConfig> data;
        try {
            String sql = "From TestNotificationConfig as tnc where tnc.test.id IN (:testIds)";
            Query<TestNotificationConfig> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TestNotificationConfig.class);
            query.setParameterList("testIds",
                    testIds.stream().map(i -> Integer.parseInt(i)).collect(Collectors.toList()));
            data = query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException(
                    "Error in TestNotificationConfigDAOImpl getTestNotificationConfigsForTestIds()", e);
        }

        return data;
    }

    @Override
    public TestNotificationConfig getForConfigOption(Integer configOptionId) {
        TestNotificationConfig data;
        try {
            String sql = "SELECT tnc From TestNotificationConfig as tnc join tnc.options as tnco where tnco.id ="
                    + " :configOptionId";
            Query<TestNotificationConfig> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TestNotificationConfig.class);
            query.setParameter("configOptionId", configOptionId);
            data = query.uniqueResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestNotificationConfigDAOImpl getForConfigOption()", e);
        }

        return data;
    }
}
