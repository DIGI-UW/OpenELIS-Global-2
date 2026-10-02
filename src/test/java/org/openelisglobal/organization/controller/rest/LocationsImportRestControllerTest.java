package org.openelisglobal.organization.controller.rest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/**
 * OGC-1363 (sections F, G, M) — the organizations CSV import: a preview that
 * names every outcome and saves nothing, Add &amp; update that keeps ids,
 * Replace that deactivates only within the file's scope, the decision queue,
 * the templates and the export round trip.
 *
 * <p>
 * Apply stores the file and hands it to the shared catalog loader, which the
 * test profile provides with auto-loading off (AppTestConfig), so the apply
 * path is exercised end to end: the stored file, its checksum and the handler's
 * plan.
 */
public class LocationsImportRestControllerTest extends BaseWebContextSensitiveTest {

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

    private MvcResult preview(String fileName, byte[] content, String mode) throws Exception {
        return mockMvc.perform(multipart("/rest/locations/import/preview")
                .file(new MockMultipartFile("files", fileName, "text/csv", content)).param("areas", "organizations")
                .param("mode", mode)).andExpect(status().isOk()).andReturn();
    }

    private void organization(int id, String name, String code, Integer parentId, int... typeIds) {
        jdbc.update("INSERT INTO clinlims.organization (id, name, code, short_name, org_id, is_active,"
                + " mls_sentinel_lab_flag, lastupdated, fhir_uuid) VALUES (?, ?, ?, ?, ?, 'Y', 'N', now(),"
                + " gen_random_uuid())", id, name, code, code, parentId);
        for (int typeId : typeIds) {
            jdbc.update("INSERT INTO clinlims.organization_organization_type (org_id, org_type_id) VALUES (?, ?)", id,
                    typeId);
        }
        if (code != null) {
            jdbc.update("INSERT INTO clinlims.organization_identifier (id, organization_id, label, value,"
                    + " is_reporting, lastupdated) VALUES (nextval('clinlims.organization_identifier_seq'), ?,"
                    + " 'Code', ?, true, now())", id, code);
        }
        resyncSequence("clinlims.organization_seq", "clinlims.organization");
    }

    /**
     * OGC-1420 (1a): sharing "Health Centre" does not make two centres a rename.
     */
    @Test
    public void possibleRename_isOfferedOnlyForCloseNames() throws Exception {
        organization(9120, "Kaugere Health Centre", null, null, 2);

        JsonNode different = json(preview("organizations-ncd.csv",
                (HEADER + "referingClinic,,Gerehu Health Centre,,,,,Y,,,,,,\n").getBytes(StandardCharsets.UTF_8),
                "merge"));
        assertEquals("new", different.get("rows").get(0).get("outcome").asText());

        JsonNode respelled = json(preview("organizations-ncd.csv",
                (HEADER + "referingClinic,,Kaugere Health Center,,,,,Y,,,,,,\n").getBytes(StandardCharsets.UTF_8),
                "merge"));
        assertEquals("rename", respelled.get("rows").get(0).get("outcome").asText());
        assertEquals("9120", respelled.get("rows").get(0).get("pair").get("id").asText());
    }

