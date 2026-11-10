package org.openelisglobal.compliance.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.compliance.service.ComplianceEvaluationResult.ParameterResult;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStatus;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ThresholdType;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.dao.SampleComplianceStandardDAO;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sample.valueholder.SampleComplianceStandard;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Judges an environmental order's validated (Finalized) results against every
 * compliance standard linked to it (FR-02), and each result against every
 * active threshold its test has under that standard (S-05, BR-005).
 *
 * <p>
 * A numeric result is Compliant when it meets every limit (Normal Range, Low
 * Limit, High Limit, Exact). A result that misses a limit is Non-compliant,
 * unless a Borderline (Advisory) zone configured for the test covers it, which
 * makes it Borderline. There is no implicit margin around a limit. A
 * select-list result takes the status its option is mapped to; a qualitative
 * threshold or an unmapped option is left Borderline for the analyst to review.
 * The Compliance Dashboard counts results by the same rule.
 */
@Service
@Transactional(readOnly = true)
public class ComplianceEvaluationServiceImpl implements ComplianceEvaluationService {

    private static final Set<ThresholdType> LIMITS = EnumSet.of(ThresholdType.RANGE, ThresholdType.MINIMUM,
            ThresholdType.MAXIMUM, ThresholdType.EXACT);

    @Autowired
    private SampleComplianceStandardDAO sampleComplianceStandardDAO;

    @Autowired
    private ComplianceThresholdService complianceThresholdService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private MicroCaseAnalysisDAO microCaseAnalysisDAO;

