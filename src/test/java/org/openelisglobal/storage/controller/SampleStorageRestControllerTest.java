package org.openelisglobal.storage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.form.SampleAssignmentForm;
import org.openelisglobal.storage.service.SampleStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Integration tests for SampleStorageRestController - Sample Assignment
 * Following TDD: Write tests BEFORE implementation
 * Tests based on contracts/storage-api.json
 */
public class SampleStorageRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleStorageService sampleStorageService;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
    }

    /**
     * T042: Test assigning sample to valid position returns HTTP 201
     * Contract: POST /rest/storage/samples/assign → 201 + assignment details
     */
    @Test
    public void testAssignSample_ValidInput_Returns201() throws Exception {
        // Given: Valid assignment form
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleId("sample-123");
        form.setPositionId("position-456");
        form.setNotes("Initial storage assignment");

        String requestBody = objectMapper.writeValueAsString(form);

        // When: POST to /rest/storage/samples/assign
        // Then: Expect 201 Created with hierarchical path
        mockMvc.perform(post("/rest/storage/samples/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.assignmentId").exists())
            .andExpect(jsonPath("$.hierarchicalPath").exists())
            .andExpect(jsonPath("$.assignedDate").exists());
    }

    /**
     * T042: Test assigning sample to occupied position returns HTTP 400
     * Validation: Cannot assign to already-occupied position
     */
    @Test
    public void testAssignSample_OccupiedPosition_Returns400() throws Exception {
        // Given: Assignment to occupied position
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleId("sample-new");
        form.setPositionId("position-occupied"); // Already occupied
        form.setNotes("Should fail");

        String requestBody = objectMapper.writeValueAsString(form);

        // When: POST assignment to occupied position
        // Then: Expect 400 Bad Request
        mockMvc.perform(post("/rest/storage/samples/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    /**
     * T042: Test assigning sample to inactive location returns HTTP 400
     * Validation: Cannot assign to inactive storage location
     */
    @Test
    public void testAssignSample_InactiveLocation_Returns400() throws Exception {
        // Given: Assignment to position in inactive hierarchy
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleId("sample-456");
        form.setPositionId("position-inactive"); // In inactive location
        form.setNotes("Should fail");

        String requestBody = objectMapper.writeValueAsString(form);

        // When: POST assignment to inactive location
        // Then: Expect 400 Bad Request
        mockMvc.perform(post("/rest/storage/samples/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    /**
     * T042: Test assigning sample with missing required fields returns HTTP 400
     * Validation: sampleId and positionId are required
     */
    @Test
    public void testAssignSample_MissingRequiredFields_Returns400() throws Exception {
        // Given: Form with missing sampleId
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setPositionId("position-123");
        // sampleId is null

        String requestBody = objectMapper.writeValueAsString(form);

        // When: POST with missing required field
        // Then: Expect 400 Bad Request
        mockMvc.perform(post("/rest/storage/samples/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
}

