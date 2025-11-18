package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
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
            String hql = "SELECT ac FROM AnalyzerConfiguration ac " +
                    "JOIN ac.analyzer a " +
                    "WHERE a.id = :analyzerId";
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
}

