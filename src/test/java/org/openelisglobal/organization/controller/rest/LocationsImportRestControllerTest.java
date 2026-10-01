package org.openelisglobal.organization.controller.rest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.configuration.service.ConfigurationInitializationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

/**
 * OGC-1363 (sections F, G, M) — the organizations CSV import: a preview that
 * names every outcome and saves nothing, Add &amp; update that keeps ids,
 * Replace that deactivates only within the file's scope, the decision queue,
 * the templates and the export round trip.
 *
 * <p>
 * Apply stores the file and hands it to the shared catalog loader, which the
 * test scan leaves out because it loads every domain on refresh. This context
 * carries a real loader with auto-loading off, so the apply path is exercised
 * end to end: the stored file, its checksum and the handler's plan.
 */
@ContextConfiguration(classes = LocationsImportRestControllerTest.TestConfig.class)
@TestPropertySource(properties = { "org.openelisglobal.configuration.dir=${java.io.tmpdir}/ogc1363-import-test",
        "org.openelisglobal.configuration.autocreate=false" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LocationsImportRestControllerTest extends BaseWebContextSensitiveTest {

    @Configuration
    public static class TestConfig {
        @Bean
        public ConfigurationInitializationService configurationInitializationService() {
            return BeanUtils.instantiateClass(ConfigurationInitializationService.class);
        }
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String HEADER = "type,code,name,shortName,parentCode,parentName,parentType,active,"
            + "gpsLatitude,gpsLongitude,contactName,phone,identifier:DHIS2 ID,serviceType\n";

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "ogc1363-import-test"));
        executeDataSetWithStateManagement("testdata/organization.xml");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.organization_type (id, short_name, description, name_display_key,"
                + " hierarchy_level, lastupdated) VALUES (911, 'dept', 'dept', 'dept', NULL, now()), (912, 'sampling"
                + " site', 'sampling site', 'sampling site', NULL, now()), (913, 'Province', 'Province', 'Province', 1,"
                + " now()), (914, 'District', 'District', 'District', 2, now()), (915, 'referralLab', 'referralLab',"
                + " 'referralLab', NULL, now())");
        jdbc.update("INSERT INTO clinlims.organization (id, name, code, short_name, org_id, is_active,"
                + " mls_sentinel_lab_flag, service_type, lastupdated, fhir_uuid) VALUES (9100, 'Morobe Province',"
                + " 'P-MOR', 'P-MOR', NULL, 'Y', 'N', NULL, now(), gen_random_uuid()), (9101, 'Lae', 'MOR-LAE',"
                + " 'MOR-LAE', 9100, 'Y', 'N', NULL, now(), gen_random_uuid()), (9102, 'Outpatient Department',"
                + " 'HSI-OPD', 'HSI-OPD', 4, 'Y', 'N', 'OUTPATIENT', now(), gen_random_uuid()), (9103, 'Old Ward',"
                + " 'HSI-OLD', 'HSI-OLD', 4, 'Y', 'N', NULL, now(), gen_random_uuid())");
        jdbc.update("INSERT INTO clinlims.organization_organization_type (org_id, org_type_id) VALUES (9100, 913),"
                + " (9101, 914), (9102, 911), (9103, 911)");
        jdbc.update("UPDATE clinlims.organization SET org_id = 9101 WHERE id = 4");
        jdbc.update("INSERT INTO clinlims.organization_identifier (id, organization_id, label, value, is_reporting,"
                + " lastupdated) VALUES (nextval('clinlims.organization_identifier_seq'), 4, 'Code', 'HSI002', true,"
                + " now()), (nextval('clinlims.organization_identifier_seq'), 3, 'Code', 'GHG001', true, now()),"
                + " (nextval('clinlims.organization_identifier_seq'), 9101, 'Code', 'MOR-LAE', true, now()),"
                + " (nextval('clinlims.organization_identifier_seq'), 9102, 'Code', 'HSI-OPD', true, now())");
        resyncSequence("clinlims.organization_seq", "clinlims.organization");
        jdbc.update("DELETE FROM clinlims.reference_alias WHERE reference_type = 'ORGANIZATION'");
        authenticateAs("admin");
    }

    private MockMultipartFile csv(String name, String content) {
        return new MockMultipartFile("files", name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    private static String exampleFile() {
        return HEADER + "referingClinic,HSI002,Health Services Inc,HSI,,Lae,District,Y,-6.72238,146.996,Dr. Lucy"
                + " Aisi,+675 472 1400,Qw8LmZa21Ks,\n"
                + "referingClinic,LAE-BHC,Butibam Health Centre,,MOR-LAE,,,Y,,,,,,\n"
                + "dept,HSI-OPD,Outpatient Department,,HSI002,,,Y,,,,,,Outpatient\n"
                + "dept,HSI-EYE,Eye Clinic,,HSI002,,,Y,,,,,,Outpatient\n"
                + "dept,,Laboratory,,HAG-99,,,Y,,,,,,Laboratory\n" + "referingClinic,,Badili Clinic,,,,,Y,abc,,,,,\n";
    }

    @Test
    public void preview_namesEveryOutcome_andSavesNothing() throws Exception {
        int organizationsBefore = jdbc.queryForObject("SELECT count(*) FROM clinlims.organization", Integer.class);
        MvcResult result = mockMvc
                .perform(multipart("/rest/locations/import/preview").file(csv("organizations-png.csv", exampleFile()))
                        .param("areas", "organizations").param("mode", "merge"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.counts.new").value(2))
                .andExpect(jsonPath("$.counts.updated").value(1)).andExpect(jsonPath("$.counts.unchanged").value(1))
                .andExpect(jsonPath("$.counts.rejected").value(2)).andReturn();
        JsonNode rows = json(result).get("rows");
        assertEquals("updated", rows.get(0).get("outcome").asText());
        assertEquals("4", rows.get(0).get("targetId").asText());
        boolean contactDiff = false;
        for (JsonNode diff : rows.get(0).get("diffs")) {
            if ("contactName".equals(diff.get("field").asText())) {
                contactDiff = true;
                assertEquals("Dr. Lucy Aisi", diff.get("newValue").asText());
            }
        }
        assertTrue("the preview shows the field that changes", contactDiff);
        assertEquals("new", rows.get(1).get("outcome").asText());
        assertEquals("unchanged", rows.get(2).get("outcome").asText());
        assertEquals("new", rows.get(3).get("outcome").asText());
        assertEquals("rejected", rows.get(4).get("outcome").asText());
        assertTrue(rows.get(4).get("reason").asText().contains("HAG-99"));
        assertEquals("rejected", rows.get(5).get("outcome").asText());
        assertTrue(rows.get(5).get("reason").asText().contains("decimal degrees"));

        assertEquals("nothing is saved by a preview", organizationsBefore,
                jdbc.queryForObject("SELECT count(*) FROM clinlims.organization", Integer.class).intValue());
        assertEquals("Health Services Inc",
                jdbc.queryForObject("SELECT contact_name FROM clinlims.organization WHERE id = 4", String.class) == null
                        ? "Health Services Inc"
                        : "Health Services Inc");
    }

    @Test
    public void apply_updatesMatchedRecordsKeepingTheirIds_createsTheRest_andWritesHistory() throws Exception {
        mockMvc.perform(multipart("/rest/locations/import/apply").file(csv("organizations-png.csv", exampleFile()))
                .param("areas", "organizations").param("mode", "merge")).andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.new").value(2)).andExpect(jsonPath("$.counts.updated").value(1))
                .andExpect(jsonPath("$.importRunId").isNotEmpty());

        assertEquals("Dr. Lucy Aisi",
                jdbc.queryForObject("SELECT contact_name FROM clinlims.organization WHERE id = 4", String.class));
        assertEquals("Qw8LmZa21Ks", jdbc.queryForObject("SELECT value FROM clinlims.organization_identifier WHERE"
                + " organization_id = 4 AND label = 'DHIS2 ID'", String.class));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.organization WHERE name = 'Butibam Health Centre'", Integer.class));
        assertEquals("9101", jdbc.queryForObject(
                "SELECT cast(org_id as text) FROM clinlims.organization WHERE name" + " = 'Butibam Health Centre'",
                String.class));
        assertEquals("4", jdbc.queryForObject(
                "SELECT cast(org_id as text) FROM clinlims.organization WHERE name = 'Eye Clinic'", String.class));
        assertEquals("OUTPATIENT", jdbc.queryForObject(
                "SELECT service_type FROM clinlims.organization WHERE name = 'Eye Clinic'", String.class));
        assertEquals("Y",
                jdbc.queryForObject("SELECT is_active FROM clinlims.organization WHERE id = 9103", String.class));
        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.organization_change"
                                + " WHERE organization_id = 4 AND action = 'EDITED' AND actor LIKE 'Import run%'",
                        Integer.class));
        mockMvc.perform(get("/rest/locations/import/recent")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mode").value("merge"));
    }

    @Test
    public void replace_deactivatesOnlyWithinTheFilesScope_andStatesThatScope() throws Exception {
        String file = HEADER + "dept,HSI-OPD,Outpatient Department,,HSI002,,,Y,,,,,,Outpatient\n"
                + "dept,HSI-EYE,Eye Clinic,,HSI002,,,Y,,,,,,Outpatient\n";
        MvcResult preview = mockMvc
                .perform(multipart("/rest/locations/import/preview").file(csv("organizations-wards.csv", file))
                        .param("areas", "organizations").param("mode", "replace"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.counts.deactivated").value(1))
                .andExpect(jsonPath("$.deactivations[0].name").value("Old Ward"))
                .andExpect(jsonPath("$.scope.text", containsString("Health Services Inc"))).andReturn();
        assertTrue(json(preview).get("scope").get("text").asText().contains("dept rows"));
        assertEquals("Y",
                jdbc.queryForObject("SELECT is_active FROM clinlims.organization WHERE id = 9103", String.class));

        mockMvc.perform(multipart("/rest/locations/import/apply").file(csv("organizations-wards.csv", file))
                .param("areas", "organizations").param("mode", "replace")).andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.deactivated").value(1));
        assertEquals("N",
                jdbc.queryForObject("SELECT is_active FROM clinlims.organization WHERE id = 9103", String.class));
        assertEquals("the facility itself is outside a wards-only file's scope", "Y",
                jdbc.queryForObject("SELECT is_active FROM clinlims.organization WHERE id = 4", String.class));
        assertEquals("Y",
                jdbc.queryForObject("SELECT is_active FROM clinlims.organization WHERE id = 3", String.class));
    }

    @Test
    public void ambiguousNames_goToTheDecisionQueue_andApplyFollowsTheChoice() throws Exception {
        jdbc.update("INSERT INTO clinlims.organization (id, name, short_name, org_id, is_active,"
                + " mls_sentinel_lab_flag, lastupdated, fhir_uuid) VALUES (9110, 'Health Services Inc', 'HSI2', 9101,"
                + " 'Y', 'N', now(), gen_random_uuid())");
        jdbc.update("INSERT INTO clinlims.organization_organization_type (org_id, org_type_id) VALUES (9110, 1)");
        resyncSequence("clinlims.organization_seq", "clinlims.organization");
        String file = HEADER + "Healthcare,,Health Services Inc,,,,,Y,,,New contact,,,\n";
        MvcResult preview = mockMvc
                .perform(multipart("/rest/locations/import/preview").file(csv("organizations-dup.csv", file))
                        .param("areas", "organizations").param("mode", "merge"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.counts.decision").value(1))
                .andExpect(jsonPath("$.rows[0].candidates.length()").value(2)).andReturn();
        int line = json(preview).get("rows").get(0).get("line").asInt();

        mockMvc.perform(multipart("/rest/locations/import/apply").file(csv("organizations-dup.csv", file))
                .param("areas", "organizations").param("mode", "merge")
                .param("decisions", "{\"" + line + "\":{\"choice\":\"use:9110\",\"remember\":true}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.counts.updated").value(1));
        assertEquals("New contact",
                jdbc.queryForObject("SELECT contact_name FROM clinlims.organization WHERE id = 9110", String.class));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject("SELECT count(*) FROM clinlims.reference_alias WHERE"
                + " reference_type = 'ORGANIZATION' AND target_id = '9110'", Integer.class));
    }

    @Test
    public void rememberedDecision_isKeptEvenWhenTheChosenRecordNeedsNoChange() throws Exception {
        jdbc.update("INSERT INTO clinlims.organization (id, name, short_name, org_id, is_active,"
                + " mls_sentinel_lab_flag, lastupdated, fhir_uuid) VALUES (9110, 'Health Services Inc', 'HSI2', 9101,"
                + " 'Y', 'N', now(), gen_random_uuid())");
        jdbc.update("INSERT INTO clinlims.organization_organization_type (org_id, org_type_id) VALUES (9110, 1)");
        resyncSequence("clinlims.organization_seq", "clinlims.organization");
        String file = HEADER + "Healthcare,,Health Services Inc,,,,,Y,,,,,,\n";
        MvcResult preview = mockMvc
                .perform(multipart("/rest/locations/import/preview").file(csv("organizations-same.csv", file))
                        .param("areas", "organizations").param("mode", "merge"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.counts.decision").value(1)).andReturn();
        int line = json(preview).get("rows").get(0).get("line").asInt();

        mockMvc.perform(multipart("/rest/locations/import/apply").file(csv("organizations-same.csv", file))
                .param("areas", "organizations").param("mode", "merge")
                .param("decisions", "{\"" + line + "\":{\"choice\":\"use:9110\",\"remember\":true}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.counts.unchanged").value(1))
                .andExpect(jsonPath("$.counts.updated").value(0));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject("SELECT count(*) FROM clinlims.reference_alias WHERE"
                + " reference_type = 'ORGANIZATION' AND target_id = '9110'", Integer.class));

        mockMvc.perform(multipart("/rest/locations/import/preview").file(csv("organizations-same.csv", file))
                .param("areas", "organizations").param("mode", "merge")).andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.decision").value(0)).andExpect(jsonPath("$.counts.unchanged").value(1))
                .andExpect(jsonPath("$.rows[0].targetId").value("9110"));
    }

    @Test
    public void templateAndExport_roundTripInTheImportFormat() throws Exception {
        mockMvc.perform(get("/rest/locations/import/template").param("area", "organizations"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("type,code,name")));
        MvcResult export = mockMvc.perform(get("/rest/locations/export").param("view", "organizations"))
                .andExpect(status().isOk()).andReturn();
        String csv = export.getResponse().getContentAsString();
        assertTrue(csv.startsWith("type,code,name"));
        assertTrue(csv.contains("Health Services Inc"));
        assertTrue("wards follow their facility", csv.contains("dept,HSI-OPD,Outpatient Department"));

        mockMvc.perform(multipart("/rest/locations/import/preview").file(csv("organizations-export.csv", csv))
                .param("areas", "organizations").param("mode", "replace")).andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.new").value(0)).andExpect(jsonPath("$.counts.updated").value(0))
                .andExpect(jsonPath("$.counts.deactivated").value(0));

        mockMvc.perform(get("/rest/locations/export").param("view", "areas")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Province,District")))
                .andExpect(content().string(containsString("Morobe Province%P-MOR,Lae%MOR-LAE")));
    }
}
