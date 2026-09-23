package org.openelisglobal.questionnaire.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.questionnaire.valueholder.Questionnaire;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Declares no methods of its own, so the gate is class-level and covers the
 * inherited BaseObjectService CRUD. A questionnaire is a programme form
 * DEFINITION — catalog configuration, the same class of object the test catalog
 * editor manages — hence test:configure rather than an order privilege.
 * QuestionnaireResponse (the patient's answers) is gated separately.
 */
@PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
public interface QuestionnaireService extends BaseObjectService<Questionnaire, Integer> {

}
