package org.openelisglobal.questionnaire.service;

import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.questionnaire.valueholder.Questionnaire;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Declares no methods of its own, so the gate is class-level and covers the
 * inherited BaseObjectService CRUD.
 *
 * <p>
 * A questionnaire is a programme form DEFINITION, the same class of object the
 * test catalog editor manages, so WRITING one is test:configure and stays that
 * way via {@link CrudPrivileges}. Reading one is not: order entry fetches it to
 * know which programme-specific fields to render. With the whole interface on
 * PRIV_TEST_CONFIGURE, which no seeded role holds, that read denied everyone
 * but Global Administrator, and ordering under Histopathology, Cytology,
 * Immunohistochemistry or any other programme carrying a questionnaire returned
 * 403 from /rest/program/{id}/questionnaire while the form silently lost its
 * fields.
 *
 * <p>
 * The sibling QuestionnaireResponseService, which holds the patient's ANSWERS,
 * is gated on PRIV_ORDER_CREATE; the definition being harder to read than the
 * answers was the inconsistency. The read now also accepts PRIV_ORDER_CREATE
 * and PRIV_ORDER_VIEW.
 */
@PreAuthorize("hasAnyAuthority('PRIV_TEST_CONFIGURE','PRIV_ORDER_CREATE','PRIV_ORDER_VIEW')")
@CrudPrivileges(write = "PRIV_TEST_CONFIGURE")
public interface QuestionnaireService extends BaseObjectService<Questionnaire, Integer> {

}
