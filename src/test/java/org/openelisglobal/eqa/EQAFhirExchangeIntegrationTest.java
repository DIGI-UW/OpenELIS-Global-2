package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.eqa.service.EQAFhirExchangeService;
import org.openelisglobal.eqa.service.EQAFhirSubmissionService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The provider↔participant exchange over the provider's store (OGC-610 /
 * OGC-613), driven on one database standing in for both instances: the
 * participant's report is built from its cycle and imported box, re-addressed
 * to the provider's box (the consignment uuid both hold) and taken in; the
 * provider's score report is built and taken in the other way. Analytes travel
 * by name, ids differ per install.
 */
public class EQAFhirExchangeIntegrationTest extends EQASpineTestBase {

    private static final long ORG = 9990L;
    private static final long ENROLLMENT = 9905L;
    private static final long ANALYTE = 9821L;
    private static final String ANALYTE_NAME = "Exchange HIV VL";
    private static final long TEST = 9991L;
    private static final int PROVIDER_BOX = 9960;
    private static final int PARTICIPANT_BOX = 9961;

    @Autowired
    private EQAFhirSubmissionService submissionService;
    @Autowired
    private EQAFhirExchangeService exchangeService;
    @Autowired
    private FhirConfig fhirConfig;

    private EQAProgram scheme;
    private EQACycle providerCycle;
    private EQACycle participantCycle;
    private UUID providerConsignment;
    private UUID participantConsignment;

    @Before
    public void seed() {
        jdbc.update("INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                + " VALUES (?, 'Exchange participant lab', 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING", ORG);
        jdbc.update("INSERT INTO clinlims.analyte (id, name, is_active, lastupdated) VALUES (?, ?, 'Y', now())"
                + " ON CONFLICT (id) DO NOTHING", ANALYTE, ANALYTE_NAME);
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " SELECT ?, 'Exchange HIV VL test', 'Exchange HIV VL test', 'Y', ?, now()"
                        + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.test WHERE id = ?)",
                TEST, UUID.randomUUID().toString(), TEST);
        jdbc.update("DELETE FROM clinlims.test_analyte WHERE id = 99821");
        jdbc.update(
                "INSERT INTO clinlims.test_analyte (id, test_id, analyte_id, lastupdated) VALUES (99821, ?, ?, now())",
                TEST, ANALYTE);
        seedEnrollment(ENROLLMENT, "Exchange programme");

        scheme = insertScheme("Exchange scheme " + System.nanoTime(), EQASchemeType.REGIONAL_PT, "CPHL");
        eqaProgramService.assignTest(scheme.getId(), TEST);
        providerCycle = readBack(insertCycle(scheme, 1));
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'SUBMISSIONS_OPEN' WHERE id = ?", providerCycle.getId());
        participantCycle = readBack(insertCycle(scheme, 2));
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'SUBMITTED' WHERE id = ?", participantCycle.getId());
        Long roundId = insertRound(participantCycle, 1, "OPEN");
        insertParticipantResult(participantCycle, readBackRound(roundId), ENROLLMENT, ANALYTE,
                EQASubmissionStatus.SUBMITTED, "105.5");

        providerConsignment = UUID.randomUUID();
        participantConsignment = UUID.randomUUID();
        box(PROVIDER_BOX, "EXCH-P", providerConsignment, "SENT", providerCycle.getId());
        box(PARTICIPANT_BOX, "EXCH-Q", participantConsignment, "RECEIVED", participantCycle.getId());
    }

