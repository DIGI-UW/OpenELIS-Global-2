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
package org.openelisglobal.result.dao;

import java.sql.Date;
import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.valueholder.TestResult;

/**
 * @author diane benz
 *         <p>
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface ResultDAO extends BaseDAO<Result, String> {

    // public boolean insertData(Result result) throws LIMSRuntimeException;

    // public void deleteData(List results) throws LIMSRuntimeException;

    List<Result> getAllResults() throws LIMSRuntimeException;

    List<Result> getPageOfResults(int startingRecNo) throws LIMSRuntimeException;

    void getData(Result result) throws LIMSRuntimeException;

    // public void updateData(Result result) throws LIMSRuntimeException;

    Result getResultById(Result result) throws LIMSRuntimeException;

    void getResultByAnalysisAndAnalyte(Result result, Analysis analysis, TestAnalyte ta) throws LIMSRuntimeException;

    void getResultByTestResult(Result result, TestResult testResult) throws LIMSRuntimeException;

    List<Result> getResultsByAnalysis(Analysis analysis) throws LIMSRuntimeException;

    List<Result> getReportableResultsByAnalysis(Analysis analysis) throws LIMSRuntimeException;

    Result getResultForAnalyteInAnalysisSet(String analyteId, List<Integer> analysisIDList) throws LIMSRuntimeException;

    Result getResultForAnalyteAndSampleItem(String analyteId, String sampleItemId) throws LIMSRuntimeException;

    Result getResultById(String resultId) throws LIMSRuntimeException;

    // public void deleteData(Result result) throws LIMSRuntimeException;

    List<Result> getResultsForAnalysisIdList(List<Integer> analysisIdList) throws LIMSRuntimeException;

    List<Result> getResultsForTestAndSample(String sampleId, String testId);

    List<Result> getResultsForSample(Sample sample) throws LIMSRuntimeException;

    List<Result> getChildResults(String resultId) throws LIMSRuntimeException;

    List<Result> getResultsForTestInDateRange(String testId, Date startDate, Date endDate) throws LIMSRuntimeException;

    List<Result> getResultsForPanelInDateRange(String panelId, Date lowDate, Date highDate) throws LIMSRuntimeException;

    List<Result> getResultsForTestSectionInDateRange(String testSectionId, Date lowDate, Date highDate)
            throws LIMSRuntimeException;
}
