package org.openelisglobal.program;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.UUID;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.program.controller.EditProgramForm;
import org.openelisglobal.program.controller.ProgramController;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.questionnaire.service.QuestionnaireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.WebApplicationContext;

/**
 * Admin → Programme entry and the Add Order questionnaire endpoint must work
 * with no FHIR store configured. The controller package is excluded from the
 * test component scan, so the controller is created against the live context.
 */
public class ProgramControllerLocalQuestionnaireTest extends BaseWebContextSensitiveTest {

    private static final String CODE = "QLOCAL";
    private static final String[] QUESTIONNAIRE_TABLES = { "questionnaire_response_answer",
            "questionnaire_response_item", "questionnaire_response", "questionnaire_item_initial",
            "questionnaire_answer_option", "questionnaire_item", "questionnaire" };

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ProgramService programService;
    @Autowired
    private QuestionnaireService questionnaireService;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private DataSource dataSource;

    private ProgramController controller;

    @Before
    public void setUp() throws Exception {
        reset(fhirConfig);
        when(fhirConfig.getLocalFhirStorePath()).thenReturn("");
        controller = webApplicationContext.getAutowireCapableBeanFactory().createBean(ProgramController.class);
        removeRows();
    }

    @After
    public void tearDown() throws Exception {
        reset(fhirConfig);
        removeRows();
    }

    @Test
    public void saveProgramme_withoutFhirStore_storesQuestionnaireAndServesItToAddOrder() {
        Program program = new Program();
        program.setCode(CODE);
        program.setProgramName("Local questionnaire programme");
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setStatus(PublicationStatus.ACTIVE);
        questionnaire.addItem().setLinkId("site").setText("Sampling site").setType(QuestionnaireItemType.STRING);
        EditProgramForm form = new EditProgramForm();
        form.setProgram(program);
        form.setAdditionalOrderEntryQuestions(questionnaire);

        controller.createProgram(form);

        Program saved = programService.getMatch("code", CODE).orElseThrow();
        UUID questionnaireUuid = saved.getQuestionnaireUUID();
        assertNotNull(questionnaireUuid);
        assertTrue(questionnaireService.getMatch("fhirUuid", questionnaireUuid).isPresent());

        Questionnaire served = controller.getAdditionalEntryQuestions(new MockHttpServletRequest(), saved.getId());
        assertEquals(questionnaireUuid.toString(), served.getIdElement().getIdPart());
        assertEquals("site", served.getItemFirstRep().getLinkId());

        EditProgramForm edit = controller.createProgram(saved.getId());
        assertEquals("site", edit.getAdditionalOrderEntryQuestions().getItemFirstRep().getLinkId());
    }

    private void removeRows() throws Exception {
        cleanRowsInCurrentConnection(QUESTIONNAIRE_TABLES);
        new JdbcTemplate(dataSource).update("DELETE FROM clinlims.program WHERE code = ?", CODE);
    }
}
