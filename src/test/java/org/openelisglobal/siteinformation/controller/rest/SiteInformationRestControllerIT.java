package org.openelisglobal.siteinformation.controller.rest;

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
 * Full-stack integration tests for {@link SiteInformationRestController}.
 *
 * <p>These tests exercise the HTTP layer using MockMvc (Option B) while
 * delegating to the real Spring MVC stack, service layer, Hibernate ORM and
 * PostgreSQL database configured for the test environment. Test data is loaded
 * via DBUnit using {@code executeDataSetWithStateManagement}, ensuring that
 * controller behaviour is validated end-to-end against persisted entities.</p>
 */
@RunWith(SpringRunner.class)
public class SiteInformationRestControllerIT extends BaseWebContextSensitiveTest {

    /**
     * Initializes the Spring test context and loads site information fixtures
     * into the PostgreSQL database using DBUnit.
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/site-information-controller.xml");
    }

    /**
     * Verifies that the SiteInformation endpoint returns 200 OK and the expected
     * site configuration data for a known identifier.
     */
    @Test
    public void testGetSiteInformationSuccess() throws Exception {
        this.mockMvc
                .perform(get("/rest/SiteInformation")
                        .param("id", "1")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paramName").value("reportsDirectory"))
                .andExpect(jsonPath("$.value").value("/reports"));
    }

    /**
     * Verifies that updating an existing site information entry via POST
     * succeeds and returns the updated value.
     */
    @Test
    public void testUpdateSiteInformationSuccess() throws Exception {
        String updatedJson = "{"
                + "\"paramName\":\"reportsDirectory\","
                + "\"description\":\"the directory for the reports\","
                + "\"value\":\"/updated-reports\","
                + "\"encrypted\":false,"
                + "\"siteInfoDomainName\":\"SiteInformation\","
                + "\"tag\":\"report\""
                + "}";

        this.mockMvc
                .perform(post("/rest/SiteInformation")
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paramName").value("reportsDirectory"))
                .andExpect(jsonPath("$.value").value("/updated-reports"));
    }
}
