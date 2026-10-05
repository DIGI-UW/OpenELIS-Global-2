package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * Creating a vector sampling site during order entry is not organization
 * administration.
 *
 * <p>
 * Saving a sampling site mirrors it into the Organization table, and that
 * mirror reaches {@code insertUnchecked}, {@code updateUnchecked} and
 * {@code linkOrganizationAndType} - all gated on PRIV_ORGANIZATION_MANAGE,
 * which is granted to NO seeded role. So nobody could create a sampling site at
 * all, though Reception creates one from the order-entry form and
 * {@code VectorSamplingSiteService#resolveOrCreateForOrder} explicitly admits
 * PRIV_ORDER_CREATE for exactly that.
 *
 * <p>
 * The mirror therefore runs in system context. The caller's own access is still
 * decided at the entry point, so this does not widen who may save a site; it
 * only stops the internal write denying them halfway through.
 */
public class SamplingSiteSyncDoesNotNeedOrganizationAdminTest {

    private static final Path SYNC = Paths
            .get("src/main/java/org/openelisglobal/vector/service/SamplingSiteOrganizationSync.java");

    private static final Path SERVICE = Paths
            .get("src/main/java/org/openelisglobal/vector/service/VectorSamplingSiteService.java");

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    @Test
    public void theOrganizationMirrorRunsInSystemContext() throws IOException {
        String body = read(SYNC);

        assertTrue("the organization mirror must run in system context, or PRIV_ORGANIZATION_MANAGE - held by"
                + " no role - denies every sampling-site save", body.contains("SystemContext.runAsSystem"));
        assertTrue("syncFromSite must still be the entry point that wraps the work, so no caller reaches the"
                + " organization writes outside the wrapper", body.contains("public void syncFromSite("));
    }

    /**
     * The inversion: system context is only acceptable because the caller is gated
     * before it is entered.
     */
    @Test
    public void theCallerIsStillGatedAtTheEntryPoint() throws IOException {
        String body = read(SERVICE);

        int at = body.indexOf("resolveOrCreateForOrder(");
        assertTrue("resolveOrCreateForOrder not found in " + SERVICE, at >= 0);
        int gate = body.lastIndexOf("@PreAuthorize", at);
        assertTrue("resolveOrCreateForOrder MUST carry its own gate: it is the only thing standing between an"
                + " unauthorized caller and the system-context organization writes", gate >= 0);
        String expression = body.substring(gate, at);

        assertTrue("the order-entry caller (Reception, via order:create) must be admitted here, since that is"
                + " the access the system-context mirror relies on having been checked. Found: " + expression.trim(),
                expression.contains("PRIV_ORDER_CREATE"));
    }
}
