package org.openelisglobal.reports.tat.service;

import static org.junit.Assert.assertEquals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATResult;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TATReportWindowIntegrationTest extends BaseWebContextSensitiveTest {

    private static final LocalDate DAY = LocalDate.of(2099, 3, 10);

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
    public void windowIncludesItsFirstMidnightAndExcludesTheNextDaysMidnight() {
        String run = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Timestamp firstMidnight = Timestamp.valueOf(DAY.atStartOfDay());
        Timestamp nextMidnight = Timestamp.valueOf(DAY.plusDays(1).atStartOfDay());
        receive("S" + run, firstMidnight);
        receive("E" + run, nextMidnight);
        entityManager.flush();

        Map<String, Timestamp> receivedByLabNumber = tatReportService
                .getAllResults(DAY, DAY, TATSegment.RECEIPT_TO_VALIDATION, TATCalculationMode.CALENDAR, null, null,
                        null, null, null, null, true)
                .stream().filter(r -> r.getLabNumber().endsWith(run))
                .collect(Collectors.toMap(TATResult::getLabNumber, TATResult::getReceived));

        assertEquals(Map.of("S" + run, firstMidnight), receivedByLabNumber);
    }

    private void receive(String accessionNumber, Timestamp received) {
        String statusId = enteredStatusId();

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
        analysis.setSysUserId(TEST_SYS_USER_ID);
        analysisService.insert(analysis);
    }

    private String enteredStatusId() {
        jdbcTemplate.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description)"
                + " SELECT 9802, 1, 'SAMPLE', 'Entered', 'restored by TATReportWindowIntegrationTest'"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.status_of_sample"
                + " WHERE name = 'Entered' AND status_type = 'SAMPLE')");
        return jdbcTemplate.queryForObject(
                "SELECT min(id) FROM clinlims.status_of_sample WHERE name = 'Entered' AND status_type = 'SAMPLE'",
                String.class);
    }
}
