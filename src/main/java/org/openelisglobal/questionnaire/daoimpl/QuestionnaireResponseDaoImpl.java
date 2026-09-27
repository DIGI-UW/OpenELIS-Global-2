package org.openelisglobal.questionnaire.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.questionnaire.dao.QuestionnaireResponseDao;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse;
import org.springframework.stereotype.Component;

@Component
public class QuestionnaireResponseDaoImpl extends BaseDAOImpl<QuestionnaireResponse, Integer>
        implements QuestionnaireResponseDao {

    public QuestionnaireResponseDaoImpl() {
        super(QuestionnaireResponse.class);
    }

}
