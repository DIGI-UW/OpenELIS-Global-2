package org.openelisglobal.questionnaire.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.questionnaire.dao.QuestionnaireResponseItemDao;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireResponseItem;
import org.springframework.stereotype.Component;

@Component
public class QuestionnaireResponseItemDaoImpl extends BaseDAOImpl<QuestionnaireResponseItem, Integer>
        implements QuestionnaireResponseItemDao {

    public QuestionnaireResponseItemDaoImpl() {
        super(QuestionnaireResponseItem.class);
    }

}
