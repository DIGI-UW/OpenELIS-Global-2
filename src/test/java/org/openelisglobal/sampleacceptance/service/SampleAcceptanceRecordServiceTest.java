package org.openelisglobal.sampleacceptance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.sampleacceptance.valueholder.SampleAcceptanceRecord;
import org.openelisglobal.sampleacceptance.valueholder.SampleAcceptanceRecord.Answer;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

/**
 * Integration tests for the per-specimen Sample Acceptance Record service (S-09
 * / OGC-580): domain resolution (sample_item -> parent sample), append-only
 * recording, status computation, evaluation, and NCE pre-fill. The constants
 * are sample_item ids; their parent samples carry the H/E/V/null domains.
 */
@Rollback
public class SampleAcceptanceRecordServiceTest extends BaseWebContextSensitiveTest {

    // sample_item ids; parent samples 1001/1002/1003/1004 carry domains H/E/V/null.
    private static final String CLINICAL_ITEM = "2001";
    private static final String ENVIRONMENTAL_ITEM = "2002";
    private static final String VECTOR_ITEM = "2003";
    private static final String NO_DOMAIN_ITEM = "2004";

    // A pooled vector order (1005): pool 5001 with three member specimens.
    private static final Integer VECTOR_POOL_ID = 5001;
    private static final List<String> POOL_MEMBERS = List.of("2010", "2011", "2012");

    @Autowired
    private SampleAcceptanceRecordService service;

