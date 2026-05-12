package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageLocationService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BiorepositoryDashboardSampleAgingUnitTest {

    @Mock
    private BioSampleService bioSampleService;
    @Mock
    private BiorepositoryQCInspectionService qcInspectionService;
    @Mock
    private SampleRetrievalService retrievalService;
    @Mock
    private org.openelisglobal.notebook.service.NotebookEntryTemperatureLogService temperatureLogService;
    @Mock
    private org.openelisglobal.notebook.service.NotebookEntryRoomEnvironmentLogService roomEnvironmentLogService;
    @Mock
    private SampleStorageService storageService;
    @Mock
    private StorageLocationService storageLocationService;

    @InjectMocks
    private BiorepositoryDashboardServiceImpl dashboardService;

    @Test
    public void sampleAgingMetrics_ComputesAverageAgeYearsFromCollectionAndReceivedDates() {
        BioSample fromCollectionDate = buildStoredSample(
                Timestamp.valueOf(LocalDate.now().minusDays(365).atStartOfDay()),
                Timestamp.valueOf(LocalDate.now().minusDays(20).atStartOfDay()));
        BioSample fromReceivedDate = buildStoredSample(
                null,
                Timestamp.valueOf(LocalDate.now().minusDays(730).atStartOfDay()));

        when(bioSampleService.getAll()).thenReturn(List.of(fromCollectionDate, fromReceivedDate));

        Map<String, Object> metrics = dashboardService.getSampleAgingMetrics();
        double averageAgeYears = ((Number) metrics.get("averageAgeYears")).doubleValue();
        assertEquals(1.5, averageAgeYears, 0.05);
    }

    private BioSample buildStoredSample(Timestamp collectionDate, Timestamp receivedTimestamp) {
        Sample sample = new Sample();
        sample.setReceivedTimestamp(receivedTimestamp);
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId(String.valueOf(System.nanoTime()));
        sampleItem.setCollectionDate(collectionDate);
        sampleItem.setSample(sample);

        BioSample bioSample = new BioSample();
        bioSample.setSampleItem(sampleItem);
        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.STORED);
        return bioSample;
    }
}
