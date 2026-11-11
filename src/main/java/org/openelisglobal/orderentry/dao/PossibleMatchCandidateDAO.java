package org.openelisglobal.orderentry.dao;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.orderentry.valueholder.PossibleMatchCandidate;

/**
 * Reads the existing records a new patient, facility or provider might
 * duplicate (FR-B6a). Each read is bounded, and only runs when the user presses
 * Create, never from search.
 */
public interface PossibleMatchCandidateDAO {

    /**
     * Patients whose first or last name is trigram-similar to either entered name
     * (so first and last names swapped still match), born within a year of
     * {@code birthDate}, or whose national ID is one edit away from
     * {@code identifier}. Blank criteria are skipped.
     */
    List<PossibleMatchCandidate> patientCandidates(String firstName, String lastName, LocalDate birthDate,
            String identifier, int limit);

    /** Every organization's id, name, code, short name, city and active flag. */
    List<PossibleMatchCandidate> facilityCandidates();

    /**
     * Providers whose first or last name is trigram-similar to either entered name.
     */
    List<PossibleMatchCandidate> providerCandidates(String firstName, String lastName, int limit);
}
