package org.openelisglobal.compliance.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.compliance.valueholder.ComplianceReportArchive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

/**
 * TDD spec for ComplianceReportArchiveService (OGC-776 Phase 5, T-503).
 *
 * Key invariants: - archiveIfAbsent saves on first call, returns the SAME row
 * on subsequent calls (immutability guard). - SHA-256 is stable (same bytes →
 * same hash, always 64 hex chars). - amendmentNumber keys versions separately
 * (0 = original, 1 = first amendment). - null amendmentNumber is treated as 0.
 */
@Rollback
public class ComplianceReportArchiveServiceTest extends BaseWebContextSensitiveTest {

    private static final String SAMPLE_ID = "1";
    private static final byte[] PDF_BYTES_V0 = "PDF content original".getBytes();
    private static final byte[] PDF_BYTES_V1 = "PDF content amendment 1".getBytes();

    @Autowired
    private ComplianceReportArchiveService archiveService;

    /**
     * First call archives the PDF and returns a persisted record with a valid
     * SHA-256.
     */
    @Test
    public void archiveIfAbsent_firstCall_persistsAndReturnsSha256() {
        ComplianceReportArchive archived = archiveService.archiveIfAbsent(Long.parseLong(SAMPLE_ID), 0, PDF_BYTES_V0,
                "testuser");

        assertNotNull("Archived record must not be null", archived);
        assertNotNull("ID must be assigned after persist", archived.getId());
        assertNotNull("sha256Hash must not be null", archived.getSha256Hash());
        assertEquals("SHA-256 must be 64 hex chars", 64, archived.getSha256Hash().length());
        assertArrayEquals("PDF bytes must be stored", PDF_BYTES_V0, archived.getPdfContent());
        assertEquals("amendmentNumber must be 0 for original", Integer.valueOf(0), archived.getAmendmentNumber());
    }

    /**
     * Second call with the same (sampleId, amendmentNumber) returns the EXISTING
     * row — not a new save. Inversion: if the immutability guard is removed, two
     * rows exist and the second call returns a new one.
     */
    @Test
    public void archiveIfAbsent_secondCall_returnsExistingRowWithoutNewSave() {
        ComplianceReportArchive first = archiveService.archiveIfAbsent(Long.parseLong(SAMPLE_ID), 0, PDF_BYTES_V0,
                "testuser");
        ComplianceReportArchive second = archiveService.archiveIfAbsent(Long.parseLong(SAMPLE_ID), 0,
                "different bytes".getBytes(), "testuser");

        assertEquals("Second call must return the same row id as the first", first.getId(), second.getId());
        assertArrayEquals("PDF content must not be overwritten", PDF_BYTES_V0, second.getPdfContent());
    }

    /**
     * Different amendmentNumbers are stored as separate rows — version isolation.
     */
    @Test
    public void archiveIfAbsent_differentAmendmentNumbers_storedSeparately() {
        ComplianceReportArchive v0 = archiveService.archiveIfAbsent(Long.parseLong(SAMPLE_ID), 0, PDF_BYTES_V0,
                "testuser");
        ComplianceReportArchive v1 = archiveService.archiveIfAbsent(Long.parseLong(SAMPLE_ID), 1, PDF_BYTES_V1,
                "testuser");

        assertNotNull(v0.getId());
        assertNotNull(v1.getId());
        assertTrue("Version rows must be distinct", !v0.getId().equals(v1.getId()));
        assertArrayEquals(PDF_BYTES_V0, v0.getPdfContent());
        assertArrayEquals(PDF_BYTES_V1, v1.getPdfContent());
    }

    /**
     * SHA-256 is stable: same bytes always produce the same hash.
     */
    @Test
    public void archiveIfAbsent_sha256IsStableAndVerifiable() {
        ComplianceReportArchive archived = archiveService.archiveIfAbsent(Long.parseLong(SAMPLE_ID), 0, PDF_BYTES_V0,
                "testuser");

        // Re-compute and compare
        ComplianceReportArchive archived2 = archiveService.archiveIfAbsent(Long.parseLong(SAMPLE_ID), 1, PDF_BYTES_V0,
                "testuser");

        assertEquals("Same bytes must produce the same SHA-256", archived.getSha256Hash(), archived2.getSha256Hash());
    }

    /**
     * SHA-256 differs for different content — not a constant.
     */
    @Test
    public void archiveIfAbsent_sha256DiffersForDifferentContent() {
        ComplianceReportArchive v0 = archiveService.archiveIfAbsent(Long.parseLong(SAMPLE_ID), 0, PDF_BYTES_V0,
                "testuser");
        ComplianceReportArchive v1 = archiveService.archiveIfAbsent(Long.parseLong(SAMPLE_ID), 1, PDF_BYTES_V1,
                "testuser");

        assertTrue("Different PDF bytes must produce different SHA-256",
                !v0.getSha256Hash().equals(v1.getSha256Hash()));
    }

    /**
     * null amendmentNumber is treated as 0 (defensive — callers may pass null
     * before first amendment).
     */
    @Test
    public void archiveIfAbsent_nullAmendmentNumber_treatedAsZero() {
        ComplianceReportArchive withNull = archiveService.archiveIfAbsent(Long.parseLong(SAMPLE_ID), null, PDF_BYTES_V0,
                "testuser");
        ComplianceReportArchive withZero = archiveService.archiveIfAbsent(Long.parseLong(SAMPLE_ID), 0, PDF_BYTES_V0,
                "testuser");

        assertEquals("null and 0 must resolve to the same row", withNull.getId(), withZero.getId());
        assertEquals("amendmentNumber must be stored as 0", Integer.valueOf(0), withNull.getAmendmentNumber());
    }

    /**
     * findBySampleIdAndAmendmentNumber returns the archived record after it is
     * saved.
     */
    @Test
    public void findBySampleIdAndAmendmentNumber_returnsArchivedRecord() {
        archiveService.archiveIfAbsent(Long.parseLong(SAMPLE_ID), 0, PDF_BYTES_V0, "testuser");

        Optional<ComplianceReportArchive> found = archiveService
                .findBySampleIdAndAmendmentNumber(Long.parseLong(SAMPLE_ID), 0);

        assertTrue("Must find the archived record", found.isPresent());
        assertArrayEquals("PDF content must match", PDF_BYTES_V0, found.get().getPdfContent());
    }
}
