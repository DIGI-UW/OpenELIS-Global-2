package org.openelisglobal.batchworkplan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanDAO;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplan;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Batch Workplan read paths, against a real database: the pending-tests
 * query and the batch list.
 *
 * <p>
 * The defect this guards: the query used to take a fixed 500-row window and
 * discard already-batched rows afterwards, in Java, so beyond 500 pending
 * analyses a window of already-batched work returned an empty page while
 * eligible work sat just past it. Lab-unit scoping was absent entirely. Both
 * exclusions now live in the query and the row cap is applied last.
 *
 * <p>
 * The batch list had the mirror problem: it returned every batch in the
 * database regardless of owner or status, so one technician saw another's work
 * and an archived batch stayed on screen with no actions left on it.
 *
 * <p>
 * The service-level lab-unit scoping is not exercised here on purpose. It
 * resolves the caller's units through the HTTP session, which no integration
 * test has; outside a request the resolver legitimately answers "no units".
 * That path is covered by the service unit tests and by live verification.
 */
public class BatchWorkplanReadPathIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String ACTOR = "1";

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private BatchWorkplanDAO batchWorkplanDAO;

    private String pendingStatusId;
    /**
     * Pending analyses that already exist for the chosen test before this class
     * runs. Every assertion excludes them, so a full-suite run where another class
     * has inserted pending work cannot turn these into false reds.
     */
    private List<String> ambientPending = new ArrayList<>();
    private String testInUnitA;
    private String testInUnitB;
    private final List<String> createdAnalysisIds = new ArrayList<>();
    private final List<String> createdSampleItemIds = new ArrayList<>();
    private final List<String> createdSampleIds = new ArrayList<>();
    private final List<Long> createdBatchIds = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        // Seed the catalog this class needs rather than assuming it survives. In a
        // full-suite run an earlier class can leave no lab unit owning a test, and
        // discovery against whatever happens to be there then fails. This fixture
        // declares test and test_section but not status_of_sample, sample or
        // analysis, so it settles the catalog without disturbing anything else.
        executeDataSetWithStateManagement("testdata/panel-item.xml");
        pendingStatusId = statusService.getStatusID(AnalysisStatus.NotStarted);

        // Two tests from two different lab units, resolved from the fixture rather
        // than hardcoded: FK ids differ between a fresh container and an upgraded one.
        List<TestSection> sections = testSectionService.getAllActiveTestSections();
        String unitA = null;
        String unitB = null;
        for (TestSection section : sections) {
            List<org.openelisglobal.test.valueholder.Test> tests = testService
                    .getTestsByTestSectionIds(Collections.singletonList(section.getId()));
            if (tests.isEmpty()) {
                continue;
            }
            if (unitA == null) {
                unitA = section.getId();
                testInUnitA = tests.get(0).getId();
            } else if (!section.getId().equals(unitA)) {
                unitB = section.getId();
                testInUnitB = tests.get(0).getId();
                break;
            }
        }
        assertTrue("fixture needs two lab units that each own at least one test",
                testInUnitA != null && testInUnitB != null);

        ambientPending = analysisService
                .getPendingAnalysesForWorkplan(Collections.singletonList(pendingStatusId),
                        Arrays.asList(testInUnitA, testInUnitB), Collections.emptyList(), 0)
                .stream().map(Analysis::getId).collect(Collectors.toList());
    }

    /**
     * The given exclusions plus whatever pending work the database already held.
     */
    private List<String> excluding(List<String> ids) {
        List<String> all = new ArrayList<>(ambientPending);
        all.addAll(ids);
        return all;
    }

    @After
    public void tearDown() throws Exception {
        // Truncate rather than delete each entity: the DAO hands back detached
        // instances outside the service transaction, which remove() refuses.
        if (!createdBatchIds.isEmpty()) {
            cleanRowsInCurrentConnection(new String[] { "batch_workplan_item", "batch_workplan" });
            createdBatchIds.clear();
        }
        for (String id : createdAnalysisIds) {
            Analysis analysis = analysisService.get(id);
            if (analysis != null) {
                analysisService.delete(analysis);
            }
        }
        for (String id : createdSampleItemIds) {
            SampleItem item = sampleItemService.get(id);
            if (item != null) {
                sampleItemService.delete(item);
            }
        }
        for (String id : createdSampleIds) {
            Sample sample = sampleService.get(id);
            if (sample != null) {
                sampleService.delete(sample);
            }
        }
        createdAnalysisIds.clear();
        createdSampleItemIds.clear();
        createdSampleIds.clear();
    }

    @Test
    public void capIsAppliedAfterExclusions_soBatchedRowsCannotCrowdOutEligibleWork() {
        // Five pending analyses. The first three, in accession order, are already
        // held by a batch. Ask the query for two rows: it must skip the excluded
        // three and answer with the next two, not stop at the cap and answer empty.
        List<String> ordered = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ordered.add(createPendingAnalysis(String.format("ITBW%05d", i), testInUnitA));
        }
        List<String> alreadyBatched = ordered.subList(0, 3);

        List<Analysis> page = analysisService.getPendingAnalysesForWorkplan(Collections.singletonList(pendingStatusId),
                Collections.singletonList(testInUnitA), excluding(alreadyBatched), 2);

        List<String> returned = page.stream().map(Analysis::getId).collect(Collectors.toList());
        assertEquals("the two eligible rows past the excluded window", Arrays.asList(ordered.get(3), ordered.get(4)),
                returned);
    }

    @Test
    public void excludedAnalysesNeverComeBack() {
        String kept = createPendingAnalysis("ITBW10001", testInUnitA);
        String batched = createPendingAnalysis("ITBW10002", testInUnitA);

        List<Analysis> page = analysisService.getPendingAnalysesForWorkplan(Collections.singletonList(pendingStatusId),
                Collections.singletonList(testInUnitA), excluding(Collections.singletonList(batched)), 50);

        List<String> returned = page.stream().map(Analysis::getId).collect(Collectors.toList());
        assertEquals(Collections.singletonList(kept), returned);
    }

    @Test
    public void onlyTestsInTheGivenUnitComeBack() {
        String mine = createPendingAnalysis("ITBW20001", testInUnitA);
        createPendingAnalysis("ITBW20002", testInUnitB);

        List<Analysis> page = analysisService.getPendingAnalysesForWorkplan(Collections.singletonList(pendingStatusId),
                Collections.singletonList(testInUnitA), excluding(Collections.emptyList()), 50);

        // Exact equality already proves the other unit's analysis is absent.
        List<String> returned = page.stream().map(Analysis::getId).collect(Collectors.toList());
        assertEquals(Collections.singletonList(mine), returned);
    }

    @Test
    public void noLabUnitMeansNoRowsRatherThanEveryRow() {
        createPendingAnalysis("ITBW30001", testInUnitA);

        List<Analysis> page = analysisService.getPendingAnalysesForWorkplan(Collections.singletonList(pendingStatusId),
                Collections.emptyList(), Collections.emptyList(), 50);

        assertEquals(Collections.emptyList(), page);
    }

    @Test
    public void theBatchListShowsOnlyTheCallersOwnBatches() {
        Long mine = createBatch("Mine", 1, BatchWorkplanStatus.DRAFT);
        createBatch("Theirs", 109, BatchWorkplanStatus.DRAFT);

        List<Long> visible = batchWorkplanDAO.getForUserInStatuses(1, openStatuses()).stream().map(BatchWorkplan::getId)
                .filter(createdBatchIds::contains).collect(Collectors.toList());

        assertEquals(Collections.singletonList(mine), visible);
    }

    @Test
    public void archivedBatchesLeaveTheWorkingView() {
        Long open = createBatch("Still working", 1, BatchWorkplanStatus.DRAFT);
        Long archived = createBatch("Done with", 1, BatchWorkplanStatus.ARCHIVED);

        List<Long> visible = batchWorkplanDAO.getForUserInStatuses(1, openStatuses()).stream().map(BatchWorkplan::getId)
                .filter(createdBatchIds::contains).collect(Collectors.toList());

        assertEquals(Collections.singletonList(open), visible);
        // The archived batch is retained for audit, just not shown as work in hand.
        assertTrue("archived batch must still exist", batchWorkplanDAO.getWithItems(archived).isPresent());
    }

    @Test
    public void anUnresolvableCallerSeesNoBatchesRatherThanEveryBatch() {
        createBatch("Mine", 1, BatchWorkplanStatus.DRAFT);

        assertEquals(Collections.emptyList(), batchWorkplanDAO.getForUserInStatuses(null, openStatuses()));
    }

    private List<BatchWorkplanStatus> openStatuses() {
        return Arrays.asList(BatchWorkplanStatus.DRAFT, BatchWorkplanStatus.ACTIVE, BatchWorkplanStatus.COMPLETED);
    }

    private Long createBatch(String name, Integer ownerId, BatchWorkplanStatus status) {
        BatchWorkplan batch = new BatchWorkplan();
        batch.setName(name);
        batch.setStatus(status);
        batch.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        batch.setCreatedByUserId(ownerId);
        batch.setUpdatedByUserId(ownerId);
        batch.setSysUserId(String.valueOf(ownerId));
        Long id = batchWorkplanDAO.insert(batch);
        createdBatchIds.add(id);
        return id;
    }

    private String createPendingAnalysis(String accessionNumber, String testId) {
        java.sql.Date today = new java.sql.Date(System.currentTimeMillis());

        Sample sample = new Sample();
        sample.setAccessionNumber(accessionNumber);
        sample.setDomain("H");
        sample.setEnteredDate(today);
        sample.setReceivedDate(today);
        sample.setSysUserId(ACTOR);
        String sampleId = sampleService.insert(sample);
        createdSampleIds.add(sampleId);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sampleService.get(sampleId));
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(statusService.getStatusID(SampleStatus.Entered));
        sampleItem.setSysUserId(ACTOR);
        String sampleItemId = sampleItemService.insert(sampleItem);
        createdSampleItemIds.add(sampleItemId);

        Analysis analysis = new Analysis();
        analysis.setSampleItem(sampleItemService.get(sampleItemId));
        analysis.setTest(testService.get(testId));
        analysis.setStatusId(pendingStatusId);
        analysis.setAnalysisType("MANUAL");
        analysis.setRevision("1");
        analysis.setIsReportable("Y");
        analysis.setStartedDate(new java.sql.Date(System.currentTimeMillis()));
        analysis.setSysUserId(ACTOR);
        String analysisId = analysisService.insert(analysis);
        createdAnalysisIds.add(analysisId);
        return analysisId;
    }
}
