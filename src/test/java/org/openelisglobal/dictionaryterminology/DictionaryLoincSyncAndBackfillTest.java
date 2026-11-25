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

    /**
     * The repair statement, run verbatim
     * (020-terminology-mapping-version-backfill): the backfill above wrote no
     * last_updated, the @Version column every Hibernate update is keyed on.
     */
    private static final String REPAIR_SQL = "UPDATE clinlims.dictionary_terminology_mapping"
            + " SET last_updated = COALESCE(lastupdated, now()) WHERE last_updated IS NULL";

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

    /**
     * 020 also lets the version column default on every mapping table, so a later
     * SQL insert that omits it cannot recreate the gap the repair closes.
     */
    @Test
    public void everyTerminologyMappingTableDefaultsItsVersionColumn() {
        List<String> withoutDefault = new ArrayList<>();
        for (String table : List.of("dictionary_terminology_mapping", "test_terminology_mapping",
                "panel_terminology_mapping", "sample_type_terminology_mapping")) {
            String columnDefault = jdbc.queryForObject(
                    "SELECT column_default FROM information_schema.columns WHERE table_schema = 'clinlims'"
                            + " AND table_name = ? AND column_name = 'last_updated'",
                    String.class, table);
            if (columnDefault == null || !columnDefault.contains("now()")) {
                withoutDefault.add(table);
            }
        }
        assertEquals("mapping tables whose last_updated has no default", List.of(), withoutDefault);
    }

    @Test
    public void repairStampsTheVersionOfEveryBackfilledMapping() {
        insertMappingWithoutVersion(LEGACY_ONLY, "LA6576-8");
        assertEquals(Integer.valueOf(1), countWithoutVersion(LEGACY_ONLY));

        jdbc.execute(REPAIR_SQL);

        assertEquals(Integer.valueOf(0), countWithoutVersion(LEGACY_ONLY));
    }

    @Test
    public void aBackfilledMappingCanBeUpdatedThroughTheServiceAfterTheRepair() {
        insertMappingWithoutVersion(LEGACY_ONLY, "LA6576-8");
        jdbc.execute(REPAIR_SQL);
        String answerId = String.valueOf(LEGACY_ONLY);

        mappingService.syncLegacyLoinc(answerId, "LA6576-8", "1");
        mappingService.saveMappingsForDictionary(answerId, List.of(desired("LOINC", "LA6576-8", "SAME_AS", "Positive")),
                "1");

        List<DictionaryTerminologyMapping> active = mappingService.getActiveByDictionaryId(answerId);
        assertEquals(1, active.size());
        assertEquals("Positive", active.get(0).getDisplayName());
        assertEquals(Integer.valueOf(0), countWithoutVersion(LEGACY_ONLY));
    }

    /**
     * A row as the 129 backfill left it on an upgraded database: the SQL named
     * lastupdated only, so the @Version column stayed NULL.
     */
    private void insertMappingWithoutVersion(long dictionaryId, String code) {
        jdbc.update(
                "INSERT INTO clinlims.dictionary_terminology_mapping"
                        + " (id, dictionary_id, source, code, relationship, is_active, lastupdated, last_updated)"
                        + " VALUES (gen_random_uuid()::varchar, ?, 'LOINC', ?, 'SAME_AS', 'Y', now(), NULL)",
                dictionaryId, code);
    }

    private Integer countWithoutVersion(long dictionaryId) {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.dictionary_terminology_mapping"
                + " WHERE dictionary_id = ? AND last_updated IS NULL", Integer.class, dictionaryId);
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
        return desired(source, code, relationship, null);
    }

    private DictionaryTerminologyMapping desired(String source, String code, String relationship, String displayName) {
        DictionaryTerminologyMapping mapping = new DictionaryTerminologyMapping();
        mapping.setSource(source);
        mapping.setCode(code);
        mapping.setRelationship(relationship);
        mapping.setDisplayName(displayName);
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
