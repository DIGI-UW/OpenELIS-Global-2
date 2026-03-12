package org.openelisglobal.sample.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.barcode.service.BarcodeInfoService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryServiceImplTest {

    @Mock
    private BarcodeInfoService barcodeInfoService;

    private SamplePatientEntryServiceImpl service;

    @Before
    public void setUp() {
        service = new SamplePatientEntryServiceImpl();
        ReflectionTestUtils.setField(service, "barcodeInfoService", barcodeInfoService);
    }

    @Test
    public void persistOrderSpecimenBarcodeCounts_persistsProvidedValues() {
        Sample sample = new Sample();
        sample.setId("sample-1");

        service.persistOrderSpecimenBarcodeCounts(sample, 4, 3);

        verify(barcodeInfoService).saveBarcodeInfoForSampleAndSampleItems(sample, 4, 3);
    }

    @Test
    public void persistOrderSpecimenBarcodeCounts_defaultsInvalidValuesToOne() {
        Sample sample = new Sample();
        sample.setId("sample-1");

        service.persistOrderSpecimenBarcodeCounts(sample, 0, -2);

        verify(barcodeInfoService).saveBarcodeInfoForSampleAndSampleItems(sample, 1, 1);
    }

    @Test
    public void persistOrderSpecimenBarcodeCounts_skipsNullSample() {
        service.persistOrderSpecimenBarcodeCounts(null, 2, 2);

        verify(barcodeInfoService, never()).saveBarcodeInfoForSampleAndSampleItems(null, 2, 2);
    }
}
