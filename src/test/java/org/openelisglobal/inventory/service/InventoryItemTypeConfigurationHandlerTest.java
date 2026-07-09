package org.openelisglobal.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * OGC-658 Part A — unit coverage for the {@code inventory-item-types}
 * domain-config CSV loader: header/column handling, blank/comment-line
 * skipping, default active flag, and multi-locale parsing.
 */
@RunWith(MockitoJUnitRunner.class)
public class InventoryItemTypeConfigurationHandlerTest {

    @Mock
    private InventoryItemTypeService inventoryItemTypeService;

    @InjectMocks
    private InventoryItemTypeConfigurationHandler handler;

    private InputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void processConfiguration_upsertsEachDataRow() throws Exception {
        String content = "code,sortOrder,active,localization:en,localization:fr\n"
                + "CONTROL,60,Y,Control Material,Matériel de contrôle\n" + "CALIBRATOR,70,Y,Calibrator,Calibrateur\n";

        handler.processConfiguration(csv(content), "example-inventory-item-types.csv");

        verify(inventoryItemTypeService).upsertSeeded(eq("CONTROL"), eq(60), eq(true), anyMap(), anyString());
        verify(inventoryItemTypeService).upsertSeeded(eq("CALIBRATOR"), eq(70), eq(true), anyMap(), anyString());
    }

    @Test
    public void processConfiguration_capturesLocalizedNamesPerRow() throws Exception {
        String content = "code,sortOrder,active,localization:en,localization:fr\n"
                + "CONTROL,60,Y,Control Material,Matériel de contrôle\n";

        handler.processConfiguration(csv(content), "test.csv");

        ArgumentCaptor<Map<String, String>> localesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(inventoryItemTypeService).upsertSeeded(eq("CONTROL"), eq(60), eq(true), localesCaptor.capture(),
                anyString());
        assertEquals("Control Material", localesCaptor.getValue().get("en"));
        assertEquals("Matériel de contrôle", localesCaptor.getValue().get("fr"));
    }

    @Test
    public void processConfiguration_defaultsActiveToTrue_whenColumnBlank() throws Exception {
        String content = "code,sortOrder,active,localization:en\n" + "STAIN,110,,Stain\n";

        handler.processConfiguration(csv(content), "test.csv");

        verify(inventoryItemTypeService).upsertSeeded(eq("STAIN"), eq(110), eq(true), anyMap(), anyString());
    }

    @Test
    public void processConfiguration_respectsExplicitInactiveFlag() throws Exception {
        String content = "code,sortOrder,active,localization:en\n" + "STAIN,110,N,Stain\n";

        handler.processConfiguration(csv(content), "test.csv");

        verify(inventoryItemTypeService).upsertSeeded(eq("STAIN"), eq(110), eq(false), anyMap(), anyString());
    }

    @Test
    public void processConfiguration_skipsBlankAndCommentLines() throws Exception {
        String content = "code,sortOrder,active,localization:en\n" + "\n" + "# a comment\n" + "STAIN,110,Y,Stain\n";

        handler.processConfiguration(csv(content), "test.csv");

        verify(inventoryItemTypeService, times(1)).upsertSeeded(anyString(), eq(110), anyBoolean(), anyMap(),
                anyString());
    }

    @Test
    public void processConfiguration_skipsRowWithBlankCode() throws Exception {
        String content = "code,sortOrder,active,localization:en\n" + ",110,Y,Stain\n";

        handler.processConfiguration(csv(content), "test.csv");

        verify(inventoryItemTypeService, never()).upsertSeeded(anyString(), any(), anyBoolean(), anyMap(), anyString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void processConfiguration_throws_whenCodeColumnMissing() throws Exception {
        String content = "name,sortOrder\nCONTROL,60\n";

        handler.processConfiguration(csv(content), "bad.csv");
    }

    @Test(expected = IllegalArgumentException.class)
    public void processConfiguration_throws_whenFileEmpty() throws Exception {
        handler.processConfiguration(csv(""), "empty.csv");
    }
}
