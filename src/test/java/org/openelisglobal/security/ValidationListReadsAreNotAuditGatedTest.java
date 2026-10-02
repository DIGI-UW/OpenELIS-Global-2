package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * Showing who recorded a result is part of reviewing it, not audit browsing.
 *
 * <p>
 * {@code ResultsValidationUtility#recordedByFromHistory} falls back to the
 * history rows to fill the "entered by" column of the validation list, and that
 * reaches {@code HistoryService#getHistoryByRefIdAndRefTableId}. While that
 * method required PRIV_AUDIT_VIEW alone - held by the Audit Trail role and
 * nobody else - GET /rest/AccessionValidation answered 403 for the Validation
 * role, so its own main screen listed nothing and a result sitting in Technical
 * Acceptance could not be reached at all.
 *
 * <p>
 * The wider audit browsing on the same interface stays on PRIV_AUDIT_VIEW: the
 * point is that one per-record lookup feeding a results screen is not the audit
 * trail.
 */
public class ValidationListReadsAreNotAuditGatedTest {

    private static final Path HISTORY_SERVICE = Paths
            .get("src/main/java/org/openelisglobal/history/service/HistoryService.java");

    private static String read() throws IOException {
        return new String(Files.readAllBytes(HISTORY_SERVICE), StandardCharsets.UTF_8);
    }

    @Test
    public void theRecordedByLookupAdmitsTheResultReviewingRoles() throws IOException {
        String body = read();

        int signature = body.indexOf("List<History> getHistoryByRefIdAndRefTableId(String Id, String Table)");
        assertTrue("getHistoryByRefIdAndRefTableId(String, String) not found in " + HISTORY_SERVICE, signature >= 0);
        int gate = body.lastIndexOf("@PreAuthorize", signature);
        assertTrue("getHistoryByRefIdAndRefTableId must carry a @PreAuthorize", gate >= 0);
        String expression = body.substring(gate, signature);

        assertTrue("the validation list reads this to fill its 'entered by' column, so PRIV_RESULT_VALIDATE"
                + " must be admitted or /rest/AccessionValidation answers 403 for the Validation role." + " Found: "
                + expression.trim(), expression.contains("PRIV_RESULT_VALIDATE"));
        assertTrue("audit browsing must still be admitted here. Found: " + expression.trim(),
                expression.contains("PRIV_AUDIT_VIEW"));
    }

    @Test
    public void theAuditBrowsingMethodsStayOnAuditView() throws IOException {
        String body = read();

        for (String signature : new String[] { "Map<String, String> getSystemAuditReferenceTableIds();",
                "long getSystemEventHistoryCount(" }) {
            int at = body.indexOf(signature);
            assertTrue(signature + " not found in " + HISTORY_SERVICE, at >= 0);
            int gate = body.lastIndexOf("@PreAuthorize", at);
            String expression = body.substring(gate, at);

            assertTrue(signature + " browses the audit trail itself and must stay on PRIV_AUDIT_VIEW alone;"
                    + " widening the per-record lookup is not licence to widen these. Found: " + expression.trim(),
                    expression.contains("PRIV_AUDIT_VIEW") && !expression.contains("PRIV_RESULT_VALIDATE"));
        }
    }
}
