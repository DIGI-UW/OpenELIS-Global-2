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
     * Technologist signed upon revising a result they or a colleague had already
     * saved. Indicates the signer changed data that existed, not that they
     * originated it.
     *
     * <p>
     * §11.50 requires each signature to say what is being attested to, and
     * authorship and revision are not the same attestation — a record showing a
     * correction signed as authored misstates who first produced the value.
     */
    MODIFIED,

    /**
     * Supervisor validated and released the results. Results become available on
     * reports and can be sent to EMR.
     */
    VALIDATED_AND_RELEASED,

    /**
     * Supervisor rejected the results. Results are returned to technologist for
     * correction.
     */
    REJECTED,

    /**
     * A reviewer signed one review level of a result that needs several sequential
     * reviews (OGC-1364). The result stays in review until its last level is
     * signed, which carries {@link #VALIDATED_AND_RELEASED}.
     */
    REVIEWED
}
