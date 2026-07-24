package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.Analyzer;

/**
 * FILE analyzer create-from-profile must treat the profile as DEFAULTS only: a
 * value the form/user already set wins; the profile fills only what's unset.
 *
 * Regression for the Add bug where a user-entered Import Directory (and any
 * other customized FILE field) was silently overwritten by the profile default
 * — because the create path persisted the form value, then
 * autoCreateFromProfile unconditionally re-set every field. (Edit never had the
 * bug; it doesn't call autoCreateFromProfile.)
 */
public class FileImportServiceAutoCreateTest {

    private FileImportServiceImpl service;
    private AnalyzerService analyzerService;

    @Before
    public void setUp() throws Exception {
        service = new FileImportServiceImpl();
        analyzerService = mock(AnalyzerService.class);
        inject("baseImportDir", "/data/analyzer-imports");
        inject("analyzerService", analyzerService);
        // bridgeRegistrationService left null — registerFile path is skipped.
    }

    private void inject(String field, Object value) throws Exception {
        Field f = FileImportServiceImpl.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(service, value);
    }

    private Map<String, Object> xlsxProfile() {
        return Map.of("configDefaults", Map.of("fileFormat", "XLSX", "hasHeader", true, "skipRows", 2),
                "supported_extensions", List.of(".xlsx"));
    }

    @Test
    public void formImportDirectoryIsPreserved_profileDoesNotOverwrite() {
        Analyzer a = new Analyzer();
        a.setImportDirectory("/lab/uat/flatfiles/incoming"); // user-entered on the Add form
        when(analyzerService.get("a1")).thenReturn(a);

        service.autoCreateFromProfile("a1", xlsxProfile(), "FluoroCycler XT", "1");

        // Survives — NOT replaced by /data/analyzer-imports/<name>/incoming.
        assertEquals("/lab/uat/flatfiles/incoming", a.getImportDirectory());
    }

    @Test
    public void unsetFieldsAreFilledFromProfile() {
        Analyzer a = new Analyzer();
        a.setImportDirectory("/lab/uat/incoming"); // form set ONLY this
        when(analyzerService.get("a2")).thenReturn(a);

        service.autoCreateFromProfile("a2", xlsxProfile(), "FluoroCycler XT", "1");

        assertEquals("/lab/uat/incoming", a.getImportDirectory()); // preserved
        assertEquals("XLSX", a.getFileFormat()); // profile-filled
        assertEquals(Boolean.TRUE, a.getHasHeader()); // profile-filled
        assertEquals(Integer.valueOf(2), a.getSkipRows()); // profile-filled
    }

    @Test
    public void blankImportDirectoryFallsBackToProfileDefault() {
        Analyzer a = new Analyzer(); // form omitted import directory (null)
        when(analyzerService.get("a3")).thenReturn(a);

        service.autoCreateFromProfile("a3", xlsxProfile(), "FluoroCycler XT", "1");

        assertEquals("/data/analyzer-imports/fluorocycler-xt/incoming", a.getImportDirectory());
    }
}
