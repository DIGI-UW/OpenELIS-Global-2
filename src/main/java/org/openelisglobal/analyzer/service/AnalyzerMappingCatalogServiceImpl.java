package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.openelisglobal.testterminology.valueholder.TestTerminologyMapping;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerMappingCatalogServiceImpl implements AnalyzerMappingCatalogService {

    private static final String LOINC = "LOINC";

    private final TestService testService;
    private final TestResultService testResultService;
    private final TestTerminologyMappingService terminologyService;
    private final DictionaryService dictionaryService;

    public AnalyzerMappingCatalogServiceImpl(TestService testService, TestResultService testResultService,
            TestTerminologyMappingService terminologyService, DictionaryService dictionaryService) {
        this.testService = testService;
        this.testResultService = testResultService;
        this.terminologyService = terminologyService;
        this.dictionaryService = dictionaryService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestOption> searchActiveTests(String query) {
        Map<String, Set<String>> loincByTestId = terminologyService.getActiveBySource(LOINC).stream()
                .collect(Collectors.groupingBy(TestTerminologyMapping::getTestId, Collectors
                        .mapping(TestTerminologyMapping::getCode, Collectors.toCollection(LinkedHashSet::new))));
        String normalizedQuery = normalize(query);
        List<TestOption> choices = new ArrayList<>();
        for (Test test : testService.getAllActiveTests(false)) {
            if (test == null || !test.isActive()) {
                continue;
            }
            Set<String> loincCodes = new LinkedHashSet<>();
            if (!isBlank(test.getLoinc())) {
                loincCodes.add(test.getLoinc().trim());
            }
            loincCodes.addAll(loincByTestId.getOrDefault(test.getId(), Set.of()));
            TestOption option = new TestOption(test.getId(), test.getName(), test.getLocalCode(),
                    List.copyOf(loincCodes));
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
            choices.add(new ResultOption(option.getId(), value, resolveLabel(value)));
        }
        choices.sort(Comparator.comparing(ResultOption::label, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ResultOption::id));
        return List.copyOf(choices);
    }

    private static boolean matches(TestOption option, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return contains(option.name(), query) || contains(option.code(), query)
                || option.loincCodes().stream().anyMatch(code -> contains(code, query));
    }

    private static boolean isUsableOption(TestResult option, String testId) {
        return option != null && !isBlank(option.getId()) && !isBlank(option.getValue())
                && Boolean.TRUE.equals(option.getIsActive()) && option.getTest() != null
                && testId.equals(option.getTest().getId())
                && TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(option.getTestResultType());
    }

    private String resolveLabel(String value) {
        if (value.matches("\\d+")) {
            Dictionary dictionary = dictionaryService.getDictionaryById(value);
            if (dictionary != null && !isBlank(dictionary.getDictEntry())) {
                return dictionary.getDictEntry();
            }
        }
        return value;
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
