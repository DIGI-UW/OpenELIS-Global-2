package org.openelisglobal.dictionaryterminology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionaryterminology.service.DictionaryTerminologyMappingService;
import org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * An answer's LOINC has two homes and they have to agree: the legacy
 * {@code dictionary.loinc_code} column, still written by the dictionary CSV and
 * Dictionary Management, and the terminology mappings that carry every system.
 */
public class DictionaryLoincSyncAndBackfillTest extends BaseWebContextSensitiveTest {

    /**
     * The changeset's statement, run verbatim (129-dictionary-terminology-mapping).
     */
    private static final String BACKFILL_SQL = "INSERT INTO clinlims.dictionary_terminology_mapping"
            + " (id, dictionary_id, source, code, relationship, is_active, lastupdated)"
            + " SELECT gen_random_uuid()::varchar, d.id, 'LOINC', trim(d.loinc_code), 'SAME_AS', 'Y', now()"
            + " FROM clinlims.dictionary d WHERE d.loinc_code IS NOT NULL AND length(trim(d.loinc_code)) > 0"
            + " AND NOT EXISTS (SELECT 1 FROM clinlims.dictionary_terminology_mapping m"
            + " WHERE m.dictionary_id = d.id AND m.source = 'LOINC' AND m.code = trim(d.loinc_code))";

    private static final long LEGACY_ONLY = 96501L;
    private static final long NO_LOINC = 96502L;
    private static final long SYNC_TARGET = 96503L;

    @Autowired
    private DictionaryTerminologyMappingService mappingService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        insertAnswer(LEGACY_ONLY, "Positive (backfill)", "LA6576-8");
        insertAnswer(NO_LOINC, "No code (backfill)", null);
        insertAnswer(SYNC_TARGET, "Sync target", null);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void backfillBringsAnAnswersLegacyCodeInAsSameAs() {
        jdbc.execute(BACKFILL_SQL);

        List<DictionaryTerminologyMapping> mappings = mappingService
                .getActiveByDictionaryId(String.valueOf(LEGACY_ONLY));

        assertEquals(1, mappings.size());
        assertEquals("LOINC", mappings.get(0).getSource());
        assertEquals("LA6576-8", mappings.get(0).getCode());
        assertEquals("SAME_AS", mappings.get(0).getRelationship());
    }

    @Test
    public void backfillLeavesAnAnswerWithNoCodeAloneAndIsSafeToRunTwice() {
        jdbc.execute(BACKFILL_SQL);
        jdbc.execute(BACKFILL_SQL);

        assertTrue(mappingService.getActiveByDictionaryId(String.valueOf(NO_LOINC)).isEmpty());
        assertEquals(1, mappingService.getActiveByDictionaryId(String.valueOf(LEGACY_ONLY)).size());
    }

    @Test
    public void aCodeWrittenToTheLegacyColumnBecomesTheAnswersLoincMapping() {
        String answerId = String.valueOf(SYNC_TARGET);
        mappingService.syncLegacyLoinc(answerId, " LA6576-8 ", "1");

        mappingService.syncLegacyLoinc(answerId, "LA6577-6", "1");

        List<DictionaryTerminologyMapping> active = mappingService.getActiveByDictionaryId(answerId);
        assertEquals(1, active.size());
        assertEquals("LA6577-6", active.get(0).getCode());
        assertEquals("SAME_AS", active.get(0).getRelationship());
    }

    @Test
    public void syncingLoincLeavesTheAnswersOtherSystemsAlone() {
        String answerId = String.valueOf(SYNC_TARGET);
        mappingService.saveMappingsForDictionary(answerId, List.of(desired("SNOMED", "10828004", "SAME_AS")), "1");

        mappingService.syncLegacyLoinc(answerId, "LA6576-8", "1");

        assertTrue(mappingService.getActiveByDictionaryId(answerId).stream()
                .anyMatch(m -> "SNOMED".equals(m.getSource()) && "10828004".equals(m.getCode())));
    }

    @Test
    public void savingTheAnswersMappingsKeepsTheLegacyColumnInStep() {
        String answerId = String.valueOf(SYNC_TARGET);

        mappingService.saveMappingsForDictionary(answerId,
                List.of(desired("SNOMED", "10828004", "SAME_AS"), desired("LOINC", "LA6576-8", "SAME_AS")), "1");
        assertEquals("LA6576-8", dictionaryService.get(answerId).getLoincCode());

        mappingService.saveMappingsForDictionary(answerId, new ArrayList<>(), "1");
        assertNull(dictionaryService.get(answerId).getLoincCode());
    }

    private DictionaryTerminologyMapping desired(String source, String code, String relationship) {
        DictionaryTerminologyMapping mapping = new DictionaryTerminologyMapping();
        mapping.setSource(source);
        mapping.setCode(code);
        mapping.setRelationship(relationship);
        return mapping;
    }

    private void insertAnswer(long id, String entry, String loinc) {
        jdbc.update("INSERT INTO clinlims.dictionary (id, dict_entry, is_active, loinc_code, lastupdated)"
                + " VALUES (?, ?, 'Y', ?, NOW())", id, entry, loinc);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.dictionary_terminology_mapping WHERE dictionary_id IN (?, ?, ?)", LEGACY_ONLY,
                NO_LOINC, SYNC_TARGET);
        jdbc.update("DELETE FROM clinlims.dictionary WHERE id IN (?, ?, ?)", LEGACY_ONLY, NO_LOINC, SYNC_TARGET);
    }
}
