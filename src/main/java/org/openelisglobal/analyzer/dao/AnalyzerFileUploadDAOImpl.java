package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerFileUpload;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerFileUploadDAOImpl extends BaseDAOImpl<AnalyzerFileUpload, Long> implements AnalyzerFileUploadDAO {

    public AnalyzerFileUploadDAOImpl() {
        super(AnalyzerFileUpload.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerFileUpload> findByAnalyzerIdAndFileHash(Integer analyzerId, String fileHashSha256) {
        try {
            if (analyzerId == null || fileHashSha256 == null || fileHashSha256.isBlank()) {
                return Optional.empty();
            }
            String hql = "FROM AnalyzerFileUpload a WHERE a.analyzerId = :analyzerId AND a.fileHashSha256 = :hash";
            Query<AnalyzerFileUpload> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerFileUpload.class);
            query.setParameter("analyzerId", analyzerId);
            query.setParameter("hash", fileHashSha256);
            AnalyzerFileUpload result = query.uniqueResult();
            return Optional.ofNullable(result);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new LIMSRuntimeException("Multiple AnalyzerFileUpload for analyzer " + analyzerId + " and hash", e);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error querying AnalyzerFileUpload: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerFileUpload> findByAnalyzerId(Integer analyzerId, int maxResults) {
        try {
            String hql = "FROM AnalyzerFileUpload a WHERE a.analyzerId = :analyzerId ORDER BY a.createdAt DESC";
            Query<AnalyzerFileUpload> query = entityManager.unwrap(Session.class).createQuery(hql,
                    AnalyzerFileUpload.class);
            query.setParameter("analyzerId", analyzerId);
            query.setMaxResults(maxResults);
            return query.getResultList();
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "findByAnalyzerId",
                    "Error finding uploads for analyzer " + analyzerId + ": " + e.getMessage());
            throw new LIMSRuntimeException("Error finding AnalyzerFileUpload by analyzer", e);
        }
    }
}
