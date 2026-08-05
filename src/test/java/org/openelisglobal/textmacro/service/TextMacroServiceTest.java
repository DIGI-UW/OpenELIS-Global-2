package org.openelisglobal.textmacro.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.textmacro.dao.TextMacroDAO;
import org.openelisglobal.textmacro.form.TextMacroAdminForm;
import org.openelisglobal.textmacro.form.TextMacroAdminQueryForm;
import org.openelisglobal.textmacro.form.TextMacroBulkRequestForm;
import org.openelisglobal.textmacro.form.TextMacroBulkResultForm;
import org.openelisglobal.textmacro.form.TextMacroPageForm;
import org.openelisglobal.textmacro.valueholder.TextMacro;
import org.openelisglobal.textmacro.valueholder.TextMacroContext;

public class TextMacroServiceTest {

    private TextMacroDAO macroDAO;
    private AuditTrailService auditTrailService;
    private TextMacroService service;

    @Before
    public void setUp() {
        macroDAO = mock(TextMacroDAO.class);
        auditTrailService = mock(AuditTrailService.class);
        service = new TextMacroServiceImpl(macroDAO, auditTrailService);
    }

    @Test
    public void createNormalizesCodeAndAttributesAuthenticatedActor() {
        TextMacroAdminForm request = request(" GPC ", "  Gram-positive cocci  ",
                Set.of("MICROBIOLOGY_CULTURE_ACTIVITY"));
        when(macroDAO.findByCode(".gpc")).thenReturn(Optional.empty());

        service.save(null, request, "42");

        ArgumentCaptor<TextMacro> saved = ArgumentCaptor.forClass(TextMacro.class);
        verify(macroDAO).insert(saved.capture());
        assertEquals(".gpc", saved.getValue().getCode());
        assertEquals("Gram-positive cocci", saved.getValue().getExpansionText());
        assertEquals(Set.of(TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY), saved.getValue().getContexts());
        assertEquals("42", saved.getValue().getLastUpdatedBy());
        assertEquals("LOCAL", saved.getValue().getProvenance());
        assertTrue(saved.getValue().isActive());
    }

    @Test
    public void createRejectsDuplicateCanonicalCode() {
        TextMacro existing = macro("existing", ".gpc", TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY);
        when(macroDAO.findByCode(".gpc")).thenReturn(Optional.of(existing));

        assertThrows(TextMacroConflictException.class, () -> service.save(null,
                request(".GPC", "Different text", Set.of("MICROBIOLOGY_CULTURE_ACTIVITY")), "42"));
    }

    @Test
    public void createRejectsUnknownContextAndMissingActor() {
        TextMacroRequestException invalidContext = assertThrows(TextMacroRequestException.class,
                () -> service.save(null, request(".gpc", "Text", Set.of("NOT_A_CONTEXT")), "42"));
        assertEquals("INVALID_MACRO_CONTEXT", invalidContext.getCode());
        TextMacroRequestException missingActor = assertThrows(TextMacroRequestException.class,
                () -> service.save(null, request(".gpc", "Text", Set.of("MICROBIOLOGY_CULTURE_ACTIVITY")), " "));
        assertEquals("AUTHENTICATED_ACTOR_REQUIRED", missingActor.getCode());
    }

    @Test
    public void validationErrorsExposeStableContractCodes() {
        TextMacroRequestException invalidCode = assertThrows(TextMacroRequestException.class,
                () -> service.save(null, request("not valid!", "Text", Set.of("MICROBIOLOGY_CULTURE_ACTIVITY")), "42"));
        assertEquals("INVALID_MACRO_CODE", invalidCode.getCode());

        TextMacroRequestException missingText = assertThrows(TextMacroRequestException.class,
                () -> service.save(null, request(".valid", " ", Set.of("MICROBIOLOGY_CULTURE_ACTIVITY")), "42"));
        assertEquals("MACRO_TEXT_REQUIRED", missingText.getCode());

        TextMacroRequestException missingContext = assertThrows(TextMacroRequestException.class,
                () -> service.save(null, request(".valid", "Text", Set.of()), "42"));
        assertEquals("MACRO_CONTEXT_REQUIRED", missingContext.getCode());
    }

    @Test
    public void runtimeLookupUsesRecognizedContextAndCapsLimit() {
        TextMacro macro = macro("macro-1", ".ng24", TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY);
        when(macroDAO.findActiveByContext(TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY, "ng", 50))
                .thenReturn(List.of(macro));

        assertEquals(".ng24", service.findActive("MICROBIOLOGY_CULTURE_ACTIVITY", " ng ", 500).get(0).code);

        verify(macroDAO).findActiveByContext(TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY, "ng", 50);
    }

    @Test
    public void adminSearchNormalizesStablePageState() {
        TextMacroAdminQueryForm query = new TextMacroAdminQueryForm();
        query.q = " gpc ";
        query.context = "MICROBIOLOGY_CULTURE_ACTIVITY";
        query.status = "inactive";
        query.sort = "updated:desc";
        query.page = 0;
        query.pageSize = 999;
        when(macroDAO.search("gpc", TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY, "INACTIVE", "updated:desc", 0, 100))
                .thenReturn(List.of());
        when(macroDAO.countSearch("gpc", TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY, "INACTIVE")).thenReturn(0L);

        TextMacroPageForm result = service.searchAdmin(query);

        assertEquals(1, result.page);
        assertEquals(20, result.pageSize);
        assertEquals(0, result.total);
    }

