package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.program.service.PathologyCaseRuleException;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;

/**
 * Embedding turns a cassette into the block slides are cut from, and the row
 * keeps its id, designation and barcode through it (FR-6.1).
 *
 * <p>
 * Each embedding leaves one audit entry naming who did it and the transition,
 * and nothing else; what is still outstanding is read from the rows (FR-6.3).
 * Embedding is idempotent, a retired cassette is refused, and a legacy row with
 * no recorded state embeds from cassette.
 */
public class PathologySampleEmbeddingTest extends PathologyCaseTestSupport {

    @Test
    public void embeddingACassette_turnsTheRowIntoABlockAndWritesOneRow() {
        saveBlocks(postedBlock(null, "Bench 2"));
        Map<String, Object> cut = blockDesignated("A1");
        int blockId = idOf(cut);

        Optional<PathologyBlock> embedded = pathologySampleService.embedBlock(blockId, TECHNICIAN_ID);

        assertTrue("a cassette on the case can be embedded", embedded.isPresent());
        Map<String, Object> row = blockRow(blockId);
        assertEquals("the stored row is now a block", "BLOCK", row.get("cassette_state"));
        assertEquals("the block keeps the designation the cassette was cut under", "A1", row.get("designation"));
        assertEquals("and the barcode its label was printed from", cut.get("barcode"), row.get("barcode"));

        List<History> audit = embedAudit();
        assertEquals("one embedding leaves exactly one audit entry", 1, audit.size());
        assertEquals("the entry is on the block itself", String.valueOf(blockId), audit.get(0).getReferenceId());
        assertEquals("the operator is the session user, not a typed field", TECHNICIAN_ID, audit.get(0).getSysUserId());
        Map<String, Object> payload = payloadOf(audit.get(0));
        assertEquals("the verb the one-character activity code cannot carry", "PATHOLOGY_BLOCK_EMBEDDED",
                payload.get("verb"));
        assertEquals("the entry names the block", "A1", payload.get("designation"));
        assertEquals("the state the row left", "CASSETTE", payload.get("from"));
        assertEquals("and the state it reached", "BLOCK", payload.get("to"));
    }

    @Test
    public void threeOfFourEmbeds_leaveTheFourthACassetteAndAuditOnlyTheTransition() {
        // The fixture's legacy block is in use with no recorded state, so it would
        // count as a fifth cassette; retiring it first leaves the case holding only
        // the four cut here, which is the bench situation this test describes.
        pathologySampleService.deactivateBlock(LEGACY_BLOCK_ID, null, TECHNICIAN_ID);
        saveBlocks(postedBlock(null, "Bench 1"), postedBlock(null, "Bench 1"), postedBlock(null, "Bench 1"),
                postedBlock(null, "Bench 1"));

        for (String designation : List.of("A1", "A2", "A3")) {
            pathologySampleService.embedBlock(idOf(blockDesignated(designation)), TECHNICIAN_ID);
        }

        Map<Object, Object> stateOfEachRowInUse = new LinkedHashMap<>();
        for (Map<String, Object> block : blocksOnCase()) {
            if (Boolean.TRUE.equals(block.get("active"))) {
                stateOfEachRowInUse.put(block.get("designation"), block.get("cassette_state"));
            }
        }
        assertEquals("the rows show three blocks and A4 still waiting",
                Map.of("A1", "BLOCK", "A2", "BLOCK", "A3", "BLOCK", "A4", "CASSETTE"), stateOfEachRowInUse);

        List<History> audit = embedAudit();
        assertEquals("three embeddings, three entries", 3, audit.size());
        Map<String, Object> third = payloadOf(audit.get(2));
        assertEquals("the third entry is A3's", "A3", third.get("designation"));
        assertEquals("and records the event alone, no counts that a later retirement would make false",
                Set.of("verb", "designation", "from", "to"), third.keySet());
    }

