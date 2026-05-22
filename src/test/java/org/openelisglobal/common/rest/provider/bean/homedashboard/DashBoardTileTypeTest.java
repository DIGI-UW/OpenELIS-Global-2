package org.openelisglobal.common.rest.provider.bean.homedashboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile.TileType;

/**
 * OGC-742 — the dashboard enum {@code ORDERS_PATIALLY_COMPLETED_TODAY} was
 * misspelled (PATIALLY vs PARTIALLY). The rename adds the corrected spelling
 * while keeping the legacy spelling around as a deprecated back-compat alias so
 * existing deployments that haven't picked up the FE rename don't 400 on the
 * path-variable bind.
 */
public class DashBoardTileTypeTest {

    @Test
    public void newCorrectSpellingExists() {
        assertNotNull(TileType.valueOf("ORDERS_PARTIALLY_COMPLETED_TODAY"));
    }

    @Test
    public void legacyMisspellingStillBindsForBackCompat() {
        // If a deployment is mid-rollout and the FE hasn't been re-served,
        // the path /home-dashboard/ORDERS_PATIALLY_COMPLETED_TODAY must
        // still resolve so users don't see a 400.
        assertNotNull(TileType.valueOf("ORDERS_PATIALLY_COMPLETED_TODAY"));
    }

    @Test
    public void enumStreamIncludesBothSpellings() {
        long matches = TileType.stream().filter(
                t -> t.name().endsWith("PARTIALLY_COMPLETED_TODAY") || t.name().endsWith("PATIALLY_COMPLETED_TODAY"))
                .count();
        assertEquals("must expose both the corrected name and the legacy alias", 2L, matches);
    }

    @Test
    public void legacyValueIsAnnotatedDeprecated() throws NoSuchFieldException {
        assertTrue("legacy spelling must be @Deprecated so callers see the warning and migrate",
                TileType.class.getField("ORDERS_PATIALLY_COMPLETED_TODAY").isAnnotationPresent(Deprecated.class));
    }
}
