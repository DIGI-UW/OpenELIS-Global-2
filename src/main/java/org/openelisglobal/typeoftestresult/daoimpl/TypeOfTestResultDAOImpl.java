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
package org.openelisglobal.typeoftestresult.daoimpl;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.typeoftestresult.dao.TypeOfTestResultDAO;
import org.openelisglobal.typeoftestresult.valueholder.TypeOfTestResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class TypeOfTestResultDAOImpl extends BaseDAOImpl<TypeOfTestResult, String> implements TypeOfTestResultDAO {

    public TypeOfTestResultDAOImpl() {
        super(TypeOfTestResult.class);
    }

    // bugzilla 1482
    @Override
    public boolean duplicateTypeOfTestResultExists(TypeOfTestResult typeOfTestResult) throws LIMSRuntimeException {
        try {

            List<TypeOfTestResult> list;

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from TypeOfTestResult t where (trim(lower(t.description)) = :param and t.id != :param2)"
                    + " or (trim(lower(t.testResultType)) = :param3 and t.id != :param2)";
            Query<TypeOfTestResult> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TypeOfTestResult.class);
            query.setParameter("param", typeOfTestResult.getDescription().toLowerCase().trim());
            query.setParameter("param3", typeOfTestResult.getTestResultType().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated
            // yet
            String typeOfTestResultId = "0";
            if (!StringUtil.isNullorNill(typeOfTestResult.getId())) {
                typeOfTestResultId = typeOfTestResult.getId();
            }
            query.setParameter("param2", typeOfTestResultId);

            list = query.list();
            return list.size() > 0;
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateTypeOfTestResultExists()", e);
        }
    }

    // bugzilla 1866 to get HL7 value
    @Override
    @Transactional(readOnly = true)
    public TypeOfTestResult getTypeOfTestResultByType(TypeOfTestResult typeOfTestResult) throws LIMSRuntimeException {
        TypeOfTestResult totr = null;
        try {
            String sql = "from TypeOfTestResult totr where upper(totr.testResultType) = :param";
            Query<TypeOfTestResult> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TypeOfTestResult.class);
            query.setParameter("param", typeOfTestResult.getTestResultType().trim().toUpperCase());

            List<TypeOfTestResult> list = query.list();

            if (list != null && list.size() > 0) {
                totr = list.get(0);
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getTypeOfTestResultByType()", e);
        }

        return totr;
    }
}
