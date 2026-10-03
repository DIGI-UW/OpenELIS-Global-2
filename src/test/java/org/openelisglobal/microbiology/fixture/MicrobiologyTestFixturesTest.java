package org.openelisglobal.microbiology.fixture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.service.MicrobiologyConfigurationService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.statusofsample.valueholder.StatusOfSample;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testmethod.service.TestMethodService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

@RunWith(MockitoJUnitRunner.class)
public class MicrobiologyTestFixturesTest {

    @Mock
    private MethodService methodService;
    @Mock
    private SampleService sampleService;
    @Mock
    private SampleItemService sampleItemService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private TestService testService;
    @Mock
    private TypeOfSampleService typeOfSampleService;
    @Mock
    private LocalizationService localizationService;
    @Mock
    private TestMethodService testMethodService;
    @Mock
    private IStatusService statusService;
    @Mock
    private StatusOfSampleService statusOfSampleService;
    @Mock
    private SystemUserService systemUserService;
    @Mock
    private MicrobiologyConfigurationService configurationService;
    @Mock
    private PersonService personService;
    @Mock
    private PatientService patientService;

    private MicrobiologyTestFixtures fixtures;

    @Before
    public void setUp() {
        when(systemUserService.getAllSystemUsers()).thenReturn(List.of(systemUser("7")));
        fixtures = new MicrobiologyTestFixtures(methodService, sampleService, sampleItemService, analysisService,
                testService, typeOfSampleService, localizationService, testMethodService, statusService,
                statusOfSampleService, systemUserService, configurationService, personService, patientService);
    }

    @Test
    public void reusesExistingSampleEnteredStatus() {
        StatusOfSample entered = new StatusOfSample();
        when(statusService.getStatusID(SampleStatus.Entered)).thenReturn("20");
        when(statusOfSampleService.getMatch("id", "20")).thenReturn(Optional.of(entered));

        assertEquals("20", fixtures.ensureSampleEnteredStatus());

        verify(statusOfSampleService, never()).insert(any(StatusOfSample.class));
        verify(statusService, never()).refreshCache();
    }

    @Test
    public void provisionsMissingSampleEnteredStatusThroughServices() {
        StatusOfSample disposed = new StatusOfSample();
        disposed.setStatusType("SAMPLE");
        disposed.setCode("900");
        when(statusOfSampleService.getAllStatusOfSamples()).thenReturn(List.of(disposed));
        when(statusService.getStatusID(SampleStatus.Entered)).thenReturn("-1", "42");

        assertEquals("42", fixtures.ensureSampleEnteredStatus());

        ArgumentCaptor<StatusOfSample> statusCaptor = ArgumentCaptor.forClass(StatusOfSample.class);
        verify(statusOfSampleService).insert(statusCaptor.capture());
        assertEquals("SampleEntered", statusCaptor.getValue().getStatusOfSampleName());
        assertEquals("SAMPLE", statusCaptor.getValue().getStatusType());
        assertEquals("901", statusCaptor.getValue().getCode());
        assertEquals("7", statusCaptor.getValue().getSysUserId());
        verify(statusService).refreshCache();
    }

    @Test
    public void returnsGeneratedStatusIdWhenCacheDoesNotRefresh() {
        when(statusOfSampleService.getAllStatusOfSamples()).thenReturn(List.of());
        when(statusOfSampleService.insert(any(StatusOfSample.class))).thenReturn("generated-42");
        when(statusService.getStatusID(SampleStatus.Entered)).thenReturn("-1");

        assertEquals("generated-42", fixtures.ensureSampleEnteredStatus());
    }

    @Test
    public void replacesStaleCachedSampleEnteredStatus() {
        when(statusService.getStatusID(SampleStatus.Entered)).thenReturn("30", "42");
        when(statusOfSampleService.getMatch("id", "30")).thenReturn(Optional.empty());
        when(statusOfSampleService.getAllStatusOfSamples()).thenReturn(List.of());

        assertEquals("42", fixtures.ensureSampleEnteredStatus());

        verify(statusOfSampleService).insert(any(StatusOfSample.class));
        verify(statusService).refreshCache();
    }

    @Test
    public void reusesExistingSampleEnteredRecordWhenCacheMisses() {
        StatusOfSample entered = new StatusOfSample();
        entered.setId("existing-42");
        entered.setStatusOfSampleName("SampleEntered");
        entered.setStatusType("SAMPLE");
        when(statusService.getStatusID(SampleStatus.Entered)).thenReturn("-1");
        when(statusOfSampleService.getAllStatusOfSamples()).thenReturn(List.of(entered));

        assertEquals("existing-42", fixtures.ensureSampleEnteredStatus());

        verify(statusOfSampleService, never()).insert(any(StatusOfSample.class));
        verify(statusService).refreshCache();
    }

