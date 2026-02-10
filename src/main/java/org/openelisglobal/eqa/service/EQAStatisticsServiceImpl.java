package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.eqa.dao.EQAResultDAO;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EQAStatisticsServiceImpl implements EQAStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(EQAStatisticsServiceImpl.class);
    private static final int SCALE = 5;
    private static final MathContext MC = new MathContext(15, RoundingMode.HALF_UP);

    @Autowired
    private EQAResultDAO eqaResultDAO;

    @Override
    public void calculateAndUpdateStatistics(Long distributionId) {
        List<EQAResult> results = eqaResultDAO.findByDistributionId(distributionId);

        List<BigDecimal> values = results.stream().map(EQAResult::getResultValue).filter(v -> v != null)
                .collect(Collectors.toList());

        if (values.size() < MIN_PARTICIPANTS_FOR_STATS) {
            logger.info("Distribution {} has only {} results, minimum {} required for statistics", distributionId,
                    values.size(), MIN_PARTICIPANTS_FOR_STATS);
            return;
        }

        BigDecimal mean = calculateMean(values);
        BigDecimal sd = calculateStandardDeviation(values, mean);

        for (EQAResult result : results) {
            if (result.getResultValue() != null) {
                BigDecimal zScore = calculateZScore(result.getResultValue(), mean, sd);
                result.setZScore(zScore);
                result.setPerformanceStatus(classifyPerformance(zScore));
                eqaResultDAO.update(result);
            }
        }
    }

    @Override
    public BigDecimal calculateMean(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(values.size()), SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateStandardDeviation(List<BigDecimal> values, BigDecimal mean) {
        if (values == null || values.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal sumSquaredDiffs = values.stream().map(v -> v.subtract(mean).pow(2, MC)).reduce(BigDecimal.ZERO,
                BigDecimal::add);

        BigDecimal variance = sumSquaredDiffs.divide(new BigDecimal(values.size() - 1), SCALE, RoundingMode.HALF_UP);

        return sqrt(variance);
    }

    @Override
    public BigDecimal calculateZScore(BigDecimal value, BigDecimal mean, BigDecimal standardDeviation) {
        if (standardDeviation == null || standardDeviation.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.subtract(mean).divide(standardDeviation, SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public EQAPerformanceStatus classifyPerformance(BigDecimal zScore) {
        if (zScore == null) {
            return null;
        }
        BigDecimal absZScore = zScore.abs();
        if (absZScore.compareTo(Z_SCORE_UNACCEPTABLE_THRESHOLD) >= 0) {
            return EQAPerformanceStatus.UNACCEPTABLE;
        } else if (absZScore.compareTo(Z_SCORE_ACCEPTABLE_THRESHOLD) >= 0) {
            return EQAPerformanceStatus.QUESTIONABLE;
        } else {
            return EQAPerformanceStatus.ACCEPTABLE;
        }
    }

    /**
     * Newton's method square root for BigDecimal.
     */
    private BigDecimal sqrt(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal x = new BigDecimal(Math.sqrt(value.doubleValue()), MC);
        // Refine with one Newton iteration for precision
        x = value.divide(x, MC).add(x).divide(new BigDecimal(2), MC);
        return x.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
