/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzerresults.daoimpl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzerresults.dao.AnalyzerResultsDAO;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerResultsDAOImpl extends BaseDAOImpl<AnalyzerResults, String> implements AnalyzerResultsDAO {

    public AnalyzerResultsDAOImpl() {
        super(AnalyzerResults.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerResults> getDuplicateResultByAccessionAndTest(AnalyzerResults result) {
        try {
            if (result.getAnalyzerId() == null || result.getAnalyzerId().trim().isEmpty()) {
                return null;
            }

            List<AnalyzerResults> list = new ArrayList<>();

            // OGC-1129: a multiplex test stages one row per component, all sharing the
            // same testName. Include component_id so distinct components of one test are
            // not treated as duplicates of each other (null = PRIMARY, today's behavior).
            String sql = "from AnalyzerResults a where a.analyzerId = :analyzerId and "
                    + "a.accessionNumber = :assessionNumber and a.testName = :testName and "
                    + "((:componentId is null and a.componentId is null) or a.componentId = :componentId)";
            Query<AnalyzerResults> query = entityManager.unwrap(Session.class).createQuery(sql, AnalyzerResults.class);
            query.setParameter("analyzerId", result.getAnalyzerId());
            query.setParameter("assessionNumber", result.getAccessionNumber());
            query.setParameter("testName", result.getTestName());
            query.setParameter("componentId", result.getComponentId());

            list = query.list();

            return list.size() > 0 ? list : null;

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateAnalyzerResultsExists()", e);
        }
    }

    @Override
    public AnalyzerResults readAnalyzerResults(String idString) throws LIMSRuntimeException {
        AnalyzerResults data = null;
        try {
            data = entityManager.unwrap(Session.class).get(AnalyzerResults.class, idString);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AnalyzerResults readAnalyzerResults()", e);
        }
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerResults> findHeldResultValuesByProfile(String profileId, int profileRevision) {
        try {
            String hql = "FROM AnalyzerResults a WHERE a.importIssueReason = :reason "
                    + "AND a.sourceProfileId = :profileId AND a.sourceProfileRevision = :profileRevision "
                    + "ORDER BY a.lastupdated DESC NULLS LAST, a.id DESC";
            Query<AnalyzerResults> query = entityManager.unwrap(Session.class).createQuery(hql, AnalyzerResults.class);
            query.setParameter("reason", AnalyzerResults.IMPORT_ISSUE_UNKNOWN_RESULT_VALUE);
            query.setParameter("profileId", profileId);
            query.setParameter("profileRevision", profileRevision);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error finding held analyzer result values", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countHeldResultsByAnalyzerIds(List<String> analyzerIds) {
        if (analyzerIds == null || analyzerIds.isEmpty()) {
            return Map.of();
        }
        try {
            String hql = "SELECT a.analyzerId, COUNT(a.id) FROM AnalyzerResults a "
                    + "WHERE a.analyzerId IN (:analyzerIds) AND a.importIssueReason IS NOT NULL "
                    + "GROUP BY a.analyzerId";
            Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(hql, Object[].class);
            query.setParameterList("analyzerIds", analyzerIds);
            Map<String, Long> counts = new LinkedHashMap<>();
            for (Object[] row : query.list()) {
                counts.put((String) row[0], ((Number) row[1]).longValue());
            }
            return counts;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error counting held analyzer results", e);
        }
    }
}
