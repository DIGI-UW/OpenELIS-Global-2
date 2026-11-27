package org.openelisglobal.program.service;

import java.util.List;
import org.openelisglobal.program.valueholder.pathology.PathologyConsultation;
import org.openelisglobal.program.valueholder.pathology.PathologySample;

/**
 * Second opinions on a pathology case (FR-11): asked for from one pathologist,
 * answered by that pathologist alone, or withdrawn. Each is a row of its own
 * beside the case, so asking, answering and withdrawing never lock, change or
 * version the case, and the case's save never touches them.
 */
public interface PathologyConsultationService {

    /** The reference table the three events below are audited against. */
    String REFERENCE_TABLE = "PATHOLOGY_CONSULTATION";

    /** The dictionary category the optional reason is picked from. */
    String REASON_CATEGORY = "pathology_consultation_reasons";

    String PATHOLOGY_CONSULTATION_REQUESTED = "PATHOLOGY_CONSULTATION_REQUESTED";
    String PATHOLOGY_CONSULTATION_ANSWERED = "PATHOLOGY_CONSULTATION_ANSWERED";
    String PATHOLOGY_CONSULTATION_WITHDRAWN = "PATHOLOGY_CONSULTATION_WITHDRAWN";

    /**
     * Asks a pathologist for a second opinion, in the name of the signed-in user
     * and at the server clock, while the case stands with the pathologist and is
     * not signed out.
     */
    PathologyConsultation request(Integer pathologySampleId, ConsultationDraft draft, String sysUserId);

    /**
     * Records the outcome of an awaited second opinion; only the pathologist asked
     * may, wherever the case stands.
     */
    PathologyConsultation answer(Integer pathologySampleId, Integer consultationId, ConsultationAnswer answer,
            String sysUserId);

    /**
     * Withdraws an awaited second opinion; anyone who may read the case may, and
     * the row is kept.
     */
    PathologyConsultation withdraw(Integer pathologySampleId, Integer consultationId, String sysUserId);

    /** Every second opinion on the case, newest first. */
    List<PathologyConsultation> getForCase(Integer pathologySampleId);

    /**
     * Whether a second opinion can be asked for now: the case stands with the
     * pathologist, under review, or completed and never released.
     */
    boolean takesConsultations(PathologySample pathologySample);

    /**
     * The coded conclusions that suggest a second opinion under the laboratory's
     * rule, each as its code and name (FR-11.5): empty unless a second opinion can
     * be asked for, empty once one is awaited or answered, and empty when the rule
     * is not set. A suggestion never holds the sign-out.
     *
     * @param consultations every second opinion on the case
     */
    List<String> suggestedFor(PathologySample pathologySample, List<PathologyConsultation> consultations);
}
