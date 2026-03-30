/**
 * \ * The contents of this file are subject to the Mozilla Public License Version 1.1 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.mozilla.org/MPL/
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
package org.openelisglobal.analyzerimport.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyzerimport.dao.AnalyzerTestMappingDAO;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMappingPK;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AnalyzerTestMappingDAOImpl extends BaseDAOImpl<AnalyzerTestMapping, AnalyzerTestMappingPK>
        implements AnalyzerTestMappingDAO {

    public AnalyzerTestMappingDAOImpl() {
        super(AnalyzerTestMapping.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerTestMapping> getAllForAnalyzer(String analyzerId) {
        List<AnalyzerTestMapping> list;
        try {
            String sql = "from AnalyzerTestMapping a where a.compoundId.analyzerId = :analyzerId";
            Query<AnalyzerTestMapping> query = entityManager.unwrap(Session.class).createQuery(sql,
                    AnalyzerTestMapping.class);
            query.setParameter("analyzerId", Integer.parseInt(analyzerId));
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AnalyzerTestMappingDAOImpl getAllForAnalyzer()", e);
        }

        return list;
    }
}
