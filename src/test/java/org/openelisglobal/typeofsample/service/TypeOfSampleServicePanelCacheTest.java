package org.openelisglobal.typeofsample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.typeofsample.dao.TypeOfSampleDAO;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class TypeOfSampleServicePanelCacheTest {

    @Mock
    private TypeOfSampleDAO typeOfSampleDAO;
    @Mock
    private TypeOfSamplePanelService typeOfSamplePanelService;
    @Mock
    private PanelService panelService;

    private TypeOfSampleServiceImpl service;

    @Before
    public void setUp() {
        service = new TypeOfSampleServiceImpl();
        ReflectionTestUtils.setField(service, "baseObjectDAO", typeOfSampleDAO);
        ReflectionTestUtils.setField(service, "typeOfSamplePanelService", typeOfSamplePanelService);
        ReflectionTestUtils.setField(service, "panelService", panelService);
    }

    @Test
    public void getTypeOfSampleForPanelId_initializesIdentityMapAndSkipsMissingSampleTypes() {
        TypeOfSample serum = buildTypeOfSample("10", "Serum");
        Panel panel = buildPanel("1", "Chemistry Panel");

        TypeOfSamplePanel validLink = new TypeOfSamplePanel();
        validLink.setPanelId("1");
        validLink.setTypeOfSampleId("10");

        TypeOfSamplePanel danglingLink = new TypeOfSamplePanel();
        danglingLink.setPanelId("1");
        danglingLink.setTypeOfSampleId("999");

        when(typeOfSampleDAO.getAllTypeOfSamples()).thenReturn(Collections.singletonList(serum));
        when(panelService.getAllActivePanels()).thenReturn(Collections.singletonList(panel));
        when(typeOfSamplePanelService.getTypeOfSamplePanelsForPanel("1"))
                .thenReturn(Arrays.asList(danglingLink, validLink));
        when(typeOfSampleDAO.getTypeOfSampleById("999")).thenReturn(null);
        when(typeOfSampleDAO.getTypeOfSampleById("10")).thenReturn(serum);

        List<TypeOfSample> result = service.getTypeOfSampleForPanelId("1");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(serum, result.get(0));
    }

    @Test
    public void clearCache_invalidatesPanelIdToTypeOfSampleMap() {
        TypeOfSample serum = buildTypeOfSample("10", "Serum");
        TypeOfSample plasma = buildTypeOfSample("20", "Plasma");
        Panel panel = buildPanel("1", "Chemistry Panel");

        TypeOfSamplePanel firstLink = new TypeOfSamplePanel();
        firstLink.setPanelId("1");
        firstLink.setTypeOfSampleId("10");

        TypeOfSamplePanel secondLink = new TypeOfSamplePanel();
        secondLink.setPanelId("1");
        secondLink.setTypeOfSampleId("20");

        when(typeOfSampleDAO.getAllTypeOfSamples()).thenReturn(Arrays.asList(serum, plasma));
        when(panelService.getAllActivePanels()).thenReturn(Collections.singletonList(panel));
        when(typeOfSamplePanelService.getTypeOfSamplePanelsForPanel("1"))
                .thenReturn(Collections.singletonList(firstLink)).thenReturn(Arrays.asList(firstLink, secondLink));
        when(typeOfSampleDAO.getTypeOfSampleById("10")).thenReturn(serum);
        when(typeOfSampleDAO.getTypeOfSampleById("20")).thenReturn(plasma);

        List<TypeOfSample> initial = service.getTypeOfSampleForPanelId("1");
        assertEquals(1, initial.size());

        service.clearCache();

        List<TypeOfSample> refreshed = service.getTypeOfSampleForPanelId("1");
        assertEquals(2, refreshed.size());
        assertTrue(refreshed.contains(serum));
        assertTrue(refreshed.contains(plasma));
        verify(panelService, times(2)).getAllActivePanels();
    }

    private TypeOfSample buildTypeOfSample(String id, String description) {
        TypeOfSample typeOfSample = new TypeOfSample();
        typeOfSample.setId(id);
        typeOfSample.setDescription(description);
        return typeOfSample;
    }

    private Panel buildPanel(String id, String name) {
        Panel panel = new Panel();
        panel.setId(id);
        panel.setPanelName(name);
        return panel;
    }
}
