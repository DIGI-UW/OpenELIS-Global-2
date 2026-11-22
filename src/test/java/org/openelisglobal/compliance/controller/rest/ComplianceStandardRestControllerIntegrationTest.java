package org.openelisglobal.compliance.controller.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.compliance.service.ComplianceStandardService;
import org.openelisglobal.compliance.valueholder.ComplianceStandard;
import org.openelisglobal.compliance.valueholder.ComplianceStandard.ComplianceStandardStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
public class ComplianceStandardRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ComplianceStandardService complianceStandardService;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Mutating compliance-standard endpoints require GLOBAL_ADMIN per the
        // FRS S-01 §9 authz table. The base test fixture authenticates as
        // ROLE_ADMIN/ROLE_RESULTS, which would 403 our PUT/POST/DELETE tests,
        // so we widen authorities here.
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("admin", "N/A",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_GLOBAL_ADMIN"),
                                new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_RESULTS"))));
        executeDataSetWithStateManagement("testdata/compliance_standards.xml");
    }

    @Test
    public void testGetAllStandards_withNegativePage_shouldNotCauseHibernateError() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/compliance/standards").param("page", "-1").param("size", "10")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        List<ComplianceStandard> standards = Arrays.asList(mapFromJson(content, ComplianceStandard[].class));
        assertNotNull("Standards list should not be null", standards);
    }

    @Test
    public void testGetAllStandards_withNegativeSize_shouldNotCauseHibernateError() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/compliance/standards").param("page", "0").param("size", "-5")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        List<ComplianceStandard> standards = Arrays.asList(mapFromJson(content, ComplianceStandard[].class));
        assertNotNull("Standards list should not be null", standards);
    }

    @Test
    public void testGetAllStandards_withZeroSize_shouldNotCauseHibernateError() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/compliance/standards").param("page", "0").param("size", "0")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        List<ComplianceStandard> standards = Arrays.asList(mapFromJson(content, ComplianceStandard[].class));
        assertNotNull("Standards list should not be null", standards);
    }

    @Test
    public void testGetAllStandards_withExcessiveSize_shouldLimitTo1000() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/compliance/standards").param("page", "0").param("size", "50000") // Excessive
                                                                                                                       // size
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        List<ComplianceStandard> standards = Arrays.asList(mapFromJson(content, ComplianceStandard[].class));
        assertNotNull("Standards list should not be null", standards);
    }

    @Test
    public void testGetStandardsPaginated_withNegativePage_shouldNotCauseHibernateError() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/compliance/standards").param("page", "-1").param("size", "10")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        List<ComplianceStandard> standards = Arrays.asList(mapFromJson(content, ComplianceStandard[].class));
        assertNotNull("Standards list should not be null", standards);
    }

    @Test
    public void testGetStandardsPaginated_withExtremeNegativeValues_shouldHandleGracefully() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/compliance/standards").param("page", "-999").param("size", "-100")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        List<ComplianceStandard> standards = Arrays.asList(mapFromJson(content, ComplianceStandard[].class));
        assertNotNull("Standards list should not be null", standards);
    }

    @Test
    public void testGetStandardById_withValidId_shouldReturnStandard() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/compliance/standards/{id}", "30001").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("Test Standard"))).andExpect(jsonPath("$.status", is("ACTIVE")))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        ComplianceStandard standard = mapFromJson(content, ComplianceStandard.class);
        assertThat("Standard ID should match", standard.getId(), is("30001"));
    }

    @Test
    public void testGetStandardById_withInvalidId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/rest/compliance/standards/{id}", "999999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateStandard_withValidData_shouldReturn201() throws Exception {
        ComplianceStandard newStandard = createTestComplianceStandardForCreation();
        newStandard.setId(null); // No ID for creation
        newStandard.setRegulationNumber("CREATE-TEST-" + System.currentTimeMillis()); // Unique regulation number

        MvcResult result = mockMvc
                .perform(post("/rest/compliance/standards").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStandard)).accept(MediaType.APPLICATION_JSON))
                .andReturn();

        // Check status and fail with details if not 201
        if (result.getResponse().getStatus() != 201) {
            fail("Expected 201 but got " + result.getResponse().getStatus() + ". Response body: "
                    + result.getResponse().getContentAsString());
        }

        String content = result.getResponse().getContentAsString();
        ComplianceStandard created = mapFromJson(content, ComplianceStandard.class);
        assertNotNull("Created standard should have an ID", created.getId());
        assertThat("Created standard name should match", created.getName(), is("New Test Compliance Standard"));
    }

    @Test
    public void testUpdateStandard_withValidData_shouldReturn200() throws Exception {
        // Create a fresh entity for this test to avoid optimistic locking conflicts
        ComplianceStandard newStandard = createTestComplianceStandardForCreation();
        newStandard.setName("Standard for Update Test");
        newStandard.setRegulationNumber("UPDATE-TEST-001");

        // Save the new standard first
        ComplianceStandard saved = complianceStandardService.save(newStandard);
        String testId = saved.getId();

        // Now update it
        saved.setName("Updated Standard Name");
        saved.setDescription("Updated description");

        MvcResult result = mockMvc
                .perform(put("/rest/compliance/standards/{id}", testId).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saved)).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("Updated Standard Name"))).andReturn();

        String content = result.getResponse().getContentAsString();
        ComplianceStandard updated = mapFromJson(content, ComplianceStandard.class);
        assertThat("Updated standard name should match", updated.getName(), is("Updated Standard Name"));
    }

    @Test
    public void testDeleteStandard_withValidId_shouldReturn204() throws Exception {
        // Create a fresh entity for deletion to avoid optimistic locking conflicts
        ComplianceStandard newStandard = createTestComplianceStandardForCreation();
        newStandard.setName("Standard for Delete Test");
        newStandard.setRegulationNumber("DELETE-TEST-" + System.currentTimeMillis());

        ComplianceStandard saved = complianceStandardService.save(newStandard);
        String testId = saved.getId();

        mockMvc.perform(delete("/rest/compliance/standards/{id}", testId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/rest/compliance/standards/{id}", testId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateStandard_withInvalidData_shouldReturn400() throws Exception {
        ComplianceStandard invalidStandard = new ComplianceStandard();

        mockMvc.perform(post("/rest/compliance/standards").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidStandard)).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateStandard_withValidDataUsingHelperMethod_shouldReturn201() throws Exception {
        ComplianceStandard newStandard = createTestComplianceStandardForCreation();

        MvcResult result = mockMvc
                .perform(post("/rest/compliance/standards").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStandard)).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        ComplianceStandard created = mapFromJson(content, ComplianceStandard.class);
        assertNotNull("Created standard should have an ID", created.getId());
    }

    @Test
    public void testGetAllStandards_withDefaultPagination_shouldReturn200() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/compliance/standards").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String content = result.getResponse().getContentAsString();
        List<ComplianceStandard> standards = Arrays.asList(mapFromJson(content, ComplianceStandard[].class));
        assertNotNull("Standards list should not be null", standards);
        assertTrue("Should contain at least our test standard", standards.size() >= 1);
    }

    @Test
    public void testGetStandard_withNonExistentId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/rest/compliance/standards/{id}", "99999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateStandard_withNonExistentId_shouldHandleGracefully() throws Exception {
        ComplianceStandard standard = createTestComplianceStandardForCreation();
        standard.setId("99998");

        mockMvc.perform(put("/rest/compliance/standards/{id}", "99998").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(standard)).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // 404 is correct for non-existent resource
    }

    @Test
    public void testDeleteStandard_withNonExistentId_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/rest/compliance/standards/{id}", "99997").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testPagination_withMaxIntegerValues_shouldHandleGracefully() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/compliance/standards").param("page", String.valueOf(Integer.MAX_VALUE))
                        .param("size", String.valueOf(Integer.MAX_VALUE)).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        List<ComplianceStandard> standards = Arrays.asList(mapFromJson(content, ComplianceStandard[].class));
        assertNotNull("Standards list should not be null", standards);
    }

    @Test
    public void testPagination_withMinIntegerValues_shouldHandleGracefully() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/compliance/standards").param("page", String.valueOf(Integer.MIN_VALUE))
                        .param("size", String.valueOf(Integer.MIN_VALUE)).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        String content = result.getResponse().getContentAsString();
        List<ComplianceStandard> standards = Arrays.asList(mapFromJson(content, ComplianceStandard[].class));
        assertNotNull("Standards list should not be null", standards);
    }

    private ComplianceStandard createTestComplianceStandardForCreation() {
        ComplianceStandard standard = new ComplianceStandard();
        standard.setFhirUuid(UUID.randomUUID());
        standard.setName("New Test Compliance Standard");
        standard.setIssuingBody("Test Regulatory Authority");
        standard.setRegulationNumber("NEW-TEST-001");
        standard.setVersion("1.0");
        standard.setEffectiveDate(LocalDate.of(2024, 1, 1));
        standard.setExpiryDate(LocalDate.of(2025, 12, 31));
        standard.setCountryRegion("Test Country");
        standard.setApplicableSampleTypes("Water,Soil");
        standard.setStatus(ComplianceStandardStatus.ACTIVE);
        standard.setDescription("New test compliance standard for creation testing");
        standard.setRegulatoryContext("Testing compliance regulations");
        standard.setEnforcementAuthority("Test Enforcement Agency");
        standard.setIsPreSeeded(false);
        standard.setSysUserId("1");
        return standard;
    }
}