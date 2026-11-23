package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.reports.dataexport.form.ExportFilter;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.openelisglobal.reports.dataexport.service.ReportingSource;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves source relationships with actual configured records before export
 * query acceptance.
 */
@Transactional
public class SampleTestingMappingIntegrationTest extends BaseWebContextSensitiveTest {
    @Autowired
    private TestResultComponentService components;
    @Autowired
    private TestResultService options;
    @Autowired
    private ResultService results;
    @Autowired
    private AnalysisService analyses;
    @Autowired
    private SampleItemService specimens;
    @Autowired
    private IStatusService statuses;
    @Autowired
    private ObservationHistoryService observations;
    @Autowired
    private ObservationHistoryTypeService observationTypes;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("sampleTestingSource")
    private ReportingSource source;

    @Before
    public void fixture() throws Exception {
        executeDataSetWithStateManagement("testdata/reporting-sample-testing.xml");
    }

    /**
     * This fixture declares status_of_sample, and executeDataSetWithStateManagement
     * TRUNCATEs every table a dataset names before re-inserting only that dataset's
     * own rows. Declaring a SUBSET therefore deletes every status the fixture
     * omits, and the next test to resolve one by name gets "-1" and dies on an FK
     * (analysis_status_fk, sample_status_fk). Assert every domain StatusService
     * maps, not just the two this class happens to use, so a future trim of the
     * catalog fails here rather than in an unrelated suite thirty minutes later.
     */
    @Test
    public void fixturePreservesStatusCatalogForSubsequentOrderEntry() {
        for (OrderStatus status : OrderStatus.values()) {
            assertNotEquals("order status " + status + " missing from catalog", "-1", statuses.getStatusID(status));
        }
        for (AnalysisStatus status : AnalysisStatus.values()) {
            assertNotEquals("analysis status " + status + " missing from catalog", "-1", statuses.getStatusID(status));
        }
        for (SampleStatus status : SampleStatus.values()) {
            assertNotEquals("sample status " + status + " missing from catalog", "-1", statuses.getStatusID(status));
        }
        for (ExternalOrderStatus status : ExternalOrderStatus.values()) {
            assertNotEquals("external order status " + status + " missing from catalog", "-1",
                    statuses.getStatusID(status));
        }
    }

    private TestResultComponent component(String code, String label, int order) {
        TestResultComponent component = new TestResultComponent();
        component.setCode(code);
        component.setLabel(label);
        component.setDisplayOrder(order);
        component.setResultType("N");
        return component;
    }

    private Result reading(Analysis analysis, TestResult option, String value) {
        return reading(analysis, option, value, "N", 0);
    }

    private Result reading(Analysis analysis, TestResult option, String value, String type, int grouping) {
        Result result = new Result();
        result.setAnalysis(analysis);
        result.setTestResult(option);
        result.setResultType(type);
        result.setValue(value);
        result.setGrouping(grouping);
        result.setIsReportable("Y");
        result.setSysUserId(TEST_SYS_USER_ID);
        results.insert(result);
        return result;
    }

    private void observation(String typeId, String value, ObservationHistory.ValueType valueType) {
        observation(typeId, value, valueType, "1", null, null);
    }

    private void observation(String typeId, String value, ObservationHistory.ValueType valueType, String sampleId,
            String specimenId, String patientId) {
        ObservationHistory observation = new ObservationHistory();
        observation.setObservationHistoryTypeId(typeId);
        observation.setSampleId(sampleId);
        observation.setSampleItemId(specimenId);
        observation.setPatientId(patientId);
        observation.setValue(value);
        observation.setValueType(valueType);
        observations.insert(observation);
    }

    @Test
    public void componentIdentityAndUnspecifiedPrecisionSurviveStoredRepeatsAndRename() {
        List<TestResultComponent> configured = components.saveSampleResults("1",
                List.of(component("PRIMARY", "Systolic", 0), component("DIA", "Diastolic", 1)), null, null,
                TEST_SYS_USER_ID);
        TestResultComponent primary = configured.stream().filter(c -> "PRIMARY".equals(c.getCode())).findFirst()
                .orElseThrow();
        TestResult option = options.getAllMatching("componentId", primary.getId()).get(0);
        Analysis analysis = analyses.get("1");
        analysis.setStatusId(statuses.getStatusID(AnalysisStatus.Finalized));
        analyses.update(analysis);
        Result first = reading(analysis, option, "120.125");
        Result second = reading(analysis, option, "120.125");
        String firstId = first.getId();
        String secondId = second.getId();
        assertNotEquals(firstId, secondId);
        entityManager.flush();
        entityManager.clear();

        Result stored = results.get(firstId);
        assertEquals(primary.getId(), stored.getTestResult().getComponentId());
        assertEquals("120.125", stored.getValue());
        assertEquals(-1, stored.getReportingDecimalPlaces());
        assertEquals("120.125", results.get(secondId).getValue());

        List<TestResultComponent> renamed = components.getActiveComponentsByTestId("1");
        renamed.stream().filter(c -> c.getId().equals(primary.getId())).findFirst().orElseThrow()
                .setLabel("Systolic pressure");
        components.saveComponentsForTest("1", renamed, TEST_SYS_USER_ID);
        entityManager.flush();
        entityManager.clear();
        String linkedComponent = results.get(firstId).getTestResult().getComponentId();
        assertEquals(primary.getId(), linkedComponent);
        assertEquals("Systolic pressure", components.get(linkedComponent).getLabel());
    }

