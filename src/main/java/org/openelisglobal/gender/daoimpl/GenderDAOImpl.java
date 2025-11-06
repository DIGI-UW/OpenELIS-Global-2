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
package org.openelisglobal.gender.daoimpl;

import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.gender.dao.GenderDAO;
import org.openelisglobal.gender.valueholder.Gender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class GenderDAOImpl extends BaseDAOImpl<Gender, Integer> implements GenderDAO {

    public GenderDAOImpl() {
        super(Gender.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Gender getGenderByType(String type) throws LIMSRuntimeException {
        String sql = "From Gender g where g.genderType = :type";
        try {
            Query<Gender> query = entityManager.unwrap(Session.class).createQuery(sql, Gender.class);
            query.setParameter("type", type);
            Gender gender = query.uniqueResult();
            return gender;
        } catch (HibernateException e) {
            handleException(e, "getGenderByType");
        }
        return null;
    }

    // bugzilla 1482
    @Override
    public boolean duplicateGenderExists(Gender gender) throws LIMSRuntimeException {
        try {

            List<Gender> list;

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from Gender t where trim(lower(t.genderType)) = :genderType and t.id != :genderId";
            Query<Gender> query = entityManager.unwrap(Session.class).createQuery(sql, Gender.class);
            query.setParameter("genderType", gender.getGenderType().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated
            // yet
            Integer genderId = 0;
            if (gender.getId() != null) {
                genderId = gender.getId();
            }
            query.setParameter("genderId", genderId);
            list = query.list();
            return list.size() > 0;
        } catch (RuntimeException e) {

            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateGenderExists()", e);
        }
    }
}
