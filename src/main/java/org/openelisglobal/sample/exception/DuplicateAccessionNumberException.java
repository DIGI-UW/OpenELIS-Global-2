package org.openelisglobal.sample.exception;

/**
 * Exception thrown when a duplicate accession number is detected during sample
 * creation. This can occur when attempting to insert a sample with an accession
 * number that already exists in the database.
 *
 * This exception is typically caught and handled by the AccessionNumberHandler,
 * which will automatically retry with a new accession number.
 *
 * @see org.openelisglobal.sample.util.AccessionNumberHandler
 */
public class DuplicateAccessionNumberException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String accessionNumber;
    private final int attemptNumber;
    private final int maxAttempts;

    /**
     * Constructor with accession number and attempt information
     *
     * @param accessionNumber the accession number that caused the duplicate error
     * @param attemptNumber   the current attempt number
     * @param maxAttempts     the maximum number of attempts allowed
     * @param cause           the underlying exception (typically a
     *                        DataIntegrityViolationException)
     */
    public DuplicateAccessionNumberException(String accessionNumber, int attemptNumber, int maxAttempts,
            Throwable cause) {
        super(String.format("Duplicate accession number '%s' detected on attempt %d of %d. %s", accessionNumber,
                attemptNumber, maxAttempts, cause != null ? "Cause: " + cause.getMessage() : ""), cause);
        this.accessionNumber = accessionNumber;
        this.attemptNumber = attemptNumber;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Constructor with simple message
     *
     * @param message the error message
     */
    public DuplicateAccessionNumberException(String message) {
        super(message);
        this.accessionNumber = null;
        this.attemptNumber = 0;
        this.maxAttempts = 0;
    }

    /**
     * Constructor with message and cause
     *
     * @param message the error message
     * @param cause   the underlying exception
     */
    public DuplicateAccessionNumberException(String message, Throwable cause) {
        super(message, cause);
        this.accessionNumber = null;
        this.attemptNumber = 0;
        this.maxAttempts = 0;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
}
