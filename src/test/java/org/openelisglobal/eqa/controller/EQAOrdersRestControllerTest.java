package org.openelisglobal.eqa.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.eqa.controller.rest.EQAOrdersRestController;
import org.openelisglobal.eqa.service.SampleEQAService;
import org.openelisglobal.eqa.valueholder.EQAPriority;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.SampleEQA;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class EQAOrdersRestControllerTest {

    @Mock
    private SampleEQAService sampleEQAService;

    @InjectMocks
    private EQAOrdersRestController controller;

    private SampleEQA sample1;
    private SampleEQA sample2;
    private SampleEQA overdueSample;
    private EQAProgram program1;

    @Before
    public void setUp() {
        program1 = new EQAProgram();
        program1.setId(1L);
        program1.setName("Chemistry PT");
        program1.setProviderName("WHO");

        sample1 = new SampleEQA();
        sample1.setId(1L);
        sample1.setSampleId(100L);
        sample1.setEqaProviderSampleId("EQA-001");
        sample1.setEqaProgram(program1);
        sample1.setEqaPriority(EQAPriority.STANDARD);
        sample1.setEqaDeadline(Timestamp.valueOf(LocalDate.now().plusDays(7).atStartOfDay()));
        sample1.setSysUserId("1");

        sample2 = new SampleEQA();
        sample2.setId(2L);
        sample2.setSampleId(101L);
        sample2.setEqaProviderSampleId("EQA-002");
        sample2.setEqaProgram(program1);
        sample2.setEqaPriority(EQAPriority.URGENT);
        sample2.setEqaDeadline(Timestamp.valueOf(LocalDate.now().plusDays(14).atStartOfDay()));
        sample2.setSysUserId("1");

        overdueSample = new SampleEQA();
        overdueSample.setId(3L);
        overdueSample.setSampleId(102L);
        overdueSample.setEqaProviderSampleId("EQA-003");
        overdueSample.setEqaProgram(program1);
        overdueSample.setEqaPriority(EQAPriority.STANDARD);
        overdueSample.setEqaDeadline(Timestamp.valueOf(LocalDate.now().minusDays(3).atStartOfDay()));
        overdueSample.setSysUserId("1");
    }

    @Test
    public void testListOrders_NoFilters() {
        when(sampleEQAService.findEqaSamples()).thenReturn(List.of(sample1, sample2, overdueSample));

        ResponseEntity<List<Map<String, Object>>> response =
                controller.listOrders(null, null, null, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().size());
    }

    @Test
    public void testListOrders_EmptyList() {
        when(sampleEQAService.findEqaSamples()).thenReturn(new ArrayList<>());

        ResponseEntity<List<Map<String, Object>>> response =
                controller.listOrders(null, null, null, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void testListOrders_FilterByProgramId() {
        EQAProgram otherProgram = new EQAProgram();
        otherProgram.setId(99L);
        otherProgram.setName("Other Program");

        SampleEQA otherSample = new SampleEQA();
        otherSample.setId(4L);
        otherSample.setSampleId(103L);
        otherSample.setEqaProgram(otherProgram);
        otherSample.setEqaDeadline(Timestamp.valueOf(LocalDate.now().plusDays(5).atStartOfDay()));
        otherSample.setSysUserId("1");

        when(sampleEQAService.findEqaSamples()).thenReturn(List.of(sample1, otherSample));

        ResponseEntity<List<Map<String, Object>>> response = controller.listOrders(null, 1L, null, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("EQA-001", response.getBody().get(0).get("labNumber"));
    }

    @Test
    public void testListOrders_FilterByPriority() {
        when(sampleEQAService.findEqaSamples()).thenReturn(List.of(sample1, sample2));

        ResponseEntity<List<Map<String, Object>>> response =
                controller.listOrders(null, null, "URGENT", null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("URGENT", response.getBody().get(0).get("priority"));
    }

    @Test
    public void testListOrders_FilterByStatus_Overdue() {
        when(sampleEQAService.findEqaSamples()).thenReturn(List.of(sample1, overdueSample));

        ResponseEntity<List<Map<String, Object>>> response =
                controller.listOrders("OVERDUE", null, null, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("OVERDUE", response.getBody().get(0).get("status"));
    }

    @Test
    public void testListOrders_FilterBySearch() {
        when(sampleEQAService.findEqaSamples()).thenReturn(List.of(sample1, sample2));

        ResponseEntity<List<Map<String, Object>>> response =
                controller.listOrders(null, null, null, null, null, "EQA-001");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("EQA-001", response.getBody().get(0).get("labNumber"));
    }

    @Test
    public void testListOrders_SearchByProgramName() {
        when(sampleEQAService.findEqaSamples()).thenReturn(List.of(sample1, sample2));

        ResponseEntity<List<Map<String, Object>>> response =
                controller.listOrders(null, null, null, null, null, "chemistry");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    public void testListOrders_FilterByDateRange() {
        when(sampleEQAService.findEqaSamples()).thenReturn(List.of(sample1, sample2, overdueSample));

        String from = LocalDate.now().toString();
        String to = LocalDate.now().plusDays(10).toString();

        ResponseEntity<List<Map<String, Object>>> response =
                controller.listOrders(null, null, null, from, to, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    public void testListOrders_DtoShape() {
        when(sampleEQAService.findEqaSamples()).thenReturn(List.of(sample1));

        ResponseEntity<List<Map<String, Object>>> response =
                controller.listOrders(null, null, null, null, null, null);

        Map<String, Object> dto = response.getBody().get(0);
        assertNotNull(dto.get("id"));
        assertNotNull(dto.get("sampleId"));
        assertEquals("EQA-001", dto.get("labNumber"));
        assertEquals("Chemistry PT", dto.get("programName"));
        assertEquals("WHO", dto.get("providerName"));
        assertNotNull(dto.get("status"));
        assertNotNull(dto.get("deadline"));
        assertEquals("STANDARD", dto.get("priority"));
    }

    @Test
    public void testGetSummary() {
        when(sampleEQAService.findEqaSamples()).thenReturn(List.of(sample1, sample2, overdueSample));

        ResponseEntity<Map<String, Object>> response = controller.getSummary();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> summary = response.getBody();
        assertNotNull(summary.get("pending"));
        assertNotNull(summary.get("inProgress"));
        assertNotNull(summary.get("overdue"));
        assertNotNull(summary.get("completedThisMonth"));
        assertEquals(1L, summary.get("overdue"));
    }

    @Test
    public void testGetSummary_EmptyList() {
        when(sampleEQAService.findEqaSamples()).thenReturn(new ArrayList<>());

        ResponseEntity<Map<String, Object>> response = controller.getSummary();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0L, response.getBody().get("pending"));
        assertEquals(0L, response.getBody().get("overdue"));
    }
}
