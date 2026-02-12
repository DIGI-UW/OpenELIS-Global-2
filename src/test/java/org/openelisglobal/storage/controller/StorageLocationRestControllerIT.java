package org.openelisglobal.storage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Full-stack integration tests for {@link StorageLocationRestController}.
 *
 * <p>These tests exercise the HTTP layer using MockMvc (Option B) while
 * delegating to the real Spring MVC stack, service layer, Hibernate ORM and
 * PostgreSQL database used in the test environment. Test data is loaded via
 * DBUnit using {@code executeDataSetWithStateManagement}, ensuring that
 * controller behaviour is validated end-to-end against persisted entities.</p>
 */
@RunWith(SpringRunner.class)
public class StorageLocationRestControllerIT extends BaseWebContextSensitiveTest {

    /**
     * Initializes the Spring test context and loads storage-related fixtures
     * into the PostgreSQL database using DBUnit.
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/storage-location-controller.xml");
    }

    /**
     * Verifies that the rooms listing endpoint returns 200 OK and a JSON array
     * with at least one valid room entry.
     */
    @Test
    public void testGetRoomsSuccess() throws Exception {
        this.mockMvc
                .perform(get("/rest/storage/rooms").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].name").isNotEmpty());
    }

    /**
     * Verifies that creating a new storage room via POST succeeds and returns
     * the created entity with a generated identifier.
     */
    @Test
    public void testSaveStorageLocationSuccess() throws Exception {
        String roomJson = "{"
                + "\"name\":\"IT Integration Room\","
                + "\"code\":\"IT-ROOM-01\","
                + "\"description\":\"Room created by integration test\","
                + "\"active\":true"
                + "}";

        this.mockMvc
                .perform(post("/rest/storage/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("IT Integration Room"))
                .andExpect(jsonPath("$.code").value("IT-ROOM-01"));
    }

    /**
     * Verifies that requesting a non-existent room by ID returns 404 Not Found.
     */
    @Test
    public void testGetNonExistentRoom() throws Exception {
        this.mockMvc
                .perform(get("/rest/storage/rooms/999999").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
