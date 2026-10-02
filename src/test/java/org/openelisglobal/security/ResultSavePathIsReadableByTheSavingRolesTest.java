package org.openelisglobal.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

/**
 * Saving a result walks the calculated-test definitions, so those reads must
 * admit the roles that save results.
 *
 * <p>
 * {@code LogbookPersistServiceImpl} calls
 * {@code TestCalculatedUtil#addNewTestsToDBForCalculatedTests} for every saved
 * result, because one result may derive another. That loop reads
 * {@link org.openelisglobal.testcalculated.service.TestCalculationService}
 * {@code getAll()} and then
 * {@link org.openelisglobal.testcalculated.service.ResultCalculationService}
 * {@code getResultCalculationByPatientAndCalculation}. Both were gated on
 * administrative privileges that no role holds - PRIV_TEST_CONFIGURE and
 * PRIV_TESTCALC_VIEW - so the Results role's POST to /rest/LogbookResults
 * answered 403 mid-save and no result row was ever written.
 *
 * <p>
 * Configuring a calculation stays administrative. Only reading it is widened,
 * and only to the three authorities that save a result value.
 */
public class ResultSavePathIsReadableByTheSavingRolesTest {

    private static final Path TEST_CALCULATION = Paths
            .get("src/main/java/org/openelisglobal/testcalculated/service/TestCalculationService.java");

    private static final Path RESULT_CALCULATION = Paths
            .get("src/main/java/org/openelisglobal/testcalculated/service/ResultCalculationService.java");

    private static final String[] SAVING_AUTHORITIES = { "PRIV_RESULT_ENTER", "PRIV_RESULT_VALIDATE",
            "PRIV_ANALYZER_IMPORT" };

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    @Test
    public void calculationDefinitionsAreReadableWhileSavingAResult() throws IOException {
        String body = read(TEST_CALCULATION);

        int gate = body.indexOf("@PreAuthorize");
        assertTrue(
                "TestCalculationService must keep a type-level @PreAuthorize: CrudGate evaluates it for"
                        + " the CRUD inherited from BaseObjectService, and getAll() is what the save path calls",
                gate >= 0);
        String expression = body.substring(gate, body.indexOf("public interface"));

        for (String authority : SAVING_AUTHORITIES) {
            assertTrue(
                    "TestCalculationService#getAll is read while saving a result, so " + authority
                            + " must be admitted or that role cannot save at all. Found: " + expression.trim(),
                    expression.contains(authority));
        }
        assertTrue("configuring a calculation stays administrative, so PRIV_TEST_CONFIGURE must remain in"
                + " the expression. Found: " + expression.trim(), expression.contains("PRIV_TEST_CONFIGURE"));
    }

    @Test
    public void derivedResultsAreReadableAndWritableWhileSavingAResult() throws IOException {
        String body = read(RESULT_CALCULATION);

        int named = body.indexOf("List<ResultCalculation> getResultCalculationByPatientAndCalculation(");
        assertTrue("getResultCalculationByPatientAndCalculation not found in " + RESULT_CALCULATION, named >= 0);
        int gate = body.lastIndexOf("@PreAuthorize", named);
        assertTrue("getResultCalculationByPatientAndCalculation must carry a @PreAuthorize", gate >= 0);
        String expression = body.substring(gate, named);

        for (String authority : SAVING_AUTHORITIES) {
            assertTrue(
                    "getResultCalculationByPatientAndCalculation is called from the same loop that saves a"
                            + " result, so " + authority + " must be admitted. Found: " + expression.trim(),
                    expression.contains(authority));
        }

        assertTrue("ResultCalculationService's inherited insert/update are the writes the save path makes."
                + " Without @CrudPrivileges they are OPEN, which is worse than the narrow reads it had;"
                + " declare both sides", body.contains("@CrudPrivileges"));
    }
}
