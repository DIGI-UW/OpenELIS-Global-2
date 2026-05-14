package org.openelisglobal.qc.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCRuleViolationDAO;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QCChartDataServiceImpl implements QCChartDataService {

    @Autowired
    private QCResultDAO resultDAO;

    @Autowired
    private QCStatisticsService statisticsService;

    @Autowired
    private QCRuleViolationDAO violationDAO;

    @Override
    @Transactional(readOnly = true)
    public List<QCResult> getResultsByControlLotAndDateRange(String controlLotId, Timestamp startDate,
            Timestamp endDate) {
        return resultDAO.findByControlLotAndDateRange(controlLotId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCRuleViolation> getViolationsForResults(List<String> resultIds) {
        List<QCRuleViolation> violations = new ArrayList<>();
        for (String resultId : resultIds) {
            violations.addAll(violationDAO.findByTriggeringResultId(resultId));
        }
        return violations;
    }

    @Override
    @Transactional(readOnly = true)
    public QCStatistics getLatestStatistics(String controlLotId) {
        return statisticsService.getLatestStatistics(controlLotId);
    }
}
