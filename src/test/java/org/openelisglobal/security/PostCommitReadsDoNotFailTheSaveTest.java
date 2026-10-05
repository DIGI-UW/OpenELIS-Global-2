package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * A gated read that runs AFTER the write has already committed must not answer
 * 403, because the write it would be reporting on has succeeded.
 *
 * <p>
 * SamplePatientEntryRestController assembles its save response by reading back
 * the order it just persisted: rangeNotAppliedTests lists the ordered tests
 * whose reference range will not apply, for a non-blocking warning. That read
 * reaches AnalysisService#getAnalysesBySampleId. While that method required
 * PRIV_RESULT_VIEW alone, Reception — which holds order:create and may enter
 * the order — was denied, and the handler rethrew the AccessDeniedException.
 * The order was already in the database, so the caller saw 403 for a save that
 * had succeeded and re-sent it.
 *
 * <p>
 * This is the one place the usual rule is inverted. Elsewhere a controller that
 * swallows AccessDeniedException in a broad catch is the bug, and
 * {@link ControllerDenialRelabelRatchetTest} drives those out: a denial on a
 * read the caller asked for IS a 403. Here the caller asked for a save, the
 * save happened, and the denial concerns a decoration on the response — so it
 * is logged and dropped. Both halves are pinned here so neither is "tidied"
 * back into the other's shape.
 */
public class PostCommitReadsDoNotFailTheSaveTest {

    private static final Path CONTROLLER = Paths
            .get("src/main/java/org/openelisglobal/sample/controller/rest/SamplePatientEntryRestController.java");

    private static final Path ANALYSIS_SERVICE = Paths
            .get("src/main/java/org/openelisglobal/analysis/service/AnalysisService.java");

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /**
     * The post-commit warning must swallow the denial. A {@code throw denied;}
     * inside rangeNotAppliedTests turns a committed save into a 403.
     */
    @Test
    public void rangeNotAppliedTestsDoesNotRethrowTheDenial() throws IOException {
        String body = read(CONTROLLER);
        int start = body.indexOf("private List<String> rangeNotAppliedTests(");
        assertTrue("rangeNotAppliedTests not found in " + CONTROLLER + "; if it was renamed, move this"
                + " test with it rather than deleting it", start >= 0);
        int end = body.indexOf("\n    }", start);
        assertTrue("could not find the end of rangeNotAppliedTests", end > start);
        String method = body.substring(start, end);

        assertTrue(
                "rangeNotAppliedTests must CATCH AccessDeniedException, so a role that may create the"
                        + " order but not read its analyses still gets its save response",
                method.contains("AccessDeniedException"));
        assertTrue("rangeNotAppliedTests must not rethrow the denial: it runs after the order has already"
                + " committed, so a 403 here reports failure for a save that succeeded and the caller"
                + " re-sends it, creating a duplicate order", !method.contains("throw denied"));
    }

    /**
     * The read itself is which tests were ordered, not their results, so the
     * order-entry roles must be able to make it.
     */
    @Test
    public void analysesBySampleIdIsReadableByTheOrderRoles() throws IOException {
        String body = read(ANALYSIS_SERVICE);
        int signature = body.indexOf("List<Analysis> getAnalysesBySampleId(String id);");
        assertTrue("getAnalysesBySampleId not found in " + ANALYSIS_SERVICE, signature >= 0);

        // The annotation immediately preceding the signature.
        int gateStart = body.lastIndexOf("@PreAuthorize", signature);
        assertTrue("getAnalysesBySampleId must carry a @PreAuthorize", gateStart >= 0);
        String gate = body.substring(gateStart, signature);

        assertTrue("getAnalysesBySampleId returns WHICH TESTS were ordered on a sample, not their result"
                + " values, and the order-save path reads it to build its response. Gating it on"
                + " PRIV_RESULT_VIEW alone denies Reception mid-save. Keep PRIV_ORDER_VIEW here; reading"
                + " actual result values stays on PRIV_RESULT_VIEW elsewhere in the interface." + " Found: "
                + gate.trim(), gate.contains("PRIV_ORDER_VIEW"));
    }
}
