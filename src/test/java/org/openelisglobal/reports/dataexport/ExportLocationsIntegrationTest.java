package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reportdefinition.service.ReportDefinitionService;
import org.openelisglobal.reportdefinition.valueholder.ReportDefinition;
import org.openelisglobal.reports.dataexport.form.ReportingVariable;
import org.openelisglobal.reports.dataexport.form.SavedReportDefinition;
import org.openelisglobal.reports.dataexport.form.SavedReportFilters;
import org.openelisglobal.reports.dataexport.service.ExportLocations;
import org.openelisglobal.reports.dataexport.service.PredefinedExportReports;
import org.openelisglobal.reports.dataexport.service.ReportingAccess;
import org.openelisglobal.reports.dataexport.service.ReportingCatalogService;
import org.openelisglobal.reports.dataexport.service.ReportingSavedConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1652: the Location group's columns are the organization types found above
 * facilities in Organization Management, nearest the facility first, with
 * nothing to configure, and the two predefined shared reports list every
 * Location column of this installation.
 */
@Transactional
public class ExportLocationsIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ExportLocations locations;
    @Autowired
    private PredefinedExportReports predefined;
    @Autowired
    private ReportingCatalogService catalog;
    @Autowired
    private ReportDefinitionService definitions;
    @PersistenceContext
    private EntityManager entityManager;

    @Before
    public void fixture() throws Exception {
        executeDataSetWithStateManagement("testdata/reporting-sample-testing.xml");
        ExportLocationFixture.attach(entityManager, "1");
    }

    @Test
    public void theAreaNearestTheFacilityComesFirst() {
        List<String> ids = locations.catalog(List.of("TABLE")).stream().map(ReportingVariable::id).toList();

        assertEquals(List.of("referringSite", "referringSiteCode", "wardDept"), ids.subList(0, 3));
        int district = ids.indexOf("area:" + ExportLocationFixture.DISTRICT_TYPE);
        int region = ids.indexOf("area:" + ExportLocationFixture.REGION_TYPE);
        assertTrue("both levels are columns: " + ids, district > 2 && region > 2);
        assertTrue("the district, nearer the facility, before the region: " + ids, district < region);
        assertEquals("Health District", locations.catalog(List.of("TABLE")).get(district).label());
    }

    @Test
    public void onlyATypeThatSitsAboveAFacilityBecomesAColumn() {
        ExportLocationFixture.insertType(entityManager, "99716", "Province", 1);
        ExportLocationFixture.organization(entityManager, "99706", "Riverside Clinic", "99703", null,
                ExportLocationFixture.clinicType(entityManager));

        List<String> ids = locations.fieldIds();

        assertFalse("a configured address level with nothing under it: " + ids, ids.contains("area:99716"));
        assertFalse("a clinic under a hospital is not an area: " + ids,
                ids.contains("area:" + ExportLocationFixture.clinicType(entityManager)));
        assertEquals(List.of("referringSite", "referringSiteCode", "wardDept",
                "area:" + ExportLocationFixture.DISTRICT_TYPE, "area:" + ExportLocationFixture.REGION_TYPE), ids);
    }

    @Test
    public void everyReportTypeOffersTheLocationGroup() {
        for (String type : List.of("SAMPLE_TESTING", "REFERRALS", "NON_CONFORMANCE", "SAMPLE_REJECTIONS")) {
            var definition = catalog.definition(type);
            String layout = definition.layouts().get(0);
            List<String> ids = catalog.variables(definition, layout).stream().map(ReportingVariable::id).toList();
            assertTrue(type + ": " + ids, ids.containsAll(locations.fieldIds()));
        }
    }

    @Test
    public void thePredefinedReportsListEveryLocationColumnAndAreWrittenOnlyWhenTheyChange() throws Exception {
        predefined.ensure();

        assertEquals("nothing left to write", 0, predefined.ensure());
        for (String id : List.of(PredefinedExportReports.REFERRALS_ID, PredefinedExportReports.NON_CONFORMANCE_ID)) {
            SavedReportDefinition saved = read(id);
            assertTrue(id + ": " + saved.selectedVariables(),
                    saved.selectedVariables().containsAll(locations.fieldIds()));
            var available = catalog.variables(catalog.definition(saved.reportType()), saved.layout()).stream()
                    .map(ReportingVariable::id).toList();
            assertTrue("every column can be run", available.containsAll(saved.selectedVariables()));
        }
        assertTrue(read(PredefinedExportReports.REFERRALS_ID).selectedVariables().contains("referralAccessionNumber"));
    }

    @Test
    public void aNewLevelReachesThePredefinedReportsAtOnceButNotOneThatWasRemoved() throws Exception {
        predefined.ensure();
        ReportDefinition removed = stored(PredefinedExportReports.NON_CONFORMANCE_ID);
        removed.setIsActive(false);
        definitions.update(removed);
        addZoneAboveTheRegion();

        assertTrue("opens with the new column before any restart", savedReports()
                .detail(ExportLocationFixture.reportingAdmin(entityManager), PredefinedExportReports.REFERRALS_ID)
                .definition().selectedVariables().contains("area:99715"));
        assertEquals("only the active report is rewritten", 1, predefined.ensure());

        assertTrue(read(PredefinedExportReports.REFERRALS_ID).selectedVariables().contains("area:99715"));
        assertFalse(stored(PredefinedExportReports.NON_CONFORMANCE_ID).getIsActive());
        assertFalse(read(PredefinedExportReports.NON_CONFORMANCE_ID).selectedVariables().contains("area:99715"));
    }

    @Test
    public void aPredefinedReportSomebodyChangedIsLeftAsTheyLeftIt() throws Exception {
        predefined.ensure();
        ReportDefinition edited = stored(PredefinedExportReports.REFERRALS_ID);
        String theirs = new ObjectMapper().writeValueAsString(
                new SavedReportDefinition(1, "REFERRALS", "TABLE", List.of("referralAccessionNumber", "referringSite"),
                        new SavedReportFilters(List.of(), List.of(), List.of())));
        edited.setDefinitionJson(theirs);
        edited.setUpdatedBy("2");
        edited.setSysUserId("2");
        definitions.update(edited);
        addZoneAboveTheRegion();

        assertEquals("the unchanged report is still kept current", 1, predefined.ensure());

        assertEquals(theirs, stored(PredefinedExportReports.REFERRALS_ID).getDefinitionJson());
        assertEquals(List.of("referralAccessionNumber", "referringSite"), savedReports()
                .detail(ExportLocationFixture.reportingAdmin(entityManager), PredefinedExportReports.REFERRALS_ID)
                .definition().selectedVariables());
    }

    /**
     * OGC-1655: the Routine sample export ships as a shared Spreadsheet report
     * dated by order entry, with the Routine CSV Report's columns in its order and
     * then every test, and every column can be run.
     */
    @Test
    public void theRoutineSampleExportShipsWithTheRoutineColumnsThenEveryTest() throws Exception {
        predefined.ensure();

        SavedReportDefinition routine = read(PredefinedExportReports.ROUTINE_ID);
        assertEquals("Routine sample export", stored(PredefinedExportReports.ROUTINE_ID).getName());
        assertEquals("SPREADSHEET", routine.layout());
        assertEquals("orderEnteredDate", routine.filters().dateBasis());
        assertEquals(List.of("accessionNumber", "labUnit", "nationalId", "sex", "dateOfBirth", "ageYears", "ageMonths",
                "ageWeeks", "receivedDate", "enteredDate", "collectionDate", "referringSiteCode", "referringSite",
                "program", "orderStatus"), routine.selectedVariables().subList(0, 15));
        assertTrue(routine.selectedVariables().contains("test:1"));
        assertFalse("no location areas unless a user adds them",
                routine.selectedVariables().contains("area:" + ExportLocationFixture.DISTRICT_TYPE));
        assertEquals(routine.selectedVariables(), catalog
                .validateSaved(ExportLocationFixture.reportingAdmin(entityManager), routine).selectedVariables());
    }

    /**
     * OGC-1656: Sample rejections ships with the CSV Rejection Report's columns in
     * its order, on the rejections-only report type, and every column can be run.
     */
    @Test
    public void sampleRejectionsShipsWithTheRejectionReportColumns() throws Exception {
        predefined.ensure();

        SavedReportDefinition rejections = read(PredefinedExportReports.REJECTIONS_ID);
        assertEquals("Sample rejections", stored(PredefinedExportReports.REJECTIONS_ID).getName());
        assertEquals("SAMPLE_REJECTIONS", rejections.reportType());
        assertEquals(List.of("ncAccessionNumber", "ncRequestDate", "referringSite", "ncSampleType", "ncTest",
                "rejectionReason", "rejectionDate"), rejections.selectedVariables());
        assertEquals(rejections.selectedVariables(), catalog
                .validateSaved(ExportLocationFixture.reportingAdmin(entityManager), rejections).selectedVariables());
    }

    private void addZoneAboveTheRegion() {
        ExportLocationFixture.insertType(entityManager, "99715", "Health Zone", null);
        ExportLocationFixture.organization(entityManager, "99705", "Papua Zone", null, null, "99715");
        entityManager
                .createNativeQuery(
                        "UPDATE clinlims.organization SET org_id = 99705 WHERE id = " + ExportLocationFixture.REGION)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private ReportingSavedConfigService savedReports() {
        ReportingSavedConfigService service = new ReportingSavedConfigService(definitions, catalog,
                mock(ReportingAccess.class));
        ReflectionTestUtils.setField(service, "predefined", predefined);
        return service;
    }

    private ReportDefinition stored(String id) {
        return definitions.getAllMatching("reportType", "CSV_SAVED").stream().filter(row -> id.equals(row.getId()))
                .findFirst().orElseThrow();
    }

    private SavedReportDefinition read(String id) throws Exception {
        return new ObjectMapper().readValue(stored(id).getDefinitionJson(), SavedReportDefinition.class);
    }
}
