package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@RunWith(MockitoJUnitRunner.class)
public class SampleRetrievalReferenceRequestTest {

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
        when(baseObjectDAO.getNextRequestNumberSequence()).thenReturn(1);
        SystemUser user = new SystemUser();
        user.setId("1");
        when(systemUserService.get("user-1")).thenReturn(user);
        when(baseObjectDAO.insert(any(SampleRetrievalRequest.class))).thenAnswer(invocation -> {
            SampleRetrievalRequest saved = invocation.getArgument(0);
            saved.setId(100);
            savedRequest[0] = saved;
            return saved.getId();
        });
        when(baseObjectDAO.get(eq(100))).thenAnswer(invocation -> Optional.of(savedRequest[0]));
    }

    @Test
    public void createRequest_createsReferenceLine_withoutBioSampleId() {
        RetrievalItemCreate item = new RetrievalItemCreate();
        item.setRequestedSampleType("Plasma");
        item.setQuantityRequested(BigDecimal.valueOf(2));
        item.setUnitOfMeasure("mL");
        item.setRequestedOriginLab("CTD");

        SampleRetrievalRequest request = retrievalService.createRequest(
                "Research use",
                List.of(item),
                null,
                null,
                DestinationType.ANALYSIS_RETURN,
                null,
                SampleRetrievalRequest.PriorityLevel.NORMAL,
                null,
                "user-1");

        assertNotNull(request);
        assertEquals(1, request.getItems().size());
        SampleRetrievalItem savedItem = request.getItems().get(0);
        assertEquals(SampleRetrievalItem.ItemStatus.AWAITING_FULFILLMENT, savedItem.getStatus());
        assertEquals("Plasma", savedItem.getRequestedSampleType());
        assertEquals("CTD", savedItem.getRequestedOriginLab());
        org.junit.Assert.assertNull(savedItem.getBioSample());
    }
}
