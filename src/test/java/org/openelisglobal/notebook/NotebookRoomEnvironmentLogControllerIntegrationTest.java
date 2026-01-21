package org.openelisglobal.notebook;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookEntryRoomEnvironmentLogService;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookEntryRoomEnvironmentLog;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for NotebookRoomEnvironmentLogController.
 *
 * Tests verify: - GET /rest/notebook-entry/{entryId}/room-environment-logs -
 * list logs - POST /rest/notebook-entry/{entryId}/room-environment-logs -
 * create log - Validation (at least one measurement required) - Error handling
 * (invalid entry ID)
 */
public class NotebookRoomEnvironmentLogControllerIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NotebookEntryRoomEnvironmentLogService roomEnvironmentLogService;

    @Autowired
    private SystemUserService systemUserService;

    private ObjectMapper objectMapper;
    private MockHttpSession mockSession;
    private SystemUser testUser;
    private NoteBook testNotebook;
    private NotebookEntry testEntry;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();

        // Setup test user
        testUser = systemUserService.get("1");
        if (testUser == null) {
            testUser = new SystemUser();
            testUser.setLoginName("test_room_env_user");
            testUser.setFirstName("Test");
            testUser.setLastName("Room Env User");
            testUser.setIsActive("Y");
            testUser.setSysUserId("1");
            systemUserService.save(testUser);
        }

        // Set up mock session with user data for authentication
        mockSession = new MockHttpSession();
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(Integer.parseInt(testUser.getId()));
        userSessionData.setLoginName(testUser.getLoginName());
        userSessionData.setAdmin(true);
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);

        // Setup test notebook
        testNotebook = createTestNotebook("Test Notebook " + System.currentTimeMillis());

        // Setup test entry
        testEntry = createTestNotebookEntry(testNotebook, "Test Entry " + System.currentTimeMillis());
    }

    // ========== GET LOGS TESTS ==========

    @Test
    public void testGetRoomEnvironmentLogs_ReturnsArray() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/notebook-entry/" + testEntry.getId() + "/room-environment-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());
    }

    @Test
    public void testGetRoomEnvironmentLogs_ReturnsExistingLogs() throws Exception {
        // Arrange - Create test logs
        roomEnvironmentLogService.logRoomEnvironment(testEntry.getId(), "ROOM-001", "Storage Room A", 20.9, // O2 level
                45.0, // Humidity
                "Test Technician", new Timestamp(System.currentTimeMillis()), "Test notes",
                testUser.getId().toString());

        roomEnvironmentLogService.logRoomEnvironment(testEntry.getId(), "ROOM-002", "Storage Room B", 21.0, 50.0,
                "Test Technician 2", new Timestamp(System.currentTimeMillis()), "More notes",
                testUser.getId().toString());

        // Act
        MvcResult result = mockMvc.perform(get("/rest/notebook-entry/" + testEntry.getId() + "/room-environment-logs")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());
        assertTrue("Should have at least 2 logs", responseJson.size() >= 2);

        // Verify structure of returned logs
        for (JsonNode log : responseJson) {
            assertTrue("Log should have 'id' field", log.has("id"));
            assertTrue("Log should have 'entryId' field", log.has("entryId"));
            assertEquals("EntryId should match test entry", testEntry.getId().intValue(), log.get("entryId").asInt());
        }
    }

    @Test
    public void testGetRoomEnvironmentLogs_ReturnsCorrectDataFields() throws Exception {
        // Arrange - Create log with specific data
        String expectedRoomId = "ROOM-SPECIFIC-" + System.currentTimeMillis();
        String expectedRoomName = "Specific Test Room";
        Double expectedO2 = 20.5;
        Double expectedHumidity = 42.0;
        String expectedCheckedBy = "Dr. Test";
        String expectedNotes = "Specific test notes";

        roomEnvironmentLogService.logRoomEnvironment(testEntry.getId(), expectedRoomId, expectedRoomName, expectedO2,
                expectedHumidity, expectedCheckedBy, new Timestamp(System.currentTimeMillis()), expectedNotes,
                testUser.getId().toString());

        // Act
        MvcResult result = mockMvc.perform(get("/rest/notebook-entry/" + testEntry.getId() + "/room-environment-logs")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        // Find our specific log by roomId
        JsonNode targetLog = null;
        for (JsonNode log : responseJson) {
            if (log.has("roomId") && expectedRoomId.equals(log.get("roomId").asText())) {
                targetLog = log;
                break;
            }
        }

        assertNotNull("Should find log with specific roomId", targetLog);
        assertEquals("Room name should match", expectedRoomName, targetLog.get("roomName").asText());
        assertEquals("O2 level should match", expectedO2, targetLog.get("oxygenLevel").asDouble(), 0.01);
        assertEquals("Humidity should match", expectedHumidity, targetLog.get("humidity").asDouble(), 0.01);
        assertEquals("CheckedBy should match", expectedCheckedBy, targetLog.get("checkedBy").asText());
        assertEquals("Notes should match", expectedNotes, targetLog.get("notes").asText());
    }

    // ========== POST LOG TESTS ==========

    @Test
    public void testLogRoomEnvironment_Success_WithBothMeasurements() throws Exception {
        // Arrange
        String requestBody = "{" + "\"roomId\":\"ROOM-POST-001\"," + "\"roomName\":\"Post Test Room\","
                + "\"oxygenLevel\":20.8," + "\"humidity\":48.5," + "\"checkedBy\":\"Test Checker\","
                + "\"checkedDateTime\":\"2024-01-15T10:30\"," + "\"notes\":\"Test log entry\"" + "}";

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/notebook-entry/" + testEntry.getId() + "/room-environment-logs")
                        .session(mockSession).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should have 'success' field", responseJson.has("success"));
        assertTrue("Success should be true", responseJson.get("success").asBoolean());
        assertTrue("Response should have 'log' field", responseJson.has("log"));

        JsonNode createdLog = responseJson.get("log");
        assertEquals("Room ID should match", "ROOM-POST-001", createdLog.get("roomId").asText());
        assertEquals("Room name should match", "Post Test Room", createdLog.get("roomName").asText());
        assertEquals("O2 level should match", 20.8, createdLog.get("oxygenLevel").asDouble(), 0.01);
        assertEquals("Humidity should match", 48.5, createdLog.get("humidity").asDouble(), 0.01);
        assertEquals("CheckedBy should match", "Test Checker", createdLog.get("checkedBy").asText());
    }

    @Test
    public void testLogRoomEnvironment_Success_WithOnlyO2Level() throws Exception {
        // Arrange - Only O2 level, no humidity
        String requestBody = "{" + "\"roomId\":\"ROOM-O2-ONLY\"," + "\"roomName\":\"O2 Only Room\","
                + "\"oxygenLevel\":19.5," + "\"checkedBy\":\"O2 Checker\"," + "\"notes\":\"Low O2 warning test\"" + "}";

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/notebook-entry/" + testEntry.getId() + "/room-environment-logs")
                        .session(mockSession).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Success should be true", responseJson.get("success").asBoolean());
        JsonNode createdLog = responseJson.get("log");

        // Verify O2 level is set correctly
        assertTrue("Response should have oxygenLevel field", createdLog.has("oxygenLevel"));
        assertFalse("oxygenLevel should not be null", createdLog.get("oxygenLevel").isNull());
        assertEquals("O2 level should match", 19.5, createdLog.get("oxygenLevel").asDouble(), 0.01);

        // Verify humidity is not set (field absent or null when not provided)
        // Jackson typically omits null fields from serialization
        if (createdLog.has("humidity")) {
            assertTrue("Humidity should be null when not provided", createdLog.get("humidity").isNull());
        }
        // If humidity field is absent, that's also correct behavior
    }

    @Test
    public void testLogRoomEnvironment_Success_WithOnlyHumidity() throws Exception {
        // Arrange - Only humidity, no O2
        String requestBody = "{" + "\"roomId\":\"ROOM-HUMID-ONLY\"," + "\"roomName\":\"Humidity Only Room\","
                + "\"humidity\":65.0," + "\"checkedBy\":\"Humidity Checker\","
                + "\"notes\":\"High humidity warning test\"" + "}";

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/notebook-entry/" + testEntry.getId() + "/room-environment-logs")
                        .session(mockSession).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Success should be true", responseJson.get("success").asBoolean());
        JsonNode createdLog = responseJson.get("log");

        // Verify O2 level is not set (field absent or null when not provided)
        // Jackson typically omits null fields from serialization
        if (createdLog.has("oxygenLevel")) {
            assertTrue("oxygenLevel should be null when not provided", createdLog.get("oxygenLevel").isNull());
        }
        // If oxygenLevel field is absent, that's also correct behavior

        // Verify humidity is set correctly
        assertTrue("Response should have humidity field", createdLog.has("humidity"));
        assertFalse("humidity should not be null", createdLog.get("humidity").isNull());
        assertEquals("Humidity should match", 65.0, createdLog.get("humidity").asDouble(), 0.01);
    }

    // ========== VALIDATION TESTS ==========

    @Test
    public void testLogRoomEnvironment_Validation_NoMeasurements_ReturnsBadRequest() throws Exception {
        // Arrange - No O2 or humidity provided
        String requestBody = "{" + "\"roomId\":\"ROOM-NO-MEASUREMENTS\"," + "\"roomName\":\"Invalid Room\","
                + "\"checkedBy\":\"Test Checker\"" + "}";

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/notebook-entry/" + testEntry.getId() + "/room-environment-logs")
                        .session(mockSession).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        assertTrue("Response should have 'error' field", responseJson.has("error"));
        String errorMessage = responseJson.get("error").asText();
        assertTrue("Error should mention measurement required", errorMessage.toLowerCase().contains("measurement")
                || errorMessage.toLowerCase().contains("o2") || errorMessage.toLowerCase().contains("humidity"));
    }

    @Test
    public void testLogRoomEnvironment_Validation_NullMeasurements_ReturnsBadRequest() throws Exception {
        // Arrange - Explicitly null measurements
        String requestBody = "{" + "\"roomId\":\"ROOM-NULL-MEASUREMENTS\"," + "\"roomName\":\"Null Room\","
                + "\"oxygenLevel\":null," + "\"humidity\":null," + "\"checkedBy\":\"Test Checker\"" + "}";

        // Act
        MvcResult result = mockMvc
                .perform(post("/rest/notebook-entry/" + testEntry.getId() + "/room-environment-logs")
                        .session(mockSession).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest()).andReturn();

        // Assert
        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should have 'error' field", responseJson.has("error"));
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    public void testLogRoomEnvironment_InvalidEntryId_ReturnsError() throws Exception {
        // Arrange - Use non-existent entry ID
        Integer nonExistentEntryId = 999999;
        String requestBody = "{" + "\"roomId\":\"ROOM-INVALID\"," + "\"roomName\":\"Invalid Entry Room\","
                + "\"oxygenLevel\":21.0," + "\"checkedBy\":\"Test Checker\"" + "}";

        // Act
        MvcResult result = mockMvc.perform(post("/rest/notebook-entry/" + nonExistentEntryId + "/room-environment-logs")
                .session(mockSession).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andReturn();

        // Assert - Controller catches IllegalArgumentException and returns 400, or it
        // propagates as 500
        int status = result.getResponse().getStatus();
        assertTrue("Status should be 400 (bad request) or 500 (internal error) for invalid entry ID",
                status == 400 || status == 500);

        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);

        // Verify error response contains error information
        assertTrue("Response should have 'error' field", responseJson.has("error"));
        String errorMessage = responseJson.get("error").asText();
        assertFalse("Error message should not be empty", errorMessage.isEmpty());
        assertTrue("Error message should indicate entry not found", errorMessage.toLowerCase().contains("not found")
                || errorMessage.toLowerCase().contains("entry") || errorMessage.toLowerCase().contains("notebook"));
    }

    @Test
    public void testGetRoomEnvironmentLogs_InvalidEntryId_ReturnsEmptyArray() throws Exception {
        // For GET, an invalid entry ID just returns an empty array (no matching logs)
        Integer nonExistentEntryId = 999999;

        MvcResult result = mockMvc.perform(get("/rest/notebook-entry/" + nonExistentEntryId + "/room-environment-logs")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());
        assertEquals("Array should be empty for non-existent entry", 0, responseJson.size());
    }

    // ========== PERSISTENCE VERIFICATION TESTS ==========

    @Test
    public void testLogRoomEnvironment_LogIsPersistedAndRetrievable() throws Exception {
        // Arrange - Create log via POST
        String uniqueRoomId = "ROOM-PERSIST-" + System.currentTimeMillis();
        String requestBody = "{" + "\"roomId\":\"" + uniqueRoomId + "\"," + "\"roomName\":\"Persistence Test Room\","
                + "\"oxygenLevel\":21.0," + "\"humidity\":50.0," + "\"checkedBy\":\"Persistence Tester\","
                + "\"notes\":\"Persistence test notes\"" + "}";

        // Act - Create via POST
        MvcResult postResult = mockMvc
                .perform(post("/rest/notebook-entry/" + testEntry.getId() + "/room-environment-logs")
                        .session(mockSession).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()).andReturn();

        // Get the created log ID
        JsonNode postResponse = objectMapper.readTree(postResult.getResponse().getContentAsString());
        assertTrue("POST should succeed", postResponse.get("success").asBoolean());
        Integer createdLogId = postResponse.get("log").get("id").asInt();

        // Act - Retrieve via GET
        MvcResult getResult = mockMvc
                .perform(get("/rest/notebook-entry/" + testEntry.getId() + "/room-environment-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        // Assert - Find our log in the GET response
        JsonNode getResponse = objectMapper.readTree(getResult.getResponse().getContentAsString());
        JsonNode foundLog = null;
        for (JsonNode log : getResponse) {
            if (log.get("id").asInt() == createdLogId) {
                foundLog = log;
                break;
            }
        }

        assertNotNull("Should find created log via GET", foundLog);
        assertEquals("Room ID should match", uniqueRoomId, foundLog.get("roomId").asText());
        assertEquals("Room name should match", "Persistence Test Room", foundLog.get("roomName").asText());
        assertEquals("O2 level should match", 21.0, foundLog.get("oxygenLevel").asDouble(), 0.01);
        assertEquals("Humidity should match", 50.0, foundLog.get("humidity").asDouble(), 0.01);
    }

    @Test
    public void testLogRoomEnvironment_ServiceDirectCreation_VerifyFields() throws Exception {
        // Arrange - Create log directly via service
        String roomId = "ROOM-SERVICE-" + System.currentTimeMillis();
        Double expectedO2 = 19.8;
        Double expectedHumidity = 55.5;
        Timestamp checkedTime = new Timestamp(System.currentTimeMillis());

        NotebookEntryRoomEnvironmentLog createdLog = roomEnvironmentLogService.logRoomEnvironment(testEntry.getId(),
                roomId, "Service Test Room", expectedO2, expectedHumidity, "Service Tester", checkedTime,
                "Service created log", testUser.getId().toString());

        // Assert - Verify the log was created with correct data
        assertNotNull("Created log should not be null", createdLog);
        assertNotNull("Created log should have ID", createdLog.getId());
        assertEquals("Room ID should match", roomId, createdLog.getRoomId());
        assertEquals("O2 level should match", expectedO2, createdLog.getOxygenLevel(), 0.01);
        assertEquals("Humidity should match", expectedHumidity, createdLog.getHumidity(), 0.01);

        // Verify via GET endpoint
        MvcResult getResult = mockMvc
                .perform(get("/rest/notebook-entry/" + testEntry.getId() + "/room-environment-logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode getResponse = objectMapper.readTree(getResult.getResponse().getContentAsString());
        JsonNode foundLog = null;
        for (JsonNode log : getResponse) {
            if (roomId.equals(log.get("roomId").asText())) {
                foundLog = log;
                break;
            }
        }

        assertNotNull("Should find service-created log via API", foundLog);
        assertEquals("O2 should match via API", expectedO2, foundLog.get("oxygenLevel").asDouble(), 0.01);
    }

    // ========== HELPER METHODS ==========

    private NoteBook createTestNotebook(String title) {
        NoteBook notebook = new NoteBook();
        notebook.setTitle(title);
        notebook.setIsTemplate(true);
        notebook.setStatus(NoteBookStatus.ACTIVE);
        notebook.setDateCreated(new Date());
        notebook.setSysUserId(testUser.getId().toString());
        return noteBookService.save(notebook);
    }

    private NotebookEntry createTestNotebookEntry(NoteBook notebook, String title) {
        NotebookEntry entry = notebookEntryService.createEntry(notebook.getId(), title, testUser.getId().toString());
        return entry;
    }
}
