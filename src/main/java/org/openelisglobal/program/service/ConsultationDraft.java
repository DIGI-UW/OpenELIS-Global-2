package org.openelisglobal.program.service;

/**
 * A second opinion as the pathologist asks for it from the case view (FR-11.1
 * to FR-11.3). Every value is as posted, so the service refuses a missing or
 * unknown one with a message the screen translates rather than the request
 * failing to bind.
 *
 * @param consultedUserId the system user id of the pathologist asked
 * @param reasonId        a dictionary id of the reasons category, or blank
 * @param preTreatment    whether the opinion is asked for before treatment;
 *                        null when the form left it unanswered
 */
public record ConsultationDraft(String consultedUserId, String reasonId, Boolean preTreatment) {
}
