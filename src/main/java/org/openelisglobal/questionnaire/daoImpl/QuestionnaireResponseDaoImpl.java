package org.openelisglobal.questionnaire.daoImpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.questionnaire.dao.QuestionnaireResponseDao;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class QuestionnaireResponseDaoImpl extends BaseDAOImpl<QuestionnaireResponse, String>
        implements QuestionnaireResponseDao {

    public QuestionnaireResponseDaoImpl() {
        super(QuestionnaireResponse.class);
    }

}
