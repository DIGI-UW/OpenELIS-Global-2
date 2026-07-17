package org.openelisglobal.esig.valueholder;

/**
 * Defines the legal meaning of an electronic signature per 21 CFR Part 11
 * §11.50. Each signature must indicate what the signer is attesting to.
 */
public enum SignatureMeaning {

    /**
     * Technologist signed upon entering/saving test results. Indicates the signer
     * authored the data.
     */
    AUTHORED,

    /**
     * Supervisor validated and released the results. Results become available on
     * reports and can be sent to EMR.
     */
    VALIDATED_AND_RELEASED,

    /**
     * Supervisor rejected the results. Results are returned to technologist for
     * correction.
     */
    REJECTED
}
