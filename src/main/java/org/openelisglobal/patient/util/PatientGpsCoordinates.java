package org.openelisglobal.patient.util;

import java.math.BigDecimal;
import org.openelisglobal.person.valueholder.Person;

/**
 * Pure helpers for the String→BigDecimal GPS coordinate handling between the
 * patient form bean (String) and the Person entity (BigDecimal).
 *
 * Kept in its own class — without Spring dependencies — so the GPS parsing +
 * setter contract is unit-testable without standing up an application context.
 * PatientUtil and PatientManagementUpdate both delegate here.
 */
public final class PatientGpsCoordinates {

    private PatientGpsCoordinates() {
    }

    /**
     * Parse a GPS coordinate String into BigDecimal. Null / empty / blank /
     * unparseable input returns null so operator typos surface as missing data
     * rather than exceptions.
     */
    public static BigDecimal parse(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(trimmed);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    /** Set both GPS fields on Person from raw String input. */
    public static void applyToPerson(String latitudeRaw, String longitudeRaw, Person person) {
        person.setGpsLatitude(parse(latitudeRaw));
        person.setGpsLongitude(parse(longitudeRaw));
    }
}
