package org.openelisglobal.analyzer.service;

import java.util.Comparator;
import java.util.List;
import org.openelisglobal.analyzer.form.AnalyzerResultValueOption;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyzerResultValueOptionServiceImpl implements AnalyzerResultValueOptionService {

    @Autowired
    private AnalyzerTestMappingService analyzerTestMappingService;

    @Autowired
    private TestResultComponentService componentService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private DictionaryService dictionaryService;

    @Override
    public List<AnalyzerResultValueOption> getOptions(String analyzerId, String analyzerTestCode) {
        AnalyzerTestMapping mapping = analyzerTestMappingService.getAllForAnalyzer(analyzerId).stream()
                .filter(candidate -> analyzerTestCode.equals(candidate.getAnalyzerTestName())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No test mapping exists for analyzer test code: " + analyzerTestCode));
        TestResultComponent component = resolveComponent(mapping);
        return testResultService.getActiveOptionsByComponentId(component.getId()).stream()
                .map(option -> toDto(mapping, component, option))
                .sorted(Comparator
                        .comparing(option -> option.getSortOrder() == null ? Integer.MAX_VALUE : option.getSortOrder()))
                .toList();
    }

    @Override
    public List<AnalyzerResultValueOption> findOptions(String analyzerId, String analyzerTestCode) {
        try {
            return getOptions(analyzerId, analyzerTestCode);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    @Override
    public AnalyzerResultValueOption requireValidOption(String analyzerId, String analyzerTestCode, String optionId) {
        if (optionId == null || optionId.isBlank()) {
            throw new IllegalArgumentException("openelisResultOptionId is required");
        }
        return getOptions(analyzerId, analyzerTestCode).stream().filter(option -> optionId.equals(option.getId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "Result option is not active for the mapped OpenELIS test: " + optionId));
    }

    private TestResultComponent resolveComponent(AnalyzerTestMapping mapping) {
        List<TestResultComponent> activeComponents = componentService.getActiveComponentsByTestId(mapping.getTestId());
        if (mapping.getComponentId() != null && !mapping.getComponentId().isBlank()) {
            return activeComponents.stream().filter(component -> mapping.getComponentId().equals(component.getId()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException(
                            "Mapped result component is not active for OpenELIS test " + mapping.getTestId()));
        }
        return activeComponents.stream().filter(TestResultComponent::getIsPrimary).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mapped OpenELIS test has no active primary result component: " + mapping.getTestId()));
    }

    private AnalyzerResultValueOption toDto(AnalyzerTestMapping mapping, TestResultComponent component,
            TestResult option) {
        AnalyzerResultValueOption dto = new AnalyzerResultValueOption();
        dto.setId(option.getId());
        dto.setValue(option.getValue());
        dto.setLabel(resolveLabel(option.getValue()));
        dto.setResultType(option.getTestResultType());
        dto.setSortOrder(parseInteger(option.getSortOrder()));
        dto.setNormal(option.getIsNormal());
        dto.setTestId(mapping.getTestId());
        return dto;
    }

    private String resolveLabel(String value) {
        if (value == null || !value.matches("\\d+")) {
            return value;
        }
        try {
            Dictionary dictionary = dictionaryService.getDictionaryById(value);
            return dictionary == null ? value : dictionary.getDictEntry();
        } catch (RuntimeException e) {
            return value;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
