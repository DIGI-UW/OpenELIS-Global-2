package org.openelisglobal.compliance.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.compliance.service.ComplianceEvaluationResult.ParameterResult;
import org.openelisglobal.compliance.valueholder.ComplianceStatus;
import org.openelisglobal.compliance.valueholder.ComplianceThreshold;
import org.openelisglobal.compliance.valueholder.ThresholdType;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.dao.SampleComplianceStandardDAO;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sample.valueholder.SampleComplianceStandard;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ComplianceEvaluationServiceImpl implements ComplianceEvaluationService {

    private static final BigDecimal BORDERLINE_MARGIN = new BigDecimal("0.10");

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

    @Override
    public ComplianceEvaluationResult evaluate(Sample sample) {
        List<SampleComplianceStandard> links = sampleComplianceStandardDAO.getAllForSample(sample.getId());
        if (links.isEmpty()) {
            return null;
        }

        String standardId = links.get(0).getComplianceStandard().getId();

        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        if (sampleItems.isEmpty()) {
            return null;
        }

        List<ParameterResult> paramResults = new ArrayList<>();
        for (SampleItem sampleItem : sampleItems) {
            List<Analysis> analyses = analysisService.getMaxRevisionAnalysesBySample(sampleItem);
            for (Analysis analysis : analyses) {
                if (analysis.getTest() == null) {
                    continue;
                }
                String testId = analysis.getTest().getId();
                List<ComplianceThreshold> thresholds = complianceThresholdService.getThresholdsByTestAndStandard(testId,
                        standardId);
                if (thresholds.isEmpty()) {
                    continue;
                }

                List<Result> results = resultService.getResultsByAnalysis(analysis);
                if (results.isEmpty()) {
                    continue;
                }

                Result result = results.get(0);
                ComplianceThreshold threshold = thresholds.get(0);
                String rawValue = result.getValue();

                ParameterResult pr = buildParameterResult(threshold, rawValue);
                paramResults.add(pr);
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

    private ParameterResult buildParameterResult(ComplianceThreshold threshold, String rawValue) {
        ParameterResult pr = new ParameterResult();
        pr.setParameterCode(threshold.getParameterCode());
        pr.setDisplayName(threshold.getDisplayName());
        pr.setResultValue(rawValue);
        pr.setThresholdDisplay(threshold.getThresholdRangeDisplay());
        pr.setUnits(threshold.getUnits());
        pr.setStatus(evaluateStatus(threshold, rawValue));
        return pr;
    }

    private ComplianceStatus evaluateStatus(ComplianceThreshold threshold, String rawValue) {
        ThresholdType type = threshold.getThresholdType();

        if (type == ThresholdType.DESCRIPTIVE) {
            return ComplianceStatus.BORDERLINE;
        }

        if (type == ThresholdType.BORDERLINE) {
            BigDecimal value = parseBigDecimal(rawValue);
            if (value == null) {
                return ComplianceStatus.BORDERLINE;
            }
            return type.evaluate(value, threshold.getMinValue(), threshold.getMaxValue(), threshold.getTargetValue())
                    ? ComplianceStatus.BORDERLINE
                    : ComplianceStatus.NON_COMPLIANT;
        }

        if (type == ThresholdType.EXACT && threshold.getTargetValue() == null) {
            String targetText = threshold.getValueDescriptive();
            if (rawValue == null || targetText == null) {
                return ComplianceStatus.NON_COMPLIANT;
            }
            return rawValue.trim().equalsIgnoreCase(targetText.trim()) ? ComplianceStatus.COMPLIANT
                    : ComplianceStatus.NON_COMPLIANT;
        }

        BigDecimal value = parseBigDecimal(rawValue);
        if (value == null) {
            return ComplianceStatus.NON_COMPLIANT;
        }

        boolean passes = type.evaluate(value, threshold.getMinValue(), threshold.getMaxValue(),
                threshold.getTargetValue());

        if (passes) {
            return ComplianceStatus.COMPLIANT;
        }

        if (isBorderline(type, value, threshold)) {
            return ComplianceStatus.BORDERLINE;
        }

        return ComplianceStatus.NON_COMPLIANT;
    }

    private boolean isBorderline(ThresholdType type, BigDecimal value, ComplianceThreshold threshold) {
        switch (type) {
        case RANGE:
            if (threshold.getMinValue() != null && threshold.getMaxValue() != null) {
                BigDecimal range = threshold.getMaxValue().subtract(threshold.getMinValue());
                if (range.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal margin = range.multiply(BORDERLINE_MARGIN);
                    boolean nearMin = value.compareTo(threshold.getMinValue().subtract(margin)) >= 0;
                    boolean nearMax = value.compareTo(threshold.getMaxValue().add(margin)) <= 0;
                    return nearMin && nearMax;
                }
            }
            return false;
        case MINIMUM:
            if (threshold.getMinValue() != null && threshold.getMinValue().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal floor = threshold.getMinValue().multiply(BigDecimal.ONE.subtract(BORDERLINE_MARGIN));
                return value.compareTo(floor) >= 0;
            }
            return false;
        case MAXIMUM:
            if (threshold.getMaxValue() != null && threshold.getMaxValue().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ceiling = threshold.getMaxValue().multiply(BigDecimal.ONE.add(BORDERLINE_MARGIN));
                return value.compareTo(ceiling) <= 0;
            }
            return false;
        default:
            return false;
        }
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

    private BigDecimal parseBigDecimal(String value) {
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
