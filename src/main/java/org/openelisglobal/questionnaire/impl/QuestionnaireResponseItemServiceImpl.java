package org.openelisglobal.questionnaire.impl;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.questionnaire.dao.QuestionnaireResponseItemDao;
import org.openelisglobal.questionnaire.service.QuestionnaireResponseItemService;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireResponseItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionnaireResponseItemServiceImpl extends
        AuditableBaseObjectServiceImpl<QuestionnaireResponseItem, Integer> implements QuestionnaireResponseItemService {

    public QuestionnaireResponseItemServiceImpl() {
        super(QuestionnaireResponseItem.class);
    }

    @Autowired
    private QuestionnaireResponseItemDao questionnaireResponseItemDao;

    @Override
    protected QuestionnaireResponseItemDao getBaseObjectDAO() {
        return questionnaireResponseItemDao;
    }

}
