package org.openelisglobal.testcalculated.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.notifications.service.HeaderNotificationService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.openelisglobal.testterminology.valueholder.TestTerminologyMapping;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * How the eGFR on an order follows the creatinine saved on it and the patient's
 * sex and birth date, that an eGFR entered by hand is never overwritten, and
 * who hears about a correction.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class EgfrCalculationServiceImplTest {

    private static final String USER = "1";
    private static final String NOT_STARTED = "10";
    private static final String TECHNICAL_ACCEPTANCE = "11";
    private static final String FINALIZED = "12";

    @Mock
    private AnalysisService analysisService;
    @Mock
    private ResultService resultService;
    @Mock
    private TestResultService testResultService;
    @Mock
    private ResultLimitService resultLimitService;
    @Mock
    private SampleHumanService sampleHumanService;
    @Mock
    private NoteService noteService;
    @Mock
    private IStatusService statusService;
    @Mock
    private TestTerminologyMappingService terminologyMappingService;
    @Mock
    private HeaderNotificationService headerNotificationService;
    @Mock
    private UserRoleService userRoleService;
    @Mock
    private UserService userService;

    private EgfrCalculationServiceImpl service;

    private Sample sample;
    private SampleItem sampleItem;
    private Patient patient;
    private Analysis creatinine;
    private Analysis egfr;

    @Before
    public void setUp() {
        service = new EgfrCalculationServiceImpl(analysisService, resultService, testResultService, resultLimitService,
                sampleHumanService, noteService, statusService, terminologyMappingService, headerNotificationService,
                userRoleService, userService);

        ReloadableResourceBundleMessageSource messages = new ReloadableResourceBundleMessageSource();
        messages.setBasename("classpath:/languages/message");
        messages.setDefaultEncoding("UTF-8");
        messages.setUseCodeAsDefaultMessage(true);
        MessageUtil.setMessageSource(messages);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        // validator "7" works in the eGFR's lab unit, validator "8" does not
        when(userRoleService.getUserIdsForRole(Constants.ROLE_VALIDATION)).thenReturn(List.of("7", "8"));
        when(userService.filterAnalysesByLabUnitRoles(eq("7"), any(), eq(Constants.ROLE_VALIDATION)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(userService.filterAnalysesByLabUnitRoles(eq("8"), any(), eq(Constants.ROLE_VALIDATION)))
                .thenReturn(Collections.emptyList());

        when(statusService.getStatusID(AnalysisStatus.NotStarted)).thenReturn(NOT_STARTED);
        when(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance)).thenReturn(TECHNICAL_ACCEPTANCE);
        when(statusService.matches(anyString(), any(AnalysisStatus.class))).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            AnalysisStatus status = invocation.getArgument(1);
            return (status == AnalysisStatus.NotStarted && NOT_STARTED.equals(id))
                    || (status == AnalysisStatus.TechnicalAcceptance && TECHNICAL_ACCEPTANCE.equals(id))
                    || (status == AnalysisStatus.Finalized && FINALIZED.equals(id));
        });
        when(terminologyMappingService.getActiveByTestId(anyString())).thenReturn(Collections.emptyList());
        when(testResultService.getActiveTestResultsByTest(anyString())).thenReturn(Collections.emptyList());
        when(noteService.createSavableNote(any(), any(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    Note note = new Note();
                    note.setText(invocation.getArgument(2));
                    return note;
                });

        // mocks rather than setters: the date setters of these value holders format
        // the date for display, which needs the Spring context
        sample = mock(Sample.class);
        when(sample.getId()).thenReturn("100");
        when(sample.getCollectionDate()).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 10, 15, 8, 0)));
        sampleItem = new SampleItem();
        sampleItem.setId("200");
        sampleItem.setSample(sample);

        patient = mock(Patient.class);
        when(patient.getGender()).thenReturn("F");
        when(patient.getBirthDate()).thenReturn(Timestamp.valueOf(LocalDateTime.of(1999, 3, 1, 0, 0)));
        when(patient.getId()).thenReturn("900");
        when(sampleHumanService.getPatientForSample(sample)).thenReturn(patient);
        when(sampleHumanService.getSamplesForPatient("900")).thenReturn(List.of(sample));
        when(sample.getAccessionNumber()).thenReturn("DEV0126000001");

        creatinine = analysis("300", test("1", "2160-0", "mg/dL"), TECHNICAL_ACCEPTANCE);
        egfr = spy(analysis("301", test("2", "98979-8", "mL/min/1.73 m2"), NOT_STARTED));
        doNothing().when(egfr).setStartedDate(any(Timestamp.class));
        when(analysisService.getAnalysesBySampleId("100")).thenReturn(Arrays.asList(creatinine, egfr));
        when(resultService.getResultsByAnalysis(egfr)).thenReturn(new ArrayList<>());
    }

    @After
    public void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    public void fillsTheOrderedEgfrWhenCreatinineIsSaved() {
        service.updateEgfrForSavedResults(List.of(result(creatinine, "0.62")), USER);

        ArgumentCaptor<Result> written = ArgumentCaptor.forClass(Result.class);
        verify(resultService).insert(written.capture());
        assertEquals("125", written.getValue().getValue());
        assertEquals(egfr, written.getValue().getAnalysis());
        assertTrue(egfr.getResultCalculated());
        assertEquals(TECHNICAL_ACCEPTANCE, egfr.getStatusId());
        verify(analysisService).update(egfr);
        verify(noteService).createSavableNote(eq(egfr), eq(NoteType.INTERNAL), any(), any(), eq(USER));
        verify(headerNotificationService, never()).notifyUser(anyString(), anyString());
    }

    @Test
    public void recalculatesItsOwnEgfrWhenCreatinineIsCorrected() {
        Result stored = result(egfr, "125");
        stored.setId("500");
        when(resultService.getResultsByAnalysis(egfr)).thenReturn(List.of(stored));
        egfr.setResultCalculated(true);
        egfr.setStatusId(TECHNICAL_ACCEPTANCE);

        service.updateEgfrForSavedResults(List.of(result(creatinine, "1.20")), USER);

        verify(resultService).update(stored);
        assertEquals("64", stored.getValue());
        verify(headerNotificationService).notifyUser("7", "eGFR on order DEV0126000001 recalculated after the"
                + " creatinine was corrected: 125 \u2192 64 mL/min/1.73 m2. It awaits validation.");
        verify(headerNotificationService, never()).notifyUser(eq("8"), anyString());
    }

    @Test
    public void neverOverwritesAnEgfrEnteredByHand() {
        Result stored = result(egfr, "118");
        when(resultService.getResultsByAnalysis(egfr)).thenReturn(List.of(stored));
        egfr.setResultCalculated(false);

        service.updateEgfrForSavedResults(List.of(result(creatinine, "0.62")), USER);

        assertEquals("118", stored.getValue());
        verify(resultService, never()).update(any(Result.class));
        verify(resultService, never()).insert(any(Result.class));
        verify(analysisService, never()).update(any(Analysis.class));
    }

    @Test
    public void anEgfrEnteredInTheSameSaveIsKeptAndNotOverwrittenLater() {
        // the eGFR was calculated earlier; now a user types over it while also
        // correcting the creatinine
        egfr.setResultCalculated(true);
        Result typed = result(egfr, "90");

        service.updateEgfrForSavedResults(List.of(result(creatinine, "1.20"), typed), USER);

        assertEquals("90", typed.getValue());
        assertFalse("the eGFR is from now on a hand-entered value", egfr.getResultCalculated());
        verify(resultService, never()).update(any(Result.class));
        verify(resultService, never()).insert(any(Result.class));

        // a later creatinine correction leaves it alone too
        when(resultService.getResultsByAnalysis(egfr)).thenReturn(List.of(typed));
        service.updateEgfrForSavedResults(List.of(result(creatinine, "0.62")), USER);
        assertEquals("90", typed.getValue());
        verify(resultService, never()).update(any(Result.class));
    }

    @Test
    public void notesWhyWhenThePatientIsUnder18() {
        when(patient.getBirthDate()).thenReturn(Timestamp.valueOf(LocalDateTime.of(2010, 1, 1, 0, 0)));

        service.updateEgfrForSavedResults(List.of(result(creatinine, "0.62")), USER);

        verify(resultService, never()).insert(any(Result.class));
        verify(noteService).createSavableNote(eq(egfr), eq(NoteType.INTERNAL),
                eq("eGFR not calculated (CKD-EPI 2021): the patient was under 18 years old at collection."), any(),
                eq(USER));
    }

    @Test
    public void doesNothingWithoutAnOrderedEgfr() {
        when(analysisService.getAnalysesBySampleId("100")).thenReturn(List.of(creatinine));

        service.updateEgfrForSavedResults(List.of(result(creatinine, "0.62")), USER);

        verify(resultService, never()).insert(any(Result.class));
        verify(noteService, never()).insert(any(Note.class));
    }

    @Test
    public void recognisesTestsByLoincMapping() {
        org.openelisglobal.test.valueholder.Test unmapped = test("3", null, "µmol/L");
        Analysis mappedCreatinine = analysis("302", unmapped, TECHNICAL_ACCEPTANCE);
        TestTerminologyMapping mapping = new TestTerminologyMapping();
        mapping.setSource("LOINC");
        mapping.setCode("14682-9");
        when(terminologyMappingService.getActiveByTestId("3")).thenReturn(List.of(mapping));
        when(analysisService.getAnalysesBySampleId("100")).thenReturn(Arrays.asList(mappedCreatinine, egfr));

        service.updateEgfrForSavedResults(List.of(result(mappedCreatinine, "54.8")), USER);

        ArgumentCaptor<Result> written = ArgumentCaptor.forClass(Result.class);
        verify(resultService).insert(written.capture());
        assertEquals("125", written.getValue().getValue());
    }

    /** The patient was saved as female with this eGFR; now it reads male. */
    private Result calculatedEgfr(String value, String statusId) {
        Result stored = result(egfr, value);
        stored.setId("500");
        when(resultService.getResultsByAnalysis(egfr)).thenReturn(List.of(stored));
        when(resultService.getResultsByAnalysis(creatinine)).thenReturn(List.of(result(creatinine, "0.62")));
        egfr.setResultCalculated(true);
        egfr.setStatusId(statusId);
        when(patient.getGender()).thenReturn("M");
        return stored;
    }

    private void patientWasFemale() {
        service.updateEgfrForPatientChange(patient, "F", patient.getBirthDate(), USER);
    }

    @Test
    public void recalculatesItsOwnEgfrWhenThePatientSexChanges() {
        Result stored = calculatedEgfr("125", TECHNICAL_ACCEPTANCE);

        patientWasFemale();

        verify(resultService).update(stored);
        assertEquals("134", stored.getValue());
        assertEquals(TECHNICAL_ACCEPTANCE, egfr.getStatusId());
        ArgumentCaptor<String> note = ArgumentCaptor.forClass(String.class);
        verify(noteService).createSavableNote(eq(egfr), eq(NoteType.INTERNAL), note.capture(), any(), eq(USER));
        assertTrue(note.getValue(), note.getValue()
                .startsWith("Recalculated after patient sex/birth date changed: 125 \u2192 134 mL/min/1.73 m2."));
        verify(headerNotificationService).notifyUser("7", "eGFR on order DEV0126000001 recalculated after the"
                + " patient's sex or birth date changed: 125 \u2192 134 mL/min/1.73 m2. It awaits validation.");
        verify(headerNotificationService, never()).notifyUser(eq("8"), anyString());
    }

    @Test
    public void recalculatesWhenTheBirthDateChanges() {
        Result stored = calculatedEgfr("125", TECHNICAL_ACCEPTANCE);
        when(patient.getGender()).thenReturn("F");
        when(patient.getBirthDate()).thenReturn(Timestamp.valueOf(LocalDateTime.of(1959, 3, 1, 0, 0)));

        service.updateEgfrForPatientChange(patient, "F", Timestamp.valueOf(LocalDateTime.of(1999, 3, 1, 0, 0)), USER);

        verify(resultService).update(stored);
        assertFalse(stored.getValue(), "125".equals(stored.getValue()));
    }

    @Test
    public void doesNothingWhenNeitherSexNorBirthDateChanged() {
        calculatedEgfr("125", TECHNICAL_ACCEPTANCE);

        service.updateEgfrForPatientChange(patient, " M ", patient.getBirthDate(), USER);

        verify(sampleHumanService, never()).getSamplesForPatient(anyString());
        verify(resultService, never()).update(any(Result.class));
        verify(headerNotificationService, never()).notifyUser(anyString(), anyString());
    }

    @Test
    public void aPatientChangeNeverTouchesAnEgfrEnteredByHand() {
        Result stored = calculatedEgfr("118", TECHNICAL_ACCEPTANCE);
        egfr.setResultCalculated(false);

        patientWasFemale();

        assertEquals("118", stored.getValue());
        verify(resultService, never()).update(any(Result.class));
        verify(noteService, never()).createSavableNote(any(), any(), anyString(), anyString(), anyString());
        verify(headerNotificationService, never()).notifyUser(anyString(), anyString());
    }

    @Test
    public void aPatientChangeLeavesAValidatedEgfrAndTellsTheValidators() {
        Result stored = calculatedEgfr("125", FINALIZED);

        patientWasFemale();

        assertEquals("125", stored.getValue());
        assertEquals(FINALIZED, egfr.getStatusId());
        verify(resultService, never()).update(any(Result.class));
        verify(analysisService, never()).update(any(Analysis.class));
        verify(noteService)
                .createSavableNote(eq(egfr), eq(NoteType.INTERNAL),
                        eq("eGFR is already validated and was not recalculated after patient sex/birth date changed."
                                + " CKD-EPI 2021 (race-free) now gives 134 mL/min/1.73 m2 instead of 125."),
                        any(), eq(USER));
        verify(headerNotificationService).notifyUser("7",
                "eGFR 125 on order DEV0126000001 is validated but no"
                        + " longer matches the calculation after the patient's sex or birth date changed. Review it in"
                        + " result entry.");
    }

    @Test
    public void fillsAnEmptyEgfrOnceTheSexIsKnownWithoutNotifying() {
        when(resultService.getResultsByAnalysis(creatinine)).thenReturn(List.of(result(creatinine, "0.62")));

        service.updateEgfrForPatientChange(patient, null, patient.getBirthDate(), USER);

        ArgumentCaptor<Result> written = ArgumentCaptor.forClass(Result.class);
        verify(resultService).insert(written.capture());
        assertEquals("125", written.getValue().getValue());
        verify(headerNotificationService, never()).notifyUser(anyString(), anyString());
    }

    private Analysis analysis(String id, org.openelisglobal.test.valueholder.Test test, String statusId) {
        Analysis analysis = new Analysis();
        analysis.setId(id);
        analysis.setTest(test);
        analysis.setSampleItem(sampleItem);
        analysis.setStatusId(statusId);
        return analysis;
    }

    private static org.openelisglobal.test.valueholder.Test test(String id, String loinc, String unit) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(id);
        test.setLoinc(loinc);
        UnitOfMeasure unitOfMeasure = new UnitOfMeasure();
        unitOfMeasure.setUnitOfMeasureName(unit);
        test.setUnitOfMeasure(unitOfMeasure);
        return test;
    }

    private static Result result(Analysis analysis, String value) {
        Result result = new Result();
        result.setAnalysis(analysis);
        result.setValue(value);
        return result;
    }
}
