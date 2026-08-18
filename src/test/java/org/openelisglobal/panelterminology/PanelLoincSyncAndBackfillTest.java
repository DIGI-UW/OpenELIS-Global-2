package org.openelisglobal.panelterminology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panelterminology.service.PanelTerminologyMappingService;
import org.openelisglobal.panelterminology.valueholder.PanelTerminologyMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A panel's LOINC has two homes and they have to agree.
 *
 * <p>
 * {@code panel.loinc} predates {@code panel_terminology_mapping}. FHIR intake
 * still routes electronic orders by the column, while the Panel Editor reads
 * the mapping store — so a code in one and not the other is a code the lab
 * cannot see or, worse, one the editor silently clears by saving its empty set
 * over it.
 *
 * <p>
 * Covers both legs plus the one-off backfill that carries codes across for panels
 * configured before the editor existed. The editor is new, so a panel reaching the
 * backfill has a legacy code and nothing in the store.
 */
public class PanelLoincSyncAndBackfillTest extends BaseWebContextSensitiveTest {

    /**
     * The changeset's statement, run verbatim
     * (084-panel-loinc-terminology-backfill).
     */
    private static final String BACKFILL_SQL = "INSERT INTO clinlims.panel_terminology_mapping"
            + " (id, panel_id, source, code, relationship, is_active, lastupdated)"
            + " SELECT gen_random_uuid()::varchar, p.id, 'LOINC', trim(p.loinc), 'SAME_AS', 'Y', now()"
            + " FROM clinlims.panel p" + " WHERE p.loinc IS NOT NULL AND length(trim(p.loinc)) > 0"
            + " AND NOT EXISTS (SELECT 1 FROM clinlims.panel_terminology_mapping m"
            + " WHERE m.panel_id = p.id AND m.source = 'LOINC' AND m.code = trim(p.loinc))";

    private static final long LEGACY_ONLY = 96401L;
    private static final long NO_LOINC = 96403L;
    private static final long SYNC_TARGET = 96404L;
    /** Keeps each panel's localization id derivable from its own, for cleanup. */
    private static final long LOCALIZATION_OFFSET = 1000L;

    @Autowired
    private PanelTerminologyMappingService mappingService;

    @Autowired
    private PanelService panelService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();

        // Configured before the new editor: a code in the column, nothing mapped.
        insertPanel(LEGACY_ONLY, "BackfillLegacyOnly", "24331-1");
        // Nothing to carry across.
        insertPanel(NO_LOINC, "BackfillNoLoinc", null);
        insertPanel(SYNC_TARGET, "SyncTarget", null);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    // ── the migration ─────────────────────────────────────────────────────────

    @Test
    public void backfill_bringsALegacyCodeIntoTheEditorAsSameAs() {
        jdbc.execute(BACKFILL_SQL);

        List<PanelTerminologyMapping> mappings = mappingService.getActiveByPanelId(String.valueOf(LEGACY_ONLY));

        assertEquals("the legacy code becomes exactly one mapping", 1, mappings.size());
        assertEquals("LOINC", mappings.get(0).getSource());
        assertEquals("24331-1", mappings.get(0).getCode());
        assertEquals("recorded as the identifier, not as EQUIVALENT", "SAME_AS", mappings.get(0).getRelationship());
    }

    @Test
    public void backfill_leavesAPanelWithNoCodeAlone() {
        jdbc.execute(BACKFILL_SQL);

        assertTrue(mappingService.getActiveByPanelId(String.valueOf(NO_LOINC)).isEmpty());
    }

    @Test
    public void backfill_isSafeToRunTwice() {
        jdbc.execute(BACKFILL_SQL);
        int after = mappingService.getActiveByPanelId(String.valueOf(LEGACY_ONLY)).size();

        jdbc.execute(BACKFILL_SQL);

        assertEquals("a second run inserts nothing", after,
                mappingService.getActiveByPanelId(String.valueOf(LEGACY_ONLY)).size());
    }

    // ── legacy column → mappings ──────────────────────────────────────────────

    @Test
    public void aCodeEnteredOnTheLegacyPageShowsUpInTheEditor() {
        mappingService.syncLegacyLoinc(String.valueOf(SYNC_TARGET), "  2345-7  ", "1");

        List<PanelTerminologyMapping> mappings = mappingService.getActiveByPanelId(String.valueOf(SYNC_TARGET));

        assertEquals(1, mappings.size());
        assertEquals("surrounding whitespace is not part of the code", "2345-7", mappings.get(0).getCode());
        assertEquals("SAME_AS", mappings.get(0).getRelationship());
    }