    @Test
    public void embeddingTwice_changesNothingAndWritesNoSecondRow() {
        saveBlocks(postedBlock(null, "Bench 2"));
        int blockId = idOf(blockDesignated("A1"));
        pathologySampleService.embedBlock(blockId, TECHNICIAN_ID);
        Object versionAfterFirst = lastUpdatedOf(blockId);

        Optional<PathologyBlock> again = pathologySampleService.embedBlock(blockId, TECHNICIAN_ID);

        assertTrue("embedding a block answers with the block rather than failing", again.isPresent());
        assertEquals("the row is still a block", "BLOCK", blockRow(blockId).get("cassette_state"));
        assertEquals("the second call wrote nothing, so the row's version is unchanged", versionAfterFirst,
                lastUpdatedOf(blockId));
        assertEquals("and nothing happened, so nothing is audited a second time", 1, embedAudit().size());
    }

    @Test
    public void aRetiredCassette_isRefused() {
        saveBlocks(postedBlock(null, "Bench 2"));
        int blockId = idOf(blockDesignated("A1"));
        pathologySampleService.deactivateBlock(blockId, "dropped on the floor", TECHNICIAN_ID);

        PathologyCaseRuleException refusal = assertThrows(PathologyCaseRuleException.class,
                () -> pathologySampleService.embedBlock(blockId, TECHNICIAN_ID));

        assertTrue("the refusal names the block: " + refusal.getMessage(),
                refusal.getMessage().contains(String.valueOf(blockId)));
        assertEquals("the retired cassette keeps its state", "CASSETTE", blockRow(blockId).get("cassette_state"));
        assertEquals("and no embedding is audited", 0, embedAudit().size());
    }

    @Test
    public void anUnknownId_isNothingToEmbed() {
        assertTrue("an id no block carries embeds nothing",
                pathologySampleService.embedBlock(424242, TECHNICIAN_ID).isEmpty());
        assertEquals("and nothing is audited", 0, embedAudit().size());
    }

    @Test
    public void aLegacyBlockWithNoState_embedsFromCassette() {
        assertNotNull("the fixture's legacy block is on the case", blockRow(LEGACY_BLOCK_ID));
        assertNull("and carries no state", blockRow(LEGACY_BLOCK_ID).get("cassette_state"));

        pathologySampleService.embedBlock(LEGACY_BLOCK_ID, TECHNICIAN_ID);

        assertEquals("the legacy row is now a block", "BLOCK", blockRow(LEGACY_BLOCK_ID).get("cassette_state"));
        List<History> audit = embedAudit();
        assertEquals("one embedding, one entry", 1, audit.size());
        Map<String, Object> payload = payloadOf(audit.get(0));
        assertEquals("a row with no recorded state had not been embedded, so it left the cassette state", "CASSETTE",
                payload.get("from"));
        assertEquals("and is named by its legacy number", "11", payload.get("designation"));
    }

    // helpers

    /** Every embedding entry on a block of the case, oldest first. */
    private List<History> embedAudit() {
        List<History> embeddings = new ArrayList<>();
        for (Map<String, Object> block : blocksOnCase()) {
            for (History history : historyService.getHistoryByRefIdAndRefTableId(String.valueOf(block.get("id")),
                    blockReferenceTableId)) {
                if (PathologySampleService.PATHOLOGY_BLOCK_EMBEDDED.equals(payloadOf(history).get("verb"))) {
                    embeddings.add(history);
                }
            }
        }
        embeddings.sort(Comparator.comparing(history -> Long.valueOf(history.getId())));
        return embeddings;
    }

    private int idOf(Map<String, Object> blockRow) {
        return ((Number) blockRow.get("id")).intValue();
    }

    private Object lastUpdatedOf(int blockId) {
        return jdbcTemplate.queryForObject("SELECT last_updated FROM clinlims.pathology_block WHERE id = ?",
                Object.class, blockId);
    }
}
