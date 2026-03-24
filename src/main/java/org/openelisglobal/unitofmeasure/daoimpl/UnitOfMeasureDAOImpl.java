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
 */
package org.openelisglobal.unitofmeasure.daoimpl;

import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.unitofmeasure.dao.UnitOfMeasureDAO;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class UnitOfMeasureDAOImpl extends BaseDAOImpl<UnitOfMeasure, String> implements UnitOfMeasureDAO {

    public UnitOfMeasureDAOImpl() {
        super(UnitOfMeasure.class);
    }

    @Override
    @Transactional(readOnly = true)
    public UnitOfMeasure getUnitOfMeasureById(String uomId) throws LIMSRuntimeException {
        String sql = "from UnitOfMeasure uom where id = :id";
        try {
            Query<UnitOfMeasure> query = entityManager.unwrap(Session.class).createQuery(sql, UnitOfMeasure.class);
            query.setParameter("id", Integer.parseInt(uomId));
            UnitOfMeasure uom = query.uniqueResult();
            return uom;
        } catch (HibernateException e) {
            handleException(e, "getUnitOfMeeasureById");
        }

        return null;
    }

    @Override
    public boolean duplicateUnitOfMeasureExists(UnitOfMeasure unitOfMeasure) throws LIMSRuntimeException {
        try {
            List<UnitOfMeasure> list;

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from UnitOfMeasure t where trim(t.unitOfMeasureName) = :param and t.id != :param2";
            Query<UnitOfMeasure> query = entityManager.unwrap(Session.class).createQuery(sql, UnitOfMeasure.class);
            query.setParameter("param", unitOfMeasure.getUnitOfMeasureName().trim());

            // initialize with 0 (for new records where no id has been generated
            // yet
            String unitOfMeasureId = "0";
            if (!StringUtil.isNullorNill(unitOfMeasure.getId())) {
                unitOfMeasureId = unitOfMeasure.getId();
            }
            query.setParameter("param2", Integer.parseInt(unitOfMeasureId));

            list = query.list();
            return !list.isEmpty();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateUnitOfMeasureExists()", e);
        }
    }
}
