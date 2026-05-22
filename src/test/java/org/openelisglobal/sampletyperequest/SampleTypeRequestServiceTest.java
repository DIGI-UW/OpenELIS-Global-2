package org.openelisglobal.sampletyperequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.ObjectNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampletyperequest.service.SampleTypeRequestService;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link SampleTypeRequestService} /
 * {@code SampleTypeRequestServiceImpl} against a real Spring context and
 * database.
 *
 * <p>
 * {@code getRequestsBySampleId}, {@code getPendingRequestsBySampleId}, and
 * {@code getFulfilledRequestsBySampleId} delegate to
 * {@code SampleTypeRequestDAOImpl}, which currently binds {@code sampleId} as
 * {@code Integer} while {@code Sample.id} is a {@code String} in Hibernate — so
 * those methods throw at runtime for normal ids. We only integration-test their
 * null/blank early-return paths here; fixture and workflow behavior for a
 * sample is asserted via {@code get} / {@code getAll} / {@code getAllMatching}
 * instead of changing production code under test.
 */
public class SampleTypeRequestServiceTest extends BaseWebContextSensitiveTest {

    private static final String SAMPLE_WITH_REQUESTS = "2";
    private static final String SAMPLE_WITH_CANCELLED_ONLY = "1";

    private static final int REQUEST_PENDING_SERUM = 101;
    private static final int REQUEST_PENDING_BLOOD = 102;
    private static final int REQUEST_COLLECTED = 103;
    private static final int REQUEST_CANCELLED = 104;
    private static final int UNKNOWN_REQUEST_ID = 99999;

    private static final int FIXTURE_ROW_COUNT = 4;

