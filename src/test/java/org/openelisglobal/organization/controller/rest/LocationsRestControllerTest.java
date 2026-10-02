package org.openelisglobal.organization.controller.rest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.organization.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/**
 * OGC-1363 (Locations and Organizations) — the admin menu's REST surface: the
 * filtered, searchable list; a detail with identifiers, wards and location; a
 * save that keeps the reporting code and the change history in step; the
 * deactivation guard, undo and the inactive-parent block; wards under their
 * facility; the geographic-area tree; and a sampling site that keeps its site
 * record in step with its organization.
 */
public class LocationsRestControllerTest extends BaseWebContextSensitiveTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String DEPT_TYPE_ID = "911";
    private static final String SITE_TYPE_ID = "912";
    private static final String PROVINCE_TYPE_ID = "913";
    private static final String DISTRICT_TYPE_ID = "914";
    private static final String REFERRAL_TYPE_ID = "915";
    private static final String MOROBE_ID = "9100";
    private static final String LAE_ID = "9101";
    private static final String WARD_ID = "9102";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OrganizationService organizationService;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/organization.xml");
        jdbc = new JdbcTemplate(dataSource);
        insertType(DEPT_TYPE_ID, "dept", null);
        insertType(SITE_TYPE_ID, "sampling site", null);
        insertType(PROVINCE_TYPE_ID, "Province", 1);
        insertType(DISTRICT_TYPE_ID, "District", 2);
        insertType(REFERRAL_TYPE_ID, "referralLab", null);
        insertOrganization(MOROBE_ID, "Morobe Province", "P-MOR", null, "Y");
        insertOrganization(LAE_ID, "Lae", "MOR-LAE", MOROBE_ID, "Y");
        insertOrganization(WARD_ID, "Outpatient Department", "HSI-OPD", "4", "Y");
        jdbc.update("UPDATE clinlims.organization SET service_type = 'OUTPATIENT' WHERE id = 9102");
        link(MOROBE_ID, PROVINCE_TYPE_ID);
        link(LAE_ID, DISTRICT_TYPE_ID);
        link(WARD_ID, DEPT_TYPE_ID);
        jdbc.update("UPDATE clinlims.organization SET org_id = 9101 WHERE id = 4");
        jdbc.update("INSERT INTO clinlims.organization_identifier (id, organization_id, label, value, is_reporting,"
                + " lastupdated) VALUES (nextval('clinlims.organization_identifier_seq'), 4, 'Code', 'HSI002', true,"
                + " now()), (nextval('clinlims.organization_identifier_seq'), 4, 'DHIS2 ID', 'Qw8LmZa21Ks', false,"
                + " now())");
        resyncSequence("clinlims.organization_seq", "clinlims.organization");
        authenticateAs("admin");
    }

    private void insertType(String id, String name, Integer level) {
        jdbc.update(
                "INSERT INTO clinlims.organization_type (id, short_name, description, name_display_key,"
                        + " hierarchy_level, lastupdated) VALUES (?::numeric, ?, ?, ?, ?, now())",
                id, name, name, name, level);
    }

    private void insertOrganization(String id, String name, String code, String parentId, String active) {
        jdbc.update("INSERT INTO clinlims.organization (id, name, code, short_name, org_id, is_active,"
                + " mls_sentinel_lab_flag, lastupdated, fhir_uuid) VALUES (?::numeric, ?, ?, ?, ?::numeric, ?, 'N',"
                + " now(), gen_random_uuid())", id, name, code, code, parentId, active);
    }

    private void link(String organizationId, String typeId) {
        jdbc.update("INSERT INTO clinlims.organization_organization_type (org_id, org_type_id) VALUES (?::numeric,"
                + " ?::numeric)", organizationId, typeId);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    private Map<String, Object> facility(String name, String code) {
        return Map.of("kind", "facility", "name", name, "typeIds", List.of("2"), "parentId", LAE_ID, "identifiers",
                List.of(Map.of("label", "Code", "value", code, "reporting", true),
                        Map.of("label", "CLIA", "value", "CLIA-" + code, "reporting", false)),
                "gpsLatitude", -6.7224, "gpsLongitude", 146.996, "contactName", "Peter Sine", "email",
                "lab@example.org");
    }

    @Test
    public void list_showsActiveFacilitiesWithLocationAndWardCount_andFiltersByStatusTypeAndSearch() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/locations/organizations")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2)).andReturn();
        JsonNode rows = json(result).get("items");
        JsonNode hsi = null;
        for (JsonNode row : rows) {
            assertEquals("facility", row.get("kind").asText());
            if ("4".equals(row.get("id").asText())) {
                hsi = row;
            }
        }
        assertNotNull("Health Services Inc is listed", hsi);
        assertEquals("HSI002", hsi.get("code").asText());
        assertEquals("Lae", hsi.get("location").get("name").asText());
        assertEquals("District", hsi.get("location").get("levelName").asText());
        assertEquals("Morobe Province", hsi.get("location").get("path").get(0).asText());
        assertEquals(1, hsi.get("wardCount").asInt());

        mockMvc.perform(get("/rest/locations/organizations").param("status", "inactive")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].id").value("5"));
        mockMvc.perform(get("/rest/locations/organizations").param("status", "all")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3));
        mockMvc.perform(get("/rest/locations/organizations").param("type", "2").param("status", "all"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value("5"));
        mockMvc.perform(get("/rest/locations/organizations").param("q", "hsi002")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].id").value("4"));
        mockMvc.perform(get("/rest/locations/organizations").param("location", MOROBE_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].id").value("4"));
        mockMvc.perform(get("/rest/locations/organizations").param("q", "zzz-nothing")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    public void list_findsARecordByAnotherIdentifier_byAWardName_andSaysSo() throws Exception {
        mockMvc.perform(get("/rest/locations/organizations").param("q", "Qw8LmZa21Ks")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].id").value("4"))
                .andExpect(jsonPath("$.items[0].matchNote").value("DHIS2 ID Qw8LmZa21Ks"));
        mockMvc.perform(get("/rest/locations/organizations").param("q", "outpatient")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].id").value("4"))
                .andExpect(jsonPath("$.items[0].matchNote").value("ward:Outpatient Department"));
    }

    @Test
    public void get_returnsTheDetailWithIdentifiersWardsAndLocation() throws Exception {
        mockMvc.perform(get("/rest/locations/organizations/4")).andExpect(status().isOk())
                .andExpect(jsonPath("$.row.name").value("Health Services Inc"))
                .andExpect(jsonPath("$.parentId").value(LAE_ID)).andExpect(jsonPath("$.identifiers.length()").value(2))
                .andExpect(jsonPath("$.identifiers[0].label").value("Code"))
                .andExpect(jsonPath("$.identifiers[0].reporting").value(true))
                .andExpect(jsonPath("$.wards.length()").value(1))
                .andExpect(jsonPath("$.wards[0].name").value("Outpatient Department"))
                .andExpect(jsonPath("$.wards[0].serviceType").value("OUTPATIENT"))
                .andExpect(jsonPath("$.lastupdated").isNumber());
        mockMvc.perform(get("/rest/locations/organizations/424242")).andExpect(status().isNotFound());
    }

    @Test
    public void create_storesTheRecordItsReportingCodeAndItsHistory() throws Exception {
        MvcResult created = mockMvc
                .perform(post("/rest/locations/organizations").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(facility("Angau Memorial General Hospital", "ANGAU"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.detail.row.name").value("Angau Memorial General Hospital"))
                .andExpect(jsonPath("$.detail.row.code").value("ANGAU"))
                .andExpect(jsonPath("$.detail.row.location.name").value("Lae"))
                .andExpect(jsonPath("$.detail.identifiers.length()").value(2)).andReturn();
        String id = json(created).get("detail").get("row").get("id").asText();

        assertEquals("ANGAU",
                jdbc.queryForObject("SELECT code FROM clinlims.organization WHERE id = ?::numeric", String.class, id));
        assertEquals("CLIA-ANGAU", jdbc
                .queryForObject("SELECT clia_num FROM clinlims.organization WHERE id = ?::numeric", String.class, id));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject("SELECT count(*) FROM clinlims.organization_change"
                + " WHERE organization_id = ?::numeric AND action = 'CREATED'", Integer.class, id));
        mockMvc.perform(get("/rest/locations/organizations/" + id + "/history")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[0].user").isNotEmpty());
    }

    @Test
    public void create_refusesAMissingName_anOutOfRangeGps_andAnIdentifierAnotherRecordUses() throws Exception {
        Map<String, Object> blank = Map.of("kind", "facility", "name", " ", "typeIds", List.of("2"), "gpsLatitude", 120,
                "identifiers", List.of(Map.of("label", "DHIS2 ID", "value", "Qw8LmZa21Ks", "reporting", true)));
        mockMvc.perform(post("/rest/locations/organizations").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(blank))).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.name").value("Name is required"))
                .andExpect(jsonPath("$.fieldErrors.gpsLatitude", containsString("-90 to 90")))
                .andExpect(jsonPath("$.fieldErrors.identifiers", containsString("Health Services Inc")));
        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.organization WHERE trim(name) = ''", Integer.class));
    }

    @Test
    public void update_refusesAStaleSave_recordsFieldChanges_andFindsTheFormerName() throws Exception {
        JsonNode detail = json(mockMvc.perform(get("/rest/locations/organizations/4")).andReturn());
        long token = detail.get("lastupdated").asLong();
        Map<String, Object> edit = Map.of("name", "Health Services Hospital", "typeIds", List.of("1"), "parentId",
                LAE_ID, "identifiers",
                List.of(Map.of("label", "Code", "value", "HSI002", "reporting", true),
                        Map.of("label", "DHIS2 ID", "value", "Qw8LmZa21Ks", "reporting", false)),
                "contactName", "New contact", "lastupdated", token - 1000);
        mockMvc.perform(put("/rest/locations/organizations/4").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(edit))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.current.row.name").value("Health Services Inc"));

        Map<String, Object> fresh = new java.util.HashMap<>(edit);
        fresh.put("lastupdated", token);
        mockMvc.perform(put("/rest/locations/organizations/4").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(fresh))).andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.row.name").value("Health Services Hospital"));

        MvcResult history = mockMvc.perform(get("/rest/locations/organizations/4/history")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("EDITED")).andReturn();
        JsonNode changes = json(history).get(0).get("changes");
        boolean renamed = false;
        for (JsonNode change : changes) {
            if ("name".equals(change.get("field").asText())) {
                assertEquals("Health Services Inc", change.get("oldValue").asText());
                assertEquals("Health Services Hospital", change.get("newValue").asText());
                renamed = true;
            }
        }
        assertTrue("the rename is in the history", renamed);
        mockMvc.perform(get("/rest/locations/organizations").param("q", "Services Inc")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].id").value("4"))
                .andExpect(jsonPath("$.items[0].matchNote").value("formerly:Health Services Inc"));
    }

    @Test
    public void deactivate_withItsWards_isUndoneByReactivatingTheSameIds_andNeverUnderAnInactiveParent()
            throws Exception {
        mockMvc.perform(get("/rest/locations/organizations/4/usage")).andExpect(status().isOk())
                .andExpect(jsonPath("$.activeChildren.length()").value(1))
                .andExpect(jsonPath("$.activeChildren[0].name").value("Outpatient Department"));

        MvcResult off = mockMvc
                .perform(post("/rest/locations/organizations/active").contentType(MediaType.APPLICATION_JSON).content(
                        JSON.writeValueAsString(Map.of("ids", List.of("4"), "active", false, "includeChildren", true))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.changed.length()").value(2)).andReturn();
        assertEquals("N",
                jdbc.queryForObject("SELECT is_active FROM clinlims.organization WHERE id = 4", String.class));
        assertEquals("N",
                jdbc.queryForObject("SELECT is_active FROM clinlims.organization WHERE id = 9102", String.class));

        mockMvc.perform(post("/rest/locations/organizations/active").contentType(MediaType.APPLICATION_JSON).content(
                JSON.writeValueAsString(Map.of("ids", List.of(WARD_ID), "active", true, "includeChildren", false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Reactivate Health Services Inc first."));

        List<String> changed = JSON.convertValue(json(off).get("changed"),
                JSON.getTypeFactory().constructCollectionType(List.class, String.class));
        mockMvc.perform(post("/rest/locations/organizations/active").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("ids", changed, "active", true, "includeChildren", false))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.changed.length()").value(2));
        assertEquals("Y",
                jdbc.queryForObject("SELECT is_active FROM clinlims.organization WHERE id = 9102", String.class));
        assertEquals(Integer.valueOf(2),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.organization_change"
                                + " WHERE organization_id = 9102 AND action IN ('DEACTIVATED', 'REACTIVATED')",
                        Integer.class));
    }

    @Test
    public void wards_areAddedUnderTheirFacility_editedInPlace_andMoved() throws Exception {
        Map<String, Object> ward = Map.of("name", "Maternity Ward", "code", "HSI-MAT", "serviceType", "Maternity",
                "contactName", "Sister Grace Tau");
        MvcResult created = mockMvc
                .perform(post("/rest/locations/organizations/4/wards").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(ward)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.serviceType").value("MATERNITY"))
                .andExpect(jsonPath("$.code").value("HSI-MAT")).andReturn();
        String wardId = json(created).get("id").asText();
        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.organization_organization_type"
                        + " WHERE org_id = ?::numeric AND org_type_id = 911", Integer.class, wardId));
        assertEquals("4",
                jdbc.queryForObject("SELECT cast(org_id as text) FROM clinlims.organization WHERE id =" + " ?::numeric",
                        String.class, wardId));

        mockMvc.perform(post("/rest/locations/organizations/4/wards").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("name", "No service type"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.serviceType").value("Choose a service type"));

        mockMvc.perform(put("/rest/locations/organizations/4/wards/" + wardId).contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("name", "Maternity Ward B", "serviceType", "Maternity"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Maternity Ward B"));

        mockMvc.perform(post("/rest/locations/wards/" + wardId + "/move").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("parentId", "3")))).andExpect(status().isOk());
        assertEquals("3",
                jdbc.queryForObject("SELECT cast(org_id as text) FROM clinlims.organization WHERE id =" + " ?::numeric",
                        String.class, wardId));
        mockMvc.perform(get("/rest/locations/organizations/" + wardId + "/history")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("MOVED"))
                .andExpect(jsonPath("$[0].changes[0].newValue").value("Global Health Org"));
    }

    @Test
    public void areas_areBrowsedAsATree_added_searchedInContext_andGuardedOnDeactivation() throws Exception {
        mockMvc.perform(get("/rest/locations/areas/levels")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].name").value("Province"));
        mockMvc.perform(get("/rest/locations/areas")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].name").value("Morobe Province"))
                .andExpect(jsonPath("$[0].level").value(1)).andExpect(jsonPath("$[0].childCount").value(1));
        mockMvc.perform(get("/rest/locations/areas").param("parentId", MOROBE_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].name").value("Lae"))
                .andExpect(jsonPath("$[0].levelName").value("District"));

        mockMvc.perform(post("/rest/locations/areas").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("name", "Huon Gulf", "code", "MOR-HG", "parentId", MOROBE_ID))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.parentId").value(MOROBE_ID));
        mockMvc.perform(post("/rest/locations/areas").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("name", "Too deep", "parentId", LAE_ID))))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/rest/locations/areas").param("q", "lae")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].name").value("Morobe Province"))
                .andExpect(jsonPath("$[0].matched").value(false)).andExpect(jsonPath("$[1].name").value("Lae"))
                .andExpect(jsonPath("$[1].matched").value(true));

        mockMvc.perform(post("/rest/locations/areas/" + MOROBE_ID + "/active").contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("still has")));
        mockMvc.perform(post("/rest/locations/areas/" + LAE_ID + "/active").contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")).andExpect(status().isConflict());
        jdbc.update("UPDATE clinlims.organization SET org_id = NULL WHERE id = 4");
        mockMvc.perform(post("/rest/locations/areas/" + LAE_ID + "/active").contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    }

    @Test
    public void site_isSavedAsAnOrganizationThatKeepsItsSamplingSiteRecordInStep() throws Exception {
        Map<String, Object> site = Map.of("kind", "site", "name", "Bumbu Settlement light trap", "parentId", LAE_ID,
                "identifiers", List.of(Map.of("label", "Code", "value", "VT-LAE-03", "reporting", true)), "gpsLatitude",
                -6.716, "gpsLongitude", 147.001, "contactName", "John Wari", "phone", "+675 7123 4567", "site",
                Map.of("siteType", "Vector trap", "subtype", "CDC light trap", "environmentalZone",
                        "Peri-urban settlement"));
        MvcResult created = mockMvc
                .perform(post("/rest/locations/organizations").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(site)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.detail.row.kind").value("site"))
                .andExpect(jsonPath("$.detail.site.siteType").value("Vector trap")).andReturn();
        String id = json(created).get("detail").get("row").get("id").asText();
        Map<String, Object> stored = jdbc.queryForMap("SELECT code, name, type, subtype, location_org_id, active,"
                + " contact_phone FROM clinlims.vector_sampling_site WHERE organization_id = ?::numeric", id);
        assertEquals("VT-LAE-03", stored.get("code"));
        assertEquals("Vector trap", stored.get("type"));
        assertEquals(LAE_ID, stored.get("location_org_id"));
        assertEquals(Boolean.TRUE, stored.get("active"));

        mockMvc.perform(get("/rest/locations/organizations").param("view", "sites")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].siteType").value("Vector trap"));
        mockMvc.perform(get("/rest/locations/organizations")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        mockMvc.perform(post("/rest/locations/organizations/active").contentType(MediaType.APPLICATION_JSON).content(
                JSON.writeValueAsString(Map.of("ids", List.of(id), "active", false, "includeChildren", false))))
                .andExpect(status().isOk());
        assertEquals(Boolean.FALSE,
                jdbc.queryForObject(
                        "SELECT active FROM clinlims.vector_sampling_site WHERE organization_id = ?::numeric",
                        Boolean.class, id));

        Map<String, Object> noCode = Map.of("kind", "site", "name", "No code site", "parentId", LAE_ID);
        mockMvc.perform(post("/rest/locations/organizations").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(noCode))).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.identifiers").value("A sampling site needs a code"));
    }

    @Test
    public void lists_offerTheAdminManagedValueListsAndTheFacilityTypes() throws Exception {
        mockMvc.perform(get("/rest/locations/lists")).andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceTypes.length()").value(7))
                .andExpect(jsonPath("$.serviceTypes[0].label").value("Inpatient"))
                .andExpect(jsonPath("$.referralStatuses.length()").value(4))
                .andExpect(jsonPath("$.facilityTypes[*].name").isArray())
                .andExpect(jsonPath("$.areaLevels.length()").value(2))
                .andExpect(jsonPath("$.identifierLabels[*]").isArray());
    }

    /**
     * A record migrated with only a CLIA number, which is not a reporting code,
     * must still be editable: a code is optional, only a second reporting code is
     * refused.
     */
    @Test
    public void update_ofARecordWhoseOnlyIdentifierIsNotAReportingCode_isAccepted() throws Exception {
        jdbc.update("INSERT INTO clinlims.organization_identifier (id, organization_id, label, value, is_reporting,"
                + " lastupdated) VALUES (nextval('clinlims.organization_identifier_seq'), 3, 'CLIA', '05D9876543',"
                + " false, now())");
        long lastupdated = json(
                mockMvc.perform(get("/rest/locations/organizations/3")).andExpect(status().isOk()).andReturn())
                .get("lastupdated").asLong();
        Map<String, Object> body = Map.of("kind", "facility", "name", "Global Health Org", "typeIds", List.of("1"),
                "identifiers",
                List.of(Map.of("id",
                        jdbc.queryForObject("SELECT id FROM clinlims.organization_identifier WHERE organization_id = 3",
                                Integer.class),
                        "label", "CLIA", "value", "05D9876543", "reporting", false)),
                "contactName", "Reviewer", "lastupdated", lastupdated);
        mockMvc.perform(put("/rest/locations/organizations/3").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(body))).andExpect(status().isOk())
                .andExpect(jsonPath("$.detail.contactName").value("Reviewer"))
                .andExpect(jsonPath("$.detail.identifiers.length()").value(1))
                .andExpect(jsonPath("$.detail.identifiers[0].reporting").value(false));
        assertEquals("Reviewer",
                jdbc.queryForObject("SELECT contact_name FROM clinlims.organization WHERE id = 3", String.class));

        Map<String, Object> twoCodes = Map.of("kind", "facility", "name", "Global Health Org", "typeIds", List.of("1"),
                "identifiers",
                List.of(Map.of("label", "Code", "value", "GHG-A", "reporting", true),
                        Map.of("label", "DHIS2 ID", "value", "GHG-B", "reporting", true)),
                "lastupdated",
                json(mockMvc.perform(get("/rest/locations/organizations/3")).andReturn()).get("lastupdated").asLong());
        mockMvc.perform(put("/rest/locations/organizations/3").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(twoCodes))).andExpect(status().isUnprocessableEntity()).andExpect(
                        jsonPath("$.fieldErrors.identifiers").value("Only one identifier can be the reporting code"));
    }

    /**
     * The facility registry sync replaces its own records on every run; a record an
     * admin created here is not the registry's to deactivate.
     */
    @Test
    public void registrySync_deactivatesOnlyTheRecordsTheRegistrySupplied() {
        jdbc.update("UPDATE clinlims.organization SET source = 'REGISTRY' WHERE id = 3");
        organizationService.deactivateOrganizationsFromSource("REGISTRY");
        assertEquals("N",
                jdbc.queryForObject("SELECT is_active FROM clinlims.organization WHERE id = 3", String.class));
        assertEquals("Y",
                jdbc.queryForObject("SELECT is_active FROM clinlims.organization WHERE id = 4", String.class));
        assertEquals("Y",
                jdbc.queryForObject("SELECT is_active FROM clinlims.organization WHERE id = " + LAE_ID, String.class));
    }

    private Map<String, Object> with(Map<String, Object> base, String key, Object value) {
        Map<String, Object> out = new java.util.HashMap<>(base);
        out.put(key, value);
        return out;
    }

    /**
     * OGC-1420 (6a-6c): a value longer than its column is refused, naming the
     * field.
     */
    @Test
    public void save_refusesAValueLongerThanItsColumn_namingTheField_insteadOfCuttingIt() throws Exception {
        Map<String, Object> base = facility("Long Fields Clinic", "LFC-01");
        mockMvc.perform(post("/rest/locations/organizations").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(with(base, "contactName", "x".repeat(101)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.contactName", containsString("100")));
        mockMvc.perform(post("/rest/locations/organizations").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(with(base, "name", "N".repeat(201)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.name", containsString("200")));
        mockMvc.perform(post("/rest/locations/organizations").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(with(base, "identifiers",
                        List.of(Map.of("label", "Code", "value", "LFC-01", "reporting", true),
                                Map.of("label", "DHIS2 ID", "value", "D".repeat(150), "reporting", false))))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.identifiers", containsString("100")));
        mockMvc.perform(post("/rest/locations/organizations").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(with(base, "internetAddress", "w".repeat(41)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.internetAddress", containsString("40")));
        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.organization WHERE name LIKE 'Long Fields%'", Integer.class));
    }

    /** OGC-1420 (6d): a province name fits State / Province. */
    @Test
    public void save_keepsAStateOrProvinceNameInFull() throws Exception {
        MvcResult created = mockMvc
                .perform(
                        post("/rest/locations/organizations").contentType(MediaType.APPLICATION_JSON)
                                .content(JSON.writeValueAsString(with(facility("Province Field Clinic", "PFC-01"),
                                        "state", "National Capital District"))))
                .andExpect(status().isCreated()).andReturn();
        String id = json(created).get("detail").get("row").get("id").asText();
        assertEquals("National Capital District",
                jdbc.queryForObject("SELECT state FROM clinlims.organization WHERE id = ?::numeric", String.class, id));
    }

    /** OGC-1420 (7b): the default list is one name order, coded or not. */
    @Test
    public void list_defaultOrderIsByName_whetherARecordHasACodeOrNot() throws Exception {
        insertOrganization("9150", "Zulu Clinic", null, LAE_ID, "Y");
        link("9150", "2");
        insertOrganization("9151", "Alpha Clinic", "ALP-01", LAE_ID, "Y");
        link("9151", "2");
        jdbc.update("INSERT INTO clinlims.organization_identifier (id, organization_id, label, value, is_reporting,"
                + " lastupdated) VALUES (nextval('clinlims.organization_identifier_seq'), 9151, 'Code', 'ALP-01',"
                + " true, now())");
        JsonNode items = json(
                mockMvc.perform(get("/rest/locations/organizations")).andExpect(status().isOk()).andReturn())
                .get("items");
        List<String> names = new java.util.ArrayList<>();
        items.forEach(item -> names.add(item.get("name").asText()));
        List<String> sorted = new java.util.ArrayList<>(names);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        assertEquals(sorted, names);
    }

    /**
     * OGC-1420 (8): two records that came out of the upgrade with the same code can
     * still be edited; the clash is only checked for a code being set.
     */
    @Test
    public void update_ofARecordSharingALegacyCode_savesOtherChanges_butACodeBeingSetMustBeFree() throws Exception {
        insertOrganization("9160", "Upgrade Clinic One", "QADUP1", LAE_ID, "Y");
        link("9160", "2");
        insertOrganization("9161", "Upgrade Clinic Two", "QADUP1", LAE_ID, "Y");
        link("9161", "2");
        jdbc.update("INSERT INTO clinlims.organization_identifier (id, organization_id, label, value, is_reporting,"
                + " lastupdated) VALUES (nextval('clinlims.organization_identifier_seq'), 9160, 'Code', 'QADUP1',"
                + " true, now()), (nextval('clinlims.organization_identifier_seq'), 9161, 'Code', 'QADUP1', true,"
                + " now())");
        JsonNode detail = json(
                mockMvc.perform(get("/rest/locations/organizations/9160")).andExpect(status().isOk()).andReturn());
        Map<String, Object> update = new java.util.HashMap<>();
        update.put("kind", "facility");
        update.put("name", "Upgrade Clinic One");
        update.put("typeIds", List.of("2"));
        update.put("parentId", LAE_ID);
        update.put("phone", "+675 111 2222");
        update.put("identifiers", List.of(Map.of("label", "Code", "value", "QADUP1", "reporting", true)));
        update.put("lastupdated", detail.get("lastupdated"));
        mockMvc.perform(put("/rest/locations/organizations/9160").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(update))).andExpect(status().isOk());
        assertEquals("+675 111 2222",
                jdbc.queryForObject("SELECT phone FROM clinlims.organization WHERE id = 9160", String.class));

        update.put("identifiers", List.of(Map.of("label", "Code", "value", "HSI002", "reporting", true)));
        update.put("lastupdated",
                json(mockMvc.perform(get("/rest/locations/organizations/9160")).andReturn()).get("lastupdated"));
        mockMvc.perform(put("/rest/locations/organizations/9160").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(update))).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.identifiers", containsString("Health Services Inc")));
    }

    /**
     * OGC-1420 (8): codes the upgrade left on two records are listed for an admin.
     */
    @Test
    public void identifierCollisions_listsEveryValueTwoRecordsOfOneKindShare() throws Exception {
        insertOrganization("9170", "Collision Clinic One", "QADUP2", LAE_ID, "Y");
        link("9170", "2");
        insertOrganization("9171", "Collision Clinic Two", "QADUP2", LAE_ID, "Y");
        link("9171", "2");
        jdbc.update("INSERT INTO clinlims.organization_identifier (id, organization_id, label, value, is_reporting,"
                + " lastupdated) VALUES (nextval('clinlims.organization_identifier_seq'), 9170, 'Code', 'QADUP2',"
                + " true, now()), (nextval('clinlims.organization_identifier_seq'), 9171, 'Code', 'qadup2', true,"
                + " now())");
        mockMvc.perform(get("/rest/locations/identifier-collisions")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].label").value("Code"))
                .andExpect(jsonPath("$[0].value").value("QADUP2")).andExpect(jsonPath("$[0].records.length()").value(2))
                .andExpect(jsonPath("$[0].records[0].name").value("Collision Clinic One"))
                .andExpect(jsonPath("$[0].records[1].id").value("9171"));
    }
}
