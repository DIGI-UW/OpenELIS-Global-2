package org.openelisglobal.questionnaire.daoImpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.questionnaire.dao.QuestionnaireDao;
import org.openelisglobal.questionnaire.valueholder.Questionnaire;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Component
public class QuestionionnaireDaoImpl extends BaseDAOImpl<Questionnaire, String> implements QuestionnaireDao {

    QuestionionnaireDaoImpl() {
        super(Questionnaire.class);

    }

}
