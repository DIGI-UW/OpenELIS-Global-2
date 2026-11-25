package org.openelisglobal.textmacro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.history.service.HistoryService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.textmacro.form.TextMacroAdminForm;
import org.openelisglobal.textmacro.form.TextMacroAdminQueryForm;
import org.openelisglobal.textmacro.form.TextMacroBulkRequestForm;
import org.openelisglobal.textmacro.form.TextMacroBulkResultForm;
import org.openelisglobal.textmacro.form.TextMacroPageForm;
import org.openelisglobal.textmacro.form.TextMacroSummaryForm;
import org.openelisglobal.textmacro.service.TextMacroConflictException;
import org.openelisglobal.textmacro.service.TextMacroRequestException;
import org.openelisglobal.textmacro.service.TextMacroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TextMacroServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TextMacroService service;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Test
    public void serviceCreatesGeneratedIdsAndFiltersActiveMacrosByContext() {
        String suffix = uniqueSuffix();
        TextMacroAdminForm reading = service.save(null, request(".r" + suffix, "Reading phrase " + suffix, "CULTURE",
                Set.of("MICROBIOLOGY_CULTURE_READING"), true), TEST_SYS_USER_ID);
        TextMacroAdminForm history = service.save(null, request(".h" + suffix, "History phrase " + suffix, "CLINICAL",
                Set.of("MICROBIOLOGY_CLINICAL_HISTORY"), true), TEST_SYS_USER_ID);
        service.save(null, request(".i" + suffix, "Inactive phrase " + suffix, "CULTURE",
                Set.of("MICROBIOLOGY_CULTURE_READING"), false), TEST_SYS_USER_ID);

        assertNotEquals(reading.id, history.id);
        List<TextMacroSummaryForm> readingPhrases = offered("MICROBIOLOGY_CULTURE_READING", suffix);
        assertEquals(1, readingPhrases.size());
        assertEquals(reading.id, readingPhrases.get(0).id);
        assertEquals(".r" + suffix, readingPhrases.get(0).code);
        assertEquals("CULTURE", readingPhrases.get(0).category);
        assertEquals(List.of(), offered("MICROBIOLOGY_AMENDMENT_REASON", suffix));
    }

    @Test
    public void everyV2CaseFieldHasItsOwnContext() {
        String suffix = uniqueSuffix();
        List<String> contexts = List.of("MICROBIOLOGY_CLINICAL_HISTORY", "MICROBIOLOGY_CASE_NOTE",
                "MICROBIOLOGY_CULTURE_READING", "MICROBIOLOGY_OVERRIDE_REASON", "MICROBIOLOGY_AMENDMENT_REASON");
        for (int index = 0; index < contexts.size(); index++) {
            service.save(null, request(".v" + suffix + index, "Field phrase " + suffix + " " + index, "REPORTING",
                    Set.of(contexts.get(index)), true), TEST_SYS_USER_ID);
        }

        for (int index = 0; index < contexts.size(); index++) {
            List<TextMacroSummaryForm> offered = offered(contexts.get(index), suffix);
            assertEquals(1, offered.size());
            assertEquals(".v" + suffix + index, offered.get(0).code);
        }
        TextMacroRequestException removed = assertThrows(TextMacroRequestException.class,
                () -> service.findActive("MICROBIOLOGY_ANTIBIOTIC_EXPOSURE"));
        assertEquals("INVALID_MACRO_CONTEXT", removed.getCode());
    }

    @Test
    public void reservedCodesAreRejectedInAnyCase() {
        for (String reserved : List.of(".help", "LIST", " .Help ")) {
            TextMacroRequestException error = assertThrows(TextMacroRequestException.class,
                    () -> service.save(null,
                            request(reserved, "Text", "CLINICAL", Set.of("MICROBIOLOGY_CLINICAL_HISTORY"), true),
                            TEST_SYS_USER_ID));
            assertEquals("MACRO_CODE_RESERVED", error.getCode());
        }
        TextMacroAdminQueryForm query = new TextMacroAdminQueryForm();
        query.setQ(".help");
        query.setStatus("all");
        assertEquals(0, service.searchAdmin(query).total);
    }

    @Test
    public void codeAndPhraseLengthsFollowTheLibraryLimits() {
        String suffix = uniqueSuffix();
        String longestCode = ".m" + suffix + "abcde";
        String longestPhrase = "x".repeat(500);
        TextMacroAdminForm saved = service.save(null,
                request(longestCode, longestPhrase, "CULTURE", Set.of("MICROBIOLOGY_CASE_NOTE"), true),
                TEST_SYS_USER_ID);
        assertEquals(15, saved.code.length());
        assertEquals(longestPhrase, service.getAdmin(saved.id).expansionText);

        TextMacroRequestException codeTooLong = assertThrows(TextMacroRequestException.class,
                () -> service.save(null,
                        request(longestCode + "f", "Text", "CULTURE", Set.of("MICROBIOLOGY_CASE_NOTE"), true),
                        TEST_SYS_USER_ID));
        assertEquals("INVALID_MACRO_CODE", codeTooLong.getCode());
        TextMacroRequestException codeTooShort = assertThrows(TextMacroRequestException.class, () -> service.save(null,
                request(".", "Text", "CULTURE", Set.of("MICROBIOLOGY_CASE_NOTE"), true), TEST_SYS_USER_ID));
        assertEquals("INVALID_MACRO_CODE", codeTooShort.getCode());
        TextMacroRequestException phraseTooLong = assertThrows(TextMacroRequestException.class,
                () -> service.save(null,
                        request(".n" + suffix, longestPhrase + "x", "CULTURE", Set.of("MICROBIOLOGY_CASE_NOTE"), true),
                        TEST_SYS_USER_ID));
        assertEquals("MACRO_TEXT_TOO_LONG", phraseTooLong.getCode());
    }

    @Test
    public void categoryIsRequiredStoredAndFilterable() {
        String suffix = uniqueSuffix();
        TextMacroRequestException missing = assertThrows(TextMacroRequestException.class, () -> service.save(null,
                request(".c" + suffix, "Text", " ", Set.of("MICROBIOLOGY_CASE_NOTE"), true), TEST_SYS_USER_ID));
        assertEquals("MACRO_CATEGORY_REQUIRED", missing.getCode());
        TextMacroRequestException unknown = assertThrows(TextMacroRequestException.class,
                () -> service.save(null,
                        request(".c" + suffix, "Text", "NOT_A_CATEGORY", Set.of("MICROBIOLOGY_CASE_NOTE"), true),
                        TEST_SYS_USER_ID));
        assertEquals("INVALID_MACRO_CATEGORY", unknown.getCode());

        TextMacroAdminForm culture = service.save(null,
                request(".c" + suffix, "Culture " + suffix, "culture", Set.of("MICROBIOLOGY_CASE_NOTE"), true),
                TEST_SYS_USER_ID);
        TextMacroAdminForm ast = service.save(null,
                request(".a" + suffix, "AST " + suffix, "AST", Set.of("MICROBIOLOGY_OVERRIDE_REASON"), true),
                TEST_SYS_USER_ID);
        assertEquals("CULTURE", service.getAdmin(culture.id).category);

        TextMacroAdminForm edited = request(culture.code, culture.expansionText, "REPORTING",
                Set.of("MICROBIOLOGY_CASE_NOTE"), true);
        service.save(culture.id, edited, TEST_SYS_USER_ID);
        assertEquals("REPORTING", service.getAdmin(culture.id).category);

        assertEquals(List.of(culture.id), idsOf(search(suffix, "REPORTING")));
        assertEquals(List.of(ast.id), idsOf(search(suffix, "AST")));
        assertEquals(List.of(ast.id, culture.id), idsOf(search(suffix, "all")));
    }

    @Test
    public void canonicalDuplicateIsRejectedThroughService() {
        String suffix = uniqueSuffix();
        service.save(null, request(".d" + suffix, "First", "CULTURE", Set.of("MICROBIOLOGY_CASE_NOTE"), true),
                TEST_SYS_USER_ID);

        TextMacroConflictException duplicate = assertThrows(TextMacroConflictException.class, () -> service.save(null,
                request(("D" + suffix).toUpperCase(), "Second", "CULTURE", Set.of("MICROBIOLOGY_CASE_NOTE"), true),
                TEST_SYS_USER_ID));
        assertEquals("Macro code is already in use", duplicate.getMessage());
    }

    @Test
    public void fiveHundredMacrosRemainDeterministicallyPageable() {
        String suffix = uniqueSuffix();
        Set<String> generatedIds = new HashSet<>();
        for (int index = 0; index < 500; index++) {
            TextMacroAdminForm saved = service.save(null, request(String.format(".s%s%03d", suffix, index),
                    "Scale phrase " + suffix + " " + index, "CULTURE", Set.of("MICROBIOLOGY_CULTURE_READING"), true),
                    TEST_SYS_USER_ID);
            generatedIds.add(saved.id);
        }
        TextMacroAdminQueryForm query = new TextMacroAdminQueryForm();
        query.setQ(suffix);
        query.setContext("MICROBIOLOGY_CULTURE_READING");
        query.setStatus("active");
        query.setSort("code:desc");
        query.setPage(5);
        query.setPageSize(100);
        TextMacroPageForm page = service.searchAdmin(query);

        assertEquals(500, page.total);
        assertEquals(100, page.items.size());
        assertEquals(500, generatedIds.size());
        assertTrue(page.items.stream().allMatch(item -> generatedIds.contains(item.id)));
        assertEquals(String.format(".s%s%03d", suffix, 99), page.items.get(0).code);
        assertEquals(String.format(".s%s%03d", suffix, 0), page.items.get(99).code);
    }

    @Test
    public void exportAndBulkOperationsRoundTripThroughOrm() throws IOException {
        String suffix = uniqueSuffix();
        TextMacroAdminForm alpha = service.save(null, request(".a" + suffix, "Alpha, reviewed", "AST",
                Set.of("MICROBIOLOGY_OVERRIDE_REASON", "MICROBIOLOGY_CASE_NOTE"), true), TEST_SYS_USER_ID);
        TextMacroAdminForm zeta = service.save(null,
                request(".z" + suffix, "+ve growth", "CLINICAL", Set.of("MICROBIOLOGY_CLINICAL_HISTORY"), true),
                TEST_SYS_USER_ID);

        try (CSVParser parser = CSVParser.parse(new StringReader(service.exportCsv()),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
            assertEquals(List.of("code", "expansion_text", "category", "contexts", "active"), parser.getHeaderNames());
            List<CSVRecord> exported = parser.getRecords().stream().filter(row -> row.get("code").endsWith(suffix))
                    .toList();
            assertEquals(2, exported.size());
            assertEquals(alpha.code, exported.get(0).get("code"));
            assertEquals("Alpha, reviewed", exported.get(0).get("expansion_text"));
            assertEquals("AST", exported.get(0).get("category"));
            assertEquals("MICROBIOLOGY_CASE_NOTE|MICROBIOLOGY_OVERRIDE_REASON", exported.get(0).get("contexts"));
            assertEquals(zeta.code, exported.get(1).get("code"));
            assertEquals("CLINICAL", exported.get(1).get("category"));
            assertEquals("'+ve growth", exported.get(1).get("expansion_text"));
        }

        TextMacroBulkResultForm deactivated = service.bulk(bulk("DEACTIVATE", alpha.id, zeta.id), TEST_SYS_USER_ID);
        assertEquals(List.of(alpha.code, zeta.code), deactivated.affectedCodes);
        assertFalse(service.getAdmin(alpha.id).active);
        assertFalse(service.getAdmin(zeta.id).active);
        assertEquals(List.of(), offered("MICROBIOLOGY_CLINICAL_HISTORY", suffix));

        TextMacroAdminQueryForm allStatuses = new TextMacroAdminQueryForm();
        allStatuses.setQ(suffix);
        allStatuses.setStatus("all");
        TextMacroPageForm afterBulk = service.searchAdmin(allStatuses);
        assertEquals(2, afterBulk.total);
        assertTrue(afterBulk.items.stream().noneMatch(item -> item.active));

        TextMacroBulkResultForm removed = service.bulk(bulk("DELETE_LOCAL", alpha.id), TEST_SYS_USER_ID);
        assertEquals(List.of(alpha.code), removed.affectedCodes);
        TextMacroRequestException missing = assertThrows(TextMacroRequestException.class,
                () -> service.getAdmin(alpha.id));
        assertEquals("MACRO_NOT_FOUND", missing.getCode());
        assertEquals(zeta.code, service.getAdmin(zeta.id).code);
    }

    @Test
    public void contextLookupReturnsEveryActivePhraseBeyondTheFirstFifty() {
        String suffix = uniqueSuffix();
        for (int index = 0; index < 60; index++) {
            service.save(null, request(String.format(".l%s%02d", suffix, index), "Lookup phrase " + index, "CULTURE",
                    Set.of("MICROBIOLOGY_CULTURE_READING"), true), TEST_SYS_USER_ID);
        }

        List<TextMacroSummaryForm> offered = offered("MICROBIOLOGY_CULTURE_READING", suffix);

        assertEquals(60, offered.size());
        assertEquals(String.format(".l%s%02d", suffix, 0), offered.get(0).code);
        assertEquals(String.format(".l%s%02d", suffix, 59), offered.get(59).code);
        assertEquals("Lookup phrase 59", offered.get(59).expansionText);
    }

    @Test
    public void createEditBulkDeactivateAndRemoveEachWriteHistory() {
        String suffix = uniqueSuffix();
        TextMacroAdminForm created = service.save(null,
                request(".w" + suffix, "First text", "CULTURE", Set.of("MICROBIOLOGY_CASE_NOTE"), true),
                TEST_SYS_USER_ID);
        assertEquals(List.of("I"), activities(created.id));

        service.save(created.id,
                request(created.code, "Edited text", "CULTURE", Set.of("MICROBIOLOGY_CASE_NOTE"), true),
                TEST_SYS_USER_ID);
        assertEquals(List.of("I", "U"), activities(created.id));
        History edit = history(created.id).stream().filter(row -> "U".equals(row.getActivity())).findFirst()
                .orElseThrow();
        assertTrue(new String(edit.getChanges()).contains("First text"));

        service.bulk(bulk("DEACTIVATE", created.id), TEST_SYS_USER_ID);
        assertEquals(List.of("I", "U", "U"), activities(created.id));

        service.bulk(bulk("DELETE_LOCAL", created.id), TEST_SYS_USER_ID);
        assertEquals(List.of("D", "I", "U", "U"), activities(created.id));
    }

    @Test
    public void adminListFallsBackToFirstPageOfTwentyForUnsupportedPaging() {
        String suffix = uniqueSuffix();
        for (int index = 0; index < 25; index++) {
            service.save(null, request(String.format(".p%s%02d", suffix, index), "Paged phrase " + index, "CULTURE",
                    Set.of("MICROBIOLOGY_CASE_NOTE"), true), TEST_SYS_USER_ID);
        }
        TextMacroAdminQueryForm query = new TextMacroAdminQueryForm();
        query.setQ(suffix);
        query.setPage(0);
        query.setPageSize(999);

        TextMacroPageForm page = service.searchAdmin(query);

        assertEquals(1, page.page);
        assertEquals(20, page.pageSize);
        assertEquals(25, page.total);
        assertEquals(20, page.items.size());
        assertEquals(String.format(".p%s%02d", suffix, 0), page.items.get(0).code);
        assertEquals(String.format(".p%s%02d", suffix, 19), page.items.get(19).code);
    }

    @Test
    public void bulkRejectsMissingOrRepeatedIdsWithoutChangingAnyPhrase() {
        String suffix = uniqueSuffix();
        TextMacroAdminForm kept = service.save(null,
                request(".k" + suffix, "Kept phrase", "CULTURE", Set.of("MICROBIOLOGY_CASE_NOTE"), true),
                TEST_SYS_USER_ID);
        String missingId = String.valueOf(Integer.MAX_VALUE);

        TextMacroRequestException missing = assertThrows(TextMacroRequestException.class,
                () -> service.bulk(bulk("DEACTIVATE", kept.id, missingId), TEST_SYS_USER_ID));
        assertEquals("MACRO_NOT_FOUND", missing.getCode());
        TextMacroRequestException repeated = assertThrows(TextMacroRequestException.class,
                () -> service.bulk(bulk("DEACTIVATE", kept.id, kept.id), TEST_SYS_USER_ID));
        assertEquals("DUPLICATE_MACRO_IDS", repeated.getCode());

        assertTrue(service.getAdmin(kept.id).active);
        assertEquals(List.of("I"), activities(kept.id));
    }

    private List<TextMacroSummaryForm> offered(String context, String suffix) {
        return service.findActive(context).stream().filter(macro -> macro.code.contains(suffix)).toList();
    }

    private List<History> history(String macroId) {
        String table = referenceTablesService.getReferenceTableByName("text_macro").getId();
        List<History> rows = historyService.getHistoryByRefIdAndRefTableId(macroId, table);
        for (History row : rows) {
            assertEquals(macroId, row.getReferenceId());
            assertEquals(TEST_SYS_USER_ID, row.getSysUserId());
        }
        return rows;
    }

    private List<String> activities(String macroId) {
        return history(macroId).stream().map(History::getActivity).sorted().toList();
    }

    private TextMacroPageForm search(String q, String category) {
        TextMacroAdminQueryForm query = new TextMacroAdminQueryForm();
        query.setQ(q);
        query.setCategory(category);
        query.setStatus("all");
        return service.searchAdmin(query);
    }

    private List<String> idsOf(TextMacroPageForm page) {
        return page.items.stream().map(item -> item.id).toList();
    }

    private TextMacroAdminForm request(String code, String text, String category, Set<String> contexts,
            boolean active) {
        TextMacroAdminForm request = new TextMacroAdminForm();
        request.code = code;
        request.expansionText = text;
        request.category = category;
        request.contexts = contexts;
        request.active = active;
        return request;
    }

    private String uniqueSuffix() {
        String time = Long.toUnsignedString(System.nanoTime(), 36);
        return time.substring(time.length() - 8);
    }

    private TextMacroBulkRequestForm bulk(String action, String... ids) {
        TextMacroBulkRequestForm request = new TextMacroBulkRequestForm();
        request.action = action;
        request.ids = List.of(ids);
        return request;
    }
}
