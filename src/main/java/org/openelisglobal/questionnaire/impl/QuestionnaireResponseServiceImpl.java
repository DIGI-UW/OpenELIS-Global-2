package org.openelisglobal.questionnaire.impl;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.questionnaire.dao.QuestionnaireResponseDao;
import org.openelisglobal.questionnaire.service.QuestionnaireResponseService;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse;
import org.springframework.beans.factory.annotation.Autowired;

public class QuestionnaireResponseServiceImpl extends AuditableBaseObjectServiceImpl<QuestionnaireResponse, Integer>
        implements QuestionnaireResponseService {
    public QuestionnaireResponseServiceImpl() {
        super(QuestionnaireResponse.class);

    }

    @Autowired
    private QuestionnaireResponseDao questionnaireResponseDao;

    @Override
    protected QuestionnaireResponseDao getBaseObjectDAO() {
        return questionnaireResponseDao;

    }

}