    @Test
    public void collectionDateBelongsToEachSpecimenEvenUnderTheSameAccession() {
        SampleItem first = specimens.get("1");
        SampleItem second = specimens.get("2");
        second.setSample(first.getSample());
        first.setCollectionDate(Timestamp.valueOf("2026-08-01 00:00:00"));
        second.setCollectionDate(Timestamp.valueOf("2026-09-01 00:00:00"));
        specimens.update(first);
        specimens.update(second);
        entityManager.flush();
        entityManager.clear();

        List<String> eligible = entityManager
                .createQuery("select a.id from Analysis a join a.sampleItem si "
                        + "where si.sample.id = :sample and si.collectionDate >= :from and si.collectionDate < :to "
                        + "order by a.id", String.class)
                .setParameter("sample", first.getSample().getId())
                .setParameter("from", Timestamp.valueOf("2026-08-01 00:00:00"))
                .setParameter("to", Timestamp.valueOf("2026-09-01 00:00:00")).getResultList();
        assertEquals(List.of("1"), eligible);
        assertEquals(first.getSample().getId(), specimens.get("2").getSample().getId());
    }

    @Test
    public void actualSourceExportsBothStoredRepeatsInBothLayoutsAndResolvesConfiguredColumns() throws Exception {
        List<TestResultComponent> configured = components.saveSampleResults("1",
                List.of(component("PRIMARY", "Systolic", 0)), null, null, TEST_SYS_USER_ID);
        String componentId = configured.get(0).getId();
        TestResult option = options.getAllMatching("componentId", componentId).get(0);
        Analysis analysis = analyses.get("1");
        String status = statuses.getStatusID(AnalysisStatus.Finalized);
        analysis.setStatusId(status);
        analyses.update(analysis);
        SampleItem specimen = analysis.getSampleItem();
        specimen.setCollectionDate(Timestamp.valueOf("2026-08-20 10:00:00"));
        specimen.setReceivedDate(Timestamp.valueOf("2026-08-20 11:30:00"));
        specimens.update(specimen);
        reading(analysis, option, "120.125");
        reading(analysis, option, "120.125");
        entityManager.flush();
        String sectionId = analysis.getTestSection().getId();
        String accession = specimen.getSample().getAccessionNumber();
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("SPREADSHEET", "RESULT_LIST"),
                List.of("accessionNumber", "resultValue"), List.of("tests"), List.of("testIds"),
                Map.of("SPREADSHEET", List.of("accessionNumber"), "RESULT_LIST", List.of("resultValue")));
        ExportFilter filter = new ExportFilter("2026-08-01", "2026-08-31", List.of(sectionId), List.of("1"),
                List.of("FINALIZED"));
        var catalog = source.catalog();
        var pivot = catalog.stream().filter(v -> v.id().equals("component:" + componentId)).findFirst().orElseThrow();
        assertEquals("Test Localization 1 — Systolic", pivot.label());
        for (String layout : definition.layouts()) {
            var fields = catalog.stream().filter(v -> v.id().equals("accessionNumber") || v.id().equals("receivedDate")
                    || v.id().equals(layout.equals("SPREADSHEET") ? pivot.id() : "resultValue")).toList();
            StringWriter csv = new StringWriter();
            long rows = source.write(csv, new ExportSnapshot(definition, layout, fields, filter,
                    java.time.ZoneId.systemDefault().getId(), List.of(status)));
            assertEquals(2, rows);
            String[] lines = csv.toString().split("\r\n");
            assertEquals(3, lines.length);
            assertEquals(accession + ",2026-08-20,120.125", lines[1]);
            assertEquals(lines[1], lines[2]);
        }
    }

    /**
     * OGC-1652: a result row names where its sample came from: the requesting
     * facility, its code, the ward and the areas above the facility.
     */
    @Test
    public void aResultCarriesWhereItsSampleCameFrom() throws Exception {
        Analysis analysis = analyses.get("1");
        String status = statuses.getStatusID(AnalysisStatus.Finalized);
        analysis.setStatusId(status);
        analyses.update(analysis);
        SampleItem specimen = analysis.getSampleItem();
        specimen.setCollectionDate(Timestamp.valueOf("2026-08-20 10:00:00"));
        specimens.update(specimen);
        Result result = new Result();
        result.setAnalysis(analysis);
        result.setResultType("N");
        result.setValue("120");
        result.setIsReportable("Y");
        entityManager.persist(result);
        ExportLocationFixture.attach(entityManager, specimen.getSample().getId());
        entityManager.flush();
        String accession = specimen.getSample().getAccessionNumber();
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("RESULT_LIST"), List.of("accessionNumber", "resultValue"),
                List.of("location"), List.of("testIds"), Map.of("RESULT_LIST", List.of("accessionNumber")));
        List<String> ids = List.of("accessionNumber", "referringSite", "referringSiteCode", "wardDept",
                "area:" + ExportLocationFixture.DISTRICT_TYPE, "area:" + ExportLocationFixture.REGION_TYPE);
        var fields = ids.stream()
                .map(id -> source.catalog().stream().filter(v -> v.id().equals(id)).findFirst().orElseThrow()).toList();
        StringWriter csv = new StringWriter();

        source.write(csv,
                new ExportSnapshot(definition, "RESULT_LIST", fields,
                        new ExportFilter("2026-08-01", "2026-08-31", List.of(analysis.getTestSection().getId()),
                                List.of("1"), List.of("FINALIZED")),
                        java.time.ZoneId.systemDefault().getId(), List.of(status)));

        String[] lines = csv.toString().split("\r\n");
        assertEquals("﻿Accession Number,Requesting Facility,Requesting Facility Code,Ward / Dept,"
                + "Health District,Health Region", lines[0]);
        assertEquals(accession + ",Alotau Hospital,ALT-01,Maternity Ward,Milne Bay District,Southern Region", lines[1]);
    }

    @Test
    public void secondConfiguredTestArrangementExportsWithoutReportingCodeChanges() throws Exception {
        List<TestResultComponent> configured = components.saveSampleResults("2",
                List.of(component("PRIMARY", "Color", 0), component("CLARITY", "Clarity", 1)), null, null,
                TEST_SYS_USER_ID);
        TestResultComponent color = configured.stream().filter(c -> "PRIMARY".equals(c.getCode())).findFirst()
                .orElseThrow();
        TestResult option = options.getAllMatching("componentId", color.getId()).get(0);
        Analysis analysis = analyses.get("2");
        analysis.setStatusId(statuses.getStatusID(AnalysisStatus.Finalized));
        analyses.update(analysis);
        SampleItem specimen = analysis.getSampleItem();
        specimen.setCollectionDate(Timestamp.valueOf("2026-08-21 09:00:00"));
        specimens.update(specimen);
        reading(analysis, option, "2.75");
        entityManager.flush();

        String fieldId = "component:" + color.getId();
        var field = source.catalog().stream().filter(v -> v.id().equals(fieldId)).findFirst().orElseThrow();
        assertEquals("Test Localization 2 — Color", field.label());
        ExportFilter filter = new ExportFilter("2026-08-21", "2026-08-21", List.of(analysis.getTestSection().getId()),
                List.of("2"), List.of("FINALIZED"));
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("SPREADSHEET"), List.of("accessionNumber"),
                List.of("components"), List.of("testIds"), Map.of("SPREADSHEET", List.of("accessionNumber")));
        StringWriter csv = new StringWriter();

        assertEquals(1, source.write(csv, new ExportSnapshot(definition, "SPREADSHEET", List.of(field), filter,
                java.time.ZoneId.systemDefault().getId(), List.of(analysis.getStatusId()))));
        assertEquals("Test Localization 2 — Color\r\n2.75\r\n", csv.toString().substring(1));
    }

    @Test
    public void turnaroundAndCorrectionUsePersistedWorkflowState() throws Exception {
        Analysis analysis = analyses.get("1");
        analysis.setStatusId(statuses.getStatusID(AnalysisStatus.Finalized));
        analysis.setCompletedDate(Timestamp.valueOf("2026-08-20 15:00:00"));
        analysis.setReleasedDate(Timestamp.valueOf("2026-08-20 17:00:00"));
        analysis.setCorrectedSincePatientReport(true);
        analyses.update(analysis);
        SampleItem specimen = analysis.getSampleItem();
        specimen.setCollectionDate(Timestamp.valueOf("2026-08-20 10:00:00"));
        specimen.setReceivedDate(Timestamp.valueOf("2026-08-20 11:30:00"));
        specimen.getSample().setEnteredDate(Date.valueOf("2026-08-19"));
        specimens.update(specimen);
        reading(analysis, options.get("1"), "85.0");
        entityManager.flush();

        var fields = source.catalog().stream()
                .filter(v -> List.of("receivedTime", "numberOfTests", "resultStatus", "orderToResultMinutes",
                        "receivedToValidatedMinutes", "orderToCollectionMinutes", "collectionToReceivedMinutes",
                        "resultedToValidatedMinutes").contains(v.id()))
                .toList();
        ExportFilter filter = new ExportFilter("2026-08-20", "2026-08-20", List.of(analysis.getTestSection().getId()),
                List.of("1"), List.of("FINALIZED"));
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("RESULT_LIST"), fields.stream().map(v -> v.id()).toList(),
                List.of(), List.of("testIds"), Map.of("RESULT_LIST", List.of("receivedTime")));
        StringWriter csv = new StringWriter();

        assertEquals(1, source.write(csv, new ExportSnapshot(definition, "RESULT_LIST", fields, filter,
                java.time.ZoneId.systemDefault().getId(), List.of(analysis.getStatusId()))));
        assertEquals(
                "Received Time,Number of Tests Ordered,Result Status,Order to Result (min),Received to Validated (min),Order to Collection (min),Collection to Received (min),Resulted to Validated (min)\r\n"
                        + "11:30,1,Corrected,2340,330,2040,90,120\r\n",
                csv.toString().substring(1));
    }

    @Test
    public void commonSampleAndPatientAttributesComeFromTheLinkedClinicalRecords() throws Exception {
        SampleItem specimen = specimens.get("1");
        specimen.setStatusId(statuses.getStatusID(SampleStatus.Entered));
        specimens.update(specimen);
        Analysis analysis = analyses.get("1");
        analysis.setStatusId(statuses.getStatusID(AnalysisStatus.Finalized));
        analyses.update(analysis);
        reading(analysis, options.get("1"), "85.0");
        entityManager.flush();
        List<String> fieldIds = List.of("accessionNumber", "specimenId", "collectionDate", "collectionTime",
                "receivedDate", "receivedTime", "orderDate", "sampleType", "sampleStatus", "priority", "numberOfTests",
                "patientName", "dateOfBirth", "sex", "nationalId", "phoneNumber", "address");
        var catalog = source.catalog();
        var fields = fieldIds.stream()
                .map(id -> catalog.stream().filter(field -> field.id().equals(id)).findFirst().orElseThrow()).toList();
        ExportFilter filter = new ExportFilter("2023-11-15", "2023-11-15", List.of(analysis.getTestSection().getId()),
                List.of("1"), List.of("FINALIZED"));
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("RESULT_LIST"), fieldIds, List.of(), List.of("testIds"),
                Map.of("RESULT_LIST", fieldIds));
        StringWriter csv = new StringWriter();

        assertEquals(1, source.write(csv, new ExportSnapshot(definition, "RESULT_LIST", fields, filter,
                java.time.ZoneId.systemDefault().getId(), List.of(analysis.getStatusId()))));
        assertEquals(
                "Accession Number,Specimen ID,Collection Date,Collection Time,Received Date,Received Time,Order Date,Sample Type,Sample Status,Priority,Number of Tests Ordered,Patient Name,Date of Birth,Sex,National ID,Phone Number,Address\r\n"
                        + "12345,1,2023-11-15,10:00,2023-11-15,11:00,2024-06-03,Test Localization 1,"
                        + MessageUtil.getMessage("status.sample.entered")
                        + ",STAT,1,Ada Q Public,1980-02-03,F,WA-1001,555-0100,\"42 Lab Road, Seattle, WA, USA\"\r\n",
                csv.toString().substring(1));
    }

    /**
     * OGC-1680: tests and sample types read as the screens show them, in the
     * language of the user who asked for the export, not as the catalog description
     * ("Blood Test"). The field list is built in the user's language; the rows use
     * the language the job carries. A test taken on two sample types names the
     * first and counts the rest, as the test catalog does.
     */
    @Test
    public void testAndSampleTypeNamesReadInTheLanguageOfTheUser() throws Exception {
        entityManager.createNativeQuery("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id)"
                + " VALUES (99801, 1, 1), (99802, 2, 1)").executeUpdate();
        Locale previous = LocaleContextHolder.getLocale();
        try {
            LocaleContextHolder.setLocale(Locale.FRENCH);
            assertEquals("TB(TB +1)", label("test:1"));
            LocaleContextHolder.setLocale(Locale.ENGLISH);
            assertEquals("Test Localization 1(Test Localization 1 +1)", label("test:1"));
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
        Analysis analysis = analyses.get("1");
        analysis.setStatusId(statuses.getStatusID(AnalysisStatus.Finalized));
        analyses.update(analysis);
        reading(analysis, options.get("1"), "85.0");
        entityManager.flush();
        List<String> fieldIds = List.of("testName", "sampleType", "labSection");
        var fields = fieldIds.stream()
                .map(id -> source.catalog().stream().filter(field -> field.id().equals(id)).findFirst().orElseThrow())
                .toList();
        ExportFilter filter = new ExportFilter("2023-11-15", "2023-11-15", List.of(analysis.getTestSection().getId()),
                List.of("1"), List.of("FINALIZED"));
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("RESULT_LIST"), fieldIds, List.of(), List.of("testIds"),
                Map.of("RESULT_LIST", fieldIds));

        for (String[] expected : new String[][] { { "fr-FR", "TB,TB,TB" },
                { "en", "Test Localization 1,Test Localization 1,Test Localization 1" } }) {
            StringWriter csv = new StringWriter();
            assertEquals(1, source.write(csv, new ExportSnapshot(definition, "RESULT_LIST", fields, filter,
                    java.time.ZoneId.systemDefault().getId(), List.of(analysis.getStatusId()), expected[0])));
            assertEquals(expected[0], "Test Name,Sample Type,Lab Section\r\n" + expected[1] + "\r\n",
                    csv.toString().substring(1));
        }
    }

    /**
     * OGC-1680 review: a result component's translated label (OGC-1610) names its
     * field and fills the Component column in the user's language.
     */
    @Test
    public void componentLabelsReadInTheLanguageOfTheUser() throws Exception {
        List<TestResultComponent> configured = components.saveSampleResults("1",
                List.of(component("PRIMARY", "Systolic", 0), component("DIA", "Diastolic", 1)), null, null,
                TEST_SYS_USER_ID);
        TestResultComponent primary = configured.stream().filter(c -> "PRIMARY".equals(c.getCode())).findFirst()
                .orElseThrow();
        Number localization = (Number) entityManager.createNativeQuery("SELECT nextval('clinlims.localization_seq')")
                .getSingleResult();
        entityManager.createNativeQuery("INSERT INTO clinlims.localization (id, description) VALUES (?1, 'Systolic')")
                .setParameter(1, localization.longValue()).executeUpdate();
        entityManager.createNativeQuery("INSERT INTO clinlims.localization_value (id, localization_id, locale, value,"
                + " origin) VALUES (nextval('clinlims.localization_value_seq'), ?1, 'fr', 'Systolique', 'SITE')")
                .setParameter(1, localization.longValue()).executeUpdate();
        entityManager.createNativeQuery(
                "UPDATE clinlims.test_result_component SET label_localization_id = ?1 WHERE CAST(id AS varchar) = ?2")
                .setParameter(1, localization.longValue()).setParameter(2, primary.getId()).executeUpdate();
        TestResult option = options.getAllMatching("componentId", primary.getId()).get(0);
        Analysis analysis = analyses.get("1");
        analysis.setStatusId(statuses.getStatusID(AnalysisStatus.Finalized));
        analyses.update(analysis);
        reading(analysis, option, "120");
        entityManager.flush();
        entityManager.clear();

        Locale previous = LocaleContextHolder.getLocale();
        try {
            LocaleContextHolder.setLocale(Locale.FRENCH);
            assertEquals("TB — Systolique", label("component:" + primary.getId()));
            LocaleContextHolder.setLocale(Locale.ENGLISH);
            assertEquals("Test Localization 1 — Systolic", label("component:" + primary.getId()));
        } finally {
            LocaleContextHolder.setLocale(previous);
        }
        List<String> fieldIds = List.of("component");
        var fields = fieldIds.stream()
                .map(id -> source.catalog().stream().filter(field -> field.id().equals(id)).findFirst().orElseThrow())
                .toList();
        ExportFilter filter = new ExportFilter("2023-11-15", "2023-11-15", List.of(analysis.getTestSection().getId()),
                List.of("1"), List.of("FINALIZED"));
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("RESULT_LIST"), fieldIds, List.of(), List.of("testIds"),
                Map.of("RESULT_LIST", fieldIds));
        for (String[] expected : new String[][] { { "fr-FR", "Systolique" }, { "en", "Systolic" } }) {
            StringWriter csv = new StringWriter();
            assertEquals(1,
                    source.write(csv,
                            new ExportSnapshot(definition, "RESULT_LIST", fields, filter,
                                    java.time.ZoneId.systemDefault().getId(),
                                    List.of(statuses.getStatusID(AnalysisStatus.Finalized)), expected[0])));
            assertEquals(expected[0], "Component\r\n" + expected[1] + "\r\n", csv.toString().substring(1));
        }
    }

    private String label(String id) {
        return source.catalog().stream().filter(field -> field.id().equals(id)).findFirst().orElseThrow().label();
    }

    /**
     * OGC-1655: the Routine sample fields. Date Entered is when the order was
     * entered, with its time; Program is the order's program answer; Lab Unit names
     * the lab units of the specimen's tests; the age is the patient's age when the
     * specimen was collected, in whole years, months and weeks.
     */
    @Test
    public void theRoutineFieldsDescribeTheOrderAndThePatientsAgeAtCollection() throws Exception {
        String programType = entityManager
                .createQuery("select t.id from ObservationHistoryType t where t.typeName = 'program'", String.class)
                .getResultStream().findFirst().orElseGet(() -> {
                    ObservationHistoryType type = new ObservationHistoryType();
                    type.setTypeName("program");
                    type.setDescription("Program");
                    observationTypes.insert(type);
                    return type.getId();
                });
        observation(programType, "Early Infant Diagnosis", ObservationHistory.ValueType.LITERAL, "1", null, null);
        Analysis analysis = analyses.get("1");
        analysis.setStatusId(statuses.getStatusID(AnalysisStatus.Finalized));
        analyses.update(analysis);
        reading(analysis, options.get("1"), "85.0");
        entityManager.flush();
        List<String> fieldIds = List.of("accessionNumber", "enteredDate", "program", "labUnit", "ageYears", "ageMonths",
                "ageWeeks", "orderStatus");
        var catalog = source.catalog();
        var fields = fieldIds.stream()
                .map(id -> catalog.stream().filter(field -> field.id().equals(id)).findFirst().orElseThrow()).toList();
        ExportFilter filter = new ExportFilter("2023-11-15", "2023-11-15", List.of(analysis.getTestSection().getId()),
                List.of("1"), List.of("FINALIZED"));
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("SPREADSHEET"), fieldIds, List.of(), List.of("testIds"),
                Map.of("SPREADSHEET", fieldIds));
        java.time.ZoneId zone = java.time.ZoneId.systemDefault();
        StringWriter csv = new StringWriter();

        assertEquals(1, source.write(csv, new ExportSnapshot(definition, "SPREADSHEET", fields, filter, zone.getId(),
                List.of(analysis.getStatusId()))));

        String entered = Timestamp.valueOf("2024-06-03 00:00:00").toInstant().atZone(zone).toOffsetDateTime()
                .toString();
        String orderStatus = statuses.getStatusNameFromId(analysis.getSampleItem().getSample().getStatusId());
        assertNotEquals("the order's status, not the specimen's",
                statuses.getStatusNameFromId(analysis.getSampleItem().getStatusId()), orderStatus);
        assertEquals(
                "Accession Number,Date Entered,Program,Lab Unit,Age at collection (years),"
                        + "Age at collection (months),Age at collection (weeks),Order Status\r\n" + "12345," + entered
                        + ",Early Infant Diagnosis,Test Localization 1,43,525,2284," + orderStatus + "\r\n",
                csv.toString().substring(1));
    }

    /**
     * OGC-1655: the period applies to the date the user chose: the specimen's
     * collection (the default), its receipt, or the order's entry.
     */
    @Test
    public void thePeriodAppliesToTheChosenDate() throws Exception {
        Analysis analysis = analyses.get("1");
        analysis.setStatusId(statuses.getStatusID(AnalysisStatus.Finalized));
        analyses.update(analysis);
        reading(analysis, options.get("1"), "85.0");
        entityManager.flush();
        var fields = source.catalog().stream().filter(field -> field.id().equals("accessionNumber")).toList();
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("SPREADSHEET"), List.of("accessionNumber"), List.of(),
                List.of("testIds"), Map.of("SPREADSHEET", List.of("accessionNumber")));
        String section = analysis.getTestSection().getId();

        for (Object[] expected : new Object[][] { { "2023-11-15", null, 1L }, { "2023-11-15", "receivedDate", 1L },
                { "2023-11-15", "orderEnteredDate", 0L }, { "2024-06-03", null, 0L },
                { "2024-06-03", "orderEnteredDate", 1L } }) {
            ExportFilter filter = new ExportFilter((String) expected[0], (String) expected[0], List.of(section),
                    List.of("1"), List.of("FINALIZED"), (String) expected[1]);
            StringWriter csv = new StringWriter();
            assertEquals(expected[0] + " " + expected[1], expected[2],
                    source.write(csv, new ExportSnapshot(definition, "SPREADSHEET", fields, filter,
                            java.time.ZoneId.systemDefault().getId(), List.of(analysis.getStatusId()))));
        }
    }

    /**
     * OGC-1655: a specimen that was not received on its own was received with its
     * order, as order entry records it; the Received Date column and the Received
     * date period use the order's receipt rather than leaving it blank.
     */
    @Test
    public void aSpecimenWithoutItsOwnReceiptTakesItsOrdersReceivedDate() throws Exception {
        Analysis analysis = analyses.get("1");
        analysis.setStatusId(statuses.getStatusID(AnalysisStatus.Finalized));
        analyses.update(analysis);
        SampleItem specimen = analysis.getSampleItem();
        specimen.setReceivedDate(null);
        specimens.update(specimen);
        reading(analysis, options.get("1"), "85.0");
        entityManager.flush();
        List<String> fieldIds = List.of("accessionNumber", "receivedDate");
        var fields = fieldIds.stream()
                .map(id -> source.catalog().stream().filter(field -> field.id().equals(id)).findFirst().orElseThrow())
                .toList();
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("SPREADSHEET"), fieldIds, List.of(), List.of("testIds"),
                Map.of("SPREADSHEET", fieldIds));
        ExportFilter filter = new ExportFilter("2024-06-03", "2024-06-03", List.of(analysis.getTestSection().getId()),
                List.of("1"), List.of("FINALIZED"), "receivedDate");
        StringWriter csv = new StringWriter();

        assertEquals(1, source.write(csv, new ExportSnapshot(definition, "SPREADSHEET", fields, filter,
                java.time.ZoneId.systemDefault().getId(), List.of(analysis.getStatusId()))));
        assertEquals("Accession Number,Received Date\r\n12345,2024-06-03\r\n", csv.toString().substring(1));
    }

    @Test
    public void configuredObservationAppearsByStableIdentityAndUsesItsCurrentLabel() throws Exception {
        ObservationHistoryType type = new ObservationHistoryType();
        type.setTypeName("programCohort");
        type.setDescription("Program Cohort");
        observationTypes.insert(type);
        List<Dictionary> dictionary = entityManager
                .createQuery("from Dictionary d where d.dictEntry is not null order by d.id", Dictionary.class)
                .setMaxResults(1).getResultList();
        assertEquals(1, dictionary.size());
        observation(type.getId(), "Cohort A", ObservationHistory.ValueType.LITERAL);
        observation(type.getId(), "Cohort B", ObservationHistory.ValueType.LITERAL);
        observation(type.getId(), dictionary.get(0).getId(), ObservationHistory.ValueType.DICTIONARY);
        observation(type.getId(), "patient.NationalID", ObservationHistory.ValueType.KEY);
        Analysis analysis = analyses.get("1");
        analysis.setStatusId(statuses.getStatusID(AnalysisStatus.Finalized));
        analyses.update(analysis);
        SampleItem specimen = analysis.getSampleItem();
        specimen.setCollectionDate(Timestamp.valueOf("2026-08-20 10:00:00"));
        specimens.update(specimen);
        reading(analysis, options.get("1"), "85.0");
        entityManager.flush();

        String fieldId = "observation:" + type.getId();
        var field = source.catalog().stream().filter(v -> v.id().equals(fieldId)).findFirst().orElseThrow();
        assertEquals("Program Cohort", field.label());
        ExportFilter filter = new ExportFilter("2026-08-20", "2026-08-20", List.of(analysis.getTestSection().getId()),
                List.of("1"), List.of("FINALIZED"));
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("RESULT_LIST"), List.of("accessionNumber"),
                List.of("observations"), List.of("testIds"), Map.of("RESULT_LIST", List.of("accessionNumber")));
        StringWriter csv = new StringWriter();
        source.write(csv, new ExportSnapshot(definition, "RESULT_LIST", List.of(field), filter,
                java.time.ZoneId.systemDefault().getId(), List.of(analysis.getStatusId())));
        assertEquals("Program Cohort\r\nCohort A; Cohort B; " + dictionary.get(0).getDictEntry() + "; "
                + MessageUtil.getMessage("patient.NationalID") + "\r\n", csv.toString().substring(1));

        type.setDescription("Program Enrollment Cohort");
        observationTypes.update(type);
        entityManager.flush();
        entityManager.clear();
        var renamed = source.catalog().stream().filter(v -> v.id().equals(fieldId)).findFirst().orElseThrow();
        assertEquals(field.id(), renamed.id());
        assertEquals("Program Enrollment Cohort", renamed.label());
    }

    @Test
    public void configuredAnswersStayWithTheirOrderAndSpecimenInBothLayouts() throws Exception {
        ObservationHistoryType type = new ObservationHistoryType();
        type.setTypeName("collectionContext");
        type.setDescription("Collection Context");
        observationTypes.insert(type);
        observation(type.getId(), "Patient answer", ObservationHistory.ValueType.LITERAL, "1", null, "479001");
        observation(type.getId(), "Order answer", ObservationHistory.ValueType.LITERAL, "1", null, null);
        observation(type.getId(), "Linked order answer", ObservationHistory.ValueType.LITERAL, "1", null, "479001");
        observation(type.getId(), "First specimen answer", ObservationHistory.ValueType.LITERAL, "1", "1", null);
        observation(type.getId(), "Second specimen answer", ObservationHistory.ValueType.LITERAL, "1", "2", "479001");
        observation(type.getId(), "Other order answer", ObservationHistory.ValueType.LITERAL, "2", null, "479001");

        SampleItem first = specimens.get("1");
        SampleItem second = specimens.get("2");
        second.setSample(first.getSample());
        second.setCollectionDate(first.getCollectionDate());
        specimens.update(second);
        String finalized = statuses.getStatusID(AnalysisStatus.Finalized);
        for (String id : List.of("1", "2")) {
            Analysis analysis = analyses.get(id);
            analysis.setStatusId(finalized);
            analyses.update(analysis);
            reading(analysis, options.get(id), "85.0");
        }
        entityManager.flush();
        entityManager.clear();

        List<String> fieldIds = List.of("specimenId", "observation:" + type.getId());
        var catalog = source.catalog();
        var fields = fieldIds.stream()
                .map(id -> catalog.stream().filter(field -> field.id().equals(id)).findFirst().orElseThrow()).toList();
        ExportFilter filter = new ExportFilter("2023-11-15", "2023-11-15", List.of("1", "2"), List.of("1", "2"),
                List.of("FINALIZED"));
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("SPREADSHEET", "RESULT_LIST"), fieldIds,
                List.of("observations"), List.of("testIds"), Map.of("SPREADSHEET", fieldIds, "RESULT_LIST", fieldIds));
        for (String layout : definition.layouts()) {
            StringWriter csv = new StringWriter();
            assertEquals(2, source.write(csv, new ExportSnapshot(definition, layout, fields, filter,
                    java.time.ZoneId.systemDefault().getId(), List.of(finalized))));
            assertEquals(
                    "Specimen ID,Collection Context\r\n"
                            + "1,Patient answer; Order answer; Linked order answer; First specimen answer\r\n"
                            + "2,Patient answer; Order answer; Linked order answer; Second specimen answer\r\n",
                    csv.toString().substring(1));
        }
    }

    @Test
    public void specimenAnswersExportWhenTheOrderHasNoLinkedPatient() throws Exception {
        ObservationHistoryType type = new ObservationHistoryType();
        type.setTypeName("specimenContext");
        type.setDescription("Specimen Context");
        observationTypes.insert(type);
        observation(type.getId(), "Order answer", ObservationHistory.ValueType.LITERAL, "2", null, null);
        observation(type.getId(), "Specimen answer", ObservationHistory.ValueType.LITERAL, "2", "2", null);
        observation(type.getId(), "Other specimen answer", ObservationHistory.ValueType.LITERAL, "1", "1", null);
        observation(type.getId(), "Unrelated patient answer", ObservationHistory.ValueType.LITERAL, "1", null,
                "479001");
        Analysis analysis = analyses.get("2");
        analysis.setStatusId(statuses.getStatusID(AnalysisStatus.Finalized));
        analyses.update(analysis);
        reading(analysis, options.get("2"), "85.0");
        entityManager.flush();
        entityManager.clear();

        String fieldId = "observation:" + type.getId();
        var field = source.catalog().stream().filter(v -> v.id().equals(fieldId)).findFirst().orElseThrow();
        ExportFilter filter = new ExportFilter("2023-11-16", "2023-11-16", List.of("2"), List.of("2"),
                List.of("FINALIZED"));
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("RESULT_LIST"), List.of(fieldId), List.of("observations"),
                List.of("testIds"), Map.of("RESULT_LIST", List.of(fieldId)));
        StringWriter csv = new StringWriter();
        assertEquals(1, source.write(csv, new ExportSnapshot(definition, "RESULT_LIST", List.of(field), filter,
                java.time.ZoneId.systemDefault().getId(), List.of(analysis.getStatusId()))));
        assertEquals("Specimen Context\r\nOrder answer; Specimen answer\r\n", csv.toString().substring(1));
    }

    @Test
    public void resultValuesUseDictionaryQualifiersGroupedMultiselectAndVerbatimText() throws Exception {
        List<Dictionary> dictionary = entityManager
                .createQuery("from Dictionary d where d.dictEntry is not null order by d.id", Dictionary.class)
                .setMaxResults(2).getResultList();
        assertEquals(2, dictionary.size());
        Analysis analysis = analyses.get("1");
        analysis.setStatusId(statuses.getStatusID(AnalysisStatus.Finalized));
        analyses.update(analysis);
        SampleItem specimen = analysis.getSampleItem();
        specimen.setCollectionDate(Timestamp.valueOf("2026-08-20 10:00:00"));
        specimens.update(specimen);
        TestResult option = options.get("1");
        Result coded = reading(analysis, option, dictionary.get(0).getId(), "D", 0);
        Result qualifier = reading(analysis, null, "Confirmed", "A", 0);
        qualifier.setParentResult(coded);
        results.update(qualifier);
        reading(analysis, option, dictionary.get(0).getId(), "M", 7);
        reading(analysis, option, dictionary.get(1).getId(), "M", 7);
        // Exercise the full persisted VALUE width without exceeding its 200-character
        // limit.
        String freeText = ("Operator says \"repeat, please\"\r\n<tag> & " + "untruncated text ".repeat(20)).substring(0,
                200);
        reading(analysis, option, freeText, "A", 0);
        reading(analysis, option, freeText, "A", 0);
        entityManager.flush();

        ExportFilter filter = new ExportFilter("2026-08-20", "2026-08-20", List.of(analysis.getTestSection().getId()),
                List.of("1"), List.of("FINALIZED"));
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("SPREADSHEET", "RESULT_LIST"),
                List.of("accessionNumber", "resultValue"), List.of("tests"), List.of("testIds"),
                Map.of("SPREADSHEET", List.of("accessionNumber"), "RESULT_LIST", List.of("resultValue")));
        List<String> expected = List.of(freeText, freeText, dictionary.get(0).getDictEntry() + " (Confirmed)",
                dictionary.get(0).getDictEntry() + "; " + dictionary.get(1).getDictEntry());
        for (String layout : definition.layouts()) {
            String fieldId = layout.equals("SPREADSHEET") ? "test:1" : "resultValue";
            var field = source.catalog().stream().filter(v -> v.id().equals(fieldId)).findFirst().orElseThrow();
            StringWriter csv = new StringWriter();
            assertEquals(layout, 4, source.write(csv, new ExportSnapshot(definition, layout, List.of(field), filter,
                    java.time.ZoneId.systemDefault().getId(), List.of(analysis.getStatusId()))));
            assertEquals('\uFEFF', csv.toString().charAt(0));
            // Parse with an independent CSV reader: embedded line breaks are not rows.
            try (var parsed = CSVFormat.RFC4180.parse(new StringReader(csv.toString().substring(1)))) {
                var records = parsed.getRecords();
                assertEquals(layout, 5, records.size());
                assertEquals(layout.equals("SPREADSHEET") ? "Test Localization 1" : "Result Value",
                        records.get(0).get(0));
                assertTrue(records.stream().allMatch(record -> record.size() == 1));
                assertEquals(layout, expected.stream().sorted().toList(),
                        records.subList(1, records.size()).stream().map(record -> record.get(0)).sorted().toList());
            }
        }
    }
}