    @Test
    public void changingTheLegacyCodeRetiresTheOneItReplaced() {
        String panelId = String.valueOf(SYNC_TARGET);
        mappingService.syncLegacyLoinc(panelId, "2345-7", "1");

        mappingService.syncLegacyLoinc(panelId, "4548-4", "1");

        List<PanelTerminologyMapping> active = mappingService.getActiveByPanelId(panelId);
        assertEquals("only the current code stays active", 1, active.size());
        assertEquals("4548-4", active.get(0).getCode());
    }

    @Test
    public void clearingTheLegacyCodeRetiresTheMapping() {
        String panelId = String.valueOf(SYNC_TARGET);
        mappingService.syncLegacyLoinc(panelId, "2345-7", "1");

        mappingService.syncLegacyLoinc(panelId, "   ", "1");

        assertTrue(mappingService.getActiveByPanelId(panelId).isEmpty());
    }

    @Test
    public void reAddingACodeReactivatesItRatherThanCollidingOnItsUniqueKey() {
        String panelId = String.valueOf(SYNC_TARGET);
        mappingService.syncLegacyLoinc(panelId, "2345-7", "1");
        mappingService.syncLegacyLoinc(panelId, "", "1");

        mappingService.syncLegacyLoinc(panelId, "2345-7", "1");

        List<PanelTerminologyMapping> active = mappingService.getActiveByPanelId(panelId);
        assertEquals(1, active.size());
        assertEquals("2345-7", active.get(0).getCode());
    }

    @Test
    public void syncingLoincLeavesOtherTerminologySystemsAlone() {
        String panelId = String.valueOf(SYNC_TARGET);
        insertMapping(SYNC_TARGET, "SNOMED", "271649006", "BROADER_THAN");

        mappingService.syncLegacyLoinc(panelId, "2345-7", "1");

        assertTrue("the SNOMED mapping survives", mappingService.getActiveByPanelId(panelId).stream()
                .anyMatch(m -> "SNOMED".equals(m.getSource()) && "271649006".equals(m.getCode())));
    }

    // ── mappings → legacy column ──────────────────────────────────────────────

    @Test
    public void savingASameAsMappingInTheEditorReachesTheLegacyColumn() {
        String panelId = String.valueOf(SYNC_TARGET);

        mappingService.saveMappingsForPanel(panelId, List.of(desired("LOINC", "2345-7", "SAME_AS")), "1");

        assertEquals("2345-7", panelService.getPanelById(panelId).getLoinc());
    }

    @Test
    public void removingTheLastSameAsMappingClearsTheLegacyColumn() {
        String panelId = String.valueOf(SYNC_TARGET);
        mappingService.saveMappingsForPanel(panelId, List.of(desired("LOINC", "2345-7", "SAME_AS")), "1");

        mappingService.saveMappingsForPanel(panelId, new ArrayList<>(), "1");

        assertNull("nothing claims to be the panel's identifier any more",
                panelService.getPanelById(panelId).getLoinc());
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private PanelTerminologyMapping desired(String source, String code, String relationship) {
        PanelTerminologyMapping m = new PanelTerminologyMapping();
        m.setSource(source);
        m.setCode(code);
        m.setRelationship(relationship);
        m.setIsActive("Y");
        return m;
    }

    /**
     * A panel with the localization row it cannot exist without: its display name
     * lives in the generic localization tables and name_localization_id is NOT
     * NULL, which is also what the editor's Localization section reads.
     */
    private void insertPanel(long id, String name, String loinc) {
        long localizationId = id + LOCALIZATION_OFFSET;
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())",
                localizationId, "panel name");
        jdbc.update("INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                + " VALUES (?, ?, 'en', ?, NOW())", localizationId, localizationId, name);
        jdbc.update("INSERT INTO clinlims.panel (id, name, description, is_active, loinc, name_localization_id,"
                + " lastupdated) VALUES (?, ?, ?, 'Y', ?, ?, NOW())", id, name, name, loinc, localizationId);
    }

    private void insertMapping(long panelId, String source, String code, String relationship) {
        jdbc.update(
                "INSERT INTO clinlims.panel_terminology_mapping"
                        + " (id, panel_id, source, code, relationship, is_active, lastupdated, last_updated)"
                        + " VALUES (gen_random_uuid()::varchar, ?, ?, ?, ?, 'Y', NOW(), NOW())",
                panelId, source, code, relationship);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.panel_terminology_mapping WHERE panel_id IN (?, ?, ?)", LEGACY_ONLY,
                NO_LOINC, SYNC_TARGET);
        jdbc.update("DELETE FROM clinlims.panel WHERE id IN (?, ?, ?)", LEGACY_ONLY, NO_LOINC, SYNC_TARGET);
        for (long panel : new long[] { LEGACY_ONLY, NO_LOINC, SYNC_TARGET }) {
            jdbc.update("DELETE FROM clinlims.localization_value WHERE id = ?", panel + LOCALIZATION_OFFSET);
            jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", panel + LOCALIZATION_OFFSET);
        }
    }
}
