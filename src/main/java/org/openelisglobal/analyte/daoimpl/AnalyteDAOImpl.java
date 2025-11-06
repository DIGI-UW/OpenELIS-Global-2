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
package org.openelisglobal.analyte.daoimpl;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analyte.dao.AnalyteDAO;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class AnalyteDAOImpl extends BaseDAOImpl<Analyte, String> implements AnalyteDAO {

    public AnalyteDAOImpl() {
        super(Analyte.class);
    }

    @Override
    public void delete(Analyte analyte) {
        LogEvent.logWarn(this.getClass().getSimpleName(), "delete", "delete analyte is not implemented");
    }

    // bugzilla 1367 added ignoreCase
    @Override
    @Transactional(readOnly = true)
    public Analyte getAnalyteByName(Analyte analyte, boolean ignoreCase) throws LIMSRuntimeException {
        try {

            String sql = null;
            if (ignoreCase) {
                sql = "from Analyte a where trim(lower(a.analyteName)) = :param and a.isActive='Y'";

            } else {
                sql = "from Analyte a where a.analyteName = :param and a.isActive='Y'";
            }
            Query<Analyte> query = entityManager.unwrap(Session.class).createQuery(sql, Analyte.class);

            if (ignoreCase) {
                query.setParameter("param", analyte.getAnalyteName().trim().toLowerCase());
            } else {
                query.setParameter("param", analyte.getAnalyteName());
            }

            List<Analyte> list = query.list();

            Analyte ana = null;
            if (list.size() > 0) {
                ana = list.get(0);
            }

            return ana;

        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Analyte getAnalyteByName()", e);
        }
    }

    // bugzilla 1482
    @Override
    public boolean duplicateAnalyteExists(Analyte analyte) {
        try {

            List<Analyte> list = new ArrayList<>();

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates

            // bugzilla 2432 add check for local abbreviation
            String sql = "";
            if (analyte.getLocalAbbreviation() != null) {
                sql = "from Analyte a where (trim(lower(a.analyteName)) = :name and a.id != :id)"
                        + " or (trim(lower(a.localAbbreviation)) = :abbreviation and a.id != :id)";
            } else {
                sql = "from Analyte a where trim(lower(a.analyteName)) = :name and a.id != :id";
            }

            Query<Analyte> query = entityManager.unwrap(Session.class).createQuery(sql, Analyte.class);
            query.setParameter("name", analyte.getAnalyteName().toLowerCase().trim());
            // bugzilla 2432
            if (analyte.getLocalAbbreviation() != null) {
                query.setParameter("abbreviation", analyte.getLocalAbbreviation().toLowerCase().trim());
            }

            String analyteId = !StringUtil.isNullorNill(analyte.getId()) ? analyte.getId() : "0";

            query.setParameter("id", Integer.parseInt(analyteId));

            list = query.list();

            return list.size() > 0;
        } catch (RuntimeException e) {
            // buzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateAnalyteExists()", e);
        }
    }
}
