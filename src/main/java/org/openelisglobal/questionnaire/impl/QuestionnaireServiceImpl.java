package org.openelisglobal.questionnaire.impl;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.questionnaire.dao.QuestionnaireDao;
import org.openelisglobal.questionnaire.service.QuestionnaireService;
import org.openelisglobal.questionnaire.valueholder.Questionnaire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionnaireServiceImpl extends AuditableBaseObjectServiceImpl<Questionnaire, String>
        implements QuestionnaireService {
    @Autowired
    private QuestionnaireDao questionnaireDao;

    QuestionnaireServiceImpl() {
        super(Questionnaire.class);
    }

    @Override
    protected QuestionnaireDao getBaseObjectDAO() {
        return questionnaireDao;

    }

}
