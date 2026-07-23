package org.openelisglobal.calendar.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.calendar.valueholder.WeekendConfig;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WeekendConfigDAOImpl extends BaseDAOImpl<WeekendConfig, Integer> implements WeekendConfigDAO {

    private static final Logger logger = LoggerFactory.getLogger(WeekendConfigDAOImpl.class);

    public WeekendConfigDAOImpl() {
        super(WeekendConfig.class);
    }

    @Override
    public List<WeekendConfig> getAllConfigs() {
        try {
            String hql = "FROM WeekendConfig w ORDER BY w.dayOfWeek ASC";
            Query<WeekendConfig> query = entityManager.unwrap(Session.class).createQuery(hql, WeekendConfig.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error getting all weekend configs", e);
            throw new LIMSRuntimeException("Error getting all weekend configs", e);
        }
    }

    @Override
    public List<Integer> getWeekendDayNumbers() {
        try {
            String hql = "SELECT w.dayOfWeek FROM WeekendConfig w WHERE w.isWeekend = true ORDER BY w.dayOfWeek";
            Query<Integer> query = entityManager.unwrap(Session.class).createQuery(hql, Integer.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error getting weekend day numbers", e);
            throw new LIMSRuntimeException("Error getting weekend day numbers", e);
        }
    }
}
