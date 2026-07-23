package org.openelisglobal.qaevent.service;

/**
 * Service interface for generating unique NCE numbers. NCE numbers follow the
 * format: NCE-YYYY-NNNNN where YYYY is the year and NNNNN is a sequential
 * number padded to 5 digits.
 */
public interface NceNumberGeneratorService {

    /**
     * Generate a new unique NCE number for the current year.
     *
     * @return the generated NCE number (e.g., "NCE-2026-00001")
     */
    String generateNceNumber();

    /**
     * Generate a new unique NCE number for a specific year.
     *
     * @param year the year to generate the number for
     * @return the generated NCE number
     */
    String generateNceNumber(int year);

    /**
     * Get the current sequence value for the year.
     *
     * @param year the year
     * @return the current sequence value, or 0 if no NCEs exist for that year
     */
    int getCurrentSequenceForYear(int year);

    /**
     * Validate an NCE number format.
     *
     * @param nceNumber the NCE number to validate
     * @return true if the format is valid
     */
    boolean isValidFormat(String nceNumber);

    /**
     * Parse the year from an NCE number.
     *
     * @param nceNumber the NCE number
     * @return the year component
     * @throws IllegalArgumentException if the format is invalid
     */
    int parseYear(String nceNumber);

    /**
     * Parse the sequence number from an NCE number.
     *
     * @param nceNumber the NCE number
     * @return the sequence number component
     * @throws IllegalArgumentException if the format is invalid
     */
    int parseSequence(String nceNumber);
}
