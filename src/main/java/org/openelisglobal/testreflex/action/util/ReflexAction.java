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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.RuleResultScope;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.scriptlet.service.ScriptletService;
import org.openelisglobal.scriptlet.valueholder.Scriptlet;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

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
            generatedAnalysis.setStartedDate(DateUtil.getNowAsTimestamp());
            generatedAnalysis
                    .setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted));
            generatedAnalysis.setParentAnalysis(currentAnalysis);
            generatedAnalysis.setParentResult(result);
            SampleItem targetItem = sampleItemForGeneratedTest(test, currentAnalysis);
            if (targetItem != null) {
                generatedAnalysis.setSampleItem(targetItem);
                generatedAnalysis.setSampleTypeName(
                        targetItem.getTypeOfSample() == null ? null : targetItem.getTypeOfSample().getLocalizedName());
            } else {
                generatedAnalysis.setSampleItem(currentAnalysis.getSampleItem());
                generatedAnalysis.setSampleTypeName(currentAnalysis.getSampleTypeName());
            }
            generatedAnalysis.setTestSection(currentAnalysis.getTestSection());
        }
    }

    /**
     * The specimen the reflexed test is reported on.
     *
     * <p>
     * The rule says so. The builder collects a specimen alongside the test to add,
     * and that pairing is the lab's instruction about where the generated result
     * belongs: trigger on Respiratory Swab, report on DBS. The order is given that
     * specimen when it does not already hold one, because the generated test is the
     * reason to have it.
     *
     * <p>
     * Read from add_sample_type_id and never from sample_type_id - the latter
     * scopes which result triggers the rule, and reading it here would file the
     * generated test against the specimen that fired it, which is the conflation
     * the two columns exist to prevent.
     *
     * <p>
     * Only where the rule names no target specimen is one inferred from the added
     * test's own configuration, and then only when the answer is unambiguous: a
     * test the order holds several eligible specimens for names no single one, and
     * the triggering specimen stays the safer choice over guessing between them.
     */
    private SampleItem sampleItemForGeneratedTest(Test test, Analysis currentAnalysis) {
        if (test == null || currentAnalysis == null || currentAnalysis.getSampleItem() == null) {
            return null;
        }
        if (reflex != null && !GenericValidator.isBlankOrNull(reflex.getAddedSampleTypeId())) {
            return SpringContext.getBean(RuleResultScope.class).resolveOrCreateSampleItemForTarget(
                    currentAnalysis.getSampleItem().getSample(), reflex.getAddedSampleTypeId(), result.getSysUserId());
        }
        List<TypeOfSample> configured = SpringContext.getBean(TypeOfSampleService.class)
                .getTypeOfSampleForTest(test.getId());
        if (configured == null || configured.isEmpty()) {
            return null;
        }
        Set<String> allowed = new HashSet<>();
        configured.forEach(type -> allowed.add(type.getId()));
        List<SampleItem> items = SpringContext.getBean(SampleItemService.class)
                .getSampleItemsBySampleId(currentAnalysis.getSampleItem().getSample().getId());
        if (items == null) {
            return null;
        }
        SampleItem match = null;
        for (SampleItem item : items) {
            if (allowed.contains(item.getTypeOfSampleId())) {
                if (match != null) {
                    return null;
                }
                match = item;
            }
        }
        if (match == null && configured.size() == 1) {
            return SpringContext.getBean(RuleResultScope.class).resolveOrCreateSampleItemForTarget(
                    currentAnalysis.getSampleItem().getSample(), configured.get(0).getId(), result.getSysUserId());
        }
        return match;
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