    @Autowired
    private SampleTypeRequestService sampleTypeRequestService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-type-request-service.xml");
    }

    // --- Sample-id query methods: only null/blank (DAO returns before HQL) ---

    @Test
    public void getRequestsBySampleId_shouldReturnEmptyForNullSampleId() {
        Assert.assertTrue(sampleTypeRequestService.getRequestsBySampleId(null).isEmpty());
    }

    @Test
    public void getRequestsBySampleId_shouldReturnEmptyForBlankSampleId() {
        Assert.assertTrue(sampleTypeRequestService.getRequestsBySampleId("").isEmpty());
        Assert.assertTrue(sampleTypeRequestService.getRequestsBySampleId("   ").isEmpty());
    }

    @Test
    public void getPendingRequestsBySampleId_shouldReturnEmptyForNullOrBlankSampleId() {
        Assert.assertTrue(sampleTypeRequestService.getPendingRequestsBySampleId(null).isEmpty());
        Assert.assertTrue(sampleTypeRequestService.getPendingRequestsBySampleId("").isEmpty());
    }

    @Test
    public void getFulfilledRequestsBySampleId_shouldReturnEmptyForNullSampleId() {
        Assert.assertTrue(sampleTypeRequestService.getFulfilledRequestsBySampleId(null).isEmpty());
    }

    @Test
    public void fixture_shouldHaveExpectedRowsPerSample() {
        List<SampleTypeRequest> sampleTwo = requestsForSample(SAMPLE_WITH_REQUESTS);
        List<SampleTypeRequest> sampleOne = requestsForSample(SAMPLE_WITH_CANCELLED_ONLY);

        Assert.assertEquals(3, sampleTwo.size());
        Assert.assertEquals(1, sampleOne.size());
        assertEquals(Integer.valueOf(REQUEST_CANCELLED), sampleOne.get(0).getId());
    }

    @Test
    public void fixture_shouldOrderBySortOrderWithinSample() {
        List<SampleTypeRequest> ordered = requestsForSample(SAMPLE_WITH_REQUESTS);

        Assert.assertEquals(Integer.valueOf(REQUEST_PENDING_SERUM), ordered.get(0).getId());
        Assert.assertEquals(Integer.valueOf(1), ordered.get(0).getSortOrder());
        Assert.assertEquals(Integer.valueOf(REQUEST_PENDING_BLOOD), ordered.get(1).getId());
        Assert.assertEquals(Integer.valueOf(REQUEST_COLLECTED), ordered.get(2).getId());
    }

    @Test
    public void get_shouldExposeFixtureFieldValuesAndLazyFriendlyTypeOfSample() {
        SampleTypeRequest serumRequest = sampleTypeRequestService.get(REQUEST_PENDING_SERUM);

        assertEquals(Double.valueOf(2.0), serumRequest.getRequestedQuantity());
        assertEquals("101,102", serumRequest.getRequestedTests());
        assertTrue(serumRequest.isPending());
        assertFalse(serumRequest.isFulfilled());
        assertEquals("1", serumRequest.getTypeOfSample().getId());
    }

    @Test
    public void getAllMatching_shouldReflectPendingCollectedAndCancelledFixture() {
        List<SampleTypeRequest> requested = sampleTypeRequestService.getAllMatching("status",
                SampleTypeRequest.Status.REQUESTED);
        Assert.assertEquals(2, requested.size());

        Map<String, Object> collectedCriteria = new HashMap<>();
        collectedCriteria.put("status", SampleTypeRequest.Status.COLLECTED);
        Assert.assertEquals(1, sampleTypeRequestService.getAllMatching(collectedCriteria).size());

        assertEquals(SampleTypeRequest.Status.CANCELLED, sampleTypeRequestService.get(REQUEST_CANCELLED).getStatus());
    }

    @Test
    public void collectedFixture_shouldBeFulfilledWithSampleItem() {
        SampleTypeRequest collected = sampleTypeRequestService.get(REQUEST_COLLECTED);

        assertEquals(SampleTypeRequest.Status.COLLECTED, collected.getStatus());
        assertTrue(collected.isFulfilled());
        assertEquals("1", collected.getSampleItem().getId());
    }

    @Test
    public void fulfillRequest_shouldLinkSampleItemAndMarkCollected() {
        sampleTypeRequestService.fulfillRequest(REQUEST_PENDING_SERUM, "2");

        SampleTypeRequest fulfilled = sampleTypeRequestService.get(REQUEST_PENDING_SERUM);
        assertEquals(SampleTypeRequest.Status.COLLECTED, fulfilled.getStatus());
        assertEquals("2", fulfilled.getSampleItem().getId());
        assertTrue(fulfilled.isFulfilled());

        assertEquals(SampleTypeRequest.Status.REQUESTED,
                sampleTypeRequestService.get(REQUEST_PENDING_BLOOD).getStatus());
    }

    @Test(expected = ObjectNotFoundException.class)
    public void fulfillRequest_shouldRejectUnknownRequestId() {
        sampleTypeRequestService.fulfillRequest(UNKNOWN_REQUEST_ID, "1");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void fulfillRequest_shouldRejectUnknownSampleItem() {
        sampleTypeRequestService.fulfillRequest(REQUEST_PENDING_SERUM, "999");
    }

    @Test(expected = IllegalStateException.class)
    public void fulfillRequest_shouldRejectAlreadyCollectedRequest() {
        sampleTypeRequestService.fulfillRequest(REQUEST_COLLECTED, "2");
    }

    @Test(expected = IllegalStateException.class)
    public void fulfillRequest_shouldRejectCancelledRequest() {
        sampleTypeRequestService.fulfillRequest(REQUEST_CANCELLED, "4");
    }

    @Test
    public void cancelRequest_shouldMarkRequestCancelled() {
        sampleTypeRequestService.cancelRequest(REQUEST_PENDING_BLOOD);

        SampleTypeRequest cancelled = sampleTypeRequestService.get(REQUEST_PENDING_BLOOD);
        assertEquals(SampleTypeRequest.Status.CANCELLED, cancelled.getStatus());
        assertNull(cancelled.getSampleItem());
        assertFalse(cancelled.isPending());
        assertFalse(cancelled.isFulfilled());
    }

    @Test(expected = ObjectNotFoundException.class)
    public void cancelRequest_shouldRejectUnknownRequestId() {
        sampleTypeRequestService.cancelRequest(UNKNOWN_REQUEST_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void cancelRequest_shouldRejectAlreadyCollectedRequest() {
        sampleTypeRequestService.cancelRequest(REQUEST_COLLECTED);
    }

    @Test(expected = IllegalStateException.class)
    public void cancelRequest_shouldRejectAlreadyCancelledRequest() {
        sampleTypeRequestService.cancelRequest(REQUEST_CANCELLED);
    }

    @Test(expected = IllegalStateException.class)
    public void cancelRequest_shouldRejectDoubleCancel() {
        sampleTypeRequestService.cancelRequest(REQUEST_PENDING_BLOOD);
        sampleTypeRequestService.cancelRequest(REQUEST_PENDING_BLOOD);
    }

    @Test
    public void get_shouldReturnPersistedRequest() {
        SampleTypeRequest request = sampleTypeRequestService.get(REQUEST_COLLECTED);

        assertNotNull(request);
        assertEquals(SAMPLE_WITH_REQUESTS, request.getSample().getId());
        assertEquals("1", request.getTypeOfSample().getId());
        assertNotNull(request.getCreatedDate());
    }

    @Test(expected = ObjectNotFoundException.class)
    public void get_shouldThrowWhenIdNotFound() {
        sampleTypeRequestService.get(UNKNOWN_REQUEST_ID);
    }

    @Test
    public void getAll_shouldReturnAllFixtureRows() {
        Assert.assertEquals(FIXTURE_ROW_COUNT, sampleTypeRequestService.getAll().size());
    }

    @Test
    public void getMatch_shouldFindUniqueRequestById() {
        Optional<SampleTypeRequest> match = sampleTypeRequestService.getMatch("id", REQUEST_COLLECTED);

        assertTrue(match.isPresent());
        assertEquals(Integer.valueOf(REQUEST_COLLECTED), match.get().getId());
    }

    @Test
    public void getMatch_shouldReturnEmptyWhenMultipleRowsMatch() {
        assertFalse(sampleTypeRequestService.getMatch("status", SampleTypeRequest.Status.REQUESTED).isPresent());
    }

    @Test
    public void getAllOrdered_shouldSortBySortOrderAscending() {
        List<SampleTypeRequest> ordered = sampleTypeRequestService.getAllOrdered("sortOrder", false);

        for (int i = 1; i < ordered.size(); i++) {
            assertTrue(ordered.get(i - 1).getSortOrder() <= ordered.get(i).getSortOrder());
        }
    }

    @Test
    public void insert_shouldPersistNewPendingRequest() {
        Integer id = sampleTypeRequestService.insert(buildPendingRequest(10, "1", 1.0, "301", null));

        SampleTypeRequest saved = sampleTypeRequestService.get(id);
        assertEquals(SampleTypeRequest.Status.REQUESTED, saved.getStatus());
        assertEquals(SAMPLE_WITH_REQUESTS, saved.getSample().getId());
        assertEquals("301", saved.getRequestedTests());
        Assert.assertEquals(FIXTURE_ROW_COUNT + 1, sampleTypeRequestService.getAll().size());
    }

    @Test
    public void insert_shouldPersistRequestedPanels() {
        Integer id = sampleTypeRequestService.insert(buildPendingRequest(11, "2", 2.0, null, "10,11"));

        assertEquals("10,11", sampleTypeRequestService.get(id).getRequestedPanels());
    }

    @Test
    public void update_shouldPersistFieldChanges() {
        SampleTypeRequest request = sampleTypeRequestService.get(REQUEST_PENDING_SERUM);
        request.setSortOrder(99);
        request.setRequestedQuantity(9.5);
        request.setRequestedTests("999");
        request.setSysUserId(TEST_SYS_USER_ID);

        sampleTypeRequestService.update(request);

        SampleTypeRequest updated = sampleTypeRequestService.get(REQUEST_PENDING_SERUM);
        assertEquals(Integer.valueOf(99), updated.getSortOrder());
        assertEquals(Double.valueOf(9.5), updated.getRequestedQuantity());
        assertEquals("999", updated.getRequestedTests());
    }

    @Test
    public void save_shouldInsertWhenIdIsNull() {
        SampleTypeRequest saved = sampleTypeRequestService.save(buildPendingRequest(12, "3", 1.0, null, null));

        assertNotNull(saved.getId());
        assertEquals(SampleTypeRequest.Status.REQUESTED, sampleTypeRequestService.get(saved.getId()).getStatus());
    }

    @Test
    public void save_shouldUpdateWhenIdIsPresent() {
        SampleTypeRequest request = sampleTypeRequestService.get(REQUEST_PENDING_BLOOD);
        request.setRequestedPanels("55,56");
        request.setSysUserId(TEST_SYS_USER_ID);

        sampleTypeRequestService.save(request);

        assertEquals("55,56", sampleTypeRequestService.get(REQUEST_PENDING_BLOOD).getRequestedPanels());
    }

    @Test
    public void delete_shouldRemoveRequest() {
        Integer id = sampleTypeRequestService.insert(buildPendingRequest(13, "1", 1.0, null, null));
        Assert.assertEquals(FIXTURE_ROW_COUNT + 1, sampleTypeRequestService.getAll().size());

        sampleTypeRequestService.delete(sampleTypeRequestService.get(id));

        Assert.assertEquals(FIXTURE_ROW_COUNT, sampleTypeRequestService.getAll().size());
    }

    @Test(expected = ObjectNotFoundException.class)
    public void delete_shouldThrowWhenIdNotFound() {
        sampleTypeRequestService.delete(sampleTypeRequestService.get(UNKNOWN_REQUEST_ID));
    }

    private List<SampleTypeRequest> requestsForSample(String sampleId) {
        return sampleTypeRequestService.getAll().stream().filter(r -> sampleId.equals(r.getSample().getId()))
                .sorted(Comparator.comparing(SampleTypeRequest::getSortOrder)).collect(Collectors.toList());
    }

    private SampleTypeRequest buildPendingRequest(int sortOrder, String typeOfSampleId, double quantity,
            String requestedTests, String requestedPanels) {
        Sample sample = sampleService.get(SAMPLE_WITH_REQUESTS);
        TypeOfSample typeOfSample = typeOfSampleService.get(typeOfSampleId);

        SampleTypeRequest request = new SampleTypeRequest();
        request.setSample(sample);
        request.setTypeOfSample(typeOfSample);
        request.setSortOrder(sortOrder);
        request.setRequestedQuantity(quantity);
        request.setRequestedTests(requestedTests);
        request.setRequestedPanels(requestedPanels);
        request.setStatus(SampleTypeRequest.Status.REQUESTED);
        request.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        request.setSysUserId(TEST_SYS_USER_ID);
        return request;
    }
}
