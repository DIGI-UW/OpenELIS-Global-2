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
package org.openelisglobal.dictionarycategory.daoimpl;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dictionarycategory.dao.DictionaryCategoryDAO;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz bugzilla 2061-2063
 */
@Component
@Transactional
public class DictionaryCategoryDAOImpl extends BaseDAOImpl<DictionaryCategory, String>
        implements DictionaryCategoryDAO {

    public DictionaryCategoryDAOImpl() {
        super(DictionaryCategory.class);
    }

    // bugzilla 1386
    @Override
    public boolean duplicateDictionaryCategoryExists(DictionaryCategory dictionaryCategory)
            throws LIMSRuntimeException {
        try {

            List<DictionaryCategory> list = new ArrayList<>();

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            // only one of each name, description, local abbrev can exist in entire table
            String sql = "from DictionaryCategory t where "
                    + "((trim(lower(t.categoryName)) = :param and t.id != :param3) " + "or "
                    + "(trim(lower(t.description)) = :param2 and t.id != :param3) " + "or "
                    + "(trim(lower(t.localAbbreviation)) = :param4 and t.id != :param3)) ";
            Query<DictionaryCategory> query = entityManager.unwrap(Session.class).createQuery(sql,
                    DictionaryCategory.class);
            query.setParameter("param", dictionaryCategory.getCategoryName().toLowerCase().trim());
            query.setParameter("param2", dictionaryCategory.getDescription().toLowerCase().trim());
            query.setParameter("param4", dictionaryCategory.getLocalAbbreviation().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated
            // yet
            String dictId = "0";
            if (!StringUtil.isNullorNill(dictionaryCategory.getId())) {
                dictId = dictionaryCategory.getId();
            }
            query.setParameter("param3", dictId);

            list = query.list();

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateDictionaryExists()", e);
        }
    }

    @Override
    public DictionaryCategory getDictionaryCategoryByName(String name) throws LIMSRuntimeException {

        String sql = "from DictionaryCategory dc where dc.categoryName = :name";
        try {
            Query<DictionaryCategory> query = entityManager.unwrap(Session.class).createQuery(sql,
                    DictionaryCategory.class);
            query.setParameter("name", name);

            List<DictionaryCategory> categoryList = query.list();

            if (categoryList.size() > 0) {
                return categoryList.get(0);
            }
        } catch (RuntimeException e) {
            handleException(e, "getDictonaryCategoryByName");
        }

        return null;
    }
}
