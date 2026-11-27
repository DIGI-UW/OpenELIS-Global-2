package org.openelisglobal.program.service;

/**
 * The outcome the consulted pathologist records on a second opinion (FR-11.4).
 *
 * @param outcome AGREE, DISAGREE or MODIFIED, the name as posted
 * @param note    what the pathologist adds, at most 1,000 characters; blank is
 *                none
 */
public record ConsultationAnswer(String outcome, String note) {
}