    @Autowired
    private ResampleService resampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-acceptance-record.xml");
    }

    @After
    public void resetEnforcement() {
        // Enforcement lives in the ConfigurationProperties singleton; restore the
        // seeded
        // default so a MANDATORY override here can't leak into other tests sharing the
        // cached Spring context.
        ConfigurationProperties cp = ConfigurationProperties.getInstance();
        cp.setPropertyValue(Property.SAMPLE_ACCEPTANCE_CHECKLIST_ENFORCEMENT_CLINICAL, "OPTIONAL");
        cp.setPropertyValue(Property.SAMPLE_ACCEPTANCE_CHECKLIST_ENFORCEMENT_ENVIRONMENTAL, "OPTIONAL");
        cp.setPropertyValue(Property.SAMPLE_ACCEPTANCE_CHECKLIST_ENFORCEMENT_VECTOR, "OPTIONAL");
    }

    // ---- domain resolution (sample.domain H/E/V -> full word)
    // -----------------------

    @Test
    public void resolveDomain_mapsSampleDomainCodesToFullWords() {
        assertEquals("CLINICAL", service.resolveDomain(CLINICAL_ITEM));
        assertEquals("ENVIRONMENTAL", service.resolveDomain(ENVIRONMENTAL_ITEM));
        assertEquals("VECTOR", service.resolveDomain(VECTOR_ITEM));
        assertNull(service.resolveDomain(NO_DOMAIN_ITEM));
    }

    // ---- recording + status computation
    // ---------------------------------------------

    @Test
    public void recordAssessment_allPassOrNa_isAccepted() {
        SampleAcceptanceRecord record = service.recordAssessment(CLINICAL_ITEM,
                answers(answer("containerIntact", "PASS", null), answer("labelLegible", "NA", null),
                        answer("volumeAdequate", "PASS", null)),
                1);

        assertEquals(SampleAcceptanceRecord.STATUS_ACCEPTED, record.getOverallStatus());
        assertEquals("CLINICAL", record.getDomain());
        assertEquals(Integer.valueOf(2001), record.getSampleItemId());
        assertNotNull(record.getId());
        assertNotNull(record.getAssessedAt());
    }

    @Test
    public void recordAssessment_anyFail_isReview() {
        SampleAcceptanceRecord record = service
                .recordAssessment(
                        CLINICAL_ITEM, answers(answer("containerIntact", "PASS", null),
                                answer("labelLegible", "PASS", null), answer("volumeAdequate", "FAIL", "tube cracked")),
                        1);

        assertEquals(SampleAcceptanceRecord.STATUS_REVIEW, record.getOverallStatus());
    }

    @Test
    public void recordAssessment_unansweredItem_isPending() {
        SampleAcceptanceRecord record = service.recordAssessment(CLINICAL_ITEM,
                answers(answer("containerIntact", "PASS", null)), 1);

        assertEquals(SampleAcceptanceRecord.STATUS_PENDING, record.getOverallStatus());
    }

    // ---- append-only history
    // --------------------------------------------------------

    @Test
    public void recordAssessment_isAppendOnly_latestWins() {
        service.recordAssessment(CLINICAL_ITEM, answers(answer("containerIntact", "FAIL", "first pass")), 1);
        SampleAcceptanceRecord second = service.recordAssessment(CLINICAL_ITEM,
                answers(answer("containerIntact", "PASS", null), answer("labelLegible", "PASS", null),
                        answer("volumeAdequate", "PASS", null)),
                1);

        List<SampleAcceptanceRecord> history = service.getHistory(CLINICAL_ITEM);
        assertEquals(2, history.size());
        // newest first
        assertEquals(second.getId(), history.get(0).getId());
        // latest reflects the corrected assessment, the original row is retained
        SampleAcceptanceRecord latest = service.getLatest(CLINICAL_ITEM);
        assertEquals(second.getId(), latest.getId());
        assertEquals(SampleAcceptanceRecord.STATUS_ACCEPTED, latest.getOverallStatus());
    }

    // ---- evaluation
    // -----------------------------------------------------------------

    @Test
    public void evaluate_returnsResolvedDomainEnforcementAndStatus() {
        service.recordAssessment(ENVIRONMENTAL_ITEM, answers(answer("containerIntact", "PASS", null),
                answer("labelLegible", "PASS", null), answer("volumeAdequate", "PASS", null)), 1);

        SampleAcceptanceEvaluation eval = service.evaluate(ENVIRONMENTAL_ITEM);
        assertEquals("ENVIRONMENTAL", eval.getDomain());
        assertEquals("OPTIONAL", eval.getEnforcement());
        assertEquals(SampleAcceptanceRecord.STATUS_ACCEPTED, eval.getOverallStatus());
        assertEquals(3, eval.getItems().size()); // domain empty -> lab-wide fallback
        // OPTIONAL enforcement never blocks
        assertFalse(eval.isBlocked());
    }

    @Test
    public void evaluate_withNoRecord_isPendingButNotBlockedUnderOptional() {
        SampleAcceptanceEvaluation eval = service.evaluate(VECTOR_ITEM);
        assertEquals("VECTOR", eval.getDomain());
        assertEquals(SampleAcceptanceRecord.STATUS_PENDING, eval.getOverallStatus());
        assertNull(eval.getLatest());
        assertFalse(eval.isBlocked());
    }

    /**
     * Order 1001 has two live specimens (sample_item 2001 + 2005); evaluateOrder
     * returns one evaluation per specimen, each resolving the parent order's
     * domain. Backs the QA-step intake-acceptance table.
     */
    @Test
    public void evaluateOrder_returnsAnEvaluationPerLiveSpecimenOfTheOrder() {
        List<SampleAcceptanceEvaluation> evals = service.evaluateOrder("1001");

        assertEquals("both clinical specimens of order 1001 are returned", 2, evals.size());
        Set<String> ids = evals.stream().map(SampleAcceptanceEvaluation::getSampleItemId).collect(Collectors.toSet());
        assertEquals(Set.of("2001", "2005"), ids);
        assertTrue("each evaluation resolves the parent order's domain",
                evals.stream().allMatch(e -> "CLINICAL".equals(e.getDomain())));
    }

    // ---- FR-08 enforcement (blocked computation + the server-side gate)
    // -----------

    /** The computation half: MANDATORY + an unsatisfied checklist => blocked. */
    @Test
    public void evaluate_mandatoryDomainWithUnsatisfiedChecklist_isBlocked() {
        ConfigurationProperties.getInstance()
                .setPropertyValue(Property.SAMPLE_ACCEPTANCE_CHECKLIST_ENFORCEMENT_CLINICAL, "MANDATORY");

        SampleAcceptanceEvaluation eval = service.evaluate(CLINICAL_ITEM); // no record => PENDING
        assertEquals("MANDATORY", eval.getEnforcement());
        assertEquals(SampleAcceptanceRecord.STATUS_PENDING, eval.getOverallStatus());
        assertTrue("MANDATORY + unsatisfied resolved checklist must block", eval.isBlocked());
    }

    /**
     * FR-08: the gate must REFUSE a blocked sample server-side. RED until
     * {@code enforceAcceptanceGate} is implemented — reproduces "blocked is
     * computed but never enforced".
     */
    @Test
    public void enforceAcceptanceGate_blockedMandatorySample_isRefused() {
        ConfigurationProperties.getInstance()
                .setPropertyValue(Property.SAMPLE_ACCEPTANCE_CHECKLIST_ENFORCEMENT_CLINICAL, "MANDATORY");
        // sanity: the sample is genuinely blocked (so a RED here means the gate failed
        // to enforce, not that the setup was wrong)
        assertTrue("fixture sanity: sample is blocked", service.evaluate(CLINICAL_ITEM).isBlocked());

        SampleAcceptanceBlockedException ex = assertThrows(SampleAcceptanceBlockedException.class,
                () -> service.enforceAcceptanceGate(CLINICAL_ITEM));
        assertTrue("rejection names the sample and the mandatory gate",
                ex.getMessage() != null && ex.getMessage().toLowerCase().contains("mandatory"));
    }

    /** The gate must NOT over-block: under OPTIONAL it lets the sample through. */
    @Test
    public void enforceAcceptanceGate_optionalDomain_passes() {
        // VECTOR defaults to OPTIONAL; no record => PENDING but not blocked.
        assertFalse("fixture sanity: not blocked under OPTIONAL", service.evaluate(VECTOR_ITEM).isBlocked());
        service.enforceAcceptanceGate(VECTOR_ITEM); // must not throw
    }

    // ---- NCE pre-fill
    // ---------------------------------------------------------------

    @Test
    public void buildNcePrefillReason_listsFailedItemsWithNotes() {
        service.recordAssessment(CLINICAL_ITEM, answers(answer("containerIntact", "PASS", null),
                answer("volumeAdequate", "Sample volume adequate", "FAIL", "tube cracked")), 1);

        String reason = service.buildNcePrefillReason(CLINICAL_ITEM);
        assertNotNull(reason);
        assertTrue(reason.contains("Sample volume adequate (tube cracked)"));
    }

    @Test
    public void buildNcePrefillReason_noFailures_isNull() {
        service.recordAssessment(CLINICAL_ITEM, answers(answer("containerIntact", "PASS", null),
                answer("labelLegible", "PASS", null), answer("volumeAdequate", "PASS", null)), 1);
        assertNull(service.buildNcePrefillReason(CLINICAL_ITEM));
    }

    // ---- vector pool acceptance (the pool is the unit; the decision cascades to
    // every member sample_item) ------------------------------------------------

    @Test
    public void recordAssessmentForPool_cascadesAcceptedToEveryMember() {
        List<SampleAcceptanceRecord> records = service.recordAssessmentForPool(VECTOR_POOL_ID,
                answers(answer("containerIntact", "PASS", null), answer("labelLegible", "PASS", null),
                        answer("volumeAdequate", "PASS", null)),
                1);

        assertEquals("one acceptance record per live pool member", 3, records.size());
        for (String memberId : POOL_MEMBERS) {
            SampleAcceptanceRecord latest = service.getLatest(memberId);
            assertNotNull("member " + memberId + " has an acceptance record", latest);
            assertEquals(SampleAcceptanceRecord.STATUS_ACCEPTED, latest.getOverallStatus());
            assertEquals("VECTOR", latest.getDomain());
        }
    }

    /**
     * Under MANDATORY vector enforcement every member blocks until accepted; the
     * single pool decision must satisfy the gate for the whole order at once.
     */
    @Test
    public void recordAssessmentForPool_underMandatory_unblocksTheWholePoolOrder() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.SAMPLE_ACCEPTANCE_CHECKLIST_ENFORCEMENT_VECTOR,
                "MANDATORY");
        for (String memberId : POOL_MEMBERS) {
            assertTrue("member " + memberId + " blocked before accept", service.evaluate(memberId).isBlocked());
        }

        service.recordAssessmentForPool(VECTOR_POOL_ID, answers(answer("containerIntact", "PASS", null),
                answer("labelLegible", "PASS", null), answer("volumeAdequate", "PASS", null)), 1);

        service.enforceAcceptanceGateForOrder("1005"); // must not throw
        for (String memberId : POOL_MEMBERS) {
            assertFalse("member " + memberId + " unblocked after pool accept", service.evaluate(memberId).isBlocked());
        }
    }

    /**
     * Rejecting a pool flags every member rejected (all-or-nothing, no replacement
     * order) so they drop out of the order's open acceptance rows.
     */
    @Test
    public void rejectPool_marksEveryMemberRejected_andClearsTheOrdersOpenRows() {
        resampleService.rejectPool(VECTOR_POOL_ID, "container arrived leaking", 1);

        for (String memberId : POOL_MEMBERS) {
            SampleItem member = sampleItemService.get(memberId);
            assertTrue("member " + memberId + " is rejected", member.isRejected());
        }
        assertTrue("no open acceptance rows remain for the rejected pool order",
                service.evaluateOrder("1005").isEmpty());
    }

    @Test
    public void rejectPool_nullId_isClientError() {
        assertThrows(IllegalArgumentException.class, () -> resampleService.rejectPool(null, "x", 1));
    }

    @Test
    public void rejectPool_unknownOrEmptyPool_isClientError() {
        assertThrows(IllegalArgumentException.class, () -> resampleService.rejectPool(9_999_999, "x", 1));
    }

    /**
     * Rejecting any single member of a pool rejects the WHOLE pool — a fanned-out
     * member can't be rejected in isolation (its tests live on the pool), so the
     * single-item reject path detects pool membership and delegates to rejectPool.
     */
    @Test
    public void reject_onAPooledMember_delegatesAndRejectsTheWholePool() {
        resampleService.reject(POOL_MEMBERS.get(0), "one member flagged", 1);

        for (String memberId : POOL_MEMBERS) {
            assertTrue("member " + memberId + " rejected via pool delegation",
                    sampleItemService.get(memberId).isRejected());
        }
    }

    // ---- helpers
    // --------------------------------------------------------------------

    private List<Answer> answers(Answer... a) {
        List<Answer> list = new ArrayList<>();
        for (Answer answer : a) {
            list.add(answer);
        }
        return list;
    }

    private Answer answer(String itemKey, String answer, String note) {
        return new Answer(itemKey, itemKey, answer, note);
    }

    private Answer answer(String itemKey, String label, String answer, String note) {
        return new Answer(itemKey, label, answer, note);
    }
}
