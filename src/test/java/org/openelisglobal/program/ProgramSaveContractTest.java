package org.openelisglobal.program;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.program.controller.EditProgramForm;
import org.openelisglobal.program.controller.ProgramController;
import org.openelisglobal.program.service.ProgramPickerRules;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

/**
 * Programs V2 (OGC-781): the additive {@code domain} / {@code active} /
 * {@code labUnitIds} fields on the existing program save endpoint, the
 * lifecycle contract that a payload naming none of them keeps what is
 * persisted, and the admin list / order-count feeds.
 */
public class ProgramSaveContractTest extends BaseWebContextSensitiveTest {

    private static final String[] QUESTIONNAIRE_TABLES = { "questionnaire_response_answer",
            "questionnaire_response_item", "questionnaire_response", "questionnaire_item_initial",
            "questionnaire_answer_option", "questionnaire_item", "questionnaire" };

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ProgramService programService;
    @Autowired
    private QuestionnaireStorageService questionnaireStorageService;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private DataSource dataSource;
    @Autowired
    @Qualifier("auditTrailService")
    private AuditTrailService auditTrailServiceMock;

    private ProgramController controller;
    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        reset(fhirConfig);
        when(fhirConfig.getLocalFhirStorePath()).thenReturn("");
        executeDataSetWithStateManagement("testdata/program.xml");
        resyncSequence("clinlims.program_seq", "clinlims.program");
        ensureReferenceTables("program");
        ensureAuditSystemUser();
        controller = webApplicationContext.getAutowireCapableBeanFactory().createBean(ProgramController.class);
        jdbc = new JdbcTemplate(dataSource);
        cleanRowsInCurrentConnection(QUESTIONNAIRE_TABLES);
    }

    @After
    public void tearDown() throws Exception {
        reset(fhirConfig);
        cleanRowsInCurrentConnection(QUESTIONNAIRE_TABLES);
        jdbc.update("DELETE FROM clinlims.program WHERE code LIKE 'V2T%'");
    }

    @Test
    public void create_persistsDomainLifecycleAndEveryLabUnit() {
        EditProgramForm form = newForm("V2TNEW", "V2 new program");
        form.setDomain("ENVIRONMENTAL");
        form.setActive(false);
        form.setLabUnitIds(Arrays.asList("2", "1"));

        EditProgramForm saved = controller.createProgram(form);

        Program program = programService.getMatch("code", "V2TNEW").orElseThrow();
        assertEquals("ENVIRONMENTAL", program.getDomain());
        assertEquals("N", program.getIsActive());
        assertEquals(Arrays.asList("1", "2"), ProgramPickerRules.labUnitIds(program));
        assertEquals("the legacy FK mirrors the lowest lab unit", "1", program.getTestSection().getId());
        assertEquals(2, junctionRows(program.getId()));
        assertEquals(Arrays.asList("1", "2"), saved.getLabUnitIds());
        assertEquals(Boolean.FALSE, saved.getActive());
        assertEquals("ENVIRONMENTAL", saved.getDomain());
    }

    @Test
    public void update_withoutProgramsV2Fields_keepsDomainLifecycleLabUnitsAndQuestionnaire() {
        Program created = create("V2TKEEP", "V2 keep", "ENVIRONMENTAL", false, Arrays.asList("2", "3"),
                questionnaireWithOneItem());
        UUID questionnaireUuid = created.getQuestionnaireUUID();

        EditProgramForm legacyShaped = new EditProgramForm();
        Program body = new Program();
        body.setId(created.getId());
        body.setProgramName("V2 keep renamed");
        body.setCode("V2TKEEP");
        legacyShaped.setProgram(body);

        EditProgramForm response = controller.createProgram(legacyShaped);

        Program after = programService.get(created.getId());
        assertEquals("V2 keep renamed", after.getProgramName());
        assertEquals("ENVIRONMENTAL", after.getDomain());
        assertEquals("a save that does not mention the lifecycle must not reactivate", "N", after.getIsActive());
        assertEquals(Arrays.asList("2", "3"), ProgramPickerRules.labUnitIds(after));
        assertEquals(2, junctionRows(after.getId()));
        assertEquals(questionnaireUuid, after.getQuestionnaireUUID());
        assertEquals("the stored questionnaire is left alone when the payload carries none", 1,
                questionnaireStorageService.getQuestionnaire(questionnaireUuid).orElseThrow().getItem().size());
        assertEquals(1, response.getAdditionalOrderEntryQuestions().getItem().size());
        assertEquals(Boolean.FALSE, response.getActive());
    }

    @Test
    public void update_withExplicitLifecycleFlag_flipsIt() {
        Program created = create("V2TFLIP", "V2 flip", "CLINICAL", true, Collections.singletonList("1"), null);

        EditProgramForm flip = identityForm(created);
        flip.setActive(false);
        controller.createProgram(flip);
        assertEquals("N", programService.get(created.getId()).getIsActive());

        EditProgramForm back = identityForm(created);
        back.setActive(true);
        controller.createProgram(back);
        assertEquals("Y", programService.get(created.getId()).getIsActive());
        assertEquals("the flip keeps the lab units", Collections.singletonList("1"),
                ProgramPickerRules.labUnitIds(programService.get(created.getId())));
    }

    @Test
    public void update_withEmptyLabUnitIds_clearsTheUnits() {
        Program created = create("V2TCLR", "V2 clear", "CLINICAL", true, Collections.singletonList("1"), null);

        EditProgramForm form = identityForm(created);
        form.setLabUnitIds(Collections.emptyList());
        controller.createProgram(form);

        Program after = programService.get(created.getId());
        assertTrue(ProgramPickerRules.labUnitIds(after).isEmpty());
        assertNull(after.getTestSection());
        assertEquals(0, junctionRows(after.getId()));
    }

    @Test
    public void update_withOnlyTheLegacyTestSectionId_fillsTheJunction() {
        Program created = create("V2TLEG", "V2 legacy", "CLINICAL", true, Collections.emptyList(), null);

        EditProgramForm form = identityForm(created);
        form.setTestSectionId("4");
        controller.createProgram(form);

        Program after = programService.get(created.getId());
        assertEquals(Collections.singletonList("4"), ProgramPickerRules.labUnitIds(after));
        assertEquals("4", after.getTestSection().getId());
        assertEquals(1, junctionRows(after.getId()));
    }

    /**
     * The {@code clinlims.history} INSERT itself cannot be asserted here because
     * {@code AuditTrailService} is mocked in the Spring test profile; what this
     * proves is that a program save is audited with the old and new state (the real
     * row was verified on the dev stack).
     */
    @Test
    public void domainChange_isAuditedAndAcceptsLegacyCodes() {
        Program created = create("V2THIST", "V2 history", "CLINICAL", true, Collections.singletonList("1"), null);
        AuditTrailService auditMock = AopTestUtils.getTargetObject(auditTrailServiceMock);
        reset(auditMock);

        EditProgramForm form = identityForm(created);
        form.setDomain("V");
        controller.createProgram(form);

        assertEquals("VECTOR", programService.get(created.getId()).getDomain());
        ArgumentCaptor<BaseObject> newState = ArgumentCaptor.forClass(BaseObject.class);
        ArgumentCaptor<BaseObject> oldState = ArgumentCaptor.forClass(BaseObject.class);
        verify(auditMock).saveHistory(newState.capture(), oldState.capture(), anyString(),
                eq(IActionConstants.AUDIT_TRAIL_UPDATE), argThat(table -> "program".equalsIgnoreCase(table)));
        assertEquals("VECTOR", ((Program) newState.getValue()).getDomain());
        assertEquals("CLINICAL", ((Program) oldState.getValue()).getDomain());
    }

    @Test
    public void unknownDomain_isRejected() {
        EditProgramForm form = newForm("V2TBAD", "V2 bad domain");
        form.setDomain("PLANETARY");
        try {
            controller.createProgram(form);
            fail("expected a 400");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getStatusCode().value());
        }
        assertTrue(programService.getMatch("code", "V2TBAD").isEmpty());
    }

    @Test
    public void listPrograms_carriesDomainStatusAndLabUnitsPerRow() {
        Program created = create("V2TLIST", "V2 list", "ENVIRONMENTAL", false, Arrays.asList("3", "2"), null);

        Map<String, Object> row = controller.listPrograms().stream().filter(r -> created.getId().equals(r.get("id")))
                .findFirst().orElseThrow();

        assertEquals("V2 list", row.get("name"));
        assertEquals("V2TLIST", row.get("code"));
        assertEquals("ENVIRONMENTAL", row.get("domain"));
        assertEquals(Boolean.FALSE, row.get("active"));
        assertEquals(Arrays.asList("2", "3"), row.get("labUnitIds"));
    }

    @Test
    public void orderCount_isZeroForAnUnusedProgramAndRejectsNonNumericIds() {
        Program created = create("V2TCNT", "V2 count", "CLINICAL", true, Collections.emptyList(), null);
        assertEquals(0L, controller.getOrderCount(created.getId()).get("count"));
        try {
            controller.getOrderCount("abc");
            fail("expected a 400");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), e.getStatusCode().value());
        }
    }

    @Test
    public void getUnknownProgram_isA404() {
        try {
            controller.createProgram("999999");
            fail("expected a 404");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.NOT_FOUND.value(), e.getStatusCode().value());
        }
    }

    private Program create(String code, String name, String domain, boolean active, List<String> labUnitIds,
            Questionnaire questionnaire) {
        EditProgramForm form = newForm(code, name);
        form.setDomain(domain);
        form.setActive(active);
        form.setLabUnitIds(labUnitIds);
        form.setAdditionalOrderEntryQuestions(questionnaire);
        controller.createProgram(form);
        return programService.getMatch("code", code).orElseThrow();
    }

    private EditProgramForm newForm(String code, String name) {
        Program program = new Program();
        program.setCode(code);
        program.setProgramName(name);
        EditProgramForm form = new EditProgramForm();
        form.setProgram(program);
        return form;
    }

    private EditProgramForm identityForm(Program persisted) {
        Program body = new Program();
        body.setId(persisted.getId());
        body.setProgramName(persisted.getProgramName());
        body.setCode(persisted.getCode());
        EditProgramForm form = new EditProgramForm();
        form.setProgram(body);
        return form;
    }

    private Questionnaire questionnaireWithOneItem() {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setStatus(PublicationStatus.ACTIVE);
        questionnaire.addItem().setLinkId("q1").setText("Sampling point").setType(QuestionnaireItemType.STRING);
        return questionnaire;
    }

    private int junctionRows(String programId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM clinlims.program_lab_unit WHERE program_id = ?", Integer.class,
                Integer.valueOf(programId));
    }

}
