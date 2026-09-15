package org.openelisglobal.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.referral.dao.ReferralDAO;
import org.openelisglobal.referral.dao.ReferralStatusHistoryDAO;
import org.openelisglobal.referral.dto.ReferenceLabMetricsDTO;
import org.openelisglobal.referral.service.ReferenceLabResultsService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.referral.valueholder.ReferralStatusHistory;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.action.util.ResultUtil;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Refer Out from Results Entry, end to end through the save (OGC-1188).
 *
 * <p>
 * The referral used to be written straight to the deprecated {@code SENT}
 * status, with no subcontract row and no history: the Reference Lab Results
 * page counts REQUESTED/RECEIVED/IN_PROGRESS as outstanding and shipment
 * dispatch only advances DRAFT, so the row was saved and then invisible to
 * both, and every lifecycle transition no-opped on the missing subcontract row.
 *
 * <p>
 * These tests pin the three properties that made it invisible, and the trap the
 * fix opens up: the OGC-799 manual-entry hook runs in this same save and would
 * complete a referral the moment it was raised.
 */
public class ResultsEntryReferOutLifecycleTest extends BaseWebContextSensitiveTest {

    private static final String ANALYSIS_ID = "1";
    private static final String REFERRED_TEST_ID = "1";
    private static final String DESTINATION_ORG_ID = "1";
    private static final String REFERRAL_REASON_ID = "1";
    private static final String ACTOR = "1";

    @Autowired
    private LogbookResultsPersistService logbookPersistService;

    @Autowired
    private ReferenceLabResultsService referenceLabResultsService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ReferralDAO referralDAO;

    @Autowired
    private ReferralStatusHistoryDAO statusHistoryDAO;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private IStatusService statusService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/referral.xml");
        // The dataset seeds explicit ids, so every sequence this save draws from has
        // to be moved past them or the first insert collides with a seeded row.
        resyncSequence("clinlims.result_seq", "clinlims.result");
        resyncSequence("clinlims.referral_seq", "clinlims.referral");
        resyncSequence("clinlims.referral_result_seq", "clinlims.referral_result");
        resyncSequence("clinlims.referral_subcontract_seq", "clinlims.referral_subcontract");
        resyncSequence("clinlims.referral_status_history_seq", "clinlims.referral_status_history");
        resyncSequence("clinlims.note_seq", "clinlims.note");

        // The save moves the sample to "Testing Started" and files the analysis under
        // "Not Tested"; the referral fixture seeds neither, so StatusService would hand
        // back an id no row has and the sample update would break its foreign key.
        // "Testing Started" is an ORDER-domain row: that is the map OrderStatus reads.
        seedStatusIfMissing(30, 300, "ORDER", "Testing Started");
        seedStatusIfMissing(31, 4, "ANALYSIS", "Not Tested");
        statusService.refreshCache();

