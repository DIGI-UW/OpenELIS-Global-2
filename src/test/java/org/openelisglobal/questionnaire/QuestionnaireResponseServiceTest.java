package org.openelisglobal.questionnaire;

import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.questionnaire.service.QuestionnaireResponseService;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse;
import org.springframework.beans.factory.annotation.Autowired;

public class QuestionnaireResponseServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QuestionnaireResponseService questionnaireResponseService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/questionnaire-response.xml");

    }

    @Test
    public void getAllShouldReturnAllQuestionnaireResponses() {
        List<QuestionnaireResponse> questionnaireResponses = questionnaireResponseService.getAll();
        assertTrue(questionnaireResponses.size() == 3);

    }

}