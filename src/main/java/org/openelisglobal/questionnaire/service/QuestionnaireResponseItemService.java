package org.openelisglobal.questionnaire.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireResponseItem;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Declares no methods of its own, so the gate is class-level and covers the
 * inherited BaseObjectService CRUD. One answered question within a
 * QuestionnaireResponse — same patient-answer data, same privilege as
 * {@link QuestionnaireResponseService}.
 */
@PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")
public interface QuestionnaireResponseItemService extends BaseObjectService<QuestionnaireResponseItem, Integer> {

}
