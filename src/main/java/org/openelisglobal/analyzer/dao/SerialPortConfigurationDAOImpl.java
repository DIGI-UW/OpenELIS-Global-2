package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.SerialPortConfiguration;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for SerialPortConfiguration
 */
@Component
@Transactional
public class SerialPortConfigurationDAOImpl extends BaseDAOImpl<SerialPortConfiguration, String>
        implements SerialPortConfigurationDAO {

    public SerialPortConfigurationDAOImpl() {
        super(SerialPortConfiguration.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SerialPortConfiguration> findByAnalyzerId(Integer analyzerId) {
        try {
            if (analyzerId == null) {
                return Optional.empty();
            }

            String hql = "FROM SerialPortConfiguration spc WHERE spc.analyzerId = :analyzerId";
            Query<SerialPortConfiguration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SerialPortConfiguration.class);
            query.setParameter("analyzerId", analyzerId);
            SerialPortConfiguration result = query.uniqueResult();
            return Optional.ofNullable(result);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new LIMSRuntimeException("Multiple SerialPortConfiguration found for analyzer ID: " + analyzerId, e);
        } catch (Exception e) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "findByAnalyzerId",
                    "No SerialPortConfiguration found for analyzer ID: " + analyzerId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SerialPortConfiguration> findByPortName(String portName) {
        try {
            if (portName == null || portName.trim().isEmpty()) {
                return Optional.empty();
            }

            String hql = "FROM SerialPortConfiguration spc WHERE spc.portName = :portName AND spc.active = true";
            Query<SerialPortConfiguration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    SerialPortConfiguration.class);
            query.setParameter("portName", portName.trim());
            SerialPortConfiguration result = query.uniqueResult();
            return Optional.ofNullable(result);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new LIMSRuntimeException("Multiple SerialPortConfiguration found for port name: " + portName, e);
        } catch (Exception e) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "findByPortName",
                    "No SerialPortConfiguration found for port name: " + portName + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}
