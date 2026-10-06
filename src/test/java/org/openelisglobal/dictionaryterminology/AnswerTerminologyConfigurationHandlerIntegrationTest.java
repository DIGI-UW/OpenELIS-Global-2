package org.openelisglobal.dictionaryterminology;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionaryterminology.service.DictionaryTerminologyMappingService;
import org.openelisglobal.dictionaryterminology.valueholder.DictionaryTerminologyMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * An answer's codes in every system arrive by config import: the dictionary
 * CSV's loincCode column and the answer-terminology CSV.
 */
public class AnswerTerminologyConfigurationHandlerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String CATEGORY = "Answer terminology IT";
    private static final String ANSWER = "Detected (answer terminology IT)";

    @Autowired
    @Qualifier("dictionaryConfigurationHandler")
    private DomainConfigurationHandler dictionaryHandler;

    @Autowired
    @Qualifier("answerTerminologyConfigurationHandler")
    private DomainConfigurationHandler answerTerminologyHandler;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryTerminologyMappingService mappingService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        load(dictionaryHandler, "answers.csv", "category,dictEntry,localAbbreviation,isActive,sortOrder,loincCode\n"
                + CATEGORY + "," + ANSWER + ",,Y,1,LA11882-0\n");
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @Test
    public void theDictionaryCsvsLoincCodeBecomesTheAnswersLoincMapping() {
        assertEquals(Map.of("LOINC|LA11882-0", "SAME_AS"), activeMappings());
    }

    @Test
    public void anAnswerTerminologyCsvAddsCodesInOtherSystemsAndMergesOnReimport() throws Exception {
        String header = "category,dictEntry,source,code,relationship,displayName\n";
        load(answerTerminologyHandler, "answer-terminology.csv", header + CATEGORY + "," + ANSWER
                + ",SNOMED,260373001,,Detected\n" + CATEGORY + "," + ANSWER + ",CIEL,1301,SAME_AS,\n");

        load(answerTerminologyHandler, "answer-terminology.csv",
                header + CATEGORY + "," + ANSWER + ",snomed,260373001,BROADER_THAN,Detected\n");

        assertEquals(Map.of("LOINC|LA11882-0", "SAME_AS", "SNOMED|260373001", "BROADER_THAN", "CIEL|1301", "SAME_AS"),
                activeMappings());
    }

    private Map<String, String> activeMappings() {
        String answerId = dictionaryService.getDictionaryEntryByNameAndCategoryName(ANSWER, CATEGORY).getId();
        List<DictionaryTerminologyMapping> active = mappingService.getActiveByDictionaryId(answerId);
        return active.stream().collect(Collectors.toMap(m -> m.getSource() + "|" + m.getCode(),
                DictionaryTerminologyMapping::getRelationship));
    }

    private static void load(DomainConfigurationHandler handler, String fileName, String csv) throws Exception {
        handler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), fileName);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.dictionary_terminology_mapping WHERE dictionary_id IN"
                + " (SELECT d.id FROM clinlims.dictionary d JOIN clinlims.dictionary_category c"
                + " ON d.dictionary_category_id = c.id WHERE c.name = ?)", CATEGORY);
        jdbc.update("DELETE FROM clinlims.dictionary WHERE dictionary_category_id IN"
                + " (SELECT id FROM clinlims.dictionary_category WHERE name = ?)", CATEGORY);
        jdbc.update("DELETE FROM clinlims.dictionary_category WHERE name = ?", CATEGORY);
    }
}
