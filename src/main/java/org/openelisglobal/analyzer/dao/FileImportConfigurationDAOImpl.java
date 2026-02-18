package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class FileImportConfigurationDAOImpl extends BaseDAOImpl<FileImportConfiguration, String>
        implements FileImportConfigurationDAO {

    public FileImportConfigurationDAOImpl() {
        super(FileImportConfiguration.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileImportConfiguration> findByAnalyzerId(Integer analyzerId) {
        try {
            if (analyzerId == null) {
                return Optional.empty();
            }

            String hql = "FROM FileImportConfiguration fic WHERE fic.analyzerId = :analyzerId";
            Query<FileImportConfiguration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    FileImportConfiguration.class);
            query.setParameter("analyzerId", analyzerId);
            FileImportConfiguration result = query.uniqueResult();
            return Optional.ofNullable(result);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new LIMSRuntimeException("Multiple FileImportConfiguration found for analyzer ID: " + analyzerId, e);
        } catch (Exception e) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "findByAnalyzerId",
                    "No FileImportConfiguration found for analyzer ID: " + analyzerId + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileImportConfiguration> findAllActive() {
        try {
            String hql = "FROM FileImportConfiguration fic WHERE fic.active = true";
            Query<FileImportConfiguration> query = entityManager.unwrap(Session.class).createQuery(hql,
                    FileImportConfiguration.class);
            return query.getResultList();
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "findAllActive",
                    "Error finding active FileImportConfiguration: " + e.getMessage());
            throw new LIMSRuntimeException("Error finding active FileImportConfiguration", e);
        }
    }
}
