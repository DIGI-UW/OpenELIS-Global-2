package org.openelisglobal.dashboard.management.surveillance;

import static org.junit.Assert.assertEquals;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.surveillance.SurveillanceResponse.Metric;
import org.openelisglobal.dashboard.management.surveillance.SurveillanceResponse.Program;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioForm;
import org.openelisglobal.microbiology.form.MicrobiologyUatScenarioRequestForm;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroCaseOrderDetailService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.service.MicroReportReleaseService;
import org.openelisglobal.microbiology.service.MicrobiologyUatScenarioService;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstTechnique;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SurveillanceResistanceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final LocalDate COLLECTED = LocalDate.of(2026, 3, 3);
    private static final ScopeRange SCOPE = new ScopeRange(COLLECTED, COLLECTED, COLLECTED.minusDays(1),
            COLLECTED.minusDays(1));
    private static final BigDecimal SUSCEPTIBLE = new BigDecimal("4");
    private static final BigDecimal RESISTANT = new BigDecimal("32");

    @Autowired
    private SurveillanceService surveillanceService;
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private MicrobiologyUatScenarioService uatScenarioService;
    @Autowired
    private MicroIsolateService isolateService;
    @Autowired
    private MicroAstService astService;
    @Autowired
    private MicroCaseOrderDetailService orderDetailService;
    @Autowired
    private MicroReportReleaseService reportReleaseService;
    @Autowired
    private SampleItemService sampleItemService;
    @PersistenceContext
    private EntityManager entityManager;

    private String user;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        fixtures.ensureRequiredWorkflowStatuses();
        user = fixtures.defaultUserId();
        jdbcTemplate.update("DELETE FROM clinlims.surveillance_resistance_metric");
    }

    @Test
    public void resistanceIsTheShareOfTestedFirstIsolatesReadingRToAnyOfTheAntibiotics() {
        String organismId = fixtures.createOrganism("Surveillance organism", "SurvGroup", null).getId();
        MicrobiologyUatScenarioForm first = isolate(organismId, RESISTANT, SUSCEPTIBLE);
        for (int i = 1; i < 10; i++) {
            isolate(organismId, i < 3 ? RESISTANT : SUSCEPTIBLE, i < 9 ? SUSCEPTIBLE : null);
        }
        List<String> panel = astService.getPanelAntibiotics(first.astPanelId).stream()
                .map(ordered -> ordered.getAntibioticId()).toList();
        String second = code(panel.get(1));
        metric("BOTH", null, "survgroup", code(panel.get(0)) + ", " + second.toLowerCase(), 1);
        metric("SECOND_ONLY", null, "SurvGroup", second, 2);
        metric("UNSET", "zz9", null, "NOPE9", 3);

        Program amr = surveillanceService.getSurveillance(SCOPE).programs().stream()
                .filter(program -> program.code().equals("AMR")).findFirst().orElseThrow();

        assertEquals(List.of(new Metric("BOTH", "BOTH label", 30.0, 3, 10, null),
                new Metric("SECOND_ONLY", "SECOND_ONLY label", null, 0, 9, null),
                new Metric("UNSET", "UNSET label", null, 0, 0, null)), amr.metrics());
        assertEquals(10, amr.testCount());
        assertEquals(List.of("NOPE9", "zz9"), amr.missingCodes());
    }

    private String code(String antibioticId) {
        String code = "SV" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        jdbcTemplate.update("UPDATE clinlims.micro_antibiotic SET whonet_code = ? WHERE id = ?", code, antibioticId);
        return code;
    }

    private void metric(String key, String organismCodes, String organismGroup, String antibioticCodes, int order) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.surveillance_resistance_metric (id, program_id, metric_key,"
                        + " metric_label, organism_codes, organism_group, antibiotic_codes, display_order)"
                        + " SELECT nextval('clinlims.surveillance_resistance_metric_seq'), p.id, ?, ?, ?, ?, ?, ?"
                        + " FROM clinlims.surveillance_program p WHERE p.code = 'AMR'",
                key, key + " label", organismCodes, organismGroup, antibioticCodes, order);
    }

    // One new patient per call. Readings follow the M4 panel order and null skips
    // that antibiotic.
    private MicrobiologyUatScenarioForm isolate(String organismId, BigDecimal... readings) {
        MicrobiologyUatScenarioRequestForm request = new MicrobiologyUatScenarioRequestForm();
        request.scenario = "M4";
        request.scenarioKey = "surveillance-" + UUID.randomUUID();
        MicrobiologyUatScenarioForm scenario = uatScenarioService.provision(request, user);
        MicroCaseOrderDetailRequestForm purpose = new MicroCaseOrderDetailRequestForm();
        purpose.culturePurpose = "CLINICAL_DIAGNOSTIC";
        orderDetailService.saveOrderDetail(scenario.caseId, purpose, user);

        SampleItem sampleItem = sampleItemService.get(scenario.sampleItemId);
        sampleItem.setCollectionDate(Timestamp.valueOf(COLLECTED.atTime(9, 0)));
        sampleItem.setSysUserId(user);
        sampleItemService.update(sampleItem);

        MicroIsolate isolate = isolateService.createIsolate(scenario.caseId, "ISO-1", "Gram negative rods",
                "Lactose fermenting colonies", MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, user);
        isolateService.updateIdentification(isolate.getId(), organismId, "Surveillance organism",
                MicroIsolateSignificance.CLINICALLY_SIGNIFICANT, MicroIsolateIdentificationStatus.CONFIRMED,
                "MALDI_TOF", new BigDecimal("99.5"), user);
        List<String> panel = astService.getPanelAntibiotics(scenario.astPanelId).stream()
                .map(ordered -> ordered.getAntibioticId()).toList();
        List<String> ordered = IntStream.range(0, readings.length).filter(i -> readings[i] != null).mapToObj(panel::get)
                .toList();
        MicroAstTechnique technique = MicroAstTechnique.BROTH_MICRODILUTION;
        MicroAstRun run = astService.startRun(isolate.getId(), scenario.astPanelId, scenario.activeBreakpointStandardId,
                "Surveillance fixture", technique, List.of(), ordered, user);
        for (int i = 0; i < readings.length; i++) {
            if (readings[i] != null) {
                astService.recordReading(run.getId(), panel.get(i), technique.measurementType(), readings[i], user);
            }
        }
        astService.reviewRun(run.getId(), user);
        reportReleaseService.releaseFinal(scenario.caseId, user);
        entityManager.createQuery("update MicroCase c set c.workflowType = 'BACTERIOLOGY' where c.id = :id")
                .setParameter("id", scenario.caseId).executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return scenario;
    }
}
