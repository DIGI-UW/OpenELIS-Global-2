package org.openelisglobal.compliance.service;

import java.util.Optional;
import org.openelisglobal.compliance.valueholder.ComplianceReportArchive;

public interface ComplianceReportArchiveService {

    /**
     * Archives the rendered PDF bytes for (sampleId, amendmentNumber).
     * amendmentNumber = 0 represents the original released report; 1..N are
     * amendments. Immutability is enforced: if a row already exists for this
     * (sampleId, amendmentNumber), the existing row is returned and no save is
     * performed. Must only be called on the release/reissue path — never on
     * previews.
     *
     * @param sampleId        the sample being archived
     * @param amendmentNumber 0 for original, 1..N for amendments (null treated as
     *                        0)
     * @param pdfBytes        the rendered PDF bytes
     * @param userId          the user performing the action (may be null)
     * @return the archived record (existing or newly created)
     */
    ComplianceReportArchive archiveIfAbsent(Long sampleId, Integer amendmentNumber, byte[] pdfBytes, String userId);

    Optional<ComplianceReportArchive> findBySampleIdAndAmendmentNumber(Long sampleId, Integer amendmentNumber);
}
