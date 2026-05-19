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
import org.openelisglobal.questionnaire.service.QuestionnaireService;
import org.openelisglobal.questionnaire.valueholder.Questionnaire;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class QuestionnaireServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QuestionnaireService questionnaireService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/questionnaire.xml");
    }

    @Test
    public void getAll_shouldReturnAllQuestionnaires() {

        List<Questionnaire> questionnaires = questionnaireService.getAll();

        assertEquals(4, questionnaires.size());

        questionnaires.sort(Comparator.comparing(Questionnaire::getQuestionnaireName));

        assertEquals("General Patient Intake", questionnaires.get(0).getQuestionnaireName());
        assertEquals("HIV Testing Intake", questionnaires.get(1).getQuestionnaireName());
        assertEquals("Laboratory Request Form", questionnaires.get(2).getQuestionnaireName());
        assertEquals("Maternal Health Intake", questionnaires.get(3).getQuestionnaireName());
    }

    @Test
    public void insert_shouldInsertNewQuestionnaire() throws Exception {

        cleanRowsInCurrentConnection(new String[] { "questionnaire", "questionnaire_item",
                "questionnaire_answer_option", "questionnaire_item_initial" });

        Questionnaire q = new Questionnaire();
        q.setQuestionnaireName("New Form");
        q.setDescription("Test questionnaire");
        q.setCode("Q001");
        q.setStatus(Questionnaire.QuestionaireStatus.ACTIVE);
        q.setPurpose("Testing");
        q.setHasItem(false);
        q.setFhirUuid(UUID.randomUUID());
        q.setSysUserId("1");
        q.setLastupdated(new Timestamp(System.currentTimeMillis()));

        questionnaireService.insert(q);

        List<Questionnaire> questionnaires = questionnaireService.getAll();
        assertTrue(questionnaires.size() == 1);
        assertEquals("New Form", questionnaires.get(0).getQuestionnaireName());

    }

    @Test
    public void save_shouldSaveQuestionaireItemToQuestionnaire() throws Exception {

        cleanRowsInCurrentConnection(new String[] { "questionnaire_answer_option", "questionnaire_item_initial",
                "questionnaire_item", "questionnaire" });

        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setQuestionnaireName("Vitals Form");
        questionnaire.setDescription("Vitals questionnaire");
        questionnaire.setCode("VF001");
        questionnaire.setStatus(Questionnaire.QuestionaireStatus.ACTIVE);
        questionnaire.setPurpose("Vitals capture");
        questionnaire.setHasItem(true);
        questionnaire.setFhirUuid(UUID.randomUUID());
        questionnaire.setSysUserId("1");
        questionnaire.setLastupdated(new Timestamp(System.currentTimeMillis()));

        QuestionnaireItem item = new QuestionnaireItem();
        item.setQuestionnaire(questionnaire);
        item.setLinkId("1");
        item.setText("Patient Temperature");
        item.setItemType(QuestionnaireItem.QuestionnaireItemType.DECIMAL);
        item.setRequired(true);
        item.setRepeats(false);
        item.setItemOrder(1);
        item.setSysUserId("1");
        item.setLastupdated(new Timestamp(System.currentTimeMillis()));

        questionnaire.setQuestionnaireItems(new HashSet<>());
        questionnaire.getQuestionnaireItems().add(item);

        questionnaireService.insert(questionnaire);

        List<Questionnaire> questionnaires = questionnaireService.getAll();

        assertEquals(1, questionnaires.size());

        Questionnaire saved = questionnaires.get(0);

        assertEquals("Vitals Form", saved.getQuestionnaireName());

        assertTrue(saved.getQuestionnaireItems().size() == 1);

        QuestionnaireItem savedItem = saved.getQuestionnaireItems().iterator().next();

        assertEquals("Patient Temperature", savedItem.getText());

        assertEquals(QuestionnaireItem.QuestionnaireItemType.DECIMAL, savedItem.getItemType());
    }
}