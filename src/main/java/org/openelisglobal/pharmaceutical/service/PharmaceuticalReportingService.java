package org.openelisglobal.pharmaceutical.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public interface PharmaceuticalReportingService {

    Map<String, Object> getIntakeMetrics(Timestamp startDate, Timestamp endDate);

    Map<String, Object> getQCMetrics(Timestamp startDate, Timestamp endDate);

    Map<String, Object> getAssayMetrics(Timestamp startDate, Timestamp endDate);

    Map<String, Object> getOOSMetrics(Timestamp startDate, Timestamp endDate);

    Map<String, Object> getTATMetrics(Timestamp startDate, Timestamp endDate);

    Map<String, Object> getStorageMetrics();

    Map<String, Object> getDisposalMetrics(Timestamp startDate, Timestamp endDate);

    Map<String, Object> getExcursionMetrics(Timestamp startDate, Timestamp endDate);

    Map<String, Object> getDashboardSummary();

    List<Map<String, Object>> getSampleStatusDistribution();

    List<Map<String, Object>> getAssayTypeDistribution(Timestamp startDate, Timestamp endDate);

    List<Map<String, Object>> getExcursionHistory(Timestamp startDate, Timestamp endDate);

    List<Map<String, Object>> getDisposalHistory(Timestamp startDate, Timestamp endDate);

    byte[] exportReportAsCSV(String reportType, Timestamp startDate, Timestamp endDate);

    byte[] exportReportAsPDF(String reportType, Timestamp startDate, Timestamp endDate);
}
