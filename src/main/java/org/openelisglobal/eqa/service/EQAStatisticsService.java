package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.springframework.security.access.prepost.PreAuthorize;

public interface EQAStatisticsService {

    int MIN_PARTICIPANTS_FOR_STATS = 5;

    BigDecimal Z_SCORE_ACCEPTABLE_THRESHOLD = new BigDecimal("2.0");
    BigDecimal Z_SCORE_UNACCEPTABLE_THRESHOLD = new BigDecimal("3.0");

    @PreAuthorize("hasAuthority('PRIV_EQA_MANAGE')")
    void calculateAndUpdateStatistics(Long distributionId);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    BigDecimal calculateMean(List<BigDecimal> values);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    BigDecimal calculateStandardDeviation(List<BigDecimal> values, BigDecimal mean);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    BigDecimal calculateZScore(BigDecimal value, BigDecimal mean, BigDecimal standardDeviation);

    @PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
    EQAPerformanceStatus classifyPerformance(BigDecimal zScore);
}