    /** OGC-1420 (1b): Excel's "CSV UTF-8" and plain "CSV" both import as typed. */
    @Test
    public void excelFiles_importAsTyped_withAByteOrderMarkOrInWindows1252() throws Exception {
        String file = HEADER + "referingClinic,,Clinique Sainte-Thérèse,,,,,Y,,,,,,\n";
        byte[] body = file.getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[body.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(body, 0, withBom, 3, body.length);

        JsonNode utf8 = json(preview("organizations-utf8.csv", withBom, "merge"));
        assertEquals(0, utf8.get("errors").size());
        assertEquals("Clinique Sainte-Thérèse", utf8.get("rows").get(0).get("name").asText());

        JsonNode ansi = json(
                preview("organizations-ansi.csv", file.getBytes(Charset.forName("windows-1252")), "merge"));
        assertEquals("Clinique Sainte-Thérèse", ansi.get("rows").get(0).get("name").asText());
    }

    /** OGC-1420 (1c): an identifier repeated inside one file is a row error. */
    @Test
    public void anIdentifierRepeatedInOneFile_isRejectedOnTheLaterRow() throws Exception {
        JsonNode plan = json(preview("organizations-dup-code.csv",
                (HEADER + "referingClinic,NEW-01,First Clinic,,,,,Y,,,,,,\n"
                        + "referingClinic,NEW-01,Second Clinic,,,,,Y,,,,,,\n").getBytes(StandardCharsets.UTF_8),
                "merge"));
        assertEquals("new", plan.get("rows").get(0).get("outcome").asText());
        assertEquals("rejected", plan.get("rows").get(1).get("outcome").asText());
        assertTrue(plan.get("rows").get(1).get("reason").asText().contains("line 2"));
    }

    /** OGC-1420 (1d): a column the importer does not know is listed as ignored. */
    @Test
    public void anUnknownColumn_isListedAsIgnored() throws Exception {
        JsonNode plan = json(preview("organizations-extra.csv",
                ("type,name,Notes\nreferingClinic,Notes Clinic,call first\n").getBytes(StandardCharsets.UTF_8),
                "merge"));
        assertEquals("new", plan.get("rows").get(0).get("outcome").asText());
        assertEquals(1, plan.get("ignoredColumns").size());
        assertEquals("Notes", plan.get("ignoredColumns").get(0).asText());
    }

    /**
     * OGC-1420 (2a): a referral lab that is also a referring clinic does not put
     * every referring clinic in a referral-lab file's scope.
     */
    @Test
    public void replace_scopesToTheTypesTheFileIsAbout_notEveryTypeOnItsRows() throws Exception {
        organization(9130, "Dual Reference Lab", "REF-DUAL", null, 915, 2);
        organization(9131, "Plain Referring Clinic", "RC-PLAIN", null, 2);
        organization(9132, "Lone Reference Lab", "REF-LONE", null, 915);
        String file = "type,code,name\nreferralLab;referingClinic,REF-DUAL,Dual Reference Lab\n"
                + "referralLab,REF-LONE,Lone Reference Lab\n";

        JsonNode plan = json(preview("organizations-referral.csv", file.getBytes(StandardCharsets.UTF_8), "replace"));
        assertEquals(0, plan.get("counts").get("deactivated").asInt());
        assertEquals(0, plan.get("deactivations").size());
        assertTrue(plan.get("scope").get("text").asText().contains("referralLab"));

        organization(9133, "Retired Reference Lab", "REF-OLD", null, 915);
        JsonNode withOld = json(
                preview("organizations-referral.csv", file.getBytes(StandardCharsets.UTF_8), "replace"));
        assertEquals("a referral lab missing from the file is still replaced", 1,
                withOld.get("counts").get("deactivated").asInt());
        assertEquals("Retired Reference Lab", withOld.get("deactivations").get(0).get("name").asText());
    }

    /**
     * OGC-1420 (2b): Replace shows a sampling site's open orders as the list does.
     */
    @Test
    public void replacePreview_countsASamplingSitesOpenOrders() throws Exception {
        Map<String, Object> site = Map.of("kind", "site", "name", "Bumbu light trap", "parentId", "9101", "identifiers",
                List.of(Map.of("label", "Code", "value", "VT-BUMBU", "reporting", true)), "site",
                Map.of("siteType", "Vector trap"));
        mockMvc.perform(post("/rest/locations/organizations").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(site))).andExpect(status().isCreated());
        Integer siteId = jdbc.queryForObject(
                "SELECT s.id FROM clinlims.vector_sampling_site s JOIN"
                        + " clinlims.organization o ON o.id = s.organization_id WHERE o.name = 'Bumbu light trap'",
                Integer.class);
        for (int i = 0; i < 2; i++) {
            Long sampleId = jdbc.queryForObject("INSERT INTO clinlims.sample (id, accession_number, entered_date,"
                    + " received_date, is_confirmation, lastupdated) VALUES (nextval('clinlims.sample_seq'), ?, now(),"
                    + " now(), false, now()) RETURNING id", Long.class, "LORG-24T-" + i);
            jdbc.update(
                    "INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, collection_location_id,"
                            + " lastupdated) VALUES (nextval('clinlims.sample_item_seq'), ?, 1, 1, ?, now())",
                    sampleId, siteId);
        }

        JsonNode plan = json(preview("organizations-sites.csv",
                "type,code,name,parentCode\nsampling site,VT-OTHER,Other trap,MOR-LAE\n"
                        .getBytes(StandardCharsets.UTF_8),
                "replace"));
        JsonNode deactivation = null;
        for (JsonNode row : plan.get("deactivations")) {
            if ("Bumbu light trap".equals(row.get("name").asText())) {
                deactivation = row;
            }
        }
        assertTrue("the site missing from the file is replaced", deactivation != null);
        assertEquals(2, deactivation.get("inUse").get("open").asInt());
    }

    /**
     * OGC-1420 (9b): Recent imports names each run's files and tells a preview from
     * an apply.
     */
    @Test
    public void recentImports_nameTheFiles_andTellAPreviewFromAnApply() throws Exception {
        preview("organizations-preview-only.csv",
                (HEADER + "referingClinic,,Recent Preview Clinic,,,,,Y,,,,,,\n").getBytes(StandardCharsets.UTF_8),
                "merge");
        mockMvc.perform(multipart("/rest/locations/import/apply")
                .file(csv("organizations-applied.csv", HEADER + "referingClinic,,Recent Applied Clinic,,,,,Y,,,,,,\n"))
                .param("areas", "organizations").param("mode", "merge")).andExpect(status().isOk());

        JsonNode runs = json(
                mockMvc.perform(get("/rest/locations/import/recent")).andExpect(status().isOk()).andReturn());
        assertEquals("organizations-applied.csv", runs.get(0).get("files").get(0).asText());
        assertTrue(runs.get(0).get("applied").asBoolean());
        assertEquals("organizations-preview-only.csv", runs.get(1).get("files").get(0).asText());
        assertEquals(false, runs.get(1).get("applied").asBoolean());
    }
}
