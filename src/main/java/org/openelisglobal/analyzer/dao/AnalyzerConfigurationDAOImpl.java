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
            // NOTE: Uses native SQL instead of HQL because Analyzer entity uses XML mappings (legacy),
            // and Hibernate cannot resolve relationships between annotation-based entities
            // (AnalyzerConfiguration) and XML-mapped entities (Analyzer).
            // 
            // Attempted HQL: "FROM AnalyzerConfiguration WHERE analyzer.id = :analyzerId"
            // Error: "missing FROM-clause entry for table 'analyzer'"
            // 
            // This is a temporary workaround until Analyzer is migrated to JPA annotations.
            // See: specs/004-astm-analyzer-mapping/ANALYZER_XML_MAPPING_ANALYSIS.md
            //
            // analyzer_id is NUMERIC(10,0) in database, analyzerId is String in Java
            String sql = "SELECT * FROM analyzer_configuration WHERE analyzer_id = :analyzerId";
            @SuppressWarnings("unchecked")
            org.hibernate.query.NativeQuery<AnalyzerConfiguration> query = entityManager.unwrap(Session.class)
                    .createNativeQuery(sql, AnalyzerConfiguration.class);
            // Convert String ID to Integer for legacy Analyzer entity (NUMERIC in DB)
            Integer analyzerIdInt = Integer.parseInt(analyzerId);
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

