package org.openelisglobal.questionnaire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.questionnaire.service.QuestionnaireResponseService;
import org.openelisglobal.questionnaire.service.QuestionnaireService;
import org.openelisglobal.questionnaire.valueholder.Questionnaire;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireResponseAnswer;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireResponseItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class QuestionnaireResponseServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QuestionnaireResponseService questionnaireResponseService;

    @Autowired
    private QuestionnaireService questionnaireService;

    @Autowired
    private PatientService patientService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/questionnaire-response.xml");
    }

    @Test
    public void getAllShouldReturnAllQuestionnaireResponses() {

        List<QuestionnaireResponse> responses = questionnaireResponseService.getAll();

        assertEquals(3, responses.size());

        responses.sort(Comparator.comparing(QuestionnaireResponse::getId));

        assertEquals(QuestionnaireResponseStatus.COMPLETED, responses.get(0).getStatus());

        assertEquals(QuestionnaireResponseStatus.COMPLETED, responses.get(1).getStatus());

        assertEquals(QuestionnaireResponseStatus.IN_PROGRESS, responses.get(2).getStatus());
    }

    @Test
    public void insert_shouldInsertNewQuestionnaireResponse() throws Exception {

        cleanRowsInCurrentConnection(new String[] { "questionnaire_response_answer", "questionnaire_response_item",
                "questionnaire_response" });

        Questionnaire questionnaire = questionnaireService.get(1);

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setQuestionnaire(questionnaire);
        response.setFhirUuid(UUID.randomUUID());
        response.setStatus(QuestionnaireResponseStatus.COMPLETED);
        response.setSummary("New questionnaire response");
        response.setAuthored(new Timestamp(System.currentTimeMillis()));

        Patient patient = patientService.get("1");
        response.setSubject(patient);

        questionnaireResponseService.insert(response);

        List<QuestionnaireResponse> responses = questionnaireResponseService.getAll();

        assertEquals(1, responses.size());

        QuestionnaireResponse saved = responses.get(0);

        assertEquals("New questionnaire response", saved.getSummary());

        assertEquals(QuestionnaireResponseStatus.COMPLETED, saved.getStatus());
    }

    @Test
    public void save_shouldSaveQuestionnaireResponseItems() throws Exception {

        cleanRowsInCurrentConnection(new String[] { "questionnaire_response_answer", "questionnaire_response_item",
                "questionnaire_response" });

        Questionnaire questionnaire = questionnaireService.get(1);
        Patient patient = patientService.get("1");

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setQuestionnaire(questionnaire);
        response.setFhirUuid(UUID.randomUUID());
        response.setStatus(QuestionnaireResponseStatus.COMPLETED);
        response.setAuthored(new Timestamp(System.currentTimeMillis()));
        response.setSubject(patient);

        QuestionnaireResponseItem item = new QuestionnaireResponseItem();
        item.setQuestionnaireResponse(response);
        item.setLinkId("1");
        item.setText("Pregnancy Status");

        response.setItems(new HashSet<>());
        response.getItems().add(item);

        questionnaireResponseService.insert(response);

        QuestionnaireResponse saved = questionnaireResponseService.getAll().get(0);

        assertEquals(1, saved.getItems().size());

        QuestionnaireResponseItem savedItem = saved.getItems().iterator().next();

        assertEquals("Pregnancy Status", savedItem.getText());
    }

    @Test
    public void get_ShouldReturnQuestionnaireResponseGivenId() {

        QuestionnaireResponse response = questionnaireResponseService.get(1);

        assertEquals(QuestionnaireResponseStatus.COMPLETED, response.getStatus());
    }

    @Test
    public void insert_shouldSaveNestedQuestionnaireResponseItems() throws Exception {

        cleanRowsInCurrentConnection(new String[] { "questionnaire_response_answer", "questionnaire_response_item",
                "questionnaire_response" });

        Questionnaire questionnaire = questionnaireService.get(2);
        Patient patient = patientService.get("1");

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setQuestionnaire(questionnaire);
        response.setFhirUuid(UUID.randomUUID());
        response.setStatus(QuestionnaireResponseStatus.COMPLETED);
        response.setAuthored(new Timestamp(System.currentTimeMillis()));
        response.setSubject(patient);

        QuestionnaireResponseItem parent = new QuestionnaireResponseItem();
        parent.setQuestionnaireResponse(response);
        parent.setLinkId("1");
        parent.setText("HIV Assessment");

        QuestionnaireResponseItem child = new QuestionnaireResponseItem();
        child.setQuestionnaireResponse(response);
        child.setParentItem(parent);
        child.setLinkId("1.1");
        child.setText("Risk Behaviour");

        parent.setChildItems(new HashSet<>());
        parent.getChildItems().add(child);

        response.setItems(new HashSet<>());
        response.getItems().add(parent);

        questionnaireResponseService.insert(response);

        QuestionnaireResponse saved = questionnaireResponseService.getAll().get(0);

        QuestionnaireResponseItem savedParent = saved.getItems().iterator().next();

        assertEquals("HIV Assessment", savedParent.getText());

        assertEquals(1, savedParent.getChildItems().size());

        QuestionnaireResponseItem savedChild = savedParent.getChildItems().iterator().next();

        assertEquals("Risk Behaviour", savedChild.getText());
    }

    @Test
    public void insert_shouldSaveQuestionnaireResponseAnswers() throws Exception {

        cleanRowsInCurrentConnection(new String[] { "questionnaire_response_answer", "questionnaire_response_item",
                "questionnaire_response" });

        Questionnaire questionnaire = questionnaireService.get(1);
        Patient patient = patientService.get("1");

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setQuestionnaire(questionnaire);
        response.setFhirUuid(UUID.randomUUID());
        response.setStatus(QuestionnaireResponseStatus.COMPLETED);
        response.setAuthored(new Timestamp(System.currentTimeMillis()));
        response.setSubject(patient);

        QuestionnaireResponseItem item = new QuestionnaireResponseItem();
        item.setQuestionnaireResponse(response);
        item.setLinkId("1");
        item.setText("Pregnancy Status");

        QuestionnaireResponseAnswer answer = new QuestionnaireResponseAnswer();

        answer.setItem(item);
        answer.setValueBoolean(true);

        item.setAnswers(new HashSet<>());
        item.getAnswers().add(answer);

        response.setItems(new HashSet<>());
        response.getItems().add(item);

        questionnaireResponseService.insert(response);

        QuestionnaireResponse saved = questionnaireResponseService.getAll().get(0);

        QuestionnaireResponseItem savedItem = saved.getItems().iterator().next();

        assertEquals(1, savedItem.getAnswers().size());

        QuestionnaireResponseAnswer savedAnswer = savedItem.getAnswers().iterator().next();

        assertTrue(savedAnswer.getValueBoolean());
    }

    @Test
    public void insert_shouldSaveMultipleAnswers() throws Exception {

        cleanRowsInCurrentConnection(new String[] { "questionnaire_response_answer", "questionnaire_response_item",
                "questionnaire_response" });

        Questionnaire questionnaire = questionnaireService.get(3);
        Patient patient = patientService.get("1");

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setQuestionnaire(questionnaire);
        response.setFhirUuid(UUID.randomUUID());
        response.setStatus(QuestionnaireResponseStatus.IN_PROGRESS);
        response.setAuthored(new Timestamp(System.currentTimeMillis()));
        response.setSubject(patient);

        QuestionnaireResponseItem item = new QuestionnaireResponseItem();
        item.setQuestionnaireResponse(response);
        item.setLinkId("1");
        item.setText("Vitals");

        QuestionnaireResponseAnswer answer1 = new QuestionnaireResponseAnswer();

        answer1.setItem(item);
        answer1.setValueString("120/80");

        QuestionnaireResponseAnswer answer2 = new QuestionnaireResponseAnswer();

        answer2.setItem(item);
        answer2.setValueString("37.0");

        item.setAnswers(new HashSet<>());
        item.getAnswers().add(answer1);
        item.getAnswers().add(answer2);

        response.setItems(new HashSet<>());
        response.getItems().add(item);

        questionnaireResponseService.insert(response);

        QuestionnaireResponse saved = questionnaireResponseService.getAll().get(0);

        QuestionnaireResponseItem savedItem = saved.getItems().iterator().next();

        assertEquals(2, savedItem.getAnswers().size());
    }

    @Test
    public void update_shouldUpdateQuestionnaireResponse() throws Exception {

        QuestionnaireResponse response = questionnaireResponseService.get(1);

        response.setSummary("Updated summary");
        response.setStatus(QuestionnaireResponseStatus.AMENDED);

        questionnaireResponseService.update(response);

        QuestionnaireResponse updated = questionnaireResponseService.get(1);

        assertEquals("Updated summary", updated.getSummary());

        assertEquals(QuestionnaireResponseStatus.AMENDED, updated.getStatus());
    }

}