package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.SupplyDelivery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.eqa.controller.rest.EQACycleRestController;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.service.EQALabProgramEnrollmentService;
import org.openelisglobal.eqa.service.EQAPerformanceReportPDFService;
import org.openelisglobal.eqa.service.EQAReportCommentService;
import org.openelisglobal.eqa.service.SampleEQAService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.shipment.fhir.ShipmentFhirImportService;
import org.openelisglobal.shipment.fhir.ShippingBoxFhirTransform;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The participant side of a cycle (OGC-610): a consignment from an OpenELIS
 * provider names its cycle and the import opens the matching local one; a lab
 * whose provider is not an OpenELIS records the cycle itself from My Cycles;
 * and My Cycles lists only the cycles this laboratory takes part in.
 */
public class EQAParticipantCycleIntegrationTest extends EQASpineTestBase {

    private static final long ORG_ID = 9970L;
    private static final String ORG_NAME = "Participant lab 9970";

    @Autowired
    private EQACycleService cycleService;
    @Autowired
    private EQALabProgramEnrollmentService enrollmentService;
    @Autowired
    private ShipmentFhirImportService importService;
    @Autowired
    private ShippingBoxFhirTransform transform;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private SampleEQAService sampleEQAService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private EQAPerformanceReportPDFService reportService;
    @Autowired
    private EQAReportCommentService reportCommentService;

    @Before
    public void seedOrganization() {
        jdbc.update("INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                + " VALUES (?, ?, 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING", ORG_ID, ORG_NAME);
    }

    @After
    public void cleanConsignments() {
        // Imported boxes reference the cycles the base class clears afterwards.
        jdbc.update("DELETE FROM clinlims.shipping_box WHERE box_id LIKE 'PCYC-%'");
        jdbc.update("DELETE FROM clinlims.organization WHERE id = ?", ORG_ID);
    }

    @Test
    public void aNamedConsignmentCycleIsOpenedOnceWithItsDeadline() {
        EQAProgram scheme = insertScheme("Participant scheme A", EQASchemeType.REGIONAL_PT, "CPHL");
        Date distribution = Date.valueOf("2026-09-01");
        Date deadline = Date.valueOf("2026-09-15");

        EQACycle cycle = cycleService
                .ensureParticipantCycle("Participant scheme A", 7, "Round 7", distribution, deadline, USER)
                .orElseThrow(AssertionError::new);

        assertEquals(Integer.valueOf(7), cycle.getCycleNumber());
        assertEquals("Round 7", cycle.getCycleName());
        assertEquals(EQACycleStatus.PLANNED, cycle.getStatus());
        assertEquals(scheme.getId(), cycle.getScheme().getId());
        assertEquals("2026-09-15",
                jdbc.queryForObject("SELECT submission_deadline::date::text FROM clinlims.eqa_round WHERE cycle_id = ?",
                        String.class, cycle.getId()));
        assertEquals("2026-09-01",
                jdbc.queryForObject("SELECT distribution_date::date::text FROM clinlims.eqa_round WHERE cycle_id = ?",
                        String.class, cycle.getId()));

        // The import poll runs every few minutes: the same consignment must land on
        // the same cycle, and a second round must not appear.
        EQACycle again = cycleService
                .ensureParticipantCycle("Participant scheme A", 7, "Round 7 (repeat)", distribution, deadline, USER)
                .orElseThrow(AssertionError::new);
        assertEquals(cycle.getId(), again.getId());
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_cycle WHERE scheme_id = ?", Integer.class, scheme.getId()));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_round WHERE cycle_id = ?", Integer.class, cycle.getId()));

        assertFalse("a programme this instance does not know opens nothing",
                cycleService.ensureParticipantCycle("No such programme", 1, null, null, deadline, USER).isPresent());
    }

    @Test
    public void anImportedConsignmentThatNamesItsCycleLinksTheBoxToTheLocalCycle() {
        insertScheme("Participant scheme B", EQASchemeType.REGIONAL_PT, "CPHL");
        String boxCode = "PCYC-" + System.nanoTime();
        SupplyDelivery delivery = deliveryFor(boxCode);
        delivery.addExtension(cycleExtension("Participant scheme B", 3, "Round 3"));

        assertTrue("the consignment imports", importService.importSupplyDelivery(delivery));

        Long linked = jdbc.queryForObject("SELECT eqa_cycle_id FROM clinlims.shipping_box WHERE box_id = ?", Long.class,
                boxCode);
        assertNotNull("the imported box is linked to a cycle", linked);
        Map<String, Object> cycle = jdbc
                .queryForMap("SELECT cycle_number, cycle_name, status FROM clinlims.eqa_cycle WHERE id = ?", linked);
        assertEquals(3, ((Number) cycle.get("cycle_number")).intValue());
        assertEquals("Round 3", cycle.get("cycle_name"));
        assertEquals("PLANNED", cycle.get("status"));
        assertEquals("2026-09-15",
                jdbc.queryForObject("SELECT submission_deadline::date::text FROM clinlims.eqa_round WHERE cycle_id = ?",
                        String.class, linked));
    }

    @Test
    public void aConsignmentForAnUnknownProgrammeStillImportsWithoutACycle() {
        String boxCode = "PCYC-" + System.nanoTime();
        SupplyDelivery delivery = deliveryFor(boxCode);
        delivery.addExtension(cycleExtension("Programme nobody here runs", 1, "Ghost round"));

        assertTrue(importService.importSupplyDelivery(delivery));

        assertNull(jdbc.queryForObject("SELECT eqa_cycle_id FROM clinlims.shipping_box WHERE box_id = ?", Long.class,
                boxCode));
        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_cycle WHERE cycle_name = 'Ghost round'", Integer.class));
    }

    @Test
    public void anEqaBoxExportsTheCycleItBelongsTo() {
        EQAProgram scheme = insertScheme("Provider scheme C", EQASchemeType.REGIONAL_PT, "CPHL");
        Long cycleId = insertCycle(scheme, 2);
        jdbc.update("UPDATE clinlims.eqa_cycle SET cycle_name = 'Round 2', planned_start_date = '2026-09-01',"
                + " planned_end_date = '2026-09-15' WHERE id = ?", cycleId);
        ShippingBox box = new ShippingBox();
        box.setBoxId("PCYC-EXPORT");
        box.setState(BoxState.SENT);
        box.setEqaCycleId(cycleId);
        box.setCreatedDate(new Timestamp(System.currentTimeMillis()));

        SupplyDelivery delivery = new TransactionTemplate(transactionManager)
                .execute(tx -> transform.transformToSupplyDelivery(box));

        Extension ext = delivery.getExtensionByUrl(ShippingBoxFhirTransform.EXT_EQA_CYCLE);
        assertNotNull("an EQA box names its cycle", ext);
        assertEquals("Provider scheme C", ((StringType) ext.getExtensionByUrl("scheme").getValue()).getValue());
        assertEquals(2, ((IntegerType) ext.getExtensionByUrl("number").getValue()).getValue().intValue());
        assertEquals("Round 2", ((StringType) ext.getExtensionByUrl("name").getValue()).getValue());
        assertEquals("2026-09-01",
                ((DateType) ext.getExtensionByUrl("distributionDate").getValue()).getValueAsString());
        assertEquals("2026-09-15",
                ((DateType) ext.getExtensionByUrl("submissionDeadline").getValue()).getValueAsString());
    }

