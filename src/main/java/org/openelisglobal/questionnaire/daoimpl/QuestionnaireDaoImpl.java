package org.openelisglobal.questionnaire.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.questionnaire.dao.QuestionnaireDao;
import org.openelisglobal.questionnaire.valueholder.Questionnaire;
import org.springframework.stereotype.Component;

@Component
public class QuestionnaireDaoImpl extends BaseDAOImpl<Questionnaire, Integer> implements QuestionnaireDao {

    QuestionnaireDaoImpl() {
        super(Questionnaire.class);

    }

}