    @Test
    public void exportIsDeterministicAndPreservesRoundTripFields() throws IOException {
        TextMacro zeta = macro("zeta-id", ".zeta", TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY);
        zeta.setExpansionText("Line one, \"reviewed\"\nLine two");
        zeta.setContexts(
                Set.of(TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY, TextMacroContext.MICROBIOLOGY_CLINICAL_HISTORY));
        zeta.setProvenance("PACKAGE");
        zeta.setSourceKey("openelis-reviewed");
        zeta.setSourceVersion("2026.08");
        TextMacro alpha = macro("alpha-id", ".alpha", TextMacroContext.MICROBIOLOGY_ANTIBIOTIC_EXPOSURE);
        when(macroDAO.findAllWithContexts()).thenReturn(List.of(zeta, alpha));

        String csv = service.exportCsv();

        try (CSVParser parser = CSVParser.parse(new StringReader(csv),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
            List<CSVRecord> records = parser.getRecords();
            assertEquals(List.of("code", "expansion_text", "contexts", "active", "provenance", "source_key",
                    "source_version"), new ArrayList<>(parser.getHeaderMap().keySet()));
            assertEquals(".alpha", records.get(0).get("code"));
            assertEquals(".zeta", records.get(1).get("code"));
            assertEquals("Line one, \"reviewed\"\nLine two", records.get(1).get("expansion_text"));
            assertEquals("MICROBIOLOGY_CLINICAL_HISTORY|MICROBIOLOGY_CULTURE_ACTIVITY", records.get(1).get("contexts"));
            assertEquals("openelis-reviewed", records.get(1).get("source_key"));
            assertEquals("2026.08", records.get(1).get("source_version"));
        }
    }

    @Test
    public void bulkDeactivateValidatesSelectionThenAttributesEveryWrite() {
        TextMacro first = macro("first-id", ".first", TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY);
        TextMacro second = macro("second-id", ".second", TextMacroContext.MICROBIOLOGY_CLINICAL_HISTORY);
        when(macroDAO.findByIdsWithContexts(Set.of("first-id", "second-id"))).thenReturn(List.of(second, first));

        TextMacroBulkResultForm result = service.bulk(bulk("DEACTIVATE", "second-id", "first-id"), "42");

        assertEquals("DEACTIVATE", result.action);
        assertEquals(2, result.affectedCount);
        assertEquals(List.of(".first", ".second"), result.affectedCodes);
        assertTrue(!first.isActive());
        assertTrue(!second.isActive());
        assertEquals("42", first.getLastUpdatedBy());
        assertEquals("42", second.getLastUpdatedBy());
        verify(macroDAO).update(first);
        verify(macroDAO).update(second);
    }

    @Test
    public void bulkRemovalRejectsWholeSelectionWhenAnyPhraseIsPackaged() {
        TextMacro local = macro("local-id", ".local", TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY);
        TextMacro packaged = macro("package-id", ".package", TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY);
        packaged.setProvenance("PACKAGE");
        when(macroDAO.findByIdsWithContexts(Set.of("local-id", "package-id"))).thenReturn(List.of(local, packaged));

        TextMacroRequestException error = assertThrows(TextMacroRequestException.class,
                () -> service.bulk(bulk("DELETE_LOCAL", "local-id", "package-id"), "42"));

        assertEquals("PACKAGED_MACRO_REMOVAL_NOT_ALLOWED", error.getCode());
        verify(macroDAO, never()).delete(local);
        verify(macroDAO, never()).delete(packaged);
    }

    @Test
    public void bulkLocalRemovalRecordsAuthenticatedActorBeforeDelete() {
        TextMacro local = macro("local-id", ".local", TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY);
        when(macroDAO.findByIdsWithContexts(Set.of("local-id"))).thenReturn(List.of(local));
        when(macroDAO.getTableName()).thenReturn("text_macro");

        service.bulk(bulk("DELETE_LOCAL", "local-id"), "42");

        assertEquals("42", local.getSysUserId());
        verify(auditTrailService).saveHistory(null, local, "42", IActionConstants.AUDIT_TRAIL_DELETE, "text_macro");
        verify(macroDAO).delete(local);
    }

    @Test
    public void bulkRejectsMissingAndDuplicateIdsBeforeMutation() {
        TextMacro first = macro("first-id", ".first", TextMacroContext.MICROBIOLOGY_CULTURE_ACTIVITY);
        when(macroDAO.findByIdsWithContexts(Set.of("first-id", "missing-id"))).thenReturn(List.of(first));

        TextMacroRequestException missing = assertThrows(TextMacroRequestException.class,
                () -> service.bulk(bulk("ACTIVATE", "first-id", "missing-id"), "42"));
        assertEquals("MACRO_NOT_FOUND", missing.getCode());
        TextMacroRequestException duplicate = assertThrows(TextMacroRequestException.class,
                () -> service.bulk(bulk("ACTIVATE", "first-id", "first-id"), "42"));
        assertEquals("DUPLICATE_MACRO_IDS", duplicate.getCode());
        verify(macroDAO, never()).update(first);
    }

    private TextMacroAdminForm request(String code, String text, Set<String> contexts) {
        TextMacroAdminForm request = new TextMacroAdminForm();
        request.code = code;
        request.expansionText = text;
        request.contexts = contexts;
        request.active = true;
        return request;
    }

    private TextMacro macro(String id, String code, TextMacroContext context) {
        TextMacro macro = new TextMacro();
        macro.setId(id);
        macro.setCode(code);
        macro.setExpansionText("Expansion");
        macro.setContexts(Set.of(context));
        macro.setActive(true);
        macro.setProvenance("LOCAL");
        return macro;
    }

    private TextMacroBulkRequestForm bulk(String action, String... ids) {
        TextMacroBulkRequestForm request = new TextMacroBulkRequestForm();
        request.action = action;
        request.ids = List.of(ids);
        return request;
    }
}
