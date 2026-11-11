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
 * Integration tests for {@link SampleTypeRequestService} and
 * {@code SampleTypeRequestServiceImpl} using a real Spring context and database
 * fixture.
 *
 * <p>
 * Only the null/blank early-return paths for {@code getRequestsBySampleId},
 * {@code getPendingRequestsBySampleId}, and
 * {@code getFulfilledRequestsBySampleId} are exercised here. Those methods
 * currently delegate to {@code SampleTypeRequestDAOImpl} and have a legacy
 * {@code sampleId} binding mismatch with {@code Sample.id}. The remaining
 * dataset and workflow behavior is verified by {@code get}, {@code getAll}, and
 * {@code getAllMatching}.
 */
public class SampleTypeRequestServiceTest extends BaseWebContextSensitiveTest {

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
        List<SampleTypeRequest> sampleTwo = requestsForSample("2");
        List<SampleTypeRequest> sampleOne = requestsForSample("1");

        Assert.assertEquals(3, sampleTwo.size());
        Assert.assertEquals(1, sampleOne.size());
        assertEquals(Integer.valueOf(104), sampleOne.get(0).getId());
    }

    @Test
    public void fixture_shouldOrderBySortOrderWithinSample() {
        List<SampleTypeRequest> ordered = requestsForSample("2");

        Assert.assertEquals(Integer.valueOf(101), ordered.get(0).getId());
        Assert.assertEquals(Integer.valueOf(1), ordered.get(0).getSortOrder());
        Assert.assertEquals(Integer.valueOf(102), ordered.get(1).getId());
        Assert.assertEquals(Integer.valueOf(103), ordered.get(2).getId());
    }

    @Test
    public void get_shouldExposeFixtureFieldValuesAndLazyFriendlyTypeOfSample() {
        SampleTypeRequest serumRequest = sampleTypeRequestService.get(101);

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

        assertEquals(SampleTypeRequest.Status.CANCELLED, sampleTypeRequestService.get(104).getStatus());
    }

    @Test
    public void collectedFixture_shouldBeFulfilledWithSampleItem() {
        SampleTypeRequest collected = sampleTypeRequestService.get(103);

        assertEquals(SampleTypeRequest.Status.COLLECTED, collected.getStatus());
        assertTrue(collected.isFulfilled());
        assertEquals("1", collected.getSampleItem().getId());
    }

    @Test
    public void fulfillRequest_shouldLinkSampleItemAndMarkCollected() {
        sampleTypeRequestService.fulfillRequest(101, "2");

        SampleTypeRequest fulfilled = sampleTypeRequestService.get(101);
        assertEquals(SampleTypeRequest.Status.COLLECTED, fulfilled.getStatus());
        assertEquals("2", fulfilled.getSampleItem().getId());
        assertTrue(fulfilled.isFulfilled());

        assertEquals(SampleTypeRequest.Status.REQUESTED, sampleTypeRequestService.get(102).getStatus());
    }

    @Test(expected = ObjectNotFoundException.class)
    public void fulfillRequest_shouldRejectUnknownRequestId() {
        sampleTypeRequestService.fulfillRequest(99999, "1");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void fulfillRequest_shouldRejectUnknownSampleItem() {
        sampleTypeRequestService.fulfillRequest(101, "999");
    }

    @Test(expected = IllegalStateException.class)
    public void fulfillRequest_shouldRejectAlreadyCollectedRequest() {
        sampleTypeRequestService.fulfillRequest(103, "2");
    }

    @Test(expected = IllegalStateException.class)
    public void fulfillRequest_shouldRejectCancelledRequest() {
        sampleTypeRequestService.fulfillRequest(104, "4");
    }

    @Test
    public void cancelRequest_shouldMarkRequestCancelled() {
        sampleTypeRequestService.cancelRequest(102);

        SampleTypeRequest cancelled = sampleTypeRequestService.get(102);
        assertEquals(SampleTypeRequest.Status.CANCELLED, cancelled.getStatus());
        assertNull(cancelled.getSampleItem());
        assertFalse(cancelled.isPending());
        assertFalse(cancelled.isFulfilled());
    }

    @Test(expected = ObjectNotFoundException.class)
    public void cancelRequest_shouldRejectUnknownRequestId() {
        sampleTypeRequestService.cancelRequest(99999);
    }

    @Test(expected = IllegalStateException.class)
    public void cancelRequest_shouldRejectAlreadyCollectedRequest() {
        sampleTypeRequestService.cancelRequest(103);
    }

    @Test(expected = IllegalStateException.class)
    public void cancelRequest_shouldRejectAlreadyCancelledRequest() {
        sampleTypeRequestService.cancelRequest(104);
    }

    @Test(expected = IllegalStateException.class)
    public void cancelRequest_shouldRejectDoubleCancel() {
        sampleTypeRequestService.cancelRequest(102);
        sampleTypeRequestService.cancelRequest(102);
    }

    @Test
    public void get_shouldReturnPersistedRequest() {
        SampleTypeRequest request = sampleTypeRequestService.get(103);

        assertNotNull(request);
        assertEquals("2", request.getSample().getId());
        assertEquals("1", request.getTypeOfSample().getId());
        assertNotNull(request.getCreatedDate());
    }

    @Test(expected = ObjectNotFoundException.class)
    public void get_shouldThrowWhenIdNotFound() {
        sampleTypeRequestService.get(99999);
    }

    @Test
    public void getAll_shouldReturnAllFixtureRows() {
        Assert.assertEquals(4, sampleTypeRequestService.getAll().size());
    }

    @Test
    public void getMatch_shouldFindUniqueRequestById() {
        Optional<SampleTypeRequest> match = sampleTypeRequestService.getMatch("id", 103);

        assertTrue(match.isPresent());
        assertEquals(Integer.valueOf(103), match.get().getId());
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
        int before = sampleTypeRequestService.getAll().size();
        Integer id = sampleTypeRequestService.insert(buildPendingRequest(10, "1", 1.0, "301", null));

        SampleTypeRequest saved = sampleTypeRequestService.get(id);
        assertEquals(SampleTypeRequest.Status.REQUESTED, saved.getStatus());
        assertEquals("2", saved.getSample().getId());
        assertEquals("301", saved.getRequestedTests());
        Assert.assertEquals(before + 1, sampleTypeRequestService.getAll().size());
    }

    @Test
    public void insert_shouldPersistRequestedPanels() {
        Integer id = sampleTypeRequestService.insert(buildPendingRequest(11, "2", 2.0, null, "10,11"));

        assertEquals("10,11", sampleTypeRequestService.get(id).getRequestedPanels());
    }

    @Test
    public void update_shouldPersistFieldChanges() {
        SampleTypeRequest request = sampleTypeRequestService.get(101);
        request.setSortOrder(99);
        request.setRequestedQuantity(9.5);
        request.setRequestedTests("999");
        request.setSysUserId(TEST_SYS_USER_ID);

        sampleTypeRequestService.update(request);

        SampleTypeRequest updated = sampleTypeRequestService.get(101);
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
        SampleTypeRequest request = sampleTypeRequestService.get(102);
        request.setRequestedPanels("55,56");
        request.setSysUserId(TEST_SYS_USER_ID);

        sampleTypeRequestService.save(request);

        assertEquals("55,56", sampleTypeRequestService.get(102).getRequestedPanels());
    }

    @Test
    public void delete_shouldRemoveRequest() {
        int before = sampleTypeRequestService.getAll().size();
        Integer id = sampleTypeRequestService.insert(buildPendingRequest(13, "1", 1.0, null, null));
        Assert.assertEquals(before + 1, sampleTypeRequestService.getAll().size());

        sampleTypeRequestService.delete(sampleTypeRequestService.get(id));

        Assert.assertEquals(before, sampleTypeRequestService.getAll().size());
    }

    @Test(expected = ObjectNotFoundException.class)
    public void delete_shouldThrowWhenIdNotFound() {
        sampleTypeRequestService.delete(sampleTypeRequestService.get(99999));
    }

    private List<SampleTypeRequest> requestsForSample(String sampleId) {
        return sampleTypeRequestService.getAll().stream().filter(r -> sampleId.equals(r.getSample().getId()))
                .sorted(Comparator.comparing(SampleTypeRequest::getSortOrder)).collect(Collectors.toList());
    }

    private SampleTypeRequest buildPendingRequest(int sortOrder, String typeOfSampleId, double quantity,
            String requestedTests, String requestedPanels) {
        Sample sample = sampleService.get("2");
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
