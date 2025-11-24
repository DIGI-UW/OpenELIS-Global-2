package org.openelisglobal.document.service;

/**
 * Service for rate limiting document uploads per patient.
 * Prevents abuse and DoS attacks by limiting upload frequency.
 */
public interface RateLimitService {
    /**
     * Check if an upload is allowed for the given patient.
     * @param patientId The patient ID
     * @return true if upload is allowed, false if rate limit exceeded
     */
    boolean isAllowed(String patientId);

    /**
     * Record an upload attempt for rate limiting purposes.
     * @param patientId The patient ID
     */
    void recordUpload(String patientId);

    /**
     * Get the time remaining until the next upload is allowed (in seconds).
     * @param patientId The patient ID
     * @return seconds until next upload allowed, or 0 if allowed now
     */
    long getTimeUntilNextAllowed(String patientId);
}

