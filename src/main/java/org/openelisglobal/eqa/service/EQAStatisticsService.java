package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;

public interface EQAStatisticsService {

    int MIN_PARTICIPANTS_FOR_STATS = 5;

    BigDecimal Z_SCORE_ACCEPTABLE_THRESHOLD = new BigDecimal("2.0");
    BigDecimal Z_SCORE_UNACCEPTABLE_THRESHOLD = new BigDecimal("3.0");

    void calculateAndUpdateStatistics(Long distributionId);

    BigDecimal calculateMean(List<BigDecimal> values);

    BigDecimal calculateStandardDeviation(List<BigDecimal> values, BigDecimal mean);

    BigDecimal calculateZScore(BigDecimal value, BigDecimal mean, BigDecimal standardDeviation);

    EQAPerformanceStatus classifyPerformance(BigDecimal zScore);
}
