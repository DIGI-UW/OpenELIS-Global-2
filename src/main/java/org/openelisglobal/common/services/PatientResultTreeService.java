package org.openelisglobal.common.services;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.rest.provider.bean.patientHistory.PanelDisplay;
import org.openelisglobal.common.rest.provider.bean.patientHistory.ResultDisplay;
import org.openelisglobal.common.rest.provider.bean.patientHistory.ResultTree;
import org.openelisglobal.common.rest.provider.bean.patientHistory.TestDisplay;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the Patient History result tree (test section &rarr; sample type
 * &rarr; test) shown by the patient dashboard timeline and trendline.
 *
 * <p>
 * A test can be configured with several sample types and several result
 * components (OGC-949/OGC-1127), so a row of the tree is one (test, sample
 * type, component) triple rather than one test: two components of the same test
 * hold two independent results, each with its own value, unit and reference
 * range, and must never collapse into a single row.
 *
 * <p>
 * Reference ranges come from
 * {@link ResultLimitService#getResultLimitForResult}, the same selection
 * Results Entry and Result Validation use, so the range shown here is filtered
 * by the patient's age and gender and scoped to the specimen and component
 * exactly as it is everywhere else.
 */
@Service
public class PatientResultTreeService {

    // Second precision: the timeline draws one column per observation time, and
    // sub-second differences between rows saved together would split one save
    // into several columns that all read the same to the user.
    private static final String OBS_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String RANGE_SEPARATOR = " - ";
    private static final String COMPONENT_SEPARATOR = " — ";

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private TestService testService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private TestResultComponentService testResultComponentService;

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    @Transactional(readOnly = true)
    public List<ResultTree> getResultTree(String patientId) {
        Patient patient = patientService.get(patientId);
        Map<String, SectionNode> sections = new LinkedHashMap<>();
        Map<String, List<TestResultComponent>> componentsByTest = new LinkedHashMap<>();

        for (Result result : resultsForPatient(patientId)) {
            Analysis analysis = result.getAnalysis();
            TestSection section = analysis.getTest().getTestSection();
            TypeOfSample sampleType = analysis.getSampleItem() == null ? null
                    : analysis.getSampleItem().getTypeOfSample();
            if (section == null || sampleType == null) {
                continue;
            }
            SectionNode sectionNode = sections.computeIfAbsent(section.getId(), key -> new SectionNode(section));
            PanelNode panelNode = sectionNode.panels.computeIfAbsent(sampleType.getId(),
                    key -> new PanelNode(sampleType));
            addToGroup(panelNode, result, analysis, sampleType, componentsByTest);
        }

        List<ResultTree> resultTrees = new ArrayList<>();
        for (SectionNode sectionNode : sections.values()) {
            List<PanelDisplay> panelDisplays = new ArrayList<>();
            for (PanelNode panelNode : sectionNode.panels.values()) {
                PanelDisplay panelDisplay = new PanelDisplay();
                panelDisplay.setDisplay(panelNode.sampleType.getLocalizedName());
                panelDisplay.setSubSets(buildTestDisplays(panelNode, patient));
                panelDisplays.add(panelDisplay);
            }
            ResultTree resultTree = new ResultTree();
            resultTree.setDisplay(sectionNode.section.getLocalizedName());
            resultTree.setSubSets(panelDisplays);
            resultTrees.add(resultTree);
        }
        return resultTrees;
    }

    /**
     * The trendline feed: every (sample type, component) series recorded for one
     * test, optionally narrowed to a single component.
     */
    @Transactional(readOnly = true)
    public PanelDisplay getTestResultTree(String patientId, String testId, String componentId) {
        Test test = testService.get(testId.trim());
        if (test == null) {
            return null;
        }
        Patient patient = patientService.get(patientId);
        Map<String, List<TestResultComponent>> componentsByTest = new LinkedHashMap<>();
        Map<String, PanelNode> panels = new LinkedHashMap<>();

        for (Result result : resultsForPatient(patientId)) {
            Analysis analysis = result.getAnalysis();
            if (!test.getId().equals(analysis.getTest().getId())) {
                continue;
            }
            TypeOfSample sampleType = analysis.getSampleItem() == null ? null
                    : analysis.getSampleItem().getTypeOfSample();
            if (sampleType == null) {
                continue;
            }
            PanelNode panelNode = panels.computeIfAbsent(sampleType.getId(), key -> new PanelNode(sampleType));
            addToGroup(panelNode, result, analysis, sampleType, componentsByTest);
        }

        List<TestDisplay> testDisplays = new ArrayList<>();
        for (PanelNode panelNode : panels.values()) {
            for (TestDisplay testDisplay : buildTestDisplays(panelNode, patient)) {
                if (GenericValidator.isBlankOrNull(componentId) || componentId.equals(testDisplay.getComponentId())) {
                    testDisplays.add(testDisplay);
                }
            }
        }

        PanelDisplay panelDisplay = new PanelDisplay();
        panelDisplay.setDisplay(test.getLocalizedName());
        panelDisplay.setSubSets(testDisplays);
        return panelDisplay;
    }

    /**
     * The patient's results that stand on their own. A child result carries part of
     * its parent's value (the quantification of a dictionary answer, the extra
     * selections of a multi-select) and is reported with it, so it is not a row of
     * its own — the same rule Results Entry applies.
     */
    private List<Result> resultsForPatient(String patientId) {
        List<Result> results = new ArrayList<>();
        for (Sample sample : sampleHumanService.getSamplesForPatient(patientId)) {
            for (Result result : resultService.getResultsForSample(sample)) {
                if (result.getParentResult() != null) {
                    continue;
                }
                Analysis analysis = result.getAnalysis();
                if (analysis == null || analysis.getTest() == null) {
                    continue;
                }
                results.add(result);
            }
        }
        return results;
    }

    private void addToGroup(PanelNode panelNode, Result result, Analysis analysis, TypeOfSample sampleType,
            Map<String, List<TestResultComponent>> componentsByTest) {
        Test test = analysis.getTest();
        List<TestResultComponent> components = componentsByTest.computeIfAbsent(test.getId(),
                testResultComponentService::getActiveComponentsByTestId);
        TestResultComponent component = resolveComponent(result, components);
        String groupKey = test.getId() + "|" + (component == null ? "" : component.getId());
        ResultGroup group = panelNode.groups.computeIfAbsent(groupKey,
                key -> new ResultGroup(test, component, sampleType));
        group.results.add(result);
    }

    /**
     * The component a result belongs to, or null when the test is not
     * multi-component and therefore has only test-level configuration. A result
     * points at its component through its test_result row; legacy rows with no
     * component id belong to the primary.
     */
    private TestResultComponent resolveComponent(Result result, List<TestResultComponent> components) {
        if (components.size() < 2) {
            return null;
        }
        String componentId = result.getTestResult() == null ? null : result.getTestResult().getComponentId();
        if (componentId != null) {
            for (TestResultComponent component : components) {
                if (componentId.equals(component.getId())) {
                    return component;
                }
            }
        }
        for (TestResultComponent component : components) {
            if (component.getIsPrimary()) {
                return component;
            }
        }
        return components.get(0);
    }

    private List<TestDisplay> buildTestDisplays(PanelNode panelNode, Patient patient) {
        List<TestDisplay> testDisplays = new ArrayList<>();
        for (ResultGroup group : panelNode.groups.values()) {
            testDisplays.add(buildTestDisplay(group, patient));
        }
        return testDisplays;
    }

    private TestDisplay buildTestDisplay(ResultGroup group, Patient patient) {
        Test test = group.test;
        TestResultComponent component = group.component;
        String componentId = component == null ? null : component.getId();
        String configuredType = configuredResultType(test, component);

        List<ResultDisplay> resultDisplays = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat(OBS_DATE_FORMAT);
        for (Result result : group.results) {
            ResultDisplay resultDisplay = new ResultDisplay();
            resultDisplay.setValue(displayValue(result, configuredType));
            resultDisplay.setObsDatetime(observationDate(result, dateFormat));
            resultDisplays.add(resultDisplay);
        }

        Result representative = group.results.get(0);
        ResultLimit resultLimit = resultLimitService.getResultLimitForResult(representative.getAnalysis(),
                representative, patient, componentId);

        TestDisplay testDisplay = new TestDisplay();
        testDisplay.setTestName(test.getLocalizedName());
        testDisplay.setComponent(component == null ? null : component.getLabel());
        testDisplay.setComponentId(componentId);
        testDisplay.setSampleType(group.sampleType.getLocalizedName());
        testDisplay.setDisplay(
                component == null || GenericValidator.isBlankOrNull(component.getLabel()) ? test.getLocalizedName()
                        : test.getLocalizedName() + COMPONENT_SEPARATOR + component.getLabel());
        testDisplay.setConceptUuid(test.getId());
        testDisplay.setDatatype(configuredType);
        testDisplay.setUnits(unitOfMeasure(test, component));
        applyResultLimit(testDisplay, resultLimit, significantDigits(test, component));
        testDisplay.setObs(resultDisplays);
        return testDisplay;
    }

    /**
     * The reference range and the thresholds the timeline colours results against.
     * An unset bound is left null rather than folded onto a neighbouring one — a
     * missing critical limit must not make every out-of-range value critical.
     */
    private void applyResultLimit(TestDisplay testDisplay, ResultLimit resultLimit, String significantDigits) {
        if (resultLimit == null) {
            return;
        }
        testDisplay.setLowNormal(finiteOrNull(resultLimit.getLowNormal()));
        testDisplay.setHiNormal(finiteOrNull(resultLimit.getHighNormal()));
        testDisplay.setLowCritical(finiteOrNull(resultLimit.getLowCritical()));
        testDisplay.setHiCritical(finiteOrNull(resultLimit.getHighCritical()));
        testDisplay.setLowAbsolute(finiteOrNull(resultLimit.getLowValid()));
        testDisplay.setHiAbsolute(finiteOrNull(resultLimit.getHighValid()));
        testDisplay
                .setRange(resultLimitService.getDisplayReferenceRange(resultLimit, significantDigits, RANGE_SEPARATOR));
    }

    private Double finiteOrNull(double value) {
        return Double.isInfinite(value) || Double.isNaN(value) ? null : value;
    }

    /**
     * When the observation was made, so results recorded together line up in one
     * timeline column: the analysis completion stamps every component of the same
     * test at once, where each result row's own save time does not. Blank for the
     * dateless row a timeline cannot place.
     */
    private String observationDate(Result result, SimpleDateFormat dateFormat) {
        Analysis analysis = result.getAnalysis();
        java.util.Date date = analysis != null && analysis.getCompletedDate() != null
                ? new java.util.Date(analysis.getCompletedDate().getTime())
                : result.getLastupdated();
        return date == null ? "" : dateFormat.format(date);
    }

    private String displayValue(Result result, String configuredType) {
        if (GenericValidator.isBlankOrNull(result.getValue())) {
            return "";
        }
        String resultType = GenericValidator.isBlankOrNull(result.getResultType()) ? configuredType
                : result.getResultType();
        if (ResultType.isDictionaryVariant(resultType)) {
            Dictionary dictionary = dictionaryService.getDataForId(result.getValue());
            return dictionary == null ? "" : dictionary.getLocalizedName();
        }
        return resultService.getSimpleResultValue(result);
    }

    private String configuredResultType(Test test, TestResultComponent component) {
        if (component != null && !GenericValidator.isBlankOrNull(component.getResultType())) {
            return component.getResultType();
        }
        return testService.getResultType(test);
    }

    private String unitOfMeasure(Test test, TestResultComponent component) {
        if (component != null) {
            if (component.getUomId() == null) {
                return "";
            }
            UnitOfMeasure uom = unitOfMeasureService.getUnitOfMeasureById(component.getUomId());
            return uom == null || uom.getUnitOfMeasureName() == null ? "" : uom.getUnitOfMeasureName();
        }
        return test.getUnitOfMeasure() == null ? "" : test.getUnitOfMeasure().getName();
    }

    /** The precision the component's own result options are configured with. */
    private String significantDigits(Test test, TestResultComponent component) {
        for (TestResult testResult : testService.getPossibleTestResults(test)) {
            if (component == null || component.getId().equals(testResult.getComponentId())
                    || (component.getIsPrimary() && testResult.getComponentId() == null)) {
                return GenericValidator.isBlankOrNull(testResult.getSignificantDigits()) ? "0"
                        : testResult.getSignificantDigits();
            }
        }
        return "0";
    }

    private static final class SectionNode {
        private final TestSection section;
        private final Map<String, PanelNode> panels = new LinkedHashMap<>();

        private SectionNode(TestSection section) {
            this.section = section;
        }
    }

    private static final class PanelNode {
        private final TypeOfSample sampleType;
        private final Map<String, ResultGroup> groups = new LinkedHashMap<>();

        private PanelNode(TypeOfSample sampleType) {
            this.sampleType = sampleType;
        }
    }

    private static final class ResultGroup {
        private final Test test;
        private final TestResultComponent component;
        private final TypeOfSample sampleType;
        private final List<Result> results = new ArrayList<>();

        private ResultGroup(Test test, TestResultComponent component, TypeOfSample sampleType) {
            this.test = test;
            this.component = component;
            this.sampleType = sampleType;
        }
    }
}