    /**
     * A test owned by a microbiology case is released with the case's final report,
     * which the certificate requires separately, so only the other tests must be
     * validated.
     */
    @Override
    public boolean awaitsValidation(Sample sample) {
        return orderedTests(sample)
                .anyMatch(analysis -> !statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)
                        && microCaseAnalysisDAO.getActiveByAnalysisId(analysis.getId()) == null);
    }

    @Override
    public int orderedTestCount(Sample sample) {
        return (int) orderedTests(sample).count();
    }

    @Override
    public List<EnvironmentalReportRow> reportRows(Sample sample) {
        List<SampleComplianceStandard> links = sampleComplianceStandardDAO.getAllForSample(sample.getId());
        boolean nameStandards = links.size() > 1;
        List<EnvironmentalReportRow> rows = new ArrayList<>();
        List<SampleItem> items = new ArrayList<>(sampleItemService.getSampleItemsBySampleId(sample.getId()));
        items.sort(java.util.Comparator.comparing(item -> sortKey(item.getSortOrder())));
        for (SampleItem item : items) {
            String sampleType = item.getTypeOfSample() == null ? null : item.getTypeOfSample().getLocalizedName();
            for (Analysis analysis : liveTests(item)) {
                if (analysis.getTest() == null
                        || !statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                    continue;
                }
                List<Result> results = resultService.getResultsByAnalysis(analysis);
                if (results.isEmpty()) {
                    continue;
                }
                String parameter = analysis.getTest().getName();
                String result = displayValue(results);
                String unit = analysis.getTest().getUnitOfMeasure() == null ? null
                        : analysis.getTest().getUnitOfMeasure().getUnitOfMeasureName();
                String method = analysis.getMethod() != null ? analysis.getMethod().getMethodName()
                        : analysis.getTest().getMethod() == null ? null
                                : analysis.getTest().getMethod().getMethodName();
                boolean judged = false;
                for (SampleComplianceStandard link : links) {
                    ComplianceStandard standard = link.getComplianceStandard();
                    List<ComplianceThreshold> thresholds = complianceThresholdService
                            .getThresholdsByTestAndStandard(analysis.getTest().getId(), standard.getId()).stream()
                            .filter(threshold -> !Boolean.FALSE.equals(threshold.getIsActive())).toList();
                    if (thresholds.isEmpty()) {
                        continue;
                    }
                    ParameterResult judgement = buildParameterResult(thresholds, results.get(0).getValue(),
                            nameStandards ? standardLabel(standard) : null);
                    rows.add(new EnvironmentalReportRow(item.getSortOrder(), sampleType, parameter, result,
                            unit != null ? unit : judgement.getUnits(), method, judgement.getThresholdDisplay(),
                            judgement.getStatus()));
                    judged = true;
                }
                if (!judged) {
                    rows.add(new EnvironmentalReportRow(item.getSortOrder(), sampleType, parameter, result, unit,
                            method, null, null));
                }
            }
        }
        return rows;
    }

    private static int sortKey(String sortOrder) {
        try {
            return Integer.parseInt(sortOrder);
        } catch (RuntimeException e) {
            return Integer.MAX_VALUE;
        }
    }

    /** A select-list result reads as its option; a multi-select as its options. */
    private String displayValue(List<Result> results) {
        Result first = results.get(0);
        if (!ResultType.isDictionaryVariant(first.getResultType())) {
            return first.getValue();
        }
        List<Result> shown = ResultType.isMultiSelectVariant(first.getResultType()) ? results : List.of(first);
        return shown.stream().map(Result::getValue).filter(value -> value != null && !value.isBlank())
                .map(value -> dictionaryText(value.trim())).collect(Collectors.joining(", "));
    }

    private Stream<Analysis> orderedTests(Sample sample) {
        return sampleItemService.getSampleItemsBySampleId(sample.getId()).stream()
                .flatMap(item -> liveTests(item).stream());
    }

    /**
     * The analyses of each test at its latest revision among those not cancelled. A
     * test removed from the order and added back has a new analysis at a lower
     * revision than its cancelled copy, so the latest revision is taken among the
     * live ones only; several live analyses of a test at that revision all count.
     */
    private List<Analysis> liveTests(SampleItem sampleItem) {
        List<Analysis> live = analysisService.getAnalysesBySampleItem(sampleItem).stream()
                .filter(analysis -> analysis.getTest() != null)
                .filter(analysis -> !statusService.matches(analysis.getStatusId(), AnalysisStatus.Canceled)).toList();
        Map<String, Integer> latest = new HashMap<>();
        live.forEach(analysis -> latest.merge(analysis.getTest().getId(), revision(analysis), Math::max));
        return live.stream().filter(analysis -> revision(analysis) == latest.get(analysis.getTest().getId())).toList();
    }

    private static int revision(Analysis analysis) {
        String revision = analysis.getRevision();
        return revision == null || revision.isBlank() ? 0 : Integer.parseInt(revision.trim());
    }

    @Override
    public ComplianceEvaluationResult evaluate(Sample sample) {
        List<SampleComplianceStandard> links = sampleComplianceStandardDAO.getAllForSample(sample.getId());
        if (links.isEmpty()) {
            return null;
        }

        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        if (sampleItems.isEmpty()) {
            return null;
        }

        boolean nameStandards = links.size() > 1;
        List<ParameterResult> paramResults = new ArrayList<>();
        for (SampleItem sampleItem : sampleItems) {
            List<Analysis> analyses = liveTests(sampleItem);
            for (Analysis analysis : analyses) {
                if (analysis.getTest() == null
                        || !statusService.matches(analysis.getStatusId(), AnalysisStatus.Finalized)) {
                    continue;
                }
                List<Result> results = null;
                for (SampleComplianceStandard link : links) {
                    ComplianceStandard standard = link.getComplianceStandard();
                    List<ComplianceThreshold> thresholds = complianceThresholdService
                            .getThresholdsByTestAndStandard(analysis.getTest().getId(), standard.getId()).stream()
                            .filter(threshold -> !Boolean.FALSE.equals(threshold.getIsActive())).toList();
                    if (thresholds.isEmpty()) {
                        continue;
                    }
                    if (results == null) {
                        results = resultService.getResultsByAnalysis(analysis);
                    }
                    if (results.isEmpty()) {
                        break;
                    }
                    paramResults.add(buildParameterResult(thresholds, results.get(0).getValue(),
                            nameStandards ? standardLabel(standard) : null));
                }
            }
        }

        if (paramResults.isEmpty()) {
            return null;
        }

        ComplianceEvaluationResult evalResult = new ComplianceEvaluationResult();
        evalResult.setParameterResults(paramResults);
        evalResult.setOverallStatus(rollUp(paramResults));
        return evalResult;
    }

    private ParameterResult buildParameterResult(List<ComplianceThreshold> thresholds, String rawValue,
            String standardLabel) {
        ComplianceThreshold first = thresholds.get(0);
        String display = thresholds.stream().map(ComplianceThreshold::getThresholdRangeDisplay).map(String::trim)
                .filter(text -> !text.isEmpty()).collect(Collectors.joining("; "));
        ParameterResult pr = new ParameterResult();
        pr.setParameterCode(first.getParameterCode());
        pr.setDisplayName(first.getDisplayName());
        pr.setResultValue(rawValue);
        pr.setThresholdDisplay(standardLabel == null ? display : standardLabel + ": " + display);
        pr.setUnits(first.getUnits());
        pr.setStatus(evaluateStatus(thresholds, rawValue));
        return pr;
    }

    static ComplianceStatus evaluateStatus(List<ComplianceThreshold> thresholds, String rawValue,
            Function<String, String> optionText) {
        Optional<ComplianceThreshold> selectMap = ofType(thresholds, ThresholdType.SELECT_MAP).findFirst();
        if (selectMap.isPresent()) {
            return mappedStatus(selectMap.get(), rawValue, optionText);
        }
        List<ComplianceThreshold> limits = thresholds.stream()
                .filter(threshold -> LIMITS.contains(threshold.getThresholdType())).toList();
        List<ComplianceThreshold> advisory = ofType(thresholds, ThresholdType.BORDERLINE).toList();
        if (!limits.isEmpty()) {
            if (limits.stream().allMatch(limit -> meets(limit, rawValue))) {
                return ComplianceStatus.COMPLIANT;
            }
            return withinAdvisoryZone(advisory, rawValue) ? ComplianceStatus.BORDERLINE
                    : ComplianceStatus.NON_COMPLIANT;
        }
        if (ofType(thresholds, ThresholdType.DESCRIPTIVE).findAny().isPresent()) {
            return ComplianceStatus.BORDERLINE;
        }
        if (!advisory.isEmpty()) {
            return withinAdvisoryZone(advisory, rawValue) ? ComplianceStatus.BORDERLINE
                    : ComplianceStatus.NON_COMPLIANT;
        }
        return ComplianceStatus.BORDERLINE;
    }

    private ComplianceStatus evaluateStatus(List<ComplianceThreshold> thresholds, String rawValue) {
        return evaluateStatus(thresholds, rawValue, this::dictionaryText);
    }

    private static Stream<ComplianceThreshold> ofType(List<ComplianceThreshold> thresholds, ThresholdType type) {
        return thresholds.stream().filter(threshold -> threshold.getThresholdType() == type);
    }

    private static boolean meets(ComplianceThreshold limit, String rawValue) {
        if (limit.getThresholdType() == ThresholdType.EXACT && limit.getTargetValue() == null) {
            String target = limit.getValueDescriptive();
            return rawValue != null && target != null && rawValue.trim().equalsIgnoreCase(target.trim());
        }
        BigDecimal value = parseBigDecimal(rawValue);
        return value != null && limit.getThresholdType().evaluate(value, limit.getMinValue(), limit.getMaxValue(),
                limit.getTargetValue());
    }

    private static boolean withinAdvisoryZone(List<ComplianceThreshold> advisory, String rawValue) {
        BigDecimal value = parseBigDecimal(rawValue);
        return value != null && advisory.stream().anyMatch(zone -> ThresholdType.BORDERLINE.evaluate(value,
                zone.getMinValue(), zone.getMaxValue(), zone.getTargetValue()));
    }

    private static ComplianceStatus mappedStatus(ComplianceThreshold selectMap, String rawValue,
            Function<String, String> optionText) {
        if (rawValue == null || rawValue.isBlank()) {
            return ComplianceStatus.BORDERLINE;
        }
        String option = optionText.apply(rawValue.trim());
        return selectMap.getValueMappings().stream()
                .filter(mapping -> mapping.getOptionValue() != null
                        && (mapping.getOptionValue().trim().equalsIgnoreCase(option)
                                || mapping.getOptionValue().trim().equalsIgnoreCase(rawValue.trim())))
                .map(mapping -> mapping.getComplianceStatus()).findFirst().orElse(ComplianceStatus.BORDERLINE);
    }

    /**
     * A select-list result stores the dictionary id; the mapping stores its text.
     */
    private String dictionaryText(String rawValue) {
        if (!rawValue.matches("\\d+")) {
            return rawValue;
        }
        Dictionary dictionary = dictionaryService.getDictionaryById(rawValue);
        return dictionary == null || dictionary.getDictEntry() == null ? rawValue : dictionary.getDictEntry().trim();
    }

    private static String standardLabel(ComplianceStandard standard) {
        return standard.getRegulationNumber() != null && !standard.getRegulationNumber().isBlank()
                ? standard.getRegulationNumber()
                : standard.getName();
    }

    private ComplianceStatus rollUp(List<ParameterResult> results) {
        boolean anyNonCompliant = false;
        boolean anyBorderline = false;
        for (ParameterResult pr : results) {
            if (pr.getStatus() == ComplianceStatus.NON_COMPLIANT) {
                anyNonCompliant = true;
            } else if (pr.getStatus() == ComplianceStatus.BORDERLINE) {
                anyBorderline = true;
            }
        }
        if (anyNonCompliant) {
            return ComplianceStatus.NON_COMPLIANT;
        }
        if (anyBorderline) {
            return ComplianceStatus.BORDERLINE;
        }
        return ComplianceStatus.COMPLIANT;
    }

    private static BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
