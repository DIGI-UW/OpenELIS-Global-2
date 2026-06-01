package org.openelisglobal.biorepository.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.biorepository.dao.BioSampleDAO;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;

@RunWith(MockitoJUnitRunner.class)
public class BioSampleServiceEnsureTest {

    @InjectMocks
    private BioSampleServiceImpl bioSampleService;

    @Mock
    private BioSampleDAO baseObjectDAO;

    @Mock
    private SampleStorageService sampleStorageService;

    @Test
    public void ensureBioSampleForStoredSampleItem_returnsExisting() {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("42");
        BioSample existing = new BioSample();
        existing.setId(9);
        when(baseObjectDAO.getBySampleItemId(42)).thenReturn(existing);

        BioSample result = bioSampleService.ensureBioSampleForStoredSampleItem(sampleItem, "1");

        assertEquals(Integer.valueOf(9), result.getId());
        verify(sampleStorageService, never()).getSampleItemLocation(any());
    }

    @Test
    public void ensureBioSampleForStoredSampleItem_createsWhenStored() {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("42");
        when(baseObjectDAO.getBySampleItemId(42)).thenReturn(null);
        when(sampleStorageService.getSampleItemLocation("42"))
                .thenReturn(Map.of("location", "Room A > Freezer", "hierarchicalPath", "Room A > Freezer"));
        when(baseObjectDAO.existsBySampleItemId(42)).thenReturn(false);
        when(baseObjectDAO.insert(any(BioSample.class))).thenAnswer(invocation -> {
            BioSample saved = invocation.getArgument(0);
            saved.setId(99);
            when(baseObjectDAO.get(99)).thenReturn(Optional.of(saved));
            return 99;
        });

        BioSample result = bioSampleService.ensureBioSampleForStoredSampleItem(sampleItem, "1");

        assertNotNull(result);
        assertEquals(Integer.valueOf(99), result.getId());
        assertEquals(WorkflowStatus.STORED, result.getWorkflowStatus());
        verify(baseObjectDAO).insert(any(BioSample.class));
    }

    @Test
    public void ensureBioSampleForStoredSampleItem_returnsNullWhenNotStored() {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("42");
        when(baseObjectDAO.getBySampleItemId(42)).thenReturn(null);
        when(sampleStorageService.getSampleItemLocation("42")).thenReturn(Map.of());

        assertNull(bioSampleService.ensureBioSampleForStoredSampleItem(sampleItem, "1"));
    }
}
