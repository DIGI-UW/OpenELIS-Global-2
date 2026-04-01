package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzer.valueholder.AnalyzerRun;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerRunDAOImpl extends BaseDAOImpl<AnalyzerRun, Long> implements AnalyzerRunDAO {

    public AnalyzerRunDAOImpl() {
        super(AnalyzerRun.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerRun> findByAnalyzerFileUploadId(Long analyzerFileUploadId) {
        try {
            if (analyzerFileUploadId == null) {
                return Optional.empty();
            }
            String hql = "FROM AnalyzerRun r WHERE r.analyzerFileUploadId = :uploadId";
            Query<AnalyzerRun> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerRun.class);
            query.setParameter("uploadId", analyzerFileUploadId);
            AnalyzerRun result = query.uniqueResult();
            return Optional.ofNullable(result);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "findByAnalyzerFileUploadId",
                    "Error finding run for upload " + analyzerFileUploadId + ": " + e.getMessage());
            throw new LIMSRuntimeException("Error finding AnalyzerRun by upload id", e);
        }
    }
}
