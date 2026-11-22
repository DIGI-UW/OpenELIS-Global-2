package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionaryterminology.service.DictionaryTerminologyMappingService;
import org.openelisglobal.terminology.Coding;
import org.openelisglobal.terminology.TerminologySystems;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.openelisglobal.testterminology.valueholder.TestTerminologyMapping;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerMappingCatalogServiceImpl implements AnalyzerMappingCatalogService {

    private final org.openelisglobal.unitofmeasure.service.UnitOfMeasureService units;
    private final TestService testService;
    private final TestResultService testResultService;
    private final TestTerminologyMappingService terminologyService;
    private final DictionaryService dictionaryService;
    private final TypeOfSampleService sampleTypes;
    private final TypeOfSampleTestService sampleTypeTests;
    private final TestResultComponentService componentService;
    private final DictionaryTerminologyMappingService answerTerminology;

    public AnalyzerMappingCatalogServiceImpl(TestService testService, TestResultService testResultService,
            TestTerminologyMappingService terminologyService, DictionaryService dictionaryService,
            TypeOfSampleService sampleTypes, TypeOfSampleTestService sampleTypeTests,
            TestResultComponentService componentService, DictionaryTerminologyMappingService answerTerminology,
            org.openelisglobal.unitofmeasure.service.UnitOfMeasureService units) {
        this.units = units;
        this.testService = testService;
        this.testResultService = testResultService;
        this.terminologyService = terminologyService;
        this.dictionaryService = dictionaryService;
        this.sampleTypes = sampleTypes;
        this.sampleTypeTests = sampleTypeTests;
        this.componentService = componentService;
        this.answerTerminology = answerTerminology;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestOption> searchActiveTests(String query) {
        // A code scoped to one component names that part of a result, not the test.
        Map<String, List<TestTerminologyMapping>> codesByTestId = terminologyService.getAllActive().stream()
                .filter(mapping -> mapping.getComponentId() == null)
                .collect(Collectors.groupingBy(TestTerminologyMapping::getTestId));
        Map<String, String> specimenNames = sampleTypes.getAllTypeOfSamples().stream()
                .filter(type -> type.isActive() && !isBlank(type.getDescription()))
                .collect(Collectors.toMap(TypeOfSample::getId, TypeOfSample::getDescription));
        Map<String, List<String>> specimensByTest = sampleTypeTests.getAllTypeOfSampleTests().stream()
                .filter(link -> specimenNames.containsKey(link.getTypeOfSampleId()))
                .collect(Collectors.groupingBy(link -> link.getTestId(),
                        Collectors.mapping(link -> specimenNames.get(link.getTypeOfSampleId()), Collectors.toList())));
        String normalizedQuery = normalize(query);
        List<TestOption> choices = new ArrayList<>();
        for (Test test : testService.getAllActiveTests(false)) {
            if (test == null || !test.isActive()) {
                continue;
            }
            List<Coding> codings = new ArrayList<>();
            if (!isBlank(test.getLoinc())) {
                codings.add(Coding.loinc(test.getLoinc().trim()));
            }
            for (TestTerminologyMapping mapping : codesByTestId.getOrDefault(test.getId(), List.of())) {
                addCoding(codings, mapping.getSource(), mapping.getCode(), mapping.getDisplayName());
            }
            TestOption option = new TestOption(test.getId(), test.getName(), test.getLocalCode(), codings,
                    specimensByTest.getOrDefault(test.getId(), List.of()));
            if (matches(option, normalizedQuery)) {
                choices.add(option);
            }
        }
        choices.sort(Comparator.comparing(TestOption::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(TestOption::id));
        return List.copyOf(choices);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultOption> getActiveResultOptions(String testId) {
        Test test = testService.get(testId);
        if (test == null || !test.isActive()) {
            throw new IllegalArgumentException("Result Options require an active Test");
        }
        List<ResultOption> choices = new ArrayList<>();
        for (TestResult option : testResultService.getActiveTestResultsByTest(testId)) {
            if (!isUsableOption(option, testId)) {
                continue;
            }
            String value = option.getValue();
            Dictionary dictionary = findDictionary(value);
            choices.add(new ResultOption(option.getId(), value, label(dictionary, value), codingsOf(dictionary),
                    option.getComponentId()));
        }
        choices.sort(Comparator.comparing(ResultOption::label, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ResultOption::id));
        return List.copyOf(choices);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComponentOption> getActiveComponents(String testId) {
        return componentService.getActiveComponentsByTestId(testId).stream()
                .map(component -> new ComponentOption(component.getId(), component.getCode(), component.getLabel(),
                        component.getIsPrimary(), component.getResultType(), unitName(component.getUomId())))
                .toList();
    }

    private String unitName(String id) {
        if (id == null)
            return null;
        var unit = units.getUnitOfMeasureById(id);
        return unit == null ? null : unit.getUnitOfMeasureName();
    }

    private static boolean matches(TestOption option, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return contains(option.name(), query) || contains(option.code(), query)
                || option.codings().stream().anyMatch(coding -> contains(coding.code(), query));
    }

    private static boolean isUsableOption(TestResult option, String testId) {
        return option != null && !isBlank(option.getId()) && !isBlank(option.getValue())
                && Boolean.TRUE.equals(option.getIsActive()) && option.getTest() != null
                && testId.equals(option.getTest().getId())
                && TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(option.getTestResultType());
    }

    private Dictionary findDictionary(String value) {
        return value.matches("\\d+") ? dictionaryService.getDictionaryById(value) : null;
    }

    private static String label(Dictionary dictionary, String value) {
        return dictionary != null && !isBlank(dictionary.getDictEntry()) ? dictionary.getDictEntry() : value;
    }

    private List<Coding> codingsOf(Dictionary dictionary) {
        List<Coding> codings = new ArrayList<>();
        if (dictionary != null) {
            for (var mapping : answerTerminology.getActiveByDictionaryId(dictionary.getId())) {
                addCoding(codings, mapping.getSource(), mapping.getCode(), mapping.getDisplayName());
            }
        }
        return codings;
    }

    /** Adds a terminology row's coding unless it is blank or already there. */
    private static void addCoding(List<Coding> codings, String source, String code, String display) {
        String system = TerminologySystems.systemOf(source);
        if (system == null || isBlank(code)) {
            return;
        }
        Coding coding = new Coding(system, code.trim(), isBlank(display) ? null : display.trim());
        if (codings.stream().noneMatch(coding::sameAs)) {
            codings.add(coding);
        }
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