        // The fixture already seeds a referral on this analysis, and
        // getReferralByAnalysisId returns the first match. Clear it so the referral
        // these tests raise is the one the manual-entry hook sees.
        jdbcTemplate.update("DELETE FROM clinlims.referral_status_history WHERE referral_id IN"
                + " (SELECT id FROM clinlims.referral WHERE analysis_id = CAST(? AS numeric))", ANALYSIS_ID);
        jdbcTemplate.update("DELETE FROM clinlims.referral WHERE analysis_id = CAST(? AS numeric)", ANALYSIS_ID);
    }

    private void seedStatusIfMissing(int id, int code, String statusType, String name) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clinlims.status_of_sample WHERE id = ?",
                Integer.class, id);
        if (count == null || count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description, lastupdated)"
                            + " VALUES (?, ?, ?, ?, ?, now())",
                    id, code, statusType, name, name);
        }
    }

    @Test
    public void referOutWithSendDate_isDispatchedAndCountedOutstanding() {
        long outstandingBefore = outstanding();

        Referral referral = saveReferOut(today());

        assertEquals("a refer-out whose send date says the specimen left is dispatched, not left in DRAFT",
                ReferralStatus.REQUESTED, referral.getStatus());
        assertNotNull("every new referral needs the subcontract row the lifecycle transitions read",
                referral.getSubcontract());
        assertEquals("the referral the bench just raised must appear as outstanding on the Reference Lab page",
                outstandingBefore + 1, outstanding());
    }

    @Test
    public void referOutWithSendDate_writesInceptionAndDispatchHistory() {
        Referral referral = saveReferOut(today());

        List<ReferralStatusHistory> history = historyFor(referral.getId());
        assertEquals("the activity log opens at inception and records the dispatch", 2, history.size());
        assertNull("the first row opens the log: nothing to DRAFT", history.get(0).getFromStatus());
        assertEquals(ReferralStatus.DRAFT, history.get(0).getToStatus());
        assertEquals(ReferralStatus.DRAFT, history.get(1).getFromStatus());
        assertEquals(ReferralStatus.REQUESTED, history.get(1).getToStatus());
    }

    @Test
    public void referOutWithoutSendDate_staysDraftForTheShipmentBoxToDispatch() {
        long outstandingBefore = outstanding();

        Referral referral = saveReferOut(null);

        assertEquals("with no handoff date the specimen has not left yet, so the referral waits in DRAFT",
                ReferralStatus.DRAFT, referral.getStatus());
        assertEquals("DRAFT is not outstanding — the reference lab has not been asked for anything yet",
                outstandingBefore, outstanding());
        assertEquals("a DRAFT referral has only its inception row", 1, historyFor(referral.getId()).size());
    }

    /**
     * The manual-entry hook (OGC-799) completes a referral when a result is saved
     * against its analysis. A refer-out saves a result for that same analysis in
     * the same request, so without a guard the referral is completed the instant it
     * is raised — and lands in History having never been sent anywhere.
     */
    @Test
    public void referOutIsNotCompletedByTheManualEntryHookInItsOwnSave() {
        Referral referral = saveReferOut(today());

        assertFalse("a request going out is not a result coming back",
                ReferralStatus.COMPLETED.equals(referral.getStatus()));
        assertFalse("and it is not a manual entry either", Boolean.TRUE.equals(referral.getManuallyEntered()));
    }

    /**
     * A rival referral on the same test would carry its own subcontract row and its
     * own FHIR Task, and nothing downstream could say which one the reference lab
     * is working on.
     */
    @Test
    public void aSecondReferOutIsRefusedWhileTheFirstReferralIsStillOpen() {
        saveReferOut(today());

        ResultsUpdateDataSet second = attemptReferOut(today());

        assertTrue("no second referral is raised while one is open", second.getSavableReferralSets().isEmpty());
        assertEquals("the test still carries exactly one referral", 1, referralCountForAnalysis());
    }

    /** Cancelling releases the test: the bench can refer it somewhere else. */
    @Test
    public void referOutIsAllowedAgainOnceTheOpenReferralIsCancelled() {
        Referral first = saveReferOut(today());
        jdbcTemplate.update("UPDATE clinlims.referral SET status = ? WHERE id = CAST(? AS numeric)",
                ReferralStatus.CANCELLED.name(), first.getId());

        ResultsUpdateDataSet second = attemptReferOut(today());

        assertFalse("a cancelled referral no longer blocks a fresh one", second.getSavableReferralSets().isEmpty());
    }

    /** The writer without the precondition assertions, so a refusal can be read. */
    private ResultsUpdateDataSet attemptReferOut(String sendDate) {
        Analysis analysis = analysisService.get(ANALYSIS_ID);
        ResultsUpdateDataSet dataSet = new ResultsUpdateDataSet(ACTOR);
        TestResultItem item = referredTestResultItem(sendDate);
        ResultUtil.handleReferrals(item, item.getReferralItem(), resultsFor(analysis), analysis, dataSet,
                requestWithLoggedInUser());
        return dataSet;
    }

    private int referralCountForAnalysis() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clinlims.referral WHERE analysis_id = CAST(? AS numeric)", Integer.class,
                ANALYSIS_ID);
        return count == null ? 0 : count;
    }

    /** The Refer Out row posts the date in the locale format the parser expects. */
    private String today() {
        return new java.text.SimpleDateFormat(org.openelisglobal.common.util.DateUtil.getDateFormat())
                .format(new java.util.Date());
    }

    private Referral saveReferOut(String sendDate) {
        Analysis analysis = analysisService.get(ANALYSIS_ID);
        assertNotNull("precondition: testdata/referral.xml seeds analysis " + ANALYSIS_ID, analysis);

        ResultsUpdateDataSet dataSet = new ResultsUpdateDataSet(ACTOR);
        TestResultItem testResultItem = referredTestResultItem(sendDate);

        List<Result> results = resultsFor(analysis);
        // The bench saves the in-house result in the same request as the refer-out;
        // that result is what arms the OGC-799 manual-entry hook. The sample rides
        // along because the reflex pass reads it off every new result.
        Sample sample = analysis.getSampleItem().getSample();
        dataSet.getNewResults().add(new ResultSet(results.get(0), null, null,
                sampleHumanService.getPatientForSample(sample), sample, new HashMap<>(), false));

        ResultUtil.handleReferrals(testResultItem, testResultItem.getReferralItem(), results, analysis, dataSet,
                requestWithLoggedInUser());

        assertTrue("precondition: the writer appends a savable referral set",
                !dataSet.getSavableReferralSets().isEmpty());
        String referralId = dataSet.getSavableReferralSets().get(0).getReferral().getId();
        assertNull("precondition: the referral is new, not an edit of a saved one", referralId);

        logbookPersistService.persistDataSet(dataSet, new ArrayList<>(), ACTOR);

        Referral saved = dataSet.getSavableReferralSets().get(0).getReferral();
        return referralDAO.get(saved.getId()).orElseThrow();
    }

    private TestResultItem referredTestResultItem(String sendDate) {
        TestResultItem item = new TestResultItem();
        item.setAnalysisId(ANALYSIS_ID);
        item.setTestId(REFERRED_TEST_ID);
        item.setResultType("N");
        item.setResultValue("12");
        item.setRefer(true);
        item.setReferredOut(true);
        item.setTechnician("bench");

        ReferralItem referralItem = new ReferralItem();
        referralItem.setReferredInstituteId(DESTINATION_ORG_ID);
        referralItem.setReferralReasonId(REFERRAL_REASON_ID);
        referralItem.setReferredTestId(REFERRED_TEST_ID);
        referralItem.setReferrer("bench");
        referralItem.setReferredSendDate(sendDate);
        item.setReferralItem(referralItem);
        return item;
    }

    private List<Result> resultsFor(Analysis analysis) {
        Result result = new Result();
        result.setAnalysis(analysis);
        result.setResultType("N");
        result.setValue("12");
        result.setSysUserId(ACTOR);
        result.setLastupdated(new Timestamp(System.currentTimeMillis()));
        List<Result> results = new ArrayList<>();
        results.add(result);
        return results;
    }

    /**
     * {@code ControllerUtills.getSysUserId} reads the OE session first, so the
     * savable notes the writer builds get an author without a SecurityContext.
     */
    private HttpServletRequest requestWithLoggedInUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData userSessionData = new UserSessionData();
        // setSytemUserId: the typo is the real setter name on UserSessionData.
        userSessionData.setSytemUserId(Integer.parseInt(ACTOR));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
        return request;
    }

    private long outstanding() {
        ReferenceLabMetricsDTO metrics = referenceLabResultsService.getDashboardMetrics();
        return metrics.getOutstanding();
    }

    private List<ReferralStatusHistory> historyFor(String referralId) {
        return statusHistoryDAO.findByReferralIdOrderedByChangedAt(referralId);
    }
}