    @Override
    protected void cleanEqaTables() {
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.eqa_result");
            jdbc.update("DELETE FROM clinlims.eqa_distribution WHERE cycle_id IS NOT NULL");
            jdbc.update("DELETE FROM clinlims.shipping_box WHERE id IN (?, ?)", PROVIDER_BOX, PARTICIPANT_BOX);
        }
        super.cleanEqaTables();
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.test_analyte WHERE id = 99821");
            jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST);
            jdbc.update("DELETE FROM clinlims.analyte WHERE id = ?", ANALYTE);
            jdbc.update("DELETE FROM clinlims.organization WHERE id = ?", ORG);
        }
    }

    @Test
    public void aParticipantReportNamesItsConsignmentSchemeAndAnalytes() {
        Map<String, Resource> resources = submissionService.participantSubmissionResources(participantCycle.getId(),
                ENROLLMENT);

        DiagnosticReport report = report(resources);
        assertEquals(participantConsignment.toString(),
                identifier(report, EQAFhirSubmissionService.CONSIGNMENT_SUFFIX));
        assertEquals(scheme.getName(), identifier(report, EQAFhirSubmissionService.SCHEME_NAME_SUFFIX));
        assertEquals("2", identifier(report, EQAFhirSubmissionService.CYCLE_NUMBER_SUFFIX));
        List<Observation> observations = observations(resources);
        assertEquals(1, observations.size());
        assertEquals("the analyte travels by name", ANALYTE_NAME, observations.get(0).getCode().getText());
        assertEquals(0, new BigDecimal("105.5").compareTo(observations.get(0).getValueQuantity().getValue()));
    }

    @Test
    public void theProviderTakesInAParticipantReportOnce() {
        Map<String, Resource> resources = submissionService.participantSubmissionResources(participantCycle.getId(),
                ENROLLMENT);
        DiagnosticReport report = report(resources);
        // On the provider instance the same consignment is its own dispatched box.
        readdress(report, providerConsignment);

        assertTrue("the report is taken in", exchangeService.applyParticipantReport(report, observations(resources)));

        Long distributionId = jdbc.queryForObject("SELECT id FROM clinlims.eqa_distribution WHERE cycle_id = ?",
                Long.class, providerCycle.getId());
        assertNotNull("the provider's distribution is opened on demand", distributionId);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT result_value, submission_method FROM clinlims.eqa_result"
                        + " WHERE eqa_distribution_id = ? AND participant_organization_id = ? AND test_id = ?",
                distributionId, ORG, TEST);
        assertEquals(0, new BigDecimal("105.5").compareTo((BigDecimal) row.get("result_value")));
        assertEquals("FHIR", row.get("submission_method"));

        assertFalse("a replay of the same report changes nothing",
                exchangeService.applyParticipantReport(report, observations(resources)));
        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_result WHERE eqa_distribution_id = ?",
                        Integer.class, distributionId));
    }

    @Test
    public void aScoreReportIsAddressedToTheConsignmentAndTheParticipantTakesItInOnce() {
        Map<String, Resource> inbound = submissionService.participantSubmissionResources(participantCycle.getId(),
                ENROLLMENT);
        readdress(report(inbound), providerConsignment);
        exchangeService.applyParticipantReport(report(inbound), observations(inbound));
        Long distributionId = jdbc.queryForObject("SELECT id FROM clinlims.eqa_distribution WHERE cycle_id = ?",
                Long.class, providerCycle.getId());
        jdbc.update("UPDATE clinlims.eqa_result SET z_score = 1.2, performance_status = 'ACCEPTABLE'"
                + " WHERE eqa_distribution_id = ?", distributionId);

        Map<String, Resource> scores = submissionService.scoreReturnResources(distributionId, ORG);
        DiagnosticReport scoreReport = report(scores);
        assertEquals(providerConsignment.toString(), identifier(scoreReport, EQAFhirSubmissionService.SCORES_SUFFIX));
        assertEquals(providerConsignment.toString(),
                identifier(scoreReport, EQAFhirSubmissionService.CONSIGNMENT_SUFFIX));
        Observation scored = observations(scores).get(0);
        assertEquals(ANALYTE_NAME, scored.getCode().getText());
        assertEquals("ACCEPTABLE", scored.getInterpretationFirstRep().getCodingFirstRep().getCode());

        // On the participant instance the same consignment is the box it imported.
        readdress(scoreReport, participantConsignment);
        assertTrue(exchangeService.applyScoreReport(scoreReport, observations(scores)));

        Map<String, Object> result = jdbc.queryForMap("SELECT submission_status, performance_status, z_score"
                + " FROM clinlims.eqa_participant_result WHERE cycle_id = ?", participantCycle.getId());
        assertEquals("SCORED", result.get("submission_status"));
        assertEquals("ACCEPTABLE", result.get("performance_status"));
        assertEquals(0, new BigDecimal("1.2").compareTo((BigDecimal) result.get("z_score")));
        assertEquals("SCORED", jdbc.queryForObject("SELECT status FROM clinlims.eqa_cycle WHERE id = ?", String.class,
                participantCycle.getId()));

        assertFalse("a replayed score report is a no-op, not a second scoring",
                exchangeService.applyScoreReport(scoreReport, observations(scores)));
    }

    @Test
    public void aReportForAnUnknownConsignmentIsNotApplied() {
        Map<String, Resource> resources = submissionService.participantSubmissionResources(participantCycle.getId(),
                ENROLLMENT);
        DiagnosticReport report = report(resources);
        readdress(report, UUID.randomUUID());

        assertFalse(exchangeService.applyParticipantReport(report, observations(resources)));
        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_result", Integer.class));
    }

    // ---- helpers ----

    private void box(int id, String code, UUID fhirUuid, String state, Long cycleId) {
        jdbc.update(
                "INSERT INTO clinlims.shipping_box (id, box_id, fhir_uuid, destination_facility_id, state,"
                        + " created_date, archived, sys_user_id, lastupdated, eqa_cycle_id)"
                        + " VALUES (?, ?, ?, ?, ?, now(), false, ?, now(), ?)",
                id, code, fhirUuid, ORG, state, Integer.parseInt(USER), cycleId);
    }

    private org.openelisglobal.eqa.valueholder.EQARound readBackRound(Long roundId) {
        return eqaRoundDAO.get(roundId).orElseThrow(AssertionError::new);
    }

    private static DiagnosticReport report(Map<String, Resource> resources) {
        for (Resource resource : resources.values()) {
            if (resource instanceof DiagnosticReport report) {
                return report;
            }
        }
        throw new AssertionError("no DiagnosticReport in the bundle");
    }

    private static List<Observation> observations(Map<String, Resource> resources) {
        List<Observation> observations = new ArrayList<>();
        for (Resource resource : resources.values()) {
            if (resource instanceof Observation observation) {
                observations.add(observation);
            }
        }
        return observations;
    }

    private String identifier(DiagnosticReport report, String suffix) {
        String system = fhirConfig.getOeFhirSystem() + suffix;
        for (Identifier identifier : report.getIdentifier()) {
            if (system.equals(identifier.getSystem())) {
                return identifier.getValue();
            }
        }
        return null;
    }

    /**
     * What the other instance sees: the same consignment under its own box's uuid.
     */
    private void readdress(DiagnosticReport report, UUID consignment) {
        String consignmentSystem = fhirConfig.getOeFhirSystem() + EQAFhirSubmissionService.CONSIGNMENT_SUFFIX;
        String scoresSystem = fhirConfig.getOeFhirSystem() + EQAFhirSubmissionService.SCORES_SUFFIX;
        for (Identifier identifier : report.getIdentifier()) {
            if (consignmentSystem.equals(identifier.getSystem()) || scoresSystem.equals(identifier.getSystem())) {
                identifier.setValue(consignment.toString());
            }
        }
    }
}
