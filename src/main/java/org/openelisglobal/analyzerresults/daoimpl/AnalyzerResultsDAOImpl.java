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
import java.util.List;
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

            List<AnalyzerResults> list = new ArrayList<>();

            String sql = "from AnalyzerResults a where a.analyzerId = :analyzerId and "
                    + "a.accessionNumber = :assessionNumber and " + "a.testName = :testName";
            Query<AnalyzerResults> query = entityManager.unwrap(Session.class).createQuery(sql, AnalyzerResults.class);
            query.setParameter("analyzerId", Integer.parseInt(result.getAnalyzerId()));
            query.setParameter("assessionNumber", result.getAccessionNumber());
            query.setParameter("testName", result.getTestName());

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
}
