package org.openelisglobal.program;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Questionnaire;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.program.service.ProgramAutocreateService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Programme seeding used to be skipped entirely when no FHIR store was
 * configured; the bundled programmes and their questionnaires must now land in
 * the OpenELIS database on their own.
 */
public class ProgramAutocreateWithoutFhirStoreTest extends BaseWebContextSensitiveTest {

    private static final String[] QUESTIONNAIRE_TABLES = { "questionnaire_response_answer",
            "questionnaire_response_item", "questionnaire_response", "questionnaire_item_initial",
            "questionnaire_answer_option", "questionnaire_item", "questionnaire" };

    @Autowired
    private ProgramAutocreateService autocreateService;
    @Autowired
    private ProgramService programService;
    @Autowired
    private QuestionnaireStorageService storage;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private DataSource dataSource;

    private List<JsonNode> bundledProgrammes;
    private ProgramAutocreateService target;

    @Before
    public void setUp() throws Exception {
        target = AopTestUtils.getUltimateTargetObject(autocreateService);
        reset(fhirConfig, fhirUtil);
        when(fhirConfig.getLocalFhirStorePath()).thenReturn("");
        when(fhirUtil.getFhirParser()).thenReturn(FhirContext.forR4Cached().newJsonParser());
        bundledProgrammes = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (Resource resource : new PathMatchingResourcePatternResolver().getResources("classpath*:programs/*.json")) {
            try (InputStream in = resource.getInputStream()) {
                bundledProgrammes.add(mapper.readTree(in));
            }
        }
        removeSeededRows();
    }

    @After
    public void tearDown() throws Exception {
        ReflectionTestUtils.setField(target, "autocreateOn", false);
        reset(fhirConfig, fhirUtil);
        removeSeededRows();
    }

    @Test
    public void autocreate_withoutFhirStore_seedsProgrammesAndStoresTheirQuestionnairesLocally() {
        assertFalse("the bundled programme definitions must be on the classpath", bundledProgrammes.isEmpty());
        ReflectionTestUtils.setField(target, "autocreateOn", true);

        ReflectionTestUtils.invokeMethod(target, "doAutocreateProgram");

        for (JsonNode definition : bundledProgrammes) {
            String code = definition.path("program").path("code").asText();
            Program program = programService.getMatch("code", code)
                    .orElseThrow(() -> new AssertionError("programme " + code + " was not seeded"));
            UUID questionnaireUuid = program.getQuestionnaireUUID();
            assertNotNull(code + " must have a stored questionnaire UUID", questionnaireUuid);
            String configuredUuid = definition.path("program").path("questionnaireUUID").asText();
            if (!configuredUuid.isBlank()) {
                assertEquals(code, UUID.fromString(configuredUuid), questionnaireUuid);
            }

            Questionnaire questionnaire = storage.getQuestionnaire(program.getQuestionnaireUUID())
                    .orElseThrow(() -> new AssertionError("questionnaire for " + code + " is not in the database"));
            assertEquals(questionnaireUuid.toString(), questionnaire.getIdElement().getIdPart());
            assertEquals(definition.path("additionalOrderEntryQuestions").path("item").size(),
                    questionnaire.getItem().size());
            assertTrue(questionnaire.hasStatus());
            if (!definition.hasNonNull("additionalOrderEntryQuestions")) {
                assertEquals(PublicationStatus.DRAFT, questionnaire.getStatus());
            }
        }
    }

    private void removeSeededRows() throws Exception {
        cleanRowsInCurrentConnection(QUESTIONNAIRE_TABLES);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (JsonNode definition : bundledProgrammes) {
            jdbc.update("DELETE FROM clinlims.program WHERE code = ?",
                    definition.path("program").path("code").asText());
        }
    }
}
