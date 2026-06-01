package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.dao.SampleRetrievalRequestDAO;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalItem;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.DestinationType;
import org.openelisglobal.biorepository.valueholder.SampleRetrievalRequest.RequestStatus;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@RunWith(MockitoJUnitRunner.class)
public class SampleRetrievalQueueVisibilityTest {

    @InjectMocks
    private SampleRetrievalServiceImpl retrievalService;

    @Mock
    private SampleRetrievalRequestDAO baseObjectDAO;

    @Mock
    private BioSampleService bioSampleService;

    @Mock
    private SystemUserService systemUserService;

    @Mock
    private ProjectService projectService;

    private final SampleRetrievalRequest[] savedRequest = new SampleRetrievalRequest[1];

    @Before
    public void setUp() {
        when(baseObjectDAO.getNextRequestNumberSequence()).thenReturn(42);
        SystemUser user = new SystemUser();
        user.setId("1");
        user.setLoginName("requester");
        when(systemUserService.get("user-1")).thenReturn(user);
        when(systemUserService.get("approver-1")).thenReturn(user);
        when(baseObjectDAO.insert(any(SampleRetrievalRequest.class))).thenAnswer(invocation -> {
            SampleRetrievalRequest saved = invocation.getArgument(0);
            saved.setId(100);
            savedRequest[0] = saved;
            return saved.getId();
        });
        when(baseObjectDAO.get(eq(100))).thenAnswer(invocation -> Optional.of(savedRequest[0]));
        when(baseObjectDAO.update(any(SampleRetrievalRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void submitReferenceRequest_movesToPendingApproval_withRequestLineCounts() {
        RetrievalItemCreate item = new RetrievalItemCreate();
        item.setRequestedSampleType("Plasma");
        item.setQuantityRequested(BigDecimal.valueOf(2));

        SampleRetrievalRequest created = retrievalService.createRequest(
                "Research use",
                List.of(item),
                null,
                null,
                DestinationType.ANALYSIS_RETURN,
                null,
                SampleRetrievalRequest.PriorityLevel.NORMAL,
                null,
                "user-1");

        assertEquals(RequestStatus.DRAFT, created.getStatus());
        assertEquals(1, created.getRequestLineCount());
        assertEquals(0, created.getTotalItemCount());
        assertEquals(1L, created.getAwaitingFulfillmentItemCount());

        SampleRetrievalRequest submitted = retrievalService.submitForApproval(100, "user-1");

        assertEquals(RequestStatus.PENDING_APPROVAL, submitted.getStatus());
        assertEquals(1, submitted.getRequestLineCount());
        assertEquals(0, submitted.getTotalItemCount());
        assertEquals(1L, submitted.getAwaitingFulfillmentItemCount());
    }

    @Test
    public void approveReferenceRequest_keepsReferenceAwaitingFulfillment() {
        RetrievalItemCreate item = new RetrievalItemCreate();
        item.setRequestedSampleType("Serum");
        item.setQuantityRequested(BigDecimal.ONE);

        SampleRetrievalRequest created = retrievalService.createRequest(
                "Diagnostics",
                List.of(item),
                null,
                null,
                DestinationType.ANALYSIS_RETURN,
                null,
                SampleRetrievalRequest.PriorityLevel.NORMAL,
                null,
                "user-1");
        retrievalService.submitForApproval(created.getId(), "user-1");

        SampleRetrievalRequest approved = retrievalService.approveRequest(100, "Approved", "approver-1");

        assertEquals(RequestStatus.APPROVED, approved.getStatus());
        assertEquals(1, approved.getRequestLineCount());
        assertEquals(0, approved.getTotalItemCount());
        assertEquals(1L, approved.getAwaitingFulfillmentItemCount());
        assertEquals(SampleRetrievalItem.ItemStatus.AWAITING_FULFILLMENT,
                approved.getItems().get(0).getStatus());
    }

    @Test
    public void approvedReferenceOnlyRequest_cannotCompleteWithoutFulfillment() {
        RetrievalItemCreate item = new RetrievalItemCreate();
        item.setRequestedSampleType("Whole Blood");
        item.setQuantityRequested(BigDecimal.ONE);

        SampleRetrievalRequest created = retrievalService.createRequest(
                "Testing",
                List.of(item),
                null,
                null,
                DestinationType.ANALYSIS_RETURN,
                null,
                SampleRetrievalRequest.PriorityLevel.NORMAL,
                null,
                "user-1");
        retrievalService.submitForApproval(created.getId(), "user-1");
        SampleRetrievalRequest approved = retrievalService.approveRequest(100, "Approved", "approver-1");

        assertNotNull(approved.getCompletionBlockReason());
        org.junit.Assert.assertTrue(
                approved.getCompletionBlockReason().contains("awaiting sample attachment"));
    }
}
