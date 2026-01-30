package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerConfigurationDAOImpl extends BaseDAOImpl<AnalyzerConfiguration, String>
        implements AnalyzerConfigurationDAO {

    public AnalyzerConfigurationDAOImpl() {
        super(AnalyzerConfiguration.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerConfiguration> findByAnalyzerId(String analyzerId) {
        try {
            Integer analyzerIdInt = Integer.parseInt(analyzerId);

            // HQL path expression via relationship to legacy Analyzer entity
            String hql = "SELECT ac FROM AnalyzerConfiguration ac " + "JOIN ac.analyzer a "
                    + "WHERE a.id = :analyzerId";
            Query<AnalyzerConfiguration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerConfiguration.class);
            query.setParameter("analyzerId", analyzerIdInt);
            AnalyzerConfiguration result = query.uniqueResult();
            return Optional.ofNullable(result);
        } catch (NumberFormatException e) {
            // Invalid analyzer ID format
            LogEvent.logDebug(this.getClass().getSimpleName(), "findByAnalyzerId",
                    "Invalid analyzer ID format: " + analyzerId);
            return Optional.empty();
        } catch (org.hibernate.NonUniqueResultException e) {
            // Multiple results found - should not happen due to unique constraint
            throw new LIMSRuntimeException("Multiple AnalyzerConfiguration found for analyzer ID: " + analyzerId, e);
        } catch (Exception e) {
            // Log but return empty - configuration might not exist yet
            LogEvent.logDebug(this.getClass().getSimpleName(), "findByAnalyzerId",
                    "No AnalyzerConfiguration found for analyzer ID: " + analyzerId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerConfiguration> findByIpAddress(String ipAddress) {
        try {
            if (ipAddress == null || ipAddress.trim().isEmpty()) {
                return Optional.empty();
            }

            String hql = "FROM AnalyzerConfiguration ac WHERE ac.ipAddress = :ipAddress";
            Query<AnalyzerConfiguration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerConfiguration.class);
            query.setParameter("ipAddress", ipAddress.trim());
            AnalyzerConfiguration result = query.uniqueResult();
            return Optional.ofNullable(result);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new LIMSRuntimeException("Multiple AnalyzerConfiguration found for IP address: " + ipAddress, e);
        } catch (Exception e) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "findByIpAddress",
                    "No AnalyzerConfiguration found for IP address: " + ipAddress + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerConfiguration> findByAnalyzerName(String name) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return Optional.empty();
            }

            // HQL path expression via relationship to legacy Analyzer entity
            String hql = "SELECT ac FROM AnalyzerConfiguration ac " + "JOIN ac.analyzer a " + "WHERE a.name = :name";
            Query<AnalyzerConfiguration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerConfiguration.class);
            query.setParameter("name", name.trim());
            AnalyzerConfiguration result = query.uniqueResult();
            return Optional.ofNullable(result);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new LIMSRuntimeException("Multiple AnalyzerConfiguration found for analyzer name: " + name, e);
        } catch (Exception e) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "findByAnalyzerName",
                    "No AnalyzerConfiguration found for analyzer name: " + name + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerConfiguration> findGenericPluginConfigsWithPatterns() {
        String hql = "SELECT ac FROM AnalyzerConfiguration ac " +
                "JOIN FETCH ac.analyzer a " +
                "WHERE ac.genericPlugin = true AND ac.identifierPattern IS NOT NULL";
        Query<AnalyzerConfiguration> query = entityManager.unwrap(Session.class).createQuery(hql,
                AnalyzerConfiguration.class);
        return query.list();
    }
}