    @Test
    public void myCyclesListsEnrolledInHouseAndNotProviderOnlyCycles() {
        EQAProgram enrolled = insertScheme("Enrolled scheme D", EQASchemeType.REGIONAL_PT, "CPHL");
        seedEnrollment(9953, "Enrolled scheme D");
        EQAProgram inHouse = insertScheme("Own in-house scheme E", EQASchemeType.IN_HOUSE, null);
        EQAProgram foreign = insertScheme("Provider-only scheme F", EQASchemeType.REGIONAL_PT, "CPHL");
        Long mine = insertCycle(enrolled, 1);
        Long ours = insertCycle(inHouse, 1);
        Long theirs = insertCycle(foreign, 1);

        Set<Long> listed = controller().myCycles(null).stream().map(row -> ((Number) row.get("id")).longValue())
                .collect(Collectors.toSet());

        assertTrue("an enrolled programme's cycle is mine", listed.contains(mine));
        assertTrue("our own in-house cycle is mine", listed.contains(ours));
        assertFalse("a cycle of a programme this lab has not enrolled in is not", listed.contains(theirs));
    }

    @Test
    public void aParticipantCreatesACycleOnlyForAProgrammeItHasEnrolledIn() {
        insertScheme("Enrolled scheme G", EQASchemeType.REGIONAL_PT, "CPHL");
        seedEnrollment(9954, "Enrolled scheme G");
        insertScheme("Not enrolled scheme H", EQASchemeType.REGIONAL_PT, "CPHL");

        Map<String, Object> dto = controller().createMyCycle(requestForUser(),
                Map.of("schemeName", "Enrolled scheme G", "cycleName", "Round 1", "submissionDeadline", "2026-09-30"));

        assertEquals("PLANNED", dto.get("status"));
        assertEquals("Round 1", dto.get("cycleName"));
        Long cycleId = ((Number) dto.get("id")).longValue();
        assertEquals("2026-09-30",
                jdbc.queryForObject("SELECT submission_deadline::date::text FROM clinlims.eqa_round WHERE cycle_id = ?",
                        String.class, cycleId));
        assertTrue("the new cycle is listed on My Cycles", controller().myCycles(null).stream()
                .anyMatch(row -> cycleId.equals(((Number) row.get("id")).longValue())));

        try {
            controller().createMyCycle(requestForUser(), Map.of("schemeName", "Not enrolled scheme H", "cycleName",
                    "Round 1", "submissionDeadline", "2026-09-30"));
            fail("a programme the lab has not enrolled in must be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("not enrolled"));
        }
        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_cycle c"
                        + " JOIN clinlims.eqa_program p ON p.id = c.scheme_id WHERE p.name = 'Not enrolled scheme H'",
                        Integer.class));
    }

    // ---- helpers ----

    private EQACycleRestController controller() {
        return new EQACycleRestController(cycleService, sampleEQAService, sampleService, analysisService, resultService,
                reportService, reportCommentService, systemUserService, enrollmentService);
    }

    private MockHttpServletRequest requestForUser() {
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(USER));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }

    private SupplyDelivery deliveryFor(String boxCode) {
        SupplyDelivery delivery = new SupplyDelivery();
        delivery.setId(java.util.UUID.randomUUID().toString());
        delivery.addIdentifier().setSystem("http://openelis.org/shipment/box-id").setValue(boxCode);
        delivery.setOccurrence(new DateTimeType(new java.util.Date()));
        delivery.setDestination(new Reference().setDisplay(ORG_NAME));
        return delivery;
    }

    private Extension cycleExtension(String schemeName, int number, String name) {
        Extension ext = new Extension(ShippingBoxFhirTransform.EXT_EQA_CYCLE);
        ext.addExtension(new Extension("scheme", new StringType(schemeName)));
        ext.addExtension(new Extension("number", new IntegerType(number)));
        ext.addExtension(new Extension("name", new StringType(name)));
        ext.addExtension(new Extension("distributionDate", new DateType(Date.valueOf("2026-09-01"))));
        ext.addExtension(new Extension("submissionDeadline", new DateType(Date.valueOf("2026-09-15"))));
        return ext;
    }
}
