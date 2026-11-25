package org.openelisglobal.reports.dataexport.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A test's name in Custom Data Export as the application's screens show it
 * (OGC-1680): its localized name in the user's language, followed by the sample
 * type where the site names tests that way. The catalog description is the
 * base-language form ("Créatinine(Serum)"), which an English user sees nowhere
 * else. A test with no translation keeps its description, as the screens do.
 */
@Service
@Transactional(readOnly = true)
public class ExportTestNames {

    @Autowired
    private TypeOfSampleTestService typeOfSampleTests;
    @Autowired
    private TypeOfSampleService typesOfSample;

    /**
     * The field-list name of every test, its sample types summarized as the test
     * catalog does: "(Serum)", or "(Serum +1)" for a test taken on two.
     */
    public Map<String, String> labels(Collection<Test> tests, Locale locale) {
        Map<String, List<String>> linked = new HashMap<>();
        Map<String, TypeOfSample> types = Map.of();
        TypeOfSample variable = null;
        if (augmented()) {
            for (TypeOfSampleTest link : typeOfSampleTests.getAllTypeOfSampleTests()) {
                linked.computeIfAbsent(link.getTestId(), id -> new ArrayList<>()).add(link.getTypeOfSampleId());
            }
            types = typesOfSample.getAllTypeOfSamples().stream()
                    .collect(Collectors.toMap(TypeOfSample::getId, Function.identity(), (first, second) -> first));
            variable = typesOfSample.getTypeOfSampleByLocalAbbrevAndDomain("Variable", "H");
        }
        Map<String, String> labels = new HashMap<>();
        for (Test test : tests) {
            String name = localizedName(test, locale);
            if (name == null) {
                labels.put(test.getId(), test.getDescription());
                continue;
            }
            List<String> typeIds = linked.getOrDefault(test.getId(), List.of());
            TypeOfSample first = typeIds.isEmpty() ? null : types.get(typeIds.get(0));
            if (first != null && (variable == null || !first.getId().equals(variable.getId()))) {
                name += typeIds.size() > 1 ? "(" + typeName(first, locale) + " +" + (typeIds.size() - 1) + ")"
                        : "(" + typeName(first, locale) + ")";
            }
            labels.put(test.getId(), name);
        }
        return labels;
    }

    /**
     * A row's test, named with the sample type of the specimen the row is about.
     */
    public String withSpecimen(Test test, TypeOfSample specimenType, Locale locale) {
        if (test == null) {
            return null;
        }
        String name = localizedName(test, locale);
        if (name == null) {
            return test.getDescription();
        }
        return specimenType == null || !augmented() ? name : name + "(" + typeName(specimenType, locale) + ")";
    }

    /** The test's own name in the given language, without a sample type. */
    public static String name(Test test, Locale locale) {
        String name = localizedName(test, locale);
        return name == null ? test.getDescription() : name;
    }

    /**
     * A result component's label in the given language (OGC-1610), else the label
     * as entered.
     */
    public static String componentLabel(
            org.openelisglobal.testresultcomponent.valueholder.TestResultComponent component, Locale locale) {
        if (component == null) {
            return null;
        }
        Localization localization = component.getLabelLocalization();
        String value = localization == null ? null : localization.getLocalizedValue(locale);
        return GenericValidator.isBlankOrNull(value) ? component.getLabel() : value;
    }

    /** A lab unit's name in the given language, as the screens show it. */
    public static String sectionName(TestSection section, Locale locale) {
        if (section == null) {
            return null;
        }
        Localization localization = section.getLocalization();
        String value = localization == null ? null : localization.getLocalizedValue(locale);
        return GenericValidator.isBlankOrNull(value) ? section.getTestSectionName() : value;
    }

    private static String localizedName(Test test, Locale locale) {
        Localization localization = test.getLocalizedTestName();
        String value = localization == null ? null : localization.getLocalizedValue(locale);
        return GenericValidator.isBlankOrNull(value) ? null : value;
    }

    private static String typeName(TypeOfSample type, Locale locale) {
        Localization localization = type.getLocalization();
        String value = localization == null ? null : localization.getLocalizedValue(locale);
        return GenericValidator.isBlankOrNull(value) ? type.getDescription() : value;
    }

    private static boolean augmented() {
        return ConfigurationProperties.getInstance().isPropertyValueEqual(Property.TEST_NAME_AUGMENTED, "true");
    }
}
