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
package org.openelisglobal.testreflex.action.util;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.result.action.util.ResultUtil;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/*
 * The purpose of this class is to resolve whether a new test should be created for
 * a sample based on the results of previous tests.  There can be the case that more
 * than one test result can trigger a new test or more than one new test is created
 */
@Service
@Scope("prototype")
public class TestReflexResolver {

    @Autowired
    private TestReflexService testReflexService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;

    private Analysis lastValidAnalysis = null;

    public Analysis getLastValidAnalysis() {
        return lastValidAnalysis;
    }

    /*
     * Gets the test reflex associated with this test. Depends on the analyte, test
     * result and test. This could return zero or more reflexes. More than one
     * reflexes will be returned when there is more than one reflex for a test,
     * analyte and result combo
     */
    public List<TestReflex> getTestReflexesForResult(Result result) {
        String testResultId = null;
        String testId = null;
        String analyteId = result.getAnalyte() == null ? null : result.getAnalyte().getId();

        if (result.getTestResult() != null) {
            testResultId = result.getTestResult().getId();
            testId = result.getTestResult().getTest() == null ? null : result.getTestResult().getTest().getId();
        }

        List<TestReflex> reflexes = testReflexService.getTestReflexsByTestResultAnalyteTest(testResultId, analyteId,
                testId);
        // try to check if there other analyte macthicng for this result
        List<Analyte> otherMatchingAnalyte = ResultUtil.getOtherAnalyteForResult(result);
        if (otherMatchingAnalyte != null) {
            if (!otherMatchingAnalyte.isEmpty()) {
                for (Analyte otherAnalyte : otherMatchingAnalyte) {
                    reflexes.addAll(testReflexService.getTestReflexsByTestResultAnalyteTest(testResultId,
                            otherAnalyte.getId(), testId));
                }
            }
        }

        return reflexes != null ? reflexes : new ArrayList<>();
    }

    public List<TestReflex> getTestReflexsByAnalyteAndTest(Result result) {
        String testId = null;
        String analyteId = result.getAnalyte() == null ? null : result.getAnalyte().getId();

        if (result.getTestResult() != null) {
            testId = result.getTestResult().getTest() == null ? null : result.getTestResult().getTest().getId();
        }

        List<TestReflex> reflexes = testReflexService.getTestReflexsByAnalyteAndTest(analyteId, testId);
        // try to check if there other analyte macthicng for this result
        List<Analyte> otherMatchingAnalyte = ResultUtil.getOtherAnalyteForResult(result);
        if (otherMatchingAnalyte != null) {
            if (!otherMatchingAnalyte.isEmpty()) {
                for (Analyte otherAnalyte : otherMatchingAnalyte) {
                    reflexes.addAll(testReflexService.getTestReflexsByAnalyteAndTest(otherAnalyte.getId(), testId));
                }
            }
        }
        return reflexes != null ? reflexes : new ArrayList<>();
    }

    public ReflexAction getReflexAction() {
        return ReflexActionFactory.getReflexAction();
    }

    public boolean isSatisfied(TestReflex reflex, Sample sample) {

        List<Analysis> analysisList = analysisService.getAnalysesBySampleId(sample.getId());

        for (Analysis analysis : analysisList) {
            if (!SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected)
                    .equals(analysis.getStatusId())) {
                List<Result> resultList = resultService.getResultsByAnalysis(analysis);

                for (Result result : resultList) {
                    if (result.getTestResult() != null
                            && reflex.getTestResultId().equals(result.getTestResult().getId())
                            && result.getAnalyte() != null
                            && reflex.getTestAnalyte().getAnalyte().getId().equals(result.getAnalyte().getId())
                            && reflex.getTestId().equals(analysis.getTest().getId())) {

                        lastValidAnalysis = analysis;
                        return true;
                    }
                }
            }
        }

        lastValidAnalysis = null;
        return false;
    }
}
