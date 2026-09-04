package org.openelisglobal.compliance.service;

import org.springframework.security.access.prepost.PreAuthorize;

public interface LhuAmendmentService {

    /**
     * Returns true if the sample has at least one analysis bearing a
     * VALIDATED_AND_RELEASED electronic signature, which is the authoritative
     * release marker for the LHU. Generation count is NOT used — previews trigger
     * generation without constituting a release. See OGC-776 T-201.
     */
    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    boolean hasBeenReleased(Long sampleId);

    /**
     * Records a report-level amendment on the sample. Increments amendmentNumber
     * (null → 1, N → N+1), stores the prior certificate number and the reason. Must
     * be called inside a transaction (service layer only).
     *
     * @throws IllegalArgumentException if reason is blank or null
     * @throws IllegalArgumentException if the sample does not exist
     */
    @PreAuthorize("hasAuthority('PRIV_REPORT_EXPORT')")
    void applyLhuAmendment(Long sampleId, String priorCertificateNumber, String reason);

    /**
     * Returns the certificate number with an /Am.N suffix appended when
     * amendmentNumber >= 1. Preserves any existing /R revision suffix. Returns null
     * when base is null. Returns base unchanged when amendmentNumber is null or 0.
     */
    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    String certificateNumberWithAmendmentSuffix(String baseCertificateNumber, Integer amendmentNumber);
}
