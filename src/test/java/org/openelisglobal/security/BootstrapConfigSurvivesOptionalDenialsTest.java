package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * The SPA's configuration bootstrap must not 403 because ONE of its fields is
 * gated more narrowly than the endpoint.
 *
 * <p>
 * {@code GET /rest/configuration-properties} is fetched by every authenticated
 * user as the page loads, and the SPA has no config context until it answers.
 * One of the values it assembles, {@code customCriticalMessage}, comes from
 * {@link org.openelisglobal.result.service.ResultEntryAcknowledgementService},
 * which is correctly gated on the three authorities that save a result value.
 * While that denial was allowed out of the handler, every role without one of
 * them (Reports, for one) got 403 for the whole map and the patient report form
 * could not render at all.
 *
 * <p>
 * The field has exactly one consumer, the critical-value acknowledgement modal
 * on Results Entry, whose users hold the privilege. So the fix is to drop the
 * field for everyone else rather than widen the service gate, which would hand
 * the message to roles that have no use for it. Same shape as
 * {@link PostCommitReadsDoNotFailTheSaveTest}: a denial on an optional
 * decoration is logged and dropped, a denial on the thing the caller asked for
 * is a 403.
 */
public class BootstrapConfigSurvivesOptionalDenialsTest {

    private static final Path CONTROLLER = Paths
            .get("src/main/java/org/openelisglobal/common/rest/DisplayListController.java");

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    @Test
    public void criticalMessageDenialDoesNotFailTheWholeConfigMap() throws IOException {
        String body = read(CONTROLLER);

        int start = body.indexOf("private String customCriticalMessageOrBlank()");
        assertTrue("customCriticalMessageOrBlank not found in " + CONTROLLER + "; the critical message must"
                + " still be fetched through something that tolerates a denial, because"
                + " /rest/configuration-properties is loaded by every role", start >= 0);
        int end = body.indexOf("\n    }", start);
        assertTrue("could not find the end of customCriticalMessageOrBlank", end > start);
        String method = body.substring(start, end);

        assertTrue(
                "customCriticalMessageOrBlank must CATCH AccessDeniedException so roles without"
                        + " PRIV_RESULT_ENTER/VALIDATE/ANALYZER_IMPORT still get their configuration map",
                method.contains("catch (AccessDeniedException"));
        assertTrue("customCriticalMessageOrBlank must not rethrow the denial: it is one optional field of a"
                + " map that every authenticated role fetches on page load, and a 403 here leaves the SPA"
                + " with no configuration at all", !method.contains("throw denied"));
    }

    @Test
    public void theCriticalMessageItselfIsReadableByAnyAuthenticatedRole() throws IOException {
        String body = new String(
                Files.readAllBytes(Paths
                        .get("src/main/java/org/openelisglobal/result/service/ResultEntryAcknowledgementService.java")),
                StandardCharsets.UTF_8);

        int signature = body.indexOf("String getCustomCriticalMessage();");
        assertTrue("getCustomCriticalMessage not found in ResultEntryAcknowledgementService", signature >= 0);
        int gate = body.lastIndexOf("@PreAuthorize", signature);
        String expression = gate < 0 ? "(none)" : body.substring(gate, signature);

        assertTrue(
                "getCustomCriticalMessage is published in the SPA's bootstrap map, which every"
                        + " authenticated role fetches on page load. Requiring a result privilege for it took the"
                        + " whole map down with a 403. It is a static administrator-typed string, not patient or"
                        + " result data, so it stays readable by any authenticated caller. Found: " + expression.trim(),
                expression.contains("isAuthenticated()"));
    }

    @Test
    public void theCriticalMessageIsStillReadThroughTheGatedService() throws IOException {
        String body = read(CONTROLLER);
        int start = body.indexOf("private String customCriticalMessageOrBlank()");
        int end = body.indexOf("\n    }", start);
        String method = body.substring(start, Math.max(end, start));

        assertTrue(
                "the message must still come from acknowledgementService, so the gate keeps deciding who"
                        + " actually receives it; swallowing the denial is not licence to read it another way",
                method.contains("acknowledgementService.getCustomCriticalMessage()"));
    }
}
