package org.openelisglobal.orderentry.service;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.orderentry.service.PossibleMatchScorer.Scored;

/**
 * The possible-match check run when a user creates a new patient, facility or
 * provider in order entry (FRS clinical order entry v4, FR-B6a, D-216). Search
 * itself never uses it.
 */
public interface PossibleMatchService {

    String PATIENT = "patient";
    String FACILITY = "facility";
    String PROVIDER = "provider";

    List<Scored> patients(String firstName, String lastName, LocalDate birthDate, String identifier);

    List<Scored> facilities(String name, String code);

    List<Scored> providers(String firstName, String lastName);

    /**
     * Records that {@code sysUserId} chose Create new anyway for a record of
     * {@code kind} although {@code matchesShownJson} were shown.
     *
     * @return the id of the stored record
     */
    Integer recordOverride(String kind, String enteredJson, String matchesShownJson, String sysUserId);
}
