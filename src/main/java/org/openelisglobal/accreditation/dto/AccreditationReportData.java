package org.openelisglobal.accreditation.dto;

import java.util.Collections;
import java.util.List;

/**
 * OGC-686 — what a patient report should print for accreditation.
 *
 * <p>
 * Resolved once per rendered report and consumed only by the Jasper parameter
 * plumbing: never serialized to JSON, so raw logo bytes are the right shape
 * here — the report layer wraps each one in a {@code ByteArrayInputStream}
 * exactly like {@code leftHeaderImage}.
 *
 * <p>
 * At most three logos, because no jrxml uses {@code REPORT_PARAMETERS_MAP} —
 * every parameter is hand-plumbed per template, so "however many bodies
 * qualify" is not expressible and the slots are fixed at
 * {@code accredLogo1..3}.
 */
public class AccreditationReportData {

    public static final AccreditationReportData EMPTY = new AccreditationReportData(Collections.emptyList(), null);

    private final List<byte[]> logos;

    private final String notesLine;

    private final List<String> bodyNames;

    public AccreditationReportData(List<byte[]> logos, String notesLine) {
        this(logos, notesLine, Collections.emptyList());
    }

    public AccreditationReportData(List<byte[]> logos, String notesLine, List<String> bodyNames) {
        this.logos = logos == null ? Collections.emptyList() : logos;
        this.notesLine = notesLine;
        this.bodyNames = bodyNames == null ? Collections.emptyList() : bodyNames;
    }

    /** Logo bytes in display order, capped at the number of template slots. */
    public List<byte[]> getLogos() {
        return logos;
    }

    /**
     * The one-line accreditation statement, or null when no body qualifies — in
     * which case the template prints nothing at all.
     */
    public String getNotesLine() {
        return notesLine;
    }

    /**
     * The bodies the notes line names, in display order, so a report that words the
     * line from its own catalogue names the same bodies.
     */
    public List<String> getBodyNames() {
        return bodyNames;
    }
}
