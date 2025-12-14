package org.openelisglobal.pharmaceutical.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.pharmaceutical.controller.rest.PharmaceuticalSampleRestController;
import org.openelisglobal.pharmaceutical.service.PharmaceuticalSampleService;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample.LabType;
import org.openelisglobal.pharmaceutical.valueholder.PharmaceuticalSample.SampleStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integration tests for PharmaceuticalSampleRestController.
 * Tests HTTP endpoints, request/response mapping, and service delegation.
 */
@RunWith(SpringRunner.class)
public class PharmaceuticalSampleRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PharmaceuticalSampleRestController controller;

    private PharmaceuticalSampleService sampleServiceMock;
    private ObjectMapper objectMapper;
    private UserSessionData usd;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();

        // Mock the service
        sampleServiceMock = Mockito.mock(PharmaceuticalSampleService.class);
        ReflectionTestUtils.setField(controller, "pharmaceuticalSampleService", sampleServiceMock);

        // Set up user session
        usd = new UserSessionData();
        usd.setSytemUserId(1);
    }

    // ==================== GET All Tests ====================

    @Test
    public void testGetAll_ReturnsAllSamples() throws Exception {
        // Arrange
        PharmaceuticalSample sample1 = createTestSample(1, "Sample1", "PHARMA");
        PharmaceuticalSample sample2 = createTestSample(2, "Sample2", "BIOLOGICAL");
        when(sampleServiceMock.getAll()).thenReturn(Arrays.asList(sample1, sample2));

        // Act & Assert
        mockMvc.perform(get("/rest/pharmaceutical/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    public void testGetAll_EmptyList_ReturnsEmptyArray() throws Exception {
        // Arrange
        when(sampleServiceMock.getAll()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/rest/pharmaceutical/samples")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== GET By ID Tests ====================

    @Test
    public void testGetById_WithValidId_ReturnsSample() throws Exception {
        // Arrange
        PharmaceuticalSample sample = createTestSample(1, "TestSample", "PHARMA");
        when(sampleServiceMock.get(1)).thenReturn(sample);

        // Act & Assert
        mockMvc.perform(get("/rest/pharmaceutical/samples/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sampleName").value("TestSample"));
    }

    @Test
    public void testGetById_WithInvalidId_Returns404() throws Exception {
        // Arrange
        when(sampleServiceMock.get(999)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/rest/pharmaceutical/samples/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ==================== GET By Barcode Tests ====================

    @Test
    public void testGetByBarcode_WithValidBarcode_ReturnsSample() throws Exception {
        // Arrange
        PharmaceuticalSample sample = createTestSample(1, "BarcodeTest", "PHARMA");
        sample.setBarcode("BC123456");
        when(sampleServiceMock.findByBarcode("BC123456")).thenReturn(sample);

        // Act & Assert
        mockMvc.perform(get("/rest/pharmaceutical/samples/barcode/BC123456")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value("BC123456"));
    }

    @Test
    public void testGetByBarcode_WithInvalidBarcode_Returns404() throws Exception {
        // Arrange
        when(sampleServiceMock.findByBarcode("INVALID")).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/rest/pharmaceutical/samples/barcode/INVALID")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ==================== GET By Status Tests ====================

    @Test
    public void testGetByStatus_ReturnsFilteredSamples() throws Exception {
        // Arrange
        PharmaceuticalSample sample = createTestSample(1, "PendingSample", "PHARMA");
        sample.setStatus(SampleStatus.PENDING_QC);
        when(sampleServiceMock.findByStatus(SampleStatus.PENDING_QC)).thenReturn(Arrays.asList(sample));

        // Act & Assert
        mockMvc.perform(get("/rest/pharmaceutical/samples/status/PENDING_QC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==================== GET By Lab Type Tests ====================

    @Test
    public void testGetByLabType_ReturnsFilteredSamples() throws Exception {
        // Arrange
        PharmaceuticalSample sample = createTestSample(1, "PharmaSample", "PHARMA");
        when(sampleServiceMock.findByLabType(LabType.PHARMA)).thenReturn(Arrays.asList(sample));

        // Act & Assert
        mockMvc.perform(get("/rest/pharmaceutical/samples/lab-type/PHARMA")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==================== GET Details Tests ====================

    @Test
    public void testGetSampleWithDetails_ReturnsCompleteDetails() throws Exception {
        // Arrange
        Map<String, Object> details = new HashMap<>();
        details.put("id", 1);
        details.put("sampleName", "DetailedSample");
        details.put("aliquotCount", 3);
        details.put("qcStatus", "PASSED");
        when(sampleServiceMock.getSampleWithDetails(1)).thenReturn(details);

        // Act & Assert
        mockMvc.perform(get("/rest/pharmaceutical/samples/1/details")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sampleName").value("DetailedSample"))
                .andExpect(jsonPath("$.aliquotCount").value(3));
    }

    // ==================== Search Tests ====================

    @Test
    public void testSearch_WithValidQuery_ReturnsMatchingSamples() throws Exception {
        // Arrange
        PharmaceuticalSample sample = createTestSample(1, "Aspirin Sample", "PHARMA");
        when(sampleServiceMock.searchByName("Aspirin")).thenReturn(Arrays.asList(sample));

        // Act & Assert
        mockMvc.perform(get("/rest/pharmaceutical/samples/search")
                        .param("query", "Aspirin")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==================== POST Register Tests ====================

    @Test
    public void testRegisterSample_WithValidData_Returns201() throws Exception {
        // Arrange
        PharmaceuticalSample inputSample = new PharmaceuticalSample();
        inputSample.setSampleName("NewSample");
        inputSample.setLabType(LabType.PHARMA);

        PharmaceuticalSample registeredSample = createTestSample(1, "NewSample", "PHARMA");
        registeredSample.setBarcode("PHARMA-00001");
        when(sampleServiceMock.registerSample(any(PharmaceuticalSample.class), anyString()))
                .thenReturn(registeredSample);

        // Act & Assert
        mockMvc.perform(post("/rest/pharmaceutical/samples/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputSample))
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.barcode").value("PHARMA-00001"));
    }

    // ==================== PUT Update Tests ====================

    @Test
    public void testUpdateSample_WithValidData_Returns200() throws Exception {
        // Arrange
        PharmaceuticalSample existingSample = createTestSample(1, "ExistingSample", "PHARMA");
        PharmaceuticalSample updatedSample = createTestSample(1, "UpdatedSample", "PHARMA");

        when(sampleServiceMock.get(1)).thenReturn(existingSample);
        when(sampleServiceMock.update(any(PharmaceuticalSample.class))).thenReturn(updatedSample);

        // Act & Assert
        mockMvc.perform(put("/rest/pharmaceutical/samples/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedSample))
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sampleName").value("UpdatedSample"));
    }

    @Test
    public void testUpdateSample_WithInvalidId_Returns404() throws Exception {
        // Arrange
        when(sampleServiceMock.get(999)).thenReturn(null);

        PharmaceuticalSample sample = new PharmaceuticalSample();
        sample.setSampleName("NonExistent");

        // Act & Assert
        mockMvc.perform(put("/rest/pharmaceutical/samples/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sample))
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isNotFound());
    }

    // ==================== PUT Update Status Tests ====================

    @Test
    public void testUpdateStatus_WithValidStatus_Returns200() throws Exception {
        // Arrange
        PharmaceuticalSample updatedSample = createTestSample(1, "StatusSample", "PHARMA");
        updatedSample.setStatus(SampleStatus.QC_PASSED);
        when(sampleServiceMock.updateStatus(eq(1), eq(SampleStatus.QC_PASSED), anyString()))
                .thenReturn(updatedSample);

        // Act & Assert
        mockMvc.perform(put("/rest/pharmaceutical/samples/1/status")
                        .param("status", "QC_PASSED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("userSessionData", usd))
                .andExpect(status().isOk());
    }

    // ==================== DELETE Tests ====================

    @Test
    public void testDelete_WithValidId_Returns200() throws Exception {
        // Arrange - delete doesn't throw exception

        // Act & Assert
        mockMvc.perform(delete("/rest/pharmaceutical/samples/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ==================== Expiring Soon Tests ====================

    @Test
    public void testGetExpiringSoon_ReturnsExpiringSamples() throws Exception {
        // Arrange
        PharmaceuticalSample sample = createTestSample(1, "ExpiringSample", "PHARMA");
        when(sampleServiceMock.findExpiringSoon(anyInt())).thenReturn(Arrays.asList(sample));

        // Act & Assert
        mockMvc.perform(get("/rest/pharmaceutical/samples/expiring")
                        .param("daysAhead", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==================== Helper Methods ====================

    private PharmaceuticalSample createTestSample(Integer id, String name, String labType) {
        PharmaceuticalSample sample = new PharmaceuticalSample();
        sample.setId(id);
        sample.setSampleName(name);
        sample.setLabType(LabType.valueOf(labType));
        sample.setStatus(SampleStatus.REGISTERED);
        sample.setReceivedDate(new Timestamp(System.currentTimeMillis()));
        return sample;
    }
}
