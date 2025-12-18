package org.openelisglobal.storage.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.form.SampleAssignmentForm;
import org.openelisglobal.storage.form.SampleMovementForm;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class SampleStorageRestControllerFlexibleAssignmentTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        executeDataSetWithStateManagement("testdata/flexible-assignment-test-data.xml");
    }

    @Test
    public void assignSample_Returns201_WhenLocationIdAndTypeProvided() throws Exception {
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleItemId("EXT-1000");
        form.setLocationId("1000");
        form.setLocationType("device");
        form.setNotes("Test assignment");

        mockMvc.perform(post("/rest/storage/sample-items/assign").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignmentId").exists()).andExpect(jsonPath("$.hierarchicalPath").exists());
    }

    @Test
    public void assignSample_ReturnsValidResponse_WhenDeviceLevelLocation() throws Exception {
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleItemId("EXT-1000");
        form.setLocationId("1000");
        form.setLocationType("device");
        form.setPositionCoordinate("A5");
        form.setNotes("Test assignment");

        MvcResult result = mockMvc.perform(post("/rest/storage/sample-items/assign")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotNull(json.get("assignmentId"));
        assertTrue(json.get("hierarchicalPath").asText().contains("Main Lab"));
        assertTrue(json.get("hierarchicalPath").asText().contains("Freezer 1"));
        assertTrue(json.get("hierarchicalPath").asText().contains("A5"));
    }

    @Test
    public void assignSample_ReturnsValidResponse_WhenRackLocationWithCoordinate() throws Exception {
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleItemId("EXT-1000");
        form.setLocationId("1002");
        form.setLocationType("rack");
        form.setPositionCoordinate("B3");
        form.setNotes("Test assignment with coordinate");

        MvcResult result = mockMvc.perform(post("/rest/storage/sample-items/assign")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotNull(json.get("assignmentId"));
        assertTrue(json.get("hierarchicalPath").asText().contains("Rack-1"));
        assertTrue(json.get("hierarchicalPath").asText().contains("B3"));
    }

    @Test
    public void assignSample_Returns400_WhenMissingLocationIdOrType() throws Exception {
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleItemId("EXT-1000");
        form.setLocationType("device");
        form.setNotes("Test");

        mockMvc.perform(post("/rest/storage/sample-items/assign").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form))).andExpect(status().isBadRequest());

        form.setLocationId("1000");
        form.setLocationType(null);

        mockMvc.perform(post("/rest/storage/sample-items/assign").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form))).andExpect(status().isBadRequest());
    }

    @Test
    public void moveSample_Returns200_WhenMovingBetweenLocations() throws Exception {
        SampleAssignmentForm assignForm = new SampleAssignmentForm();
        assignForm.setSampleItemId("EXT-1001");
        assignForm.setLocationId("1000");
        assignForm.setLocationType("device");
        assignForm.setNotes("Initial assignment");

        mockMvc.perform(post("/rest/storage/sample-items/assign").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignForm))).andExpect(status().isCreated());

        SampleMovementForm moveForm = new SampleMovementForm();
        moveForm.setSampleItemId("EXT-1001");
        moveForm.setLocationId("1001");
        moveForm.setLocationType("shelf");
        moveForm.setReason("Moving to shelf");

        mockMvc.perform(post("/rest/storage/sample-items/move").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(moveForm))).andExpect(status().isOk())
                .andExpect(jsonPath("$.movementId").exists());
    }

    @Test
    public void moveSample_Returns200_WhenUsingStringSampleItemId() throws Exception {
        SampleAssignmentForm assignForm = new SampleAssignmentForm();
        assignForm.setSampleItemId("EXT-1001");
        assignForm.setLocationId("1000");
        assignForm.setLocationType("device");
        assignForm.setNotes("Initial assignment");

        mockMvc.perform(post("/rest/storage/sample-items/assign").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignForm))).andExpect(status().isCreated());

        SampleMovementForm moveForm = new SampleMovementForm();
        moveForm.setSampleItemId("EXT-1001");
        moveForm.setLocationId("1001");
        moveForm.setLocationType("shelf");
        moveForm.setReason("Moving to shelf");

        mockMvc.perform(post("/rest/storage/sample-items/move").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(moveForm))).andExpect(status().isOk())
                .andExpect(jsonPath("$.movementId").exists());
    }

    @Test
    public void moveSample_CreatesMovementRecord_WhenDeviceToRack() throws Exception {
        SampleAssignmentForm assignForm = new SampleAssignmentForm();
        assignForm.setSampleItemId("EXT-1002");
        assignForm.setLocationId("1000");
        assignForm.setLocationType("device");
        assignForm.setPositionCoordinate("A1");
        assignForm.setNotes("Initial assignment to device");

        MvcResult assignResult = mockMvc
                .perform(post("/rest/storage/sample-items/assign").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignForm)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.assignmentId").exists()).andReturn();

        SampleMovementForm moveForm = new SampleMovementForm();
        moveForm.setSampleItemId("EXT-1002");
        moveForm.setLocationId("1002");
        moveForm.setLocationType("rack");
        moveForm.setReason("Moving to rack");

        MvcResult moveResult = mockMvc
                .perform(post("/rest/storage/sample-items/move").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveForm)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.movementId").exists()).andReturn();

        int movementCount = countRowsInTable("sample_storage_movement");
        assertTrue("Movement record should be created for disposal", movementCount > 0);
    }

    @Test
    public void moveSample_ReturnsValidResponse_WhenDeviceToRackWithCoordinate() throws Exception {
        SampleAssignmentForm assignForm = new SampleAssignmentForm();
        assignForm.setSampleItemId("EXT-1003");
        assignForm.setLocationId("1000");
        assignForm.setLocationType("device");
        assignForm.setNotes("Initial assignment");

        mockMvc.perform(post("/rest/storage/sample-items/assign").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignForm))).andExpect(status().isCreated());

        SampleMovementForm moveForm = new SampleMovementForm();
        moveForm.setSampleItemId("EXT-1003");
        moveForm.setLocationId("1002");
        moveForm.setLocationType("rack");
        moveForm.setPositionCoordinate("C7");
        moveForm.setReason("Moving to rack");

        MvcResult result = mockMvc.perform(post("/rest/storage/sample-items/move")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(moveForm)))
                .andExpect(status().isOk()).andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotNull(json.get("movementId"));
        assertNotNull(json.get("newHierarchicalPath"));
        assertTrue(json.get("newHierarchicalPath").asText().contains("Rack-1"));
        assertTrue(json.get("newHierarchicalPath").asText().contains("C7"));

        int assignmentCount = countRowsInTable("sample_storage_assignment");
        assertTrue("Assignment should exist", assignmentCount > 0);
    }

    @Test
    public void assignSample_SavesToDatabase_WhenWithPositionCoordinate() throws Exception {
        SampleAssignmentForm form = new SampleAssignmentForm();
        form.setSampleItemId("EXT-1004");
        form.setLocationId("1000");
        form.setLocationType("device");
        form.setPositionCoordinate("A1");
        form.setNotes("Initial assignment with position");

        MvcResult result = mockMvc.perform(post("/rest/storage/sample-items/assign")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isCreated()).andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotNull(json.get("assignmentId"));

        int assignmentCount = countRowsInTable("sample_storage_assignment");
        assertTrue("Assignment should be saved to database", assignmentCount > 0);
    }

    @Test
    public void moveSample_SavesToDatabase_WhenWithPositionCoordinate() throws Exception {
        SampleAssignmentForm assignForm = new SampleAssignmentForm();
        assignForm.setSampleItemId("EXT-1005");
        assignForm.setLocationId("1000");
        assignForm.setLocationType("device");
        assignForm.setPositionCoordinate("A1");
        assignForm.setNotes("Initial assignment");

        mockMvc.perform(post("/rest/storage/sample-items/assign").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignForm))).andExpect(status().isCreated());

        SampleMovementForm moveForm = new SampleMovementForm();
        moveForm.setSampleItemId("EXT-1005");
        moveForm.setLocationId("1001");
        moveForm.setLocationType("shelf");
        moveForm.setPositionCoordinate("B5");
        moveForm.setReason("Moving to shelf with position");

        MvcResult result = mockMvc.perform(post("/rest/storage/sample-items/move")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(moveForm)))
                .andExpect(status().isOk()).andReturn();

        int movementCount = countRowsInTable("sample_storage_movement");
        assertTrue("Movement record should be saved to database", movementCount > 0);
    }
}