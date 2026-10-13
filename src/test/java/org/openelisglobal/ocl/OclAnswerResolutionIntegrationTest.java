package org.openelisglobal.ocl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * An OCL import reuses the answer a lab already has, with or without a LOINC.
 */
public class OclAnswerResolutionIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String CATEGORY = "Test Result";
    private static final String ANSWER = "Detected (OCL answer resolution IT)";
    private static final String OTHER = "Not detected (OCL answer resolution IT)";

    @Autowired
    @Qualifier("dictionaryConfigurationHandler")
    private DomainConfigurationHandler dictionaryHandler;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private String existingId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        cleanup();
        String csv = "category,dictEntry,localAbbreviation,isActive,sortOrder,loincCode\n" + CATEGORY + "," + ANSWER
                + ",,Y,1,\n";
        dictionaryHandler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                "answers.csv");
        existingId = dictionaryService.getDictionaryEntryByNameAndCategoryName(ANSWER, CATEGORY).getId();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void anExistingAnswerWhoseConceptHasNoLoincIsTheSavedAnswer() {
        Dictionary answer = new OclToOpenElisMapper("Hematology", "Serum").resolveAnswer(ANSWER, null, "OCLIT-1", "");

        assertEquals(existingId, answer.getId());
    }

    @Test
    public void anExistingAnswerTakesTheLoincItsConceptCarries() {
        Dictionary answer = new OclToOpenElisMapper("Hematology", "Serum").resolveAnswer(ANSWER, null, "OCLIT-1",
                "LA11882-0");

        assertEquals(existingId, answer.getId());
        assertEquals("LA11882-0", dictionaryService.getDictionaryById(existingId).getLoincCode());
    }

    @Test
    public void anExistingAnswerDifferingOnlyInCaseAndSpacesIsTheSavedAnswer() {
        Dictionary answer = new OclToOpenElisMapper("Hematology", "Serum")
                .resolveAnswer("  " + ANSWER.toUpperCase() + " ", null, "OCLIT-1", "");

        assertEquals(existingId, answer.getId());
    }

    @Test
    public void aNameMatchingOneAnswerAndACodeMatchingAnotherIsRefused() throws Exception {
        String csv = "category,dictEntry,localAbbreviation,isActive,sortOrder,loincCode\n" + CATEGORY + "," + OTHER
                + ",OCLIT-2,Y,1,\n";
        dictionaryHandler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                "answers.csv");

        LIMSRuntimeException refused = assertThrows(LIMSRuntimeException.class,
                () -> new OclToOpenElisMapper("Hematology", "Serum").resolveAnswer(ANSWER, null, "OCLIT-2", ""));

        assertTrue(refused.getMessage(), refused.getMessage().contains("OCLIT-2"));
    }

    private void cleanup() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM clinlims.dictionary_terminology_mapping WHERE dictionary_id IN"
                + " (SELECT id FROM clinlims.dictionary WHERE dict_entry IN (?, ?))", ANSWER, OTHER);
        jdbc.update("DELETE FROM clinlims.dictionary WHERE dict_entry IN (?, ?)", ANSWER, OTHER);
    }
}
