package org.openelisglobal.microbiology.fixture;

import static org.junit.Assert.assertEquals;
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
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.microbiology.service.MicrobiologyConfigurationService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.statusofsample.valueholder.StatusOfSample;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.test.service.TestService;

@RunWith(MockitoJUnitRunner.class)
public class MicrobiologyTestFixturesTest {

    @Mock
    private MethodService methodService;
    @Mock
    private SampleService sampleService;
    @Mock
    private SampleItemService sampleItemService;
    @Mock
    private TestService testService;
    @Mock
    private IStatusService statusService;
    @Mock
    private StatusOfSampleService statusOfSampleService;
    @Mock
    private SystemUserService systemUserService;
    @Mock
    private MicrobiologyConfigurationService configurationService;

    private MicrobiologyTestFixtures fixtures;

    @Before
    public void setUp() {
        fixtures = new MicrobiologyTestFixtures(methodService, sampleService, sampleItemService, testService,
                statusService, statusOfSampleService, systemUserService, configurationService);
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
        verify(statusService).refreshCache();
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
}
