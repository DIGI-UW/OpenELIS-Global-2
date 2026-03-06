package org.openelisglobal.localization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openelisglobal.localization.valueholder.SupportedLocale;

@ExtendWith(MockitoExtension.class)
class SupportedLocaleConfigurationHandlerTest {

    @Mock
    private SupportedLocaleService supportedLocaleService;

    @InjectMocks
    private SupportedLocaleConfigurationHandler handler;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
    }

    @Test
    void testGetDomainName() {
        assertEquals("locales", handler.getDomainName());
    }

    @Test
    void testGetFileExtension() {
        assertEquals("csv", handler.getFileExtension());
    }

    @Test
    void testGetLoadOrder() {
        assertEquals(50, handler.getLoadOrder());
    }

    @Test
    void testProcessConfiguration_createsNewLocales() throws Exception {
        String csv = "localeCode,displayName,isActive,isFallback,sortOrder\n" + "en,English,Y,Y,1\n"
                + "fr,Français,Y,N,2\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        when(supportedLocaleService.getByLocaleCode("en")).thenReturn(Optional.empty());
        when(supportedLocaleService.getByLocaleCode("fr")).thenReturn(Optional.empty());
        when(supportedLocaleService.getFallback()).thenReturn(Optional.empty());
        when(supportedLocaleService.insert(any(SupportedLocale.class))).thenReturn("1", "2");

        handler.processConfiguration(inputStream, "test-locales.csv");

        ArgumentCaptor<SupportedLocale> captor = ArgumentCaptor.forClass(SupportedLocale.class);
        verify(supportedLocaleService, times(2)).insert(captor.capture());

        SupportedLocale englishLocale = captor.getAllValues().get(0);
        assertEquals("en", englishLocale.getLocaleCode());
        assertEquals("English", englishLocale.getDisplayName());
        assertEquals(true, englishLocale.isActive());
        assertEquals(true, englishLocale.isFallback());
        assertEquals(1, englishLocale.getSortOrder());

        SupportedLocale frenchLocale = captor.getAllValues().get(1);
        assertEquals("fr", frenchLocale.getLocaleCode());
        assertEquals("Français", frenchLocale.getDisplayName());
        assertEquals(true, frenchLocale.isActive());
        assertEquals(false, frenchLocale.isFallback());
        assertEquals(2, frenchLocale.getSortOrder());
    }

    @Test
    void testProcessConfiguration_updatesExistingLocale() throws Exception {
        String csv = "localeCode,displayName,isActive,isFallback,sortOrder\n" + "en,English Updated,Y,N,1\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        SupportedLocale existingLocale = new SupportedLocale();
        existingLocale.setId("1");
        existingLocale.setLocaleCode("en");
        existingLocale.setDisplayName("English");

        when(supportedLocaleService.getByLocaleCode("en")).thenReturn(Optional.of(existingLocale));

        handler.processConfiguration(inputStream, "test-locales.csv");

        verify(supportedLocaleService, never()).insert(any(SupportedLocale.class));
        verify(supportedLocaleService, times(1)).update(any(SupportedLocale.class));

        assertEquals("English Updated", existingLocale.getDisplayName());
    }

    @Test
    void testProcessConfiguration_skipsCommentLines() throws Exception {
        String csv = "localeCode,displayName,isActive,isFallback,sortOrder\n" + "# This is a comment\n"
                + "en,English,Y,Y,1\n" + "# Another comment\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        when(supportedLocaleService.getByLocaleCode("en")).thenReturn(Optional.empty());
        when(supportedLocaleService.getFallback()).thenReturn(Optional.empty());
        when(supportedLocaleService.insert(any(SupportedLocale.class))).thenReturn("1");

        handler.processConfiguration(inputStream, "test-locales.csv");

        verify(supportedLocaleService, times(1)).insert(any(SupportedLocale.class));
    }

    @Test
    void testProcessConfiguration_skipsEmptyLines() throws Exception {
        String csv = "localeCode,displayName,isActive,isFallback,sortOrder\n" + "\n" + "en,English,Y,Y,1\n" + "\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        when(supportedLocaleService.getByLocaleCode("en")).thenReturn(Optional.empty());
        when(supportedLocaleService.getFallback()).thenReturn(Optional.empty());
        when(supportedLocaleService.insert(any(SupportedLocale.class))).thenReturn("1");

        handler.processConfiguration(inputStream, "test-locales.csv");

        verify(supportedLocaleService, times(1)).insert(any(SupportedLocale.class));
    }

    @Test
    void testProcessConfiguration_emptyFile_throwsException() {
        String csv = "";
        InputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class,
                () -> handler.processConfiguration(inputStream, "empty-locales.csv"));
    }

    @Test
    void testProcessConfiguration_missingLocaleCodeColumn_throwsException() {
        String csv = "displayName,isActive,isFallback,sortOrder\n" + "English,Y,Y,1\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class,
                () -> handler.processConfiguration(inputStream, "missing-column.csv"));
    }

    @Test
    void testProcessConfiguration_missingDisplayNameColumn_throwsException() {
        String csv = "localeCode,isActive,isFallback,sortOrder\n" + "en,Y,Y,1\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class,
                () -> handler.processConfiguration(inputStream, "missing-column.csv"));
    }

    @Test
    void testProcessConfiguration_clearsExistingFallbackWhenSettingNew() throws Exception {
        String csv = "localeCode,displayName,isActive,isFallback,sortOrder\n" + "fr,Français,Y,Y,1\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        SupportedLocale existingFallback = new SupportedLocale();
        existingFallback.setId("1");
        existingFallback.setLocaleCode("en");
        existingFallback.setFallback(true);

        when(supportedLocaleService.getByLocaleCode("fr")).thenReturn(Optional.empty());
        when(supportedLocaleService.getFallback()).thenReturn(Optional.of(existingFallback));
        when(supportedLocaleService.insert(any(SupportedLocale.class))).thenReturn("2");

        handler.processConfiguration(inputStream, "test-locales.csv");

        // Verify that the existing fallback was cleared
        verify(supportedLocaleService, times(1)).update(existingFallback);
        assertEquals(false, existingFallback.isFallback());
    }

    @Test
    void testProcessConfiguration_defaultsActiveToTrue() throws Exception {
        String csv = "localeCode,displayName,sortOrder\n" + "en,English,1\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        when(supportedLocaleService.getByLocaleCode("en")).thenReturn(Optional.empty());
        when(supportedLocaleService.insert(any(SupportedLocale.class))).thenReturn("1");

        handler.processConfiguration(inputStream, "test-locales.csv");

        ArgumentCaptor<SupportedLocale> captor = ArgumentCaptor.forClass(SupportedLocale.class);
        verify(supportedLocaleService).insert(captor.capture());

        assertEquals(true, captor.getValue().isActive());
    }

    @Test
    void testProcessConfiguration_defaultsFallbackToFalse() throws Exception {
        String csv = "localeCode,displayName,sortOrder\n" + "en,English,1\n";

        InputStream inputStream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        when(supportedLocaleService.getByLocaleCode("en")).thenReturn(Optional.empty());
        when(supportedLocaleService.insert(any(SupportedLocale.class))).thenReturn("1");

        handler.processConfiguration(inputStream, "test-locales.csv");

        ArgumentCaptor<SupportedLocale> captor = ArgumentCaptor.forClass(SupportedLocale.class);
        verify(supportedLocaleService).insert(captor.capture());

        assertEquals(false, captor.getValue().isFallback());
    }
}
