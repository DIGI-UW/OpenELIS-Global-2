package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.reports.dataexport.form.ExportFilter;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;

/**
 * OGC-1507 BR-4: an export job keeps the language of the user who asked for it
 * through the stored request, and a job stored before it did prints in English.
 */
public class ExportSnapshotLocaleTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void theStoredRequestKeepsTheUsersLanguage() throws Exception {
        ExportSnapshot asked = new ExportSnapshot(definition(), "TABLE", List.of(),
                new ExportFilter("2026-05-05", "2026-05-05", List.of("1"), List.of(), List.of()), "UTC", List.of(),
                "fr-FR");

        ExportSnapshot read = mapper.readValue(mapper.writeValueAsString(asked), ExportSnapshot.class);

        assertEquals("fr-FR", read.locale());
        assertEquals(Locale.forLanguageTag("fr-FR"), read.userLocale());
    }

    @Test
    public void aJobStoredBeforeTheLanguageWasKeptPrintsInEnglish() throws Exception {
        ExportSnapshot older = new ExportSnapshot(definition(), "TABLE", List.of(),
                new ExportFilter("2026-05-05", "2026-05-05", List.of("1"), List.of(), List.of()), "UTC", List.of());
        String json = mapper.writeValueAsString(older).replace(",\"locale\":null", "");

        ExportSnapshot read = mapper.readValue(json, ExportSnapshot.class);

        assertNull(read.locale());
        assertEquals(Locale.ENGLISH, read.userLocale());
    }

    private static ReportSourceConfig definition() {
        return new ReportSourceConfig("NON_CONFORMANCE", 1, "Non-Conformance", "NON_CONFORMANCE", "eventOrRecordedDate",
                List.of("TABLE"), List.of("ncOccurrenceId"), List.of(), List.of("labSectionIds"),
                Map.of("TABLE", List.of()));
    }
}
