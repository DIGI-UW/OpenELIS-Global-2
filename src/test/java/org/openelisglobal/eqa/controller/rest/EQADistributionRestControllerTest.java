package org.openelisglobal.eqa.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.eqa.service.EQADistributionService;
import org.openelisglobal.eqa.service.EQAProgramService;
import org.openelisglobal.eqa.valueholder.EQADistribution;
import org.openelisglobal.eqa.valueholder.EQADistributionStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class EQADistributionRestControllerTest {

    @Mock
    private EQADistributionService distributionService;

    @Mock
    private EQAProgramService programService;

    @InjectMocks
    private EQADistributionRestController controller;

    @Test
    public void testCreateDistribution_ValidData_ReturnsOk() {
        EQAProgram program = new EQAProgram();
        program.setId(1L);
        program.setName("Test Program");
        when(programService.get(1L)).thenReturn(program);
        when(distributionService.insert(any(EQADistribution.class))).thenReturn(100L);

        Map<String, Object> body = new HashMap<>();
        body.put("distributionName", "Round 1 2026");
        body.put("programId", 1);
        body.put("deadline", "2026-06-30");
        body.put("participantOrganizationIds", Arrays.asList(10L, 20L, 30L));

        ResponseEntity<?> response = controller.createDistribution(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> result = (Map<?, ?>) response.getBody();
        assertNotNull(result);
        assertEquals(100L, result.get("id"));
        assertEquals("DRAFT", result.get("status"));
    }

    @Test
    public void testCreateDistribution_MissingRequiredFields_ReturnsBadRequest() {
        Map<String, Object> body = new HashMap<>();
        body.put("distributionName", "Round 1");

        ResponseEntity<?> response = controller.createDistribution(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testCreateDistribution_TooFewParticipants_ReturnsBadRequest() {
        Map<String, Object> body = new HashMap<>();
        body.put("distributionName", "Round 1");
        body.put("programId", 1);
        body.put("deadline", "2026-06-30");
        body.put("participantOrganizationIds", Collections.singletonList(10L));

        ResponseEntity<?> response = controller.createDistribution(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testListDistributions_ReturnsAll() {
        EQADistribution d1 = createDistribution(1L, "Round 1", EQADistributionStatus.DRAFT);
        EQADistribution d2 = createDistribution(2L, "Round 2", EQADistributionStatus.SHIPPED);
        when(distributionService.getAll()).thenReturn(Arrays.asList(d1, d2));

        ResponseEntity<Map<String, Object>> response = controller.listDistributions(null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.get("totalCount"));
    }

    @Test
    public void testListDistributions_FilterByStatus() {
        EQADistribution d1 = createDistribution(1L, "Round 1", EQADistributionStatus.DRAFT);
        when(distributionService.findByStatus(EQADistributionStatus.DRAFT)).thenReturn(Collections.singletonList(d1));

        ResponseEntity<Map<String, Object>> response = controller.listDistributions(null, "DRAFT");

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.get("totalCount"));
    }

    @Test
    public void testGetDistribution_Found_ReturnsOk() {
        EQADistribution d = createDistribution(1L, "Round 1", EQADistributionStatus.PREPARED);
        when(distributionService.get(1L)).thenReturn(d);

        ResponseEntity<?> response = controller.getDistribution(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals("Round 1", body.get("distributionName"));
        assertEquals("PREPARED", body.get("status"));
    }

    @Test
    public void testGetDistribution_NotFound_Returns404() {
        when(distributionService.get(999L))
                .thenThrow(new ObjectNotFoundException(999L, "EQADistribution"));

        ResponseEntity<?> response = controller.getDistribution(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testAdvanceStatus_DraftToPrepared_ReturnsOk() {
        EQADistribution advanced = createDistribution(1L, "Round 1", EQADistributionStatus.PREPARED);
        when(distributionService.advanceStatus(1L)).thenReturn(advanced);

        ResponseEntity<?> response = controller.advanceStatus(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals("PREPARED", body.get("status"));
    }

    @Test
    public void testAdvanceStatus_AlreadyCompleted_ReturnsBadRequest() {
        when(distributionService.advanceStatus(1L))
                .thenThrow(new IllegalStateException("Distribution is already completed"));

        ResponseEntity<?> response = controller.advanceStatus(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testAdvanceStatus_NotFound_Returns404() {
        when(distributionService.advanceStatus(999L))
                .thenThrow(new IllegalArgumentException("Distribution not found"));

        ResponseEntity<?> response = controller.advanceStatus(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testGenerateBarcodes_ValidDistribution_ReturnsOk() {
        EQADistribution d = createDistribution(1L, "Round 1", EQADistributionStatus.PREPARED);
        when(distributionService.get(1L)).thenReturn(d);

        ResponseEntity<?> response = controller.generateBarcodes(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals("barcodes_generated", body.get("status"));
    }

    @Test
    public void testGenerateBarcodes_NotFound_Returns404() {
        when(distributionService.get(999L))
                .thenThrow(new ObjectNotFoundException(999L, "EQADistribution"));

        ResponseEntity<?> response = controller.generateBarcodes(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private EQADistribution createDistribution(Long id, String name, EQADistributionStatus status) {
        EQADistribution d = new EQADistribution();
        d.setId(id);
        d.setDistributionName(name);
        d.setStatus(status);
        d.setDeadline(new Timestamp(System.currentTimeMillis() + 86400000));
        d.setDistributionDate(new Timestamp(System.currentTimeMillis()));

        EQAProgram program = new EQAProgram();
        program.setId(1L);
        program.setName("Test Program");
        d.setEqaProgram(program);

        return d;
    }
}
