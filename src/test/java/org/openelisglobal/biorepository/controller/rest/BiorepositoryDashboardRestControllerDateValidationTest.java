package org.openelisglobal.biorepository.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.service.BiorepositoryDashboardService;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BiorepositoryDashboardRestControllerDateValidationTest {

    @Mock
    private BiorepositoryDashboardService dashboardService;

    @InjectMocks
    private BiorepositoryDashboardRestController controller;

    @Test
    public void retrievalStats_InvalidStartDate_ReturnsBadRequest() {
        ResponseEntity<Map<String, Object>> response = controller.getRetrievalStats(mock(HttpServletRequest.class),
                "2026/04/21", null);
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(String.valueOf(response.getBody().get("error")).contains("Expected YYYY-MM-DD"));
        verifyZeroInteractions(dashboardService);
    }

    @Test
    public void disposalStats_InvalidEndDate_ReturnsBadRequest() {
        ResponseEntity<Map<String, Object>> response = controller.getDisposalStats(mock(HttpServletRequest.class), null,
                "21-04-2026");
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(String.valueOf(response.getBody().get("error")).contains("Expected YYYY-MM-DD"));
        verifyZeroInteractions(dashboardService);
    }
}
