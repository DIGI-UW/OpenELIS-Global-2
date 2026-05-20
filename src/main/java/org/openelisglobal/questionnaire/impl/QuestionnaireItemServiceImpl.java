package org.openelisglobal.questionnaire.impl;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.questionnaire.dao.QuestionnaireItemDao;
import org.openelisglobal.questionnaire.service.QuestionnaireItemService;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionnaireItemServiceImpl extends AuditableBaseObjectServiceImpl<QuestionnaireItem, Integer>
        implements QuestionnaireItemService {
    public QuestionnaireItemServiceImpl() {
        super(QuestionnaireItem.class);
    }

    @Autowired
    private QuestionnaireItemDao questionnaireItemDao;

    @Override
    protected QuestionnaireItemDao getBaseObjectDAO() {
        return questionnaireItemDao;

    }

}
