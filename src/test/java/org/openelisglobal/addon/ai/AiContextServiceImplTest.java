package org.openelisglobal.addon.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.addon.security.AddonToken;
import org.openelisglobal.addon.service.AddonTokenService;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The ai.context of a token: structure, de-identification, history, views. The
 * services are mocked; the de-identification is real.
 */
public class AiContextServiceImplTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final AiContextServiceImpl service = new AiContextServiceImpl();
    private final SampleService samples = mock(SampleService.class);
    private final SampleHumanService sampleHuman = mock(SampleHumanService.class);
    private final AnalysisService analyses = mock(AnalysisService.class);
    private final ResultService results = mock(ResultService.class);
    private final PatientIdentityService identities = mock(PatientIdentityService.class);
    private final IStatusService status = mock(IStatusService.class);
    private final AddonTokenService tokens = mock(AddonTokenService.class);

    private final Map<String, List<Analysis>> analysesOf = new HashMap<>();
    private final Map<String, List<Sample>> ordersOf = new HashMap<>();

    private Patient lan;
    private Patient binh;

    @Before
    public void wire() {
        ReflectionTestUtils.setField(service, "sampleService", samples);
        ReflectionTestUtils.setField(service, "sampleHumanService", sampleHuman);
        ReflectionTestUtils.setField(service, "analysisService", analyses);
        ReflectionTestUtils.setField(service, "resultService", results);
        ReflectionTestUtils.setField(service, "patientIdentityService", identities);
        ReflectionTestUtils.setField(service, "statusService", status);
        ReflectionTestUtils.setField(service, "tokenService", tokens);
        service.setClock(Clock.fixed(Instant.parse("2026-10-14T03:00:00Z"), ZoneOffset.UTC));
        when(analyses.getAnalysesBySampleId(anyString()))
                .thenAnswer(i -> analysesOf.getOrDefault((String) i.getArgument(0), List.of()));
        when(samples.getSamplesForPatient(anyString()))
                .thenAnswer(i -> ordersOf.getOrDefault((String) i.getArgument(0), List.of()));
        when(status.getAnalysisStatusForID("6")).thenReturn(AnalysisStatus.Finalized);
        when(status.getAnalysisStatusForID("4")).thenReturn(AnalysisStatus.TechnicalAcceptance);
        when(status.getAnalysisStatusForID("9")).thenReturn(AnalysisStatus.Canceled);
        when(results.getResultValue(any(Result.class), eq(", "), eq(true), eq(false)))
                .thenAnswer(i -> ((Result) i.getArgument(0)).getValue());
        when(tokens.opaqueId("assistant", "133")).thenReturn("pseudoA");

        lan = patient("133", "F", "1957-01-01", "Nguyễn", "Thị", "Lan", "001157012345");
        binh = patient("200", "M", "1930-05-05", "Trần", "Văn", "Bình", "079123456789");
        PatientIdentity id = new PatientIdentity();
        id.setIdentityData("BN000123");
        when(identities.getPatientIdentitiesForPatient("133")).thenReturn(List.of(id));

        order(lan, "1", "DEV01260000000000063", "2026-10-14 07:00:00",
                analysis("11", "EGFR", "98979-8", "eGFR (CKD-EPI 2021)", "59", 90.0, null, "4"),
                analysis("12", "NOTE", null, "Note", "Ms Nguyễn Thị Lan, call 0939123456", null, null, "6"),
                analysis("13", "GLU", null, "Glucose", "6.4", 3.9, 6.1, "9"));
        order(lan, "2", "DEV01260000000000050", "2026-07-14 07:00:00",
                analysis("21", "EGFR", "98979-8", "eGFR (CKD-EPI 2021)", "61", 90.0, null, "6"));
        order(lan, "3", "DEV01260000000000040", "2026-04-14 07:00:00",
                analysis("31", "EGFR", "98979-8", "eGFR (CKD-EPI 2021)", "66", 90.0, null, "6"));
        order(binh, "4", "DEV01260000000000070", "2026-10-13 09:00:00",
                analysis("41", "EGFR", "98979-8", "eGFR (CKD-EPI 2021)", "44", 90.0, null, "4"));
    }

    private Patient patient(String id, String sex, String birth, String last, String middle, String first,
            String nationalId) {
        Person person = new Person();
        person.setLastName(last);
        person.setMiddleName(middle);
        person.setFirstName(first);
        person.setCellPhone("0939123456");
        Patient p = new Patient();
        p.setId(id);
        p.setGender(sex);
        ReflectionTestUtils.setField(p, "birthDate", Timestamp.valueOf(birth + " 00:00:00"));
        p.setNationalId(nationalId);
        p.setPerson(person);
        return p;
    }

    private void order(Patient patient, String id, String accession, String received, Analysis... list) {
        Sample s = new Sample();
        s.setId(id);
        s.setAccessionNumber(accession);
        ReflectionTestUtils.setField(s, "receivedTimestamp", Timestamp.valueOf(received)); // the setter needs Spring
        when(samples.getSampleByAccessionNumber(accession)).thenReturn(s);
        when(sampleHuman.getPatientForSample(s)).thenReturn(patient);
        analysesOf.put(id, List.of(list));
        ordersOf.computeIfAbsent(patient.getId(), k -> new ArrayList<>()).add(s);
    }

    private Analysis analysis(String id, String code, String loinc, String name, String value, Double low, Double high,
            String statusId) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("t-" + code);
        test.setLocalCode(code);
        test.setLoinc(loinc);
        test.setDescription(name);
        UnitOfMeasure unit = new UnitOfMeasure();
        unit.setUnitOfMeasureName("EGFR".equals(code) ? "mL/min/1.73m²" : "mmol/L");
        test.setUnitOfMeasure(unit);
        TestSection section = new TestSection();
        section.setTestSectionName("Biochemistry");
        Analysis a = new Analysis();
        a.setId(id);
        a.setTest(test);
        a.setTestSection(section);
        a.setStatusId(statusId);
        Result r = new Result();
        r.setValue(value);
        r.setMinNormal(low);
        r.setMaxNormal(high);
        when(results.getResultsByAnalysis(a)).thenReturn(List.of(r));
        return a;
    }

    private static AddonToken.Claims token(String... accessions) {
        return new AddonToken.Claims("assistant", "7", List.of("ai.context"), List.of(accessions), Long.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Object o) {
        return (List<Map<String, Object>>) o;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return (Map<String, Object>) o;
    }

    @Test
    public void thePatientViewHasTheNewestDayAndEarlierResultsWithoutIdentifiers() throws Exception {
        Map<String, Object> ctx = service
                .context(token("DEV01260000000000063", "DEV01260000000000050", "DEV01260000000000040"), "patient");
        assertEquals("7", ctx.get("user"));
        assertEquals("p_pseudoA", ctx.get("subject"));
        assertEquals("patient", ctx.get("view"));
        List<Map<String, Object>> cases = list(ctx.get("cases"));
        assertEquals(1, cases.size());
        Map<String, Object> c = cases.get(0);
        assertEquals(Map.of("sex", "F", "ageYears", 69), c.get("patient"));
        assertEquals(0L, c.get("receivedDaysAgo"));
        List<Map<String, Object>> rows = list(c.get("results"));
        assertEquals("canceled tests are left out", 2, rows.size());
        assertEquals("59", rows.get(0).get("value"));
        assertEquals("98979-8", rows.get(0).get("loinc"));
        assertEquals("TechnicalAcceptance", rows.get(0).get("status"));
        assertEquals(90.0, rows.get(0).get("low"));
        assertEquals("Ms [removed], call [removed]", rows.get(1).get("value"));
        List<Map<String, Object>> previous = list(c.get("previous"));
        assertEquals(2, previous.size());
        assertEquals("61", previous.get(0).get("value"));
        assertEquals(92L, previous.get(0).get("daysBefore"));
        assertEquals("66", previous.get(1).get("value"));
        assertEquals(183L, previous.get(1).get("daysBefore"));

        String json = JSON.writeValueAsString(ctx);
        for (String bad : List.of("Nguyễn", "Lan", "DEV0126", "0939", "001157012345", "BN000123", "1957", "2026",
                "133")) {
            assertFalse(bad + " leaked: " + json, json.contains(bad));
        }
    }

    @Test
    public void theCasesViewHasOneCasePerOrderAndOneEarlierResult() {
        Map<String, Object> ctx = service.context(token("DEV01260000000000063", "DEV01260000000000070"), "cases");
        assertEquals("mixed", ctx.get("subject"));
        List<Map<String, Object>> cases = list(ctx.get("cases"));
        assertEquals(2, cases.size());
        assertEquals(1, cases.get(0).get("label"));
        assertEquals(1, list(cases.get(0).get("previous")).size());
        assertEquals(2, cases.get(1).get("label"));
        assertEquals(Map.of("sex", "M", "ageYears", 90, "ageCapped", true), cases.get(1).get("patient"));
        assertEquals(1L, cases.get(1).get("receivedDaysAgo"));
        assertEquals("44", list(cases.get(1).get("results")).get(0).get("value"));
    }

    @Test
    public void aPatientViewOfSeveralPatientsFallsBackToCases() {
        Map<String, Object> ctx = service.context(token("DEV01260000000000063", "DEV01260000000000070"), "patient");
        assertEquals("cases", ctx.get("view"));
    }

    @Test
    public void unknownLabNumbersGiveAnEmptyContext() {
        Map<String, Object> ctx = service.context(token("NOPE1"), "cases");
        assertEquals("none", ctx.get("subject"));
        assertEquals(List.of(), ctx.get("cases"));
    }

    @Test
    public void aQuestionLosesTheIdentifiersOfThePatientsInContext() {
        String q = service.deidentify(token("DEV01260000000000063"),
                "Is NGUYEN THI LAN (BN000123, 001157012345) G3a with eGFR 59?");
        assertEquals("Is [removed] ([removed], [removed]) G3a with eGFR 59?", q);
        assertTrue(service.deidentify(token(), "x".repeat(5000)).length() <= AiContextServiceImpl.TEXT_MAX);
    }

    @Test
    public void ageUnderOneYearIsInMonthsAndUnknownBirthHasNoAge() {
        Patient baby = new Patient();
        baby.setGender("F");
        ReflectionTestUtils.setField(baby, "birthDate", Timestamp.valueOf("2026-05-01 00:00:00"));
        assertEquals(Map.of("sex", "F", "ageMonths", 5),
                AiContextServiceImpl.patientOf(baby, java.time.LocalDate.of(2026, 10, 14)));
        Patient unknown = new Patient();
        Map<String, Object> p = AiContextServiceImpl.patientOf(unknown, java.time.LocalDate.of(2026, 10, 14));
        assertNull(p.get("sex"));
        assertFalse(p.containsKey("ageYears"));
    }
}
