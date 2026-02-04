package org.openelisglobal.sample.util;

import jakarta.persistence.EntityManager;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.sample.dao.SampleDAO;
import org.openelisglobal.sample.exception.DuplicateAccessionNumberException;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Generic utility class for handling safe accession number generation and
 * insertion with automatic retry logic for duplicate accession numbers.
 *
 * This class provides a thread-safe mechanism to generate accession numbers and
 * insert samples while handling duplicate key violations gracefully. It can be
 * used by any manifest import service to ensure accession number uniqueness
 * across single and multi-instance deployments.
 *
 * Features: - Synchronized access to prevent concurrent duplicate generation -
 * Database-level verification before insertion - Configurable retry logic
 * (default: 100 attempts) - Exception handling for
 * DataIntegrityViolationException - Automatic database flush to ensure
 * visibility - Detailed logging and error reporting
 *
 * Usage Example:
 * 
 * <pre>
 * {@code
 * AccessionNumberHandler handler = new AccessionNumberHandler(sampleService, sampleDAO, entityManager, logger);
 *
 * Sample sample = new Sample();
 * sample.setSysUserId(userId);
 * sample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
 *
 * String sampleIdDb = handler.generateAndInsertWithUniqueAccessionNumber(sample,
 *         AccessionNumberHandler.DEFAULT_MAX_ATTEMPTS);
 * }
 * </pre>
 *
 * @see DuplicateAccessionNumberException
 * @see org.openelisglobal.sample.service.SampleService
 * @see org.openelisglobal.sample.dao.SampleDAO
 */
public class AccessionNumberHandler {

    private static final Object ACCESSION_NUMBER_LOCK = new Object();
    public static final int DEFAULT_MAX_ATTEMPTS = 100;
    private static final String CLASS_NAME = AccessionNumberHandler.class.getSimpleName();

    private final SampleService sampleService;
    private final SampleDAO sampleDAO;
    private final EntityManager entityManager;
    private final Class<?> callerClass;

    private IAccessionNumberGenerator accessionNumberGenerator;

    /**
     * Constructor for AccessionNumberHandler
     *
     * @param sampleService the sample service for database operations
     * @param sampleDAO     the sample DAO for fallback accession number generation
     * @param entityManager the entity manager for flushing changes
     * @param callerClass   the class using this handler (for logging purposes)
     */
    public AccessionNumberHandler(SampleService sampleService, SampleDAO sampleDAO, EntityManager entityManager,
            Class<?> callerClass) {
        this.sampleService = sampleService;
        this.sampleDAO = sampleDAO;
        this.entityManager = entityManager;
        this.callerClass = callerClass;
    }

    /**
     * Generates a unique accession number and inserts the sample with automatic
     * retry logic for duplicate keys.
     *
     * This method is synchronized to prevent concurrent accession number generation
     * from creating duplicates within a single instance. However, it also includes
     * exception handling for DataIntegrityViolationException to handle cases where
     * another instance inserts a conflicting accession number between the check and
     * insert operations.
     *
     * @param sample      the sample to insert (must have sysUserId, enteredDate,
     *                    and receivedTimestamp set)
     * @param maxAttempts the maximum number of retry attempts (use
     *                    DEFAULT_MAX_ATTEMPTS for 100)
     * @return the database ID of the inserted sample
     * @throws DuplicateAccessionNumberException if unable to generate a unique
     *                                           accession number after maxAttempts
     * @throws DataIntegrityViolationException   if database constraint violation
     *                                           occurs other than duplicate
     *                                           accession number
     */
    public String generateAndInsertWithUniqueAccessionNumber(Sample sample, int maxAttempts) {
        synchronized (ACCESSION_NUMBER_LOCK) {
            String generatedAccessionNumber = null;
            int attempts = 0;

            while (generatedAccessionNumber == null && attempts < maxAttempts) {
                String candidateNumber = getNextAccessionNumberInternal();

                // Check if this accession number already exists in the database
                Sample existingSample = sampleService.getSampleByAccessionNumber(candidateNumber);
                if (existingSample == null) {
                    generatedAccessionNumber = candidateNumber;
                } else {
                    attempts++;
                    LogEvent.logWarn(CLASS_NAME, "generateAndInsertWithUniqueAccessionNumber",
                            "Accession number '" + candidateNumber + "' already exists. " + "Retrying (attempt "
                                    + attempts + "/" + maxAttempts + ")");
                }
            }

            if (generatedAccessionNumber == null) {
                String errorMsg = "Failed to generate unique accession number after " + maxAttempts + " attempts";
                LogEvent.logError(CLASS_NAME, "generateAndInsertWithUniqueAccessionNumber", errorMsg);
                throw new DuplicateAccessionNumberException(errorMsg);
            }

            // Set the accession number on the sample
            sample.setAccessionNumber(generatedAccessionNumber);

            try {
                // Insert sample with the verified unique accession number
                sampleService.insertDataWithAccessionNumber(sample);

                // Flush to ensure the database sees this sample before the next iteration
                entityManager.flush();

                String sampleId = sample.getId();
                LogEvent.logInfo(CLASS_NAME, "generateAndInsertWithUniqueAccessionNumber",
                        "Successfully created sample with accession number '" + generatedAccessionNumber + "' and ID '"
                                + sampleId + "'");

                return sampleId;
            } catch (DataIntegrityViolationException e) {
                // Handle duplicate key violation that may occur due to race condition
                // between check and insert in multi-instance deployments
                if (isDuplicateAccessionNumberViolation(e)) {
                    attempts++;
                    if (attempts < maxAttempts) {
                        LogEvent.logWarn(e);
                        // Clear the accession number for retry
                        sample.setAccessionNumber(null);
                        return generateAndInsertWithUniqueAccessionNumber(sample, maxAttempts - attempts);
                    } else {
                        String errorMsg = "Failed to insert sample after " + maxAttempts
                                + " attempts due to duplicate accession numbers";
                        LogEvent.logError(errorMsg, e);
                        throw new DuplicateAccessionNumberException(generatedAccessionNumber, attempts, maxAttempts, e);
                    }
                } else {
                    // Not a duplicate accession number violation, re-throw
                    throw e;
                }
            }
        }
    }

    /**
     * Checks if a DataIntegrityViolationException is caused by a duplicate
     * accession number constraint violation.
     *
     * This method checks for the "accnum_uk" unique constraint that exists on the
     * ACCESSION_NUMBER column in the SAMPLE table.
     *
     * @param e the DataIntegrityViolationException to check
     * @return true if the exception is caused by a duplicate accession number
     *         violation
     */
    private boolean isDuplicateAccessionNumberViolation(DataIntegrityViolationException e) {
        String message = e.getMessage();
        if (message != null) {
            // Check for PostgreSQL unique constraint violation on accession number
            if (message.contains("accnum_uk")
                    || message.contains("duplicate key") && message.contains("accession_number")) {
                return true;
            }
        }

        // Check the underlying cause message
        Throwable cause = e.getCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null && (causeMessage.contains("accnum_uk")
                    || causeMessage.contains("duplicate key") && causeMessage.contains("accession_number"))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the next accession number using the accession number generator or
     * fallback to DAO if generator is unavailable.
     *
     * This method should be called from within the synchronized block to ensure
     * thread safety.
     *
     * @return the next candidate accession number
     */
    private String getNextAccessionNumberInternal() {
        if (accessionNumberGenerator == null) {
            accessionNumberGenerator = AccessionNumberUtil.getMainAccessionNumberGenerator();
        }
        if (accessionNumberGenerator != null) {
            return accessionNumberGenerator.getNextAccessionNumber(null, true);
        }
        // Fallback to old method if generator not available
        return sampleDAO.getNextAccessionNumber();
    }

    /**
     * Generates a unique accession number with default maximum attempts.
     *
     * Convenience method that calls
     * {@link #generateAndInsertWithUniqueAccessionNumber(Sample, int)} with
     * DEFAULT_MAX_ATTEMPTS (100).
     *
     * @param sample the sample to insert
     * @return the database ID of the inserted sample
     * @throws DuplicateAccessionNumberException if unable to generate a unique
     *                                           accession number
     */
    public String generateAndInsertWithUniqueAccessionNumber(Sample sample) {
        return generateAndInsertWithUniqueAccessionNumber(sample, DEFAULT_MAX_ATTEMPTS);
    }

    /**
     * Gets the current accession number generator.
     *
     * @return the accession number generator, or null if not initialized
     */
    public IAccessionNumberGenerator getAccessionNumberGenerator() {
        return accessionNumberGenerator;
    }

    /**
     * Sets a custom accession number generator.
     *
     * Useful for testing or when you want to use a specific generator instance.
     *
     * @param generator the accession number generator to use
     */
    public void setAccessionNumberGenerator(IAccessionNumberGenerator generator) {
        this.accessionNumberGenerator = generator;
    }
}