    @Test
    public void ensuresEveryRequiredWorkflowStatus() {
        when(statusService.getStatusID(SampleStatus.Entered)).thenReturn("20");
        when(statusService.getStatusID(AnalysisStatus.NotStarted)).thenReturn("21");
        when(statusService.getStatusID(AnalysisStatus.Finalized)).thenReturn("22");
        when(statusOfSampleService.getMatch("id", "20")).thenReturn(Optional.of(new StatusOfSample()));
        when(statusOfSampleService.getMatch("id", "21")).thenReturn(Optional.of(new StatusOfSample()));
        when(statusOfSampleService.getMatch("id", "22")).thenReturn(Optional.of(new StatusOfSample()));

        fixtures.ensureRequiredWorkflowStatuses();

        verify(statusService).getStatusID(SampleStatus.Entered);
        verify(statusService).getStatusID(AnalysisStatus.NotStarted);
        verify(statusService).getStatusID(AnalysisStatus.Finalized);
        verify(statusOfSampleService, never()).insert(any(StatusOfSample.class));
    }

    @Test
    public void provisionsMissingAnalysisNotStartedStatusThroughServices() {
        when(statusService.getStatusID(AnalysisStatus.NotStarted)).thenReturn("-1", "43");
        when(statusOfSampleService.getAllStatusOfSamples()).thenReturn(List.of());

        assertEquals("43", fixtures.ensureAnalysisNotStartedStatus());

        ArgumentCaptor<StatusOfSample> statusCaptor = ArgumentCaptor.forClass(StatusOfSample.class);
        verify(statusOfSampleService).insert(statusCaptor.capture());
        assertEquals("Not Tested", statusCaptor.getValue().getStatusOfSampleName());
        assertEquals("ANALYSIS", statusCaptor.getValue().getStatusType());
        assertEquals("900", statusCaptor.getValue().getCode());
        verify(statusService).refreshCache();
    }

    @Test
    public void provisionsMissingAnalysisFinalizedStatusThroughServices() {
        when(statusService.getStatusID(AnalysisStatus.Finalized)).thenReturn("-1", "44");
        when(statusOfSampleService.getAllStatusOfSamples()).thenReturn(List.of());

        assertEquals("44", fixtures.ensureAnalysisFinalizedStatus());

        ArgumentCaptor<StatusOfSample> statusCaptor = ArgumentCaptor.forClass(StatusOfSample.class);
        verify(statusOfSampleService).insert(statusCaptor.capture());
        assertEquals("Finalized", statusCaptor.getValue().getStatusOfSampleName());
        assertEquals("ANALYSIS", statusCaptor.getValue().getStatusType());
        assertEquals("900", statusCaptor.getValue().getCode());
        verify(statusService).refreshCache();
    }

    @Test
    public void provisionsIsolatedMethodThroughService() {
        when(methodService.insert(any(Method.class))).thenReturn("55");

        assertEquals("55", fixtures.createMethodId());

        ArgumentCaptor<Method> methodCaptor = ArgumentCaptor.forClass(Method.class);
        verify(methodService).insert(methodCaptor.capture());
        assertTrue(methodCaptor.getValue().getMethodName().startsWith("Micro "));
        assertTrue(methodCaptor.getValue().getCode().startsWith("MCR"));
        assertEquals("Y", methodCaptor.getValue().getIsActive());
        assertEquals("7", methodCaptor.getValue().getSysUserId());
        verify(methodService, never()).getAllActiveMethods();
    }

    @Test
    public void provisionsIsolatedSpecimenTypeThroughService() {
        when(localizationService.insert(any(Localization.class))).thenReturn("60");
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("61");

        TypeOfSample typeOfSample = fixtures.createTypeOfSample();

        ArgumentCaptor<TypeOfSample> typeCaptor = ArgumentCaptor.forClass(TypeOfSample.class);
        verify(typeOfSampleService).insert(typeCaptor.capture());
        ArgumentCaptor<Localization> localizationCaptor = ArgumentCaptor.forClass(Localization.class);
        verify(localizationService).insert(localizationCaptor.capture());
        assertEquals("61", typeOfSample.getId());
        assertEquals(localizationCaptor.getValue(), typeOfSample.getLocalization());
        assertTrue(localizationCaptor.getValue().getEnglish().startsWith("Micro specimen "));
        assertEquals("7", localizationCaptor.getValue().getSysUserId());
        assertTrue(typeCaptor.getValue().getDescription().startsWith("Micro specimen "));
        assertEquals("H", typeCaptor.getValue().getDomain());
        assertTrue(typeCaptor.getValue().getIsActive());
        assertEquals("7", typeCaptor.getValue().getSysUserId());
    }

    @Test
    public void provisionsActiveSampleTypeThroughServicesWhenNoneExists() {
        when(typeOfSampleService.getAllTypeOfSamples()).thenReturn(List.of());
        when(typeOfSampleService.insert(any(TypeOfSample.class))).thenReturn("generated-sample-type");

        TypeOfSample sampleType = fixtures.getOrCreateActiveSampleType();

        assertEquals("generated-sample-type", sampleType.getId());
        assertEquals("Microbiology integration specimen", sampleType.getDescription());
        assertTrue(sampleType.getIsActive());
        assertEquals("7", sampleType.getSysUserId());
        verify(localizationService).insert(any(Localization.class));
        verify(typeOfSampleService).insert(sampleType);
    }

    private SystemUser systemUser(String id) {
        SystemUser user = new SystemUser();
        user.setId(id);
        user.setIsActive("Y");
        return user;
    }
}
