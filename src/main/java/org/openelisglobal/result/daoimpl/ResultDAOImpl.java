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
package org.openelisglobal.result.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.result.dao.ResultDAO;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class ResultDAOImpl extends BaseDAOImpl<Result, String> implements ResultDAO {

    public ResultDAOImpl() {
        super(Result.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Result result) throws LIMSRuntimeException {
        try {
            Result re = entityManager.unwrap(Session.class).get(Result.class, result.getId());
            if (re != null) {
                PropertyUtils.copyProperties(result, re);
            } else {
                result.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {

            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Result getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getResultByAnalysisAndAnalyte(Result result, Analysis analysis, TestAnalyte ta)
            throws LIMSRuntimeException {
        List<Result> results;
        try {
            Analyte analyte = ta.getAnalyte();

            String sql = "from Result r where r.analysis = :analysisId and r.analyte = :analyteId";
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            query.setParameter("analysisId", Integer.parseInt(analysis.getId()));
            query.setParameter("analyteId", Integer.parseInt(analyte.getId()));

            results = query.list();
            Result thisResult;
            if (results != null && results.size() > 0) {
                thisResult = results.get(0);
            } else {
                thisResult = null;
            }
            if (thisResult != null) {
                PropertyUtils.copyProperties(result, thisResult);
            } else {
                result.setId(null);
            }

        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {

            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Result getResultByAnalysisAndAnalyte()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsByAnalysis(Analysis analysis) throws LIMSRuntimeException {
        try {

            String sql = "from Result r where r.analysis = :analysisId order by r.id";
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            query.setParameter("analysisId", Integer.parseInt(analysis.getId()));

            List<Result> results = query.list();
            return results;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Result getResultByAnalysis()", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.result.dao.ResultDAO#getResultByTestResult(us
     * .mn.state.health.lims.result.valueholder.Result,
     * org.openelisglobal.testresult.valueholder.TestResult)
     */
    @Override
    @Transactional(readOnly = true)
    public void getResultByTestResult(Result result, TestResult testResult) throws LIMSRuntimeException {
        List<Result> results;
        try {
            String sql = "from Result r where r.testResult = :testResultId";
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            query.setParameter("testResultId", Integer.parseInt(testResult.getId()));

            results = query.list();
            Result thisResult;
            if (results != null && results.size() > 0) {
                thisResult = results.get(0);
            } else {
                thisResult = null;
            }
            if (thisResult != null) {
                PropertyUtils.copyProperties(result, thisResult);
            } else {
                result.setId(null);
            }

        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Result getResultByTestResult()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getAllResults() throws LIMSRuntimeException {
        List<Result> results;
        try {
            String sql = "from Result";
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            results = query.list();
        } catch (RuntimeException e) {

            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Result getAllResults()", e);
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getPageOfResults(int startingRecNo) throws LIMSRuntimeException {
        List<Result> results;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from Result r order by r.id";
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            results = query.list();
        } catch (RuntimeException e) {

            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Result getPageOfResults()", e);
        }

        return results;
    }

    public Result readResult(String idString) {
        Result data;
        try {
            data = entityManager.unwrap(Session.class).get(Result.class, idString);
        } catch (RuntimeException e) {

            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Result readResult()", e);
        }

        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public Result getResultById(Result result) throws LIMSRuntimeException {
        return getResultById(result.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Result getResultById(String resultId) throws LIMSRuntimeException {
        try {
            Result result = entityManager.unwrap(Session.class).get(Result.class, resultId);
            return result;
        } catch (RuntimeException e) {
            handleException(e, "getResultById");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getReportableResultsByAnalysis(Analysis analysis) throws LIMSRuntimeException {
        try {

            String sql = "from Result r where r.analysis = :param1 and r.isReportable = " + enquote(YES);
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            query.setParameter("param1", analysis);

            List<Result> results = query.list();
            return results;

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Result getReportableResultsByAnalysis()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Result getResultForAnalyteInAnalysisSet(String analyteId, List<Integer> analysisIDList)
            throws LIMSRuntimeException {

        try {

            String sql = "from Result r where r.analyte = :analyteId and r.analysis in (:analysisIdList)";
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            query.setParameter("analyteId", Integer.parseInt(analyteId));
            query.setParameterList("analysisIdList", analysisIDList);

            List<Result> results = query.list();
            if (results.size() > 0) {
                return results.get(0);
            }

        } catch (RuntimeException e) {
            handleException(e, "getResultForAnalyteInAnalysisSet");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Result getResultForAnalyteAndSampleItem(String analyteId, String sampleItemId) throws LIMSRuntimeException {

        try {
            String sql = "from Result r where r.analyte.id = :analyteId and r.analysis.sampleItem.id ="
                    + " :sampleItemId";
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            query.setParameter("analyteId", Integer.parseInt(analyteId));
            query.setParameter("sampleItemId", Integer.parseInt(sampleItemId));

            List<Result> results = query.list();

            if (results.size() > 0) {
                return results.get(0);
            }
        } catch (RuntimeException e) {
            handleException(e, "getResultForAnalyteAndSampleItem");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsForAnalysisIdList(List<Integer> analysisIdList) throws LIMSRuntimeException {
        if (analysisIdList.isEmpty()) {
            return null;
        }
        String sql = "from Result r where r.analysis IN (:analysisList)";

        try {
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            query.setParameterList("analysisList", analysisIdList);

            List<Result> resultList = query.list();
            return resultList;
        } catch (HibernateException e) {
            handleException(e, "getResultsForAnalysisIdList");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsForTestAndSample(String sampleId, String testId) {
        String sql = "FROM Result r WHERE r.analysis.sampleItem.sample.id = :sampleId AND r.testResult.test.id ="
                + " :testId";

        try {
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            query.setParameter("testId", Integer.valueOf(testId));
            query.setParameter("sampleId", Integer.valueOf(sampleId));

            List<Result> resultList = query.list();

            return resultList;
        } catch (HibernateException e) {
            handleException(e, "getResultsForTestAndSample");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsForSample(Sample sample) throws LIMSRuntimeException {
        String sql = "From Result r where r.analysis.sampleItem.sample.id = :sampleId";

        try {
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            query.setParameter("sampleId", Integer.parseInt(sample.getId()));
            List<Result> results = query.list();
            return results;
        } catch (HibernateException e) {
            handleException(e, "getResultsForSample");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getChildResults(String resultId) throws LIMSRuntimeException {
        String sql = "From Result r where r.parentResult.id = :parentId";

        try {
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            query.setParameter("parentId", Integer.parseInt(resultId));
            List<Result> results = query.list();
            return results;
        } catch (HibernateException e) {
            handleException(e, "getChildResults");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsForTestInDateRange(String testId, Date startDate, Date endDate)
            throws LIMSRuntimeException {
        String sql = "FROM Result r WHERE r.analysis.test.id = :testId AND r.lastupdated BETWEEN :lowDate AND"
                + " :highDate";

        try {
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            query.setParameter("testId", Integer.valueOf(testId));
            query.setParameter("lowDate", startDate);
            query.setParameter("highDate", endDate);

            List<Result> resultList = query.list();
            return resultList;
        } catch (HibernateException e) {
            handleException(e, "getResultsForTestInDateRange");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsForPanelInDateRange(String panelId, Date lowDate, Date highDate)
            throws LIMSRuntimeException {
        String sql = "FROM Result r WHERE r.analysis.panel.id = :panelId AND r.lastupdated BETWEEN :lowDate AND"
                + " :highDate order by r.id";

        try {
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            query.setParameter("panelId", Integer.valueOf(panelId));
            query.setParameter("lowDate", lowDate);
            query.setParameter("highDate", highDate);

            List<Result> resultList = query.list();
            return resultList;
        } catch (HibernateException e) {
            handleException(e, "getResultsForPanelInDateRange");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Result> getResultsForTestSectionInDateRange(String testSectionId, Date lowDate, Date highDate)
            throws LIMSRuntimeException {
        String sql = "FROM Result r WHERE r.analysis.testSection.id = :testSectionId AND r.lastupdated BETWEEN"
                + " :lowDate AND :highDate";

        try {
            Query<Result> query = entityManager.unwrap(Session.class).createQuery(sql, Result.class);
            query.setParameter("testSectionId", Integer.valueOf(testSectionId));
            query.setParameter("lowDate", lowDate);
            query.setParameter("highDate", highDate);

            List<Result> resultList = query.list();
            return resultList;
        } catch (HibernateException e) {
            handleException(e, "getResultsForTestSectionInDateRange");
        }
        return null;
    }
}
