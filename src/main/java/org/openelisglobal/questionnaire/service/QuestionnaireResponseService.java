package org.openelisglobal.questionnaire.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.questionnaire.valueholder.QuestionnaireResponse;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Declares no methods of its own, so the gate is class-level and covers the
 * inherited BaseObjectService CRUD. A response holds the ANSWERS captured for a
 * patient during order entry, so it takes the order-entry privilege rather than
 * the configuration one that guards the questionnaire definition.
 */
@PreAuthorize("hasAuthority('PRIV_ORDER_CREATE')")
public interface QuestionnaireResponseService extends BaseObjectService<QuestionnaireResponse, Integer> {

}
