package org.openelisglobal.common.services;

import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Whether a recorded result is the one a rule is about.
 *
 * <p>
 * A result is identified by test AND specimen AND component — the unified test
 * catalogue lets one test hold a component per analyte and run on several
 * specimens, so "the result of test X" names up to a dozen different
 * measurements. Alert, calculated-result and reflex rules all have to answer
 * the same question about the same three axes, and each of them used to answer
 * it by test alone: a rule authored for a numeric Ct Value was handed the coded
 * PCR Result beside it and evaluated against that.
 *
 * <p>
 * Scope is expressed as it is for reference ranges (OGC-1145): null means
 * unscoped, matching every component or every specimen. That is what a rule
 * authored before components existed meant, so those rules keep working
 * untouched.
 */
@Service
public class RuleResultScope {

    @Autowired
    private TestResultComponentService testResultComponentService;

    /**
     * True when {@code result} is the measurement the rule names.
     *
     * @param componentId  the component the rule is about, or null for any
     * @param sampleTypeId the specimen the rule is about, or null for any
     */
    @Transactional(readOnly = true)
    public boolean matches(Result result, String componentId, String sampleTypeId) {
        if (result == null) {
            return false;
        }
        return matchesComponent(result, componentId) && matchesSampleType(result.getAnalysis(), sampleTypeId);
    }

    /** True when the analysis was run on the specimen the rule names. */
    public boolean matchesSampleType(Analysis analysis, String sampleTypeId) {
        if (GenericValidator.isBlankOrNull(sampleTypeId)) {
            return true;
        }
        if (analysis == null || analysis.getSampleItem() == null) {
            return false;
        }
        return sampleTypeId.equals(analysis.getSampleItem().getTypeOfSampleId());
    }

    /** True when the result belongs to the component the rule names. */
    @Transactional(readOnly = true)
    public boolean matchesComponent(Result result, String componentId) {
        if (GenericValidator.isBlankOrNull(componentId)) {
            return true;
        }
        return componentId.equals(componentIdOf(result));
    }

    /**
     * The component a result belongs to. A result points at its component through
     * the test_result row it was bound to on save; a row written before components
     * existed carries none, and belongs to the primary — the same reading Results
     * Entry and reference-range selection already apply, so a rule cannot disagree
     * with the screen the result was entered on.
     */
    @Transactional(readOnly = true)
    public String componentIdOf(Result result) {
        if (result == null || result.getTestResult() == null) {
            return null;
        }
        String componentId = result.getTestResult().getComponentId();
        if (!GenericValidator.isBlankOrNull(componentId)) {
            return componentId;
        }
        org.openelisglobal.test.valueholder.Test test = result.getTestResult().getTest();
        return test == null ? null : primaryComponentId(test.getId());
    }

    /** The id of a test's primary component, or null when it has none. */
    @Transactional(readOnly = true)
    public String primaryComponentId(String testId) {
        if (GenericValidator.isBlankOrNull(testId)) {
            return null;
        }
        List<TestResultComponent> components = testResultComponentService.getActiveComponentsByTestId(testId);
        if (components == null || components.isEmpty()) {
            return null;
        }
        for (TestResultComponent component : components) {
            if (component.getIsPrimary()) {
                return component.getId();
            }
        }
        return components.get(0).getId();
    }

    /**
     * The result type the rule's operators have to be valid against: the
     * component's own type when it declares one, the test's only otherwise. A
     * multi-component test has no single type — pairing a numeric operator with the
     * parent test's coded type is how a Ct rule ends up unable to fire.
     */
    @Transactional(readOnly = true)
    public String resultTypeForComponent(String testId, String componentId, String testLevelType) {
        if (GenericValidator.isBlankOrNull(componentId)) {
            return testLevelType;
        }
        List<TestResultComponent> components = testResultComponentService.getActiveComponentsByTestId(testId);
        if (components == null) {
            return testLevelType;
        }
        for (TestResultComponent component : components) {
            if (componentId.equals(component.getId())) {
                return GenericValidator.isBlankOrNull(component.getResultType()) ? testLevelType
                        : component.getResultType();
            }
        }
        return testLevelType;
    }
}
