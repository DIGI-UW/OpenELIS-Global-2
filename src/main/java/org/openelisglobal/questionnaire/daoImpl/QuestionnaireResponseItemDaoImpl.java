package org.openelisglobal.questionnaire.daoImpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.questionnaire.dao.QuestionnaireResponseItemDao;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireResponseItem;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class QuestionnaireResponseItemDaoImpl extends BaseDAOImpl<QuestionnaireResponseItem, String>
        implements QuestionnaireResponseItemDao {

    public QuestionnaireResponseItemDaoImpl() {
        super(QuestionnaireResponseItem.class);
    }

}
