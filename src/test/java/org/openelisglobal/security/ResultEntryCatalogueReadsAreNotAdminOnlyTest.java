package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * The result-entry screen reads the catalogue, so those reads cannot require
 * the catalogue-administration privilege.
 *
 * <p>
 * Expanding a row on /Results calls two endpoints that
 * {@code ResultEntryRestController} itself gates {@code hasRole('RESULTS')} -
 * {@code test/{id}/interpretations} and {@code test/{id}/reagents} - to show
 * the technician what each result value means and which reagents the test
 * consumes. Both reached services whose type-level gate demanded
 * PRIV_TEST_CONFIGURE, which no result-entry role holds, so both answered 403
 * on the Results role's own screen.
 *
 * <p>
 * Configuring interpretations and reagent links stays on PRIV_TEST_CONFIGURE.
 * Only the two reads widen, to the same PRIV_CATALOGUE_VIEW that
 * {@code TestResultComponentService#getActiveComponentsByTestId} - called by
 * the same handler one line earlier - already admits.
 */
public class ResultEntryCatalogueReadsAreNotAdminOnlyTest {

    private static final Path INTERPRETATION = Paths.get(
            "src/main/java/org/openelisglobal/testresultinterpretation/service/TestResultInterpretationService.java");

    private static final Path REAGENT_LINK = Paths
            .get("src/main/java/org/openelisglobal/testreagentlink/service/TestReagentLinkService.java");

    private static String gateAbove(Path file, String signature) throws IOException {
        String body = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        int at = body.indexOf(signature);
        assertTrue(signature + " not found in " + file, at >= 0);
        int gate = body.lastIndexOf("@PreAuthorize", at);
        assertTrue(signature + " must carry its own @PreAuthorize; without one the type-level"
                + " PRIV_TEST_CONFIGURE applies and the Results role is denied", gate >= 0);
        return body.substring(gate, at);
    }

    @Test
    public void theInterpretationReadAdmitsTheCatalogueViewer() throws IOException {
        String gate = gateAbove(INTERPRETATION,
                "List<TestResultInterpretation> getActiveByComponentId(String componentId);");

        assertTrue("getActiveByComponentId is read by the result-entry screen, so PRIV_CATALOGUE_VIEW must be"
                + " admitted or /rest/results-entry/test/{id}/interpretations answers 403 for Results." + " Found: "
                + gate.trim(), gate.contains("PRIV_CATALOGUE_VIEW"));
        assertTrue("configuring interpretations stays administrative. Found: " + gate.trim(),
                gate.contains("PRIV_TEST_CONFIGURE"));
    }

    @Test
    public void theReagentLinkReadAdmitsTheCatalogueViewer() throws IOException {
        String gate = gateAbove(REAGENT_LINK, "List<TestReagentLink> getByTestId(String testId);");

        assertTrue(
                "getByTestId is read by the result-entry screen, so PRIV_CATALOGUE_VIEW must be admitted"
                        + " or /rest/results-entry/test/{id}/reagents answers 403 for Results. Found: " + gate.trim(),
                gate.contains("PRIV_CATALOGUE_VIEW"));
        assertTrue("linking reagents to a test stays administrative. Found: " + gate.trim(),
                gate.contains("PRIV_TEST_CONFIGURE"));
    }

    /**
     * Inversion: the writes beside them must NOT have been widened.
     */
    @Test
    public void theAdministrativeWritesStayOnTestConfigure() throws IOException {
        String body = new String(Files.readAllBytes(Paths
                .get("src/main/java/org/openelisglobal/testresultcomponent/service/TestResultComponentService.java")),
                StandardCharsets.UTF_8);

        for (String signature : new String[] { "List<TestResultComponent> saveComponentsForTest(",
                "List<TestResultComponent> saveSampleResults(" }) {
            int at = body.indexOf(signature);
            assertTrue(signature + " not found", at >= 0);
            String gate = body.substring(body.lastIndexOf("@PreAuthorize", at), at);
            assertTrue(
                    signature + " writes the catalogue and must stay on PRIV_TEST_CONFIGURE alone;"
                            + " widening the reads is not licence to widen these. Found: " + gate.trim(),
                    gate.contains("PRIV_TEST_CONFIGURE") && !gate.contains("PRIV_CATALOGUE_VIEW"));
        }
    }
}
