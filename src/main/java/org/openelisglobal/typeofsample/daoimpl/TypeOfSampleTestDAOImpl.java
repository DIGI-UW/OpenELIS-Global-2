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
package org.openelisglobal.typeofsample.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.typeofsample.dao.TypeOfSampleTestDAO;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TypeOfSampleTestDAOImpl extends BaseDAOImpl<TypeOfSampleTest, String> implements TypeOfSampleTestDAO {

    public TypeOfSampleTestDAOImpl() {
        super(TypeOfSampleTest.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TypeOfSampleTest typeOfSample) throws LIMSRuntimeException {
        try {
            TypeOfSampleTest tos = entityManager.unwrap(Session.class).get(TypeOfSampleTest.class,
                    typeOfSample.getId());
            if (tos != null) {
                PropertyUtils.copyProperties(typeOfSample, tos);
            } else {
                typeOfSample.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {

            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TypeOfSampleTest getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSampleTest> getAllTypeOfSampleTests() throws LIMSRuntimeException {

        List<TypeOfSampleTest> list;

        try {
            String sql = "from TypeOfSampleTest";
            Query<TypeOfSampleTest> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TypeOfSampleTest.class);
            // query.setMaxResults(10);
            // query.setFirstResult(3);
            list = query.list();
        } catch (RuntimeException e) {

            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TypeOfSampleTest getAllTypeOfSamples()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSampleTest> getPageOfTypeOfSampleTests(int startingRecNo) throws LIMSRuntimeException {
        List<TypeOfSampleTest> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                    + 1;

            String sql = "from TypeOfSampleTest t order by t.typeOfSampleId, t.testId";
            Query<TypeOfSampleTest> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TypeOfSampleTest.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TypeOfSampleTest getPageOfTypeOfSamples()", e);
        }

        return list;
    }

    public TypeOfSampleTest readTypeOfSample(String idString) {
        TypeOfSampleTest tos;
        try {
            tos = entityManager.unwrap(Session.class).get(TypeOfSampleTest.class, idString);
        } catch (RuntimeException e) {

            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TypeOfSampleTest readTypeOfSample()", e);
        }

        return tos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSampleTest> getTypeOfSampleTestsForSampleType(String sampleTypeId) throws LIMSRuntimeException {
        String sql = "from TypeOfSampleTest tt where tt.typeOfSampleId = :sampleId";

        try {
            if (sampleTypeId.equals("null")) {
                // so parseInt doesn't throw
                sampleTypeId = "0";
            }
            Query<TypeOfSampleTest> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TypeOfSampleTest.class);
            query.setParameter("sampleId", Integer.parseInt(sampleTypeId));
            List<TypeOfSampleTest> list = query.list();
            return list;
        } catch (RuntimeException e) {
            handleException(e, "getTypeOfSampleTestsForSampleType");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public TypeOfSampleTest getTypeOfSampleTestForTest(String testId) throws LIMSRuntimeException {

        String sql = "from TypeOfSampleTest tt where tt.testId = :testId";

        try {
            Query<TypeOfSampleTest> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TypeOfSampleTest.class);
            query.setParameter("testId", Integer.parseInt(testId));
            List<TypeOfSampleTest> list = query.list();
            return list.size() > 0 ? list.get(0) : null;
        } catch (RuntimeException e) {
            handleException(e, "getTypeOfSampleTestForTest");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSampleTest> getTypeOfSampleTestsForTest(String testId) throws LIMSRuntimeException {
        String sql = "from TypeOfSampleTest tt where tt.testId = :testId";

        try {
            Query<TypeOfSampleTest> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TypeOfSampleTest.class);
            query.setParameter("testId", Integer.parseInt(testId));
            List<TypeOfSampleTest> list = query.list();
            return list;
        } catch (RuntimeException e) {
            handleException(e, "getTypeOfSampleTestsForTest");
        }
        return null;
    }

    @Override
    public Integer getTotalTypeOfSampleTestCount() throws LIMSRuntimeException {
        return this.getCount();
    }
}
