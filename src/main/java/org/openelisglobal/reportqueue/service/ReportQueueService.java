package org.openelisglobal.reportqueue.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.reportqueue.dto.ReportQueuePageDTO;
import org.openelisglobal.reportqueue.dto.ReportQueuePreferenceDTO;
import org.openelisglobal.reportqueue.dto.ReportQueueRequest;
import org.openelisglobal.reportqueue.dto.ReportQueueRowDetailDTO;
import org.openelisglobal.reportqueue.valueholder.ReportVersion;

/**
 * The Report Print Queue (OGC-431, FRS OGC-1031 r4 V1): which patient reports
 * need printing, one print pipeline that versions and archives every print, and
 * the per-user settings of the page.
 */
public interface ReportQueueService {

    /**
     * The outcome of one print: the PDF, the version it belongs to and whether it
     * was a reprint.
     */
    final class PrintOutcome {
        private final byte[] pdf;
        private final int version;
        private final boolean reprint;
        private final String labNumber;

        public PrintOutcome(byte[] pdf, int version, boolean reprint, String labNumber) {
            this.pdf = pdf;
            this.version = version;
            this.reprint = reprint;
            this.labNumber = labNumber;
        }

        public byte[] getPdf() {
            return pdf;
        }

        public int getVersion() {
            return version;
        }

        public boolean isReprint() {
            return reprint;
        }

        public String getLabNumber() {
            return labNumber;
        }
    }

    /**
     * Thrown when an order has no released result and so cannot be printed (FRS
     * BR-2).
     */
    final class NothingReleasedException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public NothingReleasedException(String labNumber) {
            super(labNumber);
        }
    }

    ReportQueuePageDTO list(ReportQueueRequest request, String sysUserId);

    Optional<ReportQueueRowDetailDTO> detail(Long sampleId, String sysUserId);

    /**
     * Prints one order: a new version when its released content changed since the
     * last print, otherwise a reprint.
     */
    Optional<PrintOutcome> print(Long sampleId, String sysUserId);

    /**
     * Prints several orders through the same pipeline and joins the PDFs, each from
     * its own page 1.
     */
    byte[] printPatientReports(List<String> sampleIds, String sysUserId);

    /**
     * The archived version, when it exists and its order is within the user's
     * scope.
     */
    Optional<ReportVersion> version(Long versionId, String sysUserId);

    ReportQueuePreferenceDTO preferences(String sysUserId);

    ReportQueuePreferenceDTO savePreferences(String sysUserId, ReportQueuePreferenceDTO preference);
}
