package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.StringWriter;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
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
        assertEquals(-1, stored.getSignificantDigits());
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
        assertEquals("Blood Test — Systolic", pivot.label());
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
        assertEquals("Urine Test — Color", field.label());
        ExportFilter filter = new ExportFilter("2026-08-21", "2026-08-21", List.of(analysis.getTestSection().getId()),
                List.of("2"), List.of("FINALIZED"));
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("SPREADSHEET"), List.of("accessionNumber"),
                List.of("components"), List.of("testIds"), Map.of("SPREADSHEET", List.of("accessionNumber")));
        StringWriter csv = new StringWriter();

        assertEquals(1, source.write(csv, new ExportSnapshot(definition, "SPREADSHEET", List.of(field), filter,
                java.time.ZoneId.systemDefault().getId(), List.of(analysis.getStatusId()))));
        assertEquals("Urine Test — Color\r\n2.75\r\n", csv.toString().substring(1));
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
                        + "12345,1,2023-11-15,10:00,2023-11-15,11:00,2024-06-03,Blood Sample,SampleEntered,STAT,1,Ada Q Public,1980-02-03,F,WA-1001,555-0100,\"42 Lab Road, Seattle, WA, USA\"\r\n",
                csv.toString().substring(1));
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
        reading(analysis, option, "operator free text", "A", 0);
        entityManager.flush();

        var field = source.catalog().stream().filter(v -> v.id().equals("resultValue")).findFirst().orElseThrow();
        ExportFilter filter = new ExportFilter("2026-08-20", "2026-08-20", List.of(analysis.getTestSection().getId()),
                List.of("1"), List.of("FINALIZED"));
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("RESULT_LIST"), List.of("resultValue"), List.of(),
                List.of("testIds"), Map.of("RESULT_LIST", List.of("resultValue")));
        StringWriter csv = new StringWriter();

        assertEquals(3, source.write(csv, new ExportSnapshot(definition, "RESULT_LIST", List.of(field), filter,
                java.time.ZoneId.systemDefault().getId(), List.of(analysis.getStatusId()))));
        String exported = csv.toString();
        assertTrue(exported.contains("\r\n" + dictionary.get(0).getDictEntry() + " (Confirmed)\r\n"));
        assertTrue(exported.contains(
                "\r\n" + dictionary.get(0).getDictEntry() + "; " + dictionary.get(1).getDictEntry() + "\r\n"));
        assertTrue(exported.contains("\r\noperator free text\r\n"));
    }
}
