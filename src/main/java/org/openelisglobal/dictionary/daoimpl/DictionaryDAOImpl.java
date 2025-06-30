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
package org.openelisglobal.dictionary.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.dictionary.dao.DictionaryDAO;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class DictionaryDAOImpl extends BaseDAOImpl<Dictionary, String> implements DictionaryDAO {

    public DictionaryDAOImpl() {
        super(Dictionary.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Dictionary dictionary) throws LIMSRuntimeException {
        try {
            Dictionary d = entityManager.unwrap(Session.class).get(Dictionary.class, dictionary.getId());
            if (d != null) {
                PropertyUtils.copyProperties(dictionary, d);
            } else {
                dictionary.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Dictionary getData()", e);
        }
    }

    // this is for autocomplete
    // modified for 2062
    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> getDictionaryEntrysByCategoryAbbreviation(String filter, String categoryFilter)
            throws LIMSRuntimeException {
        try {
            String sql = "";

            if (filter != null) {
                // bugzilla 1847: use dictEntryDisplayValue
                // bugzilla 2440 sql is incorrect - duplicate entries displaying
                // in dropdown
                if (!StringUtil.isNullorNill(categoryFilter)) {
                    sql = "from Dictionary d "
                            + "where (d.localAbbreviation is not null and upper(d.localAbbreviation) || "
                            + enquote(IActionConstants.LOCAL_CODE_DICT_ENTRY_SEPARATOR_STRING)
                            + " || upper(d.dictEntry) like upper(:param1) and d.isActive= " + enquote(YES)
                            + " and d.dictionaryCategory.categoryName = :param2) OR (d.localAbbreviation is"
                            + " null and upper(d.dictEntry) like upper(:param1) and d.isActive= " + enquote(YES)
                            + " and d.dictionaryCategory.categoryName = :param2)";
                } else {
                    sql = "from Dictionary d "
                            + "where (d.localAbbreviation is not null and upper(d.localAbbreviation) || "
                            + enquote(IActionConstants.LOCAL_CODE_DICT_ENTRY_SEPARATOR_STRING)
                            + " || upper(d.dictEntry) like upper(:param1) and d.isActive= " + enquote(YES)
                            + ") OR (d.localAbbreviation is null and upper(d.dictEntry) like upper(:param1)"
                            + " and d.isActive= " + enquote(YES) + ")";
                }
            } else {
                if (!StringUtil.isNullorNill(categoryFilter)) {
                    sql = "from Dictionary d where d.dictEntry like :param1 and d.isActive= " + enquote(YES)
                            + " and d.dictionaryCategory.categoryName = :param2";
                } else {
                    sql = "from Dictionary d where d.dictEntry like :param1 and d.isActive= " + enquote(YES);
                }
            }

            sql += " order by d.dictEntry asc";
            Query<Dictionary> query = entityManager.unwrap(Session.class).createQuery(sql, Dictionary.class);
            query.setParameter("param1", filter + "%");
            if (!StringUtil.isNullorNill(categoryFilter)) {
                query.setParameter("param2", categoryFilter);
            }

            return query.list();

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException(
                    "Error in Dictionary getDictionaryEntrys(String filter, String categoryFilter)", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> getDictionaryEntrysByCategoryNameLocalizedSort(String categoryName)
            throws LIMSRuntimeException {
        List<Dictionary> entries = getDictionaryEntrysByCategoryAbbreviation("categoryName", categoryName, false);
        BaseObject.sortByLocalizedName(entries);
        return entries;
    }

    /**
     * @see DictionaryDAO#getDictionaryEntrysByCategoryAbbreviation(String, String,
     *      boolean)
     */
    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> getDictionaryEntrysByCategoryAbbreviation(String fieldName, String fieldValue,
            boolean orderByDictEntry) throws LIMSRuntimeException {
        try {
            String sql = "from Dictionary d where d.isActive= " + enquote(YES);

            if (!StringUtil.isNullorNill(fieldValue) && this.columnNameIsInjectionSafe(fieldName)) {
                sql += " and d.dictionaryCategory." + fieldName + " = :param1";
            }

            if (orderByDictEntry) {
                sql += " order by d.dictEntry asc";
            } else {
                sql += " order by d.sortOrder asc";
            }
            Query<Dictionary> query = entityManager.unwrap(Session.class).createQuery(sql, Dictionary.class);
            query.setParameter("param1", fieldValue);

            List<Dictionary> list = query.list();
            return list;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException(
                    "Error in Dictionary getDictionaryEntrysByCategoryAbbreviation(String categoryFilter)", e);
        }
    }

    /*
     * note 1: The case of no category will throw a conversion error for postgres
     * note 2: The error message claims that there is a duplicate entry, it can also
     * be a duplicate abbreviation in the same category
     */
    // not case sensitive hemolysis and Hemolysis are considered duplicates
    // description within category is unique AND local abbreviation within category
    // is unique
    @Override
    public boolean duplicateDictionaryExists(Dictionary dictionary) throws LIMSRuntimeException {
        try {
            String sql = null;
            if (dictionary.getDictionaryCategory() != null) {
                sql = "from Dictionary t where ((trim(lower(t.dictEntry)) = :param and"
                        + " trim(lower(t.dictionaryCategory.categoryName)) = :param2 and t.id != :param3)"
                        + " or (trim(lower(t.localAbbreviation)) = :param4 and"
                        + " trim(lower(t.dictionaryCategory.categoryName)) = :param2 and t.id != :param3))" + " ";

            } else {
                sql = "from Dictionary t where ((trim(lower(t.dictEntry)) = :param and t.dictionaryCategory"
                        + " is null and t.id != :param3) or (trim(lower(t.localAbbreviation)) = :param4 and"
                        + " t.dictionaryCategory is null and t.id != :param3)) ";
            }
            Query<Dictionary> query = entityManager.unwrap(Session.class).createQuery(sql, Dictionary.class);
            query.setParameter("param", dictionary.getDictEntry().toLowerCase().trim());
            query.setParameter("param4", dictionary.getLocalAbbreviation().toLowerCase().trim());
            if (dictionary.getDictionaryCategory() != null) {
                query.setParameter("param2", dictionary.getDictionaryCategory().getCategoryName().toLowerCase().trim());
            }

            // initialize with 0 (for new records where no id has been generated yet
            String dictId = "0";
            if (!StringUtil.isNullorNill(dictionary.getId())) {
                dictId = dictionary.getId();
            }
            query.setParameter("param3", Integer.parseInt(dictId));

            return !query.list().isEmpty();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateDictionaryExists()", e);
        }
    }

    // bugzilla 2061-2063
    @Override
    public boolean isDictionaryFrozen(Dictionary dictionary) throws LIMSRuntimeException {
        try {
            String sql = "";
            // TODO: when we add other tables that reference dictionary we need
            // to check those here also check references from other tables depending on
            // dictionary
            // category local abbrev code
            if (dictionary.getDictionaryCategory().getLocalAbbreviation().equals(
                    ConfigurationProperties.getInstance().getPropertyValue("dictionary.category.qaevent.type"))) {
                sql = "from QaEvent q where q.type = :param";
                // bugzilla 2221: at this time there are only 2 categories as
                // far as this isFrozen() logic:
                // either referenced in QA_EVENT.TYPE OR in TEST_RESULT.VALUE
            } else {
                sql = "from TestResult tr where tr.value = :param and tr.test.isActive = " + enquote(YES);
            }

            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("param", dictionary.getId());

            return !query.list().isEmpty();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in dictionaryIsInUse()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Dictionary getDictionaryById(String dictionaryId) throws LIMSRuntimeException {
        try {
            return entityManager.unwrap(Session.class).get(Dictionary.class, dictionaryId);
        } catch (RuntimeException e) {
            handleException(e, "getDictionaryById");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Dictionary getDataForId(String dictionaryId) throws LIMSRuntimeException {
        String sql = "from Dictionary d where d.id = :id";
        try {
            Query<Dictionary> query = entityManager.unwrap(Session.class).createQuery(sql, Dictionary.class);
            query.setParameter("id", Integer.parseInt(dictionaryId));
            return query.uniqueResult();

        } catch (HibernateException e) {
            handleException(e, "getDataForId");
        }
        return null;
    }
}
