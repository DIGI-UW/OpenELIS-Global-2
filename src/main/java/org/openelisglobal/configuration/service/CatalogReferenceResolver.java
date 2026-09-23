package org.openelisglobal.configuration.service;

import java.util.List;
import org.openelisglobal.configuration.valueholder.UnresolvedReference;
import org.openelisglobal.test.service.LegacyTestVariantFinder;
import org.openelisglobal.test.service.LegacyTestVariantFinder.LegacyTestVariant;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * How catalog files name the records they refer to. Every lookup tries the
 * catalog's own names first, then the aliases remembered from earlier
 * decisions; a name that still resolves to nothing is noted for the "Needs your
 * decision" queue (see {@link ImportRunContext}) and reported as null so the
 * caller skips its row with a reason.
 */
@Service
public class CatalogReferenceResolver {

    @Autowired
    private TestService testService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    @Autowired
    private TestResultComponentService componentService;

    @Autowired
    private LegacyTestVariantFinder legacyVariantFinder;

    @Autowired
    private ReferenceAliasService aliasService;

    /**
     * The test a file names: by plain description (exact, then normalized), by
     * local code, by a remembered alias, and finally, when the row names a
     * specimen, the legacy {@code Name(Specimen)} record for that specimen.
     */
    public Test resolveTest(String name, String sampleTypeName, String context) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Test test = testService.getTestByDescription(name);
        if (test == null) {
            test = testService.getTestByNormalizedDescription(name);
        }
        if (test == null) {
            test = testService.getTestByLocalCode(name);
        }
        if (test == null) {
            String aliased = aliasService.resolve(UnresolvedReference.TYPE_TEST, name);
            if (aliased != null) {
                test = testService.get(aliased);
            }
        }
        if (test == null && sampleTypeName != null && !sampleTypeName.isBlank()) {
            TypeOfSample specimen = findSampleType(sampleTypeName);
            if (specimen != null) {
                for (LegacyTestVariant variant : legacyVariantFinder.find(name)) {
                    if (variant.specimen().getId().equals(specimen.getId())) {
                        test = variant.test();
                        break;
                    }
                }
            }
        }
        if (test == null) {
            ImportRunContext.addPending(UnresolvedReference.TYPE_TEST, name, context);
        }
        return test;
    }

    public TypeOfSample resolveSampleType(String name, String context) {
        if (name == null || name.isBlank()) {
            return null;
        }
        TypeOfSample sampleType = findSampleType(name);
        if (sampleType == null) {
            String aliased = aliasService.resolve(UnresolvedReference.TYPE_SAMPLE_TYPE, name);
            if (aliased != null) {
                sampleType = typeOfSampleService.getTypeOfSampleById(aliased);
            }
        }
        if (sampleType == null) {
            ImportRunContext.addPending(UnresolvedReference.TYPE_SAMPLE_TYPE, name, context);
        }
        return sampleType;
    }

    public TestSection resolveTestSection(String name, String context) {
        if (name == null || name.isBlank()) {
            return null;
        }
        TestSection section = testSectionService.getTestSectionByName(name);
        if (section == null) {
            String aliased = aliasService.resolve(UnresolvedReference.TYPE_TEST_SECTION, name);
            if (aliased != null) {
                section = testSectionService.getTestSectionById(aliased);
            }
        }
        if (section == null) {
            ImportRunContext.addPending(UnresolvedReference.TYPE_TEST_SECTION, name, context);
        }
        return section;
    }

    public UnitOfMeasure resolveUnitOfMeasure(String name, String context) {
        if (name == null || name.isBlank()) {
            return null;
        }
        UnitOfMeasure probe = new UnitOfMeasure();
        probe.setUnitOfMeasureName(name);
        UnitOfMeasure unit = unitOfMeasureService.getUnitOfMeasureByName(probe);
        if (unit == null) {
            String aliased = aliasService.resolve(UnresolvedReference.TYPE_UNIT_OF_MEASURE, name);
            if (aliased != null) {
                unit = unitOfMeasureService.getUnitOfMeasureById(aliased);
            }
        }
        if (unit == null) {
            ImportRunContext.addPending(UnresolvedReference.TYPE_UNIT_OF_MEASURE, name, context);
        }
        return unit;
    }

    /** A test's result component by code; blank means the primary component. */
    public TestResultComponent resolveComponent(String testId, String code, String context) {
        List<TestResultComponent> components = componentService.getActiveComponentsByTestId(testId);
        if (code == null || code.isBlank()) {
            for (TestResultComponent component : components) {
                if (Boolean.TRUE.equals(component.getIsPrimary())) {
                    return component;
                }
            }
            return components.isEmpty() ? null : components.get(0);
        }
        for (TestResultComponent component : components) {
            if (code.equalsIgnoreCase(component.getCode())) {
                return component;
            }
        }
        ImportRunContext.addPending(UnresolvedReference.TYPE_RESULT_COMPONENT, code, context);
        return null;
    }

    /**
     * The record a queued name would resolve to now, or null. Used to close the
     * decision queue's items once the catalog has caught up; it asks the same
     * lookups a load does, so a remembered alias counts.
     */
    public String idFor(String referenceType, String name) {
        switch (referenceType) {
        case UnresolvedReference.TYPE_TEST -> {
            Test test = resolveTest(name, null, null);
            return test == null ? null : test.getId();
        }
        case UnresolvedReference.TYPE_SAMPLE_TYPE -> {
            TypeOfSample sampleType = resolveSampleType(name, null);
            return sampleType == null ? null : sampleType.getId();
        }
        case UnresolvedReference.TYPE_TEST_SECTION -> {
            TestSection section = resolveTestSection(name, null);
            return section == null ? null : section.getId();
        }
        case UnresolvedReference.TYPE_UNIT_OF_MEASURE -> {
            UnitOfMeasure unit = resolveUnitOfMeasure(name, null);
            return unit == null ? null : unit.getId();
        }
        default -> {
            return null;
        }
        }
    }

    private TypeOfSample findSampleType(String name) {
        for (TypeOfSample sampleType : typeOfSampleService.getAllTypeOfSamples()) {
            if (name.equalsIgnoreCase(sampleType.getLocalizedName())
                    || name.equalsIgnoreCase(sampleType.getDescription())
                    || name.equalsIgnoreCase(sampleType.getLocalAbbreviation())) {
                return sampleType;
            }
        }
        return null;
    }
}
