package org.openelisglobal.compliance.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

/**
 * TDD spec for LhuAmendmentService (OGC-776 Phase 2, T-203).
 *
 * Red → green → refactor. Tests must fail before the implementation exists.
 * Inversion tests: if the increment logic is replaced with a hardcoded value,
 * the second-amendment test fails. If the null-guard is removed, the
 * blank-reason test passes trivially and breaks the contract.
 */
@Rollback
public class LhuAmendmentServiceTest extends BaseWebContextSensitiveTest {

    private static final String SAMPLE_ID = "1";
    private static final String ACCESSION = "24-00001";

    @Autowired
    private LhuAmendmentService lhuAmendmentService;

    @Autowired
    private SampleService sampleService;

    /**
     * A fresh (never-amended) order has amendmentNumber == null. Untouched orders
     * must remain completely unaffected by the feature.
     */
    @Test
    public void nonAmendedOrder_amendmentNumberIsNull() {
        Sample sample = sampleService.get(SAMPLE_ID);
        assertNotNull("Fixture sample must exist", sample);
        assertNull("Never-amended order must have null amendmentNumber", sample.getAmendmentNumber());
    }

    /**
     * First amendment sets amendmentNumber to 1. Inversion: if the implementation
     * hardcodes 1, the second test below breaks.
     */
    @Test
    public void firstAmendment_setsAmendmentNumberToOne() {
        lhuAmendmentService.applyLhuAmendment(Long.parseLong(SAMPLE_ID), ACCESSION, "Incorrect pH result");

        Sample updated = sampleService.get(SAMPLE_ID);
        assertEquals("First amendment must set amendmentNumber to 1", Integer.valueOf(1), updated.getAmendmentNumber());
        assertEquals("amendsLhuNumber must record the prior certificate number", ACCESSION,
                updated.getAmendsLhuNumber());
        assertEquals("amendmentReason must be stored", "Incorrect pH result", updated.getAmendmentReason());
    }

    /**
     * Second amendment increments to 2. This is the inversion test: if the
     * implementation hardcodes 1 rather than (current + 1), this test fails.
     */
    @Test
    public void secondAmendment_incrementsAmendmentNumberToTwo() {
        lhuAmendmentService.applyLhuAmendment(Long.parseLong(SAMPLE_ID), ACCESSION, "First correction");
        lhuAmendmentService.applyLhuAmendment(Long.parseLong(SAMPLE_ID), ACCESSION + "/Am.1", "Second correction");

        Sample updated = sampleService.get(SAMPLE_ID);
        assertEquals("Second amendment must set amendmentNumber to 2", Integer.valueOf(2),
                updated.getAmendmentNumber());
        assertEquals("amendmentReason must reflect the latest reason", "Second correction",
                updated.getAmendmentReason());
    }

    /**
     * Blank reason must be rejected — ISO 17025 §7.8.8 requires a stated reason.
     */
    @Test
    public void blankReason_throwsIllegalArgumentException() {
        try {
            lhuAmendmentService.applyLhuAmendment(Long.parseLong(SAMPLE_ID), ACCESSION, "");
            fail("blank reason must throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // pass
        }
    }

    /**
     * Null reason also rejected.
     */
    @Test
    public void nullReason_throwsIllegalArgumentException() {
        try {
            lhuAmendmentService.applyLhuAmendment(Long.parseLong(SAMPLE_ID), ACCESSION, null);
            fail("null reason must throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // pass
        }
    }

    /**
     * certificateNumberWithAmendmentSuffix appends /Am.N when amendmentNumber >= 1.
     */
    @Test
    public void certificateSuffix_appendsAmN_whenAmended() {
        assertEquals("24-00001/Am.1", lhuAmendmentService.certificateNumberWithAmendmentSuffix("24-00001", 1));
        assertEquals("24-00001/Am.2", lhuAmendmentService.certificateNumberWithAmendmentSuffix("24-00001", 2));
    }

    /**
     * No suffix when amendmentNumber is null or 0.
     */
    @Test
    public void certificateSuffix_noSuffix_whenNullOrZero() {
        assertEquals("24-00001", lhuAmendmentService.certificateNumberWithAmendmentSuffix("24-00001", null));
        assertEquals("24-00001", lhuAmendmentService.certificateNumberWithAmendmentSuffix("24-00001", 0));
    }

    /**
     * /R revision suffix is preserved — the amendment suffix appends after it.
     */
    @Test
    public void certificateSuffix_preservesRevisionSuffix() {
        assertEquals("LHU-2026-0042/R/Am.1",
                lhuAmendmentService.certificateNumberWithAmendmentSuffix("LHU-2026-0042/R", 1));
    }

    /**
     * Null base returns null (defensive — callers may not have a certificate number
     * yet).
     */
    @Test
    public void certificateSuffix_nullBase_returnsNull() {
        assertNull(lhuAmendmentService.certificateNumberWithAmendmentSuffix(null, 1));
    }
}
