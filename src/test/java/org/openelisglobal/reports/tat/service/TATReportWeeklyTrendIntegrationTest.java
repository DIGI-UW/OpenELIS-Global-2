package org.openelisglobal.reports.tat.service;

import static org.junit.Assert.assertEquals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.openelisglobal.reports.tat.bean.TATTrendResponse.TrendDataPoint;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TATReportWeeklyTrendIntegrationTest extends BaseWebContextSensitiveTest {

    private static final LocalDate WEEK_START = LocalDate.of(2025, 12, 29);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 1, 4);

    @Autowired
    private TATReportService tatReportService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void weekStraddlingNewYearIsOneBucketKeyedByItsIsoYear() {
        int ambient = countsByWeek().values().stream().mapToInt(Integer::intValue).sum();
        String run = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        receive("D" + run, LocalDateTime.of(2025, 12, 30, 9, 0));
        receive("J" + run, LocalDateTime.of(2026, 1, 2, 9, 0));
        entityManager.flush();

        assertEquals(Map.of("2026-W01", ambient + 2), countsByWeek());
    }

    private Map<String, Integer> countsByWeek() {
        return tatReportService
                .getTrend(WEEK_START, WEEK_END, TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR, null,
                        null, null, null, null, null, true, "WEEKLY", null)
                .getSeries().get(0).getDataPoints().stream()
                .collect(Collectors.toMap(TrendDataPoint::getPeriod, TrendDataPoint::getCount));
    }

    private void receive(String accessionNumber, LocalDateTime receivedAt) {
        String statusId = enteredStatusId();
        Timestamp received = Timestamp.valueOf(receivedAt);

        Sample sample = new Sample();
        sample.setAccessionNumber(accessionNumber);
        sample.setDomain("H");
        sample.setEnteredDate(new java.sql.Date(received.getTime()));
        sample.setReceivedTimestamp(received);
        sample.setSysUserId(TEST_SYS_USER_ID);
        String sampleId = sampleService.insert(sample);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sampleService.get(sampleId));
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(statusId);
        sampleItem.setSysUserId(TEST_SYS_USER_ID);
        String sampleItemId = sampleItemService.insert(sampleItem);

        Analysis analysis = new Analysis();
        analysis.setSampleItem(sampleItemService.get(sampleItemId));
        analysis.setAnalysisType("MANUAL");
        analysis.setStatusId(statusId);
        analysis.setRevision("1");
        analysis.setReleasedDate(Timestamp.valueOf(receivedAt.plusHours(2)));
        analysis.setSysUserId(TEST_SYS_USER_ID);
        analysisService.insert(analysis);
    }

    private String enteredStatusId() {
        jdbcTemplate.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description)"
                + " SELECT 9802, 1, 'SAMPLE', 'Entered', 'restored by TATReportWeeklyTrendIntegrationTest'"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.status_of_sample"
                + " WHERE name = 'Entered' AND status_type = 'SAMPLE')");
        return jdbcTemplate.queryForObject(
                "SELECT min(id) FROM clinlims.status_of_sample WHERE name = 'Entered' AND status_type = 'SAMPLE'",
                String.class);
    }
}
