package org.openelisglobal.accreditation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

/**
 * OGC-686 — a row of the Accrediting Bodies table.
 *
 * <p>
 * Assembled inside the service transaction (enrolled count and derived status
 * included) so nothing lazy escapes to the controller. Public fields, like the
 * Test Catalog editor's response DTOs — this is a wire shape, not a domain
 * object, and accessors would be 100 lines of nothing.
 */
public class AccreditingBodyView {

    public Long id;
    public String code;
    public String name;
    public String logoImageId;

    /**
     * The app's shared ObjectMapper leaves {@code WRITE_DATES_AS_TIMESTAMPS} on, so
     * an un-annotated {@code LocalDate} serializes as a {@code [y,m,d]} array.
     * Pinning the shape keeps the wire contract a plain ISO date string, the same
     * convention the pathology/cytology display items use.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate expiresOn;

    public String logoVisibilityMode;
    public Short thresholdPct;
    public Short displayOrder;
    public Boolean active;

    /** Tests currently enrolled under this body. Zero also means "deletable". */
    public long enrolledTestCount;

    /** Derived chip: ACTIVE / EXPIRING / EXPIRED / INACTIVE. */
    public String status;
}
