package org.openelisglobal.questionnaire.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireItem;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Declares no methods of its own, so the gate is class-level and covers the
 * inherited BaseObjectService CRUD. An item is one question within a
 * questionnaire DEFINITION, so it carries the same configuration privilege as
 * {@link QuestionnaireService}.
 */
@PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
public interface QuestionnaireItemService extends BaseObjectService<QuestionnaireItem, Integer> {

}
