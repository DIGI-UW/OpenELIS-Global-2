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
package org.openelisglobal.common.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.dao.DatabaseChangeLogDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.valueholder.DatabaseChangeLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DatabaseChangeLogDAOImpl implements DatabaseChangeLogDAO {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public DatabaseChangeLog getLastExecutedChange() throws LIMSRuntimeException {
        List<DatabaseChangeLog> results;

        try {
            String sql = "from DatabaseChangeLog dcl order by dcl.executed desc";
            Query<DatabaseChangeLog> query = entityManager.unwrap(Session.class).createQuery(sql,
                    DatabaseChangeLog.class);

            results = query.list();

            if (results != null && results.get(0) != null) {
                return results.get(0);
            }

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DatabaseChangeLogDAOImpl getLastExecutedChange()", e);
        }

        return null;
    }
}
