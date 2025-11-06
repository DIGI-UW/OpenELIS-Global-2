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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.testreflex.action.util;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.scriptlet.service.ScriptletService;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testreflex.valueholder.TestReflex;

public abstract class ReflexAction {

    protected static final String INTERPERET_TYPE = "I";

    private Analysis generatedAnalysis;
    private boolean flagAction = false;

    private String flag;
    protected ObservationHistory observation;
    protected TestReflex reflex;
    protected Result result;
    protected Result finalResult;

    protected static AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    private TestService testService = SpringContext.getBean(TestService.class);
    private ScriptletService scriptletService = SpringContext.getBean(ScriptletService.class);

    /*
     * Creates a new Analysis from a testReflex based on the current analysis of the
     * result. Points to the same sample, and sets parent child relationship.
     */
    public void handleReflex(TestReflex reflex, Result result, String actionSelectionId) {
        this.reflex = reflex;
        this.result = result;

        String flag = reflex.getFlags();
        if (!GenericValidator.isBlankOrNull(flag)) {
            handleFlagAction(reflex, actionSelectionId);
        } else {
            handleTestsAndScriptlets(reflex);
        }
    }

    private void handleTestsAndScriptlets(TestReflex reflex) {
        Test test = reflex.getAddedTest();
        if (test != null) {
            createReflexedAnalysis(test);
        }

        Scriptlet scriptlet = reflex.getActionScriptlet();
        if (scriptlet != null) {
            handleScriptletAction(scriptlet);
        }
    }

    private void handleTestAction(String testId) {
        createReflexedAnalysis(testService.getActiveTestById(Integer.parseInt(testId)));
    }

    protected void createReflexedAnalysis(Test test) {
        if (test != null) {
            Analysis currentAnalysis = result.getAnalysis();
            analysisService.getData(currentAnalysis);

            generatedAnalysis = new Analysis();
            generatedAnalysis.setTest(test);
            generatedAnalysis.setIsReportable(currentAnalysis.getIsReportable());
            generatedAnalysis.setAnalysisType(currentAnalysis.getAnalysisType());
            generatedAnalysis.setRevision(currentAnalysis.getRevision());
            generatedAnalysis.setStartedDate(DateUtil.getNowAsSqlDate());
            generatedAnalysis
                    .setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted));
            generatedAnalysis.setParentAnalysis(currentAnalysis);
            generatedAnalysis.setParentResult(result);
            generatedAnalysis.setSampleItem(currentAnalysis.getSampleItem());
            generatedAnalysis.setTestSection(currentAnalysis.getTestSection());
            generatedAnalysis.setSampleTypeName(currentAnalysis.getSampleTypeName());
        }
    }

    /*
     * This method should respond to directions from the flag
     */
    protected void handleFlagAction(TestReflex reflex, String actionSelectionId) {
        if (TestReflexUtil.isUserChoiceReflex(reflex) && actionSelectionId != null) {
            String[] parsedSelection = actionSelectionId.split("_");

            if ("script".equals(parsedSelection[0])) {
                handleScriptletAction(parsedSelection[1]);
            } else if ("test".equals(parsedSelection[0])) {
                handleTestAction(parsedSelection[1]);
            }
        }
    }

    private void handleScriptletAction(String scriptletId) {
        handleScriptletAction(scriptletService.getScriptletById(scriptletId));
    }

    protected abstract void handleScriptletAction(Scriptlet scriptlet);

    public Analysis getNewAnalysis() {
        return generatedAnalysis;
    }

    public boolean isFlagAction() {
        return flagAction;
    }

    public String getFlag() {
        return flag;
    }

    public ObservationHistory getObservation() {
        return observation;
    }

    public TestReflex getReflex() {
        return reflex;
    }

    public Result getFinalResult() {
        return finalResult;
    }
}
