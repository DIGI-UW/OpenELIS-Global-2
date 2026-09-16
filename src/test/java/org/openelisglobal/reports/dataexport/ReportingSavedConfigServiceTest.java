package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.reportdefinition.service.ReportDefinitionService;
import org.openelisglobal.reportdefinition.valueholder.ReportDefinition;
import org.openelisglobal.reports.dataexport.form.SavedReportDefinition;
import org.openelisglobal.reports.dataexport.form.SavedReportFilters;
import org.openelisglobal.reports.dataexport.form.SavedReportMutation;
import org.openelisglobal.reports.dataexport.service.ReportingAccess;
import org.openelisglobal.reports.dataexport.service.ReportingCatalogService;
import org.openelisglobal.reports.dataexport.service.ReportingException;
import org.openelisglobal.reports.dataexport.service.ReportingSavedConfigService;

@RunWith(MockitoJUnitRunner.class)
public class ReportingSavedConfigServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-13T20:00:00Z");
    @Mock
    private ReportDefinitionService definitions;
    @Mock
    private ReportingCatalogService catalog;
    @Mock
    private ReportingAccess access;
    private ReportingSavedConfigService service;
    private SavedReportDefinition saved;

    @Before
    public void setUp() {
        service = new ReportingSavedConfigService(definitions, catalog, access, Clock.fixed(NOW, ZoneOffset.UTC));
        saved = new SavedReportDefinition(1, "SAMPLE_TESTING", "SPREADSHEET", List.of("accessionNumber", "test:7"),
                new SavedReportFilters(List.of("4"), List.of("7"), List.of("FINALIZED")));
        when(catalog.validateSaved("11", saved)).thenReturn(saved);
        when(definitions.insert(any())).thenAnswer(call -> ((ReportDefinition) call.getArgument(0)).getId());
    }

    @Test
    public void createStoresAValidatedSharedDefinitionWithoutDates() {
        var created = service.create("11", new SavedReportMutation(" Monthly viral load ", null, saved));

        ArgumentCaptor<ReportDefinition> stored = ArgumentCaptor.forClass(ReportDefinition.class);
        verify(definitions).insert(stored.capture());
        assertEquals("Monthly viral load", stored.getValue().getName());
        assertEquals("CSV_SAVED", stored.getValue().getReportType());
        assertTrue(stored.getValue().getIsPublic());
        assertTrue(stored.getValue().getIsActive());
        assertEquals("11", stored.getValue().getCreatedBy());
        assertEquals("11", stored.getValue().getUpdatedBy());
        assertFalse(stored.getValue().getDefinitionJson().contains("dateFrom"));
        assertEquals(saved, created.definition());
        assertEquals(NOW.toString(), created.version());
    }

    @Test
    public void listIsSharedSearchableAndExcludesOtherDefinitionKinds() {
        ReportDefinition matching = stored("CSV-1", "Monthly viral load", "11", true, "CSV_SAVED", saved);
        ReportDefinition inactive = stored("CSV-2", "Monthly old", "12", false, "CSV_SAVED", saved);
        ReportDefinition source = stored("SOURCE-1", "Monthly source", "system", true, "CSV_SOURCE", saved);
        when(definitions.getAllMatching("reportType", "CSV_SAVED")).thenReturn(List.of(inactive, matching));

        var page = service.list("12", 0, 20, "viral");

        assertEquals(1, page.reports().size());
        assertEquals("CSV-1", page.reports().get(0).id());
        assertEquals("11", page.reports().get(0).createdBy());
        assertFalse(page.hasMore());
        assertEquals("CSV_SOURCE", source.getReportType());
    }

    @Test
    public void staleUpdateIsRejectedAndCurrentUpdateKeepsIdentity() {
        ReportDefinition current = stored("CSV-1", "Old", "11", true, "CSV_SAVED", saved);
        when(definitions.get("CSV-1")).thenReturn(current);
        Timestamp nextVersion = Timestamp.from(NOW.plusSeconds(1));
        when(definitions.update(current)).thenAnswer(call -> {
            current.setLastupdated(nextVersion);
            return current;
        });
        SavedReportMutation stale = new SavedReportMutation("New", "2026-09-12T00:00:00Z", saved);

        ReportingException error = assertThrows(ReportingException.class, () -> service.update("12", "CSV-1", stale));
        assertEquals(409, error.status());

        var updated = service.update("12", "CSV-1",
                new SavedReportMutation("New", current.getLastupdated().toInstant().toString(), saved));
        verify(definitions).update(current);
        assertEquals("CSV-1", updated.id());
        assertEquals(nextVersion.toInstant().toString(), updated.version());
        assertEquals("New", current.getName());
        assertEquals("12", current.getUpdatedBy());
    }

    @Test
    public void removeIsVersionedAndLeavesTheDefinitionForPastJobs() {
        ReportDefinition current = stored("CSV-1", "Monthly", "11", true, "CSV_SAVED", saved);
        when(definitions.get("CSV-1")).thenReturn(current);

        service.remove("12", "CSV-1", current.getLastupdated().toInstant().toString());

        assertFalse(current.getIsActive());
        assertEquals("12", current.getUpdatedBy());
        verify(definitions).update(current);
    }

    private ReportDefinition stored(String id, String name, String creator, boolean active, String type,
            SavedReportDefinition definition) {
        ReportDefinition result = new ReportDefinition();
        result.setId(id);
        result.setName(name);
        result.setCreatedBy(creator);
        result.setUpdatedBy(creator);
        result.setCreatedDate(Timestamp.from(NOW));
        result.setLastupdated(Timestamp.from(NOW));
        result.setIsActive(active);
        result.setIsPublic(true);
        result.setReportType(type);
        result.setDefinitionJson(new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(definition).toString());
        return result;
    }
}
