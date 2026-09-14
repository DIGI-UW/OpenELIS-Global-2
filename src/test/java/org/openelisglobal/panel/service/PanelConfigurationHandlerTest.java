package org.openelisglobal.panel.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationValueService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelterminology.service.PanelTerminologyMappingService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;

/**
 * Unit tests for {@link PanelConfigurationHandler}, focused on the new {@code
 * loinc} column: a panel's LOINC must be written to {@code panel.loinc} and
 * bridged into the panel terminology mappings as a LOINC / SAME_AS entry,
 * exactly as the test loader does — and only when the column is present.
 *
 * <p>
 * The collaborators are Mockito mocks injected into the {@code @Autowired}
 * private fields by reflection, and the {@link DisplayListService} singleton
 * (refreshed once at the end of a load) is swapped for a mock so the real
 * {@code processConfiguration} can run without a Spring context or a database.
 */
public class PanelConfigurationHandlerTest {

    private PanelConfigurationHandler handler;

    private PanelService panelService;
    private PanelItemService panelItemService;
    private TestService testService;
    private LocalizationService localizationService;
    private LocalizationValueService localizationValueService;
    private TypeOfSampleService typeOfSampleService;
    private TypeOfSamplePanelService typeOfSamplePanelService;
    private PanelTerminologyMappingService panelTerminologyMappingService;

    private DisplayListService previousDisplayListInstance;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        handler = new PanelConfigurationHandler();

        panelService = mock(PanelService.class);
        panelItemService = mock(PanelItemService.class);
        testService = mock(TestService.class);
        localizationService = mock(LocalizationService.class);
        localizationValueService = mock(LocalizationValueService.class);
        typeOfSampleService = mock(TypeOfSampleService.class);
        typeOfSamplePanelService = mock(TypeOfSamplePanelService.class);
        panelTerminologyMappingService = mock(PanelTerminologyMappingService.class);

        inject("panelService", panelService);
        inject("panelItemService", panelItemService);
        inject("testService", testService);
        inject("localizationService", localizationService);
        inject("localizationValueService", localizationValueService);
        inject("typeOfSampleService", typeOfSampleService);
        inject("typeOfSamplePanelService", typeOfSamplePanelService);
        inject("panelTerminologyMappingService", panelTerminologyMappingService);

        // New panel path, deterministic ids, empty reconcile lists.
        when(panelService.getPanelByName(anyString())).thenReturn(null);
        when(panelService.insert(any(Panel.class))).thenReturn("42");
        when(localizationService.insert(any())).thenReturn("loc1");
        when(panelItemService.getPanelItemsForPanel(anyString())).thenReturn(Collections.emptyList());
        when(typeOfSamplePanelService.getTypeOfSamplePanelsForPanel(anyString())).thenReturn(Collections.emptyList());

        // DisplayListService.getInstance() returns a private static field that is
        // null outside Spring; swap in a mock so refreshLists() is a no-op.
        previousDisplayListInstance = DisplayListService.getInstance();
        setDisplayListInstance(mock(DisplayListService.class));
    }

    @After
    public void tearDown() throws Exception {
        setDisplayListInstance(previousDisplayListInstance);
    }

    @Test
    public void loincColumn_setsPanelLoincAndSyncsSameAsMapping() throws Exception {
        String csv = "panelName,loinc\n" + "Lipid Panel,24331-1\n";

        handler.processConfiguration(stream(csv), "panels.csv");

        ArgumentCaptor<Panel> panelCaptor = ArgumentCaptor.forClass(Panel.class);
        verify(panelService).insert(panelCaptor.capture());
        assertEquals("panel.loinc must carry the CSV LOINC", "24331-1", panelCaptor.getValue().getLoinc());

        verify(panelTerminologyMappingService).syncLegacyLoinc("42", "24331-1", "1");
    }

    @Test
    public void noLoincColumn_neverTouchesLoincOrMappings() throws Exception {
        String csv = "panelName,isActive\n" + "Lipid Panel,Y\n";

        handler.processConfiguration(stream(csv), "panels.csv");

        ArgumentCaptor<Panel> panelCaptor = ArgumentCaptor.forClass(Panel.class);
        verify(panelService).insert(panelCaptor.capture());
        assertNull("panel.loinc must stay untouched when no loinc column is present",
                panelCaptor.getValue().getLoinc());

        verify(panelTerminologyMappingService, never()).syncLegacyLoinc(anyString(), any(), anyString());
    }

    @Test
    public void loincColumnPresentButBlank_clearsMappingExplicitly() throws Exception {
        String csv = "panelName,loinc\n" + "Lipid Panel,\n";

        handler.processConfiguration(stream(csv), "panels.csv");

        // A present-but-empty loinc cell is an explicit clear: the value flows
        // through and syncLegacyLoinc("") soft-deletes any existing LOINC mapping.
        verify(panelTerminologyMappingService).syncLegacyLoinc(eq("42"), eq(""), eq("1"));
    }

    @Test
    public void getDomainName_isPanels() {
        assertEquals("panels", handler.getDomainName());
    }

    @Test
    public void getLoadOrder_isAfterTests() {
        assertEquals(300, handler.getLoadOrder());
    }

    @Test
    public void emptyFile_throws() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Panel configuration file panels.csv is empty");
        handler.processConfiguration(stream(""), "panels.csv");
    }

    @Test
    public void missingPanelNameColumn_throws() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("must have a 'panelName' column");
        handler.processConfiguration(stream("sampleTypes,tests\nBlood,CBC\n"), "panels.csv");
    }

    private InputStream stream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field field = PanelConfigurationHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(handler, value);
    }

    private void setDisplayListInstance(DisplayListService value) throws Exception {
        Field field = DisplayListService.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, value);
    }
}
