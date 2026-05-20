package org.openelisglobal.questionnaire.daoImpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.questionnaire.dao.QuestionnaireItemDao;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireItem;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class QuestionnaireItemDaoImpl extends BaseDAOImpl<QuestionnaireItem, Integer> implements QuestionnaireItemDao {

    public QuestionnaireItemDaoImpl() {
        super(QuestionnaireItem.class);

    }

}
