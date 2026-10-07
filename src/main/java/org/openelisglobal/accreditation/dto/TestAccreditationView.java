package org.openelisglobal.accreditation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

/**
 * OGC-686 — a row of the Test Accreditations table: one (test × body)
 * enrollment.
 *
 * <p>
 * Test and body labels are resolved inside the service transaction, so the
 * table renders without the controller touching a lazy association. The status
 * carried here is the <em>body's</em> status — enrollments have no expiry of
 * their own.
 */
public class TestAccreditationView {

    public Long id;

    public String testId;
    public String testName;

    public Long accreditingBodyId;
    public String bodyCode;
    public String bodyName;

    /**
     * ISO date string on the wire — see the note on {@link AccreditingBodyView}.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate effectiveFrom;

    /** The owning body's expiry — carried so the table needs no second lookup. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate bodyExpiresOn;

    /** The owning body's derived status chip. */
    public String status;
}
