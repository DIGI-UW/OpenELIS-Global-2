package org.openelisglobal.eqa.service;

import java.util.List;

/**
 * Pre-approved interpretive comments on a cycle's performance report (OGC-934).
 *
 * <p>
 * The library is the {@code EQA Report Comment} dictionary category, so the
 * existing Dictionary Management screen maintains it and deactivating an entry
 * stops new use without touching text already printed. An attachment is a note
 * against the cycle, which carries the author and the timestamp already.
 *
 * <p>
 * Nothing here accepts comment text: a caller names a library entry by id and
 * the service resolves the wording, so free text cannot reach a signed report.
 */
public interface EQAReportCommentService {

    /** The dictionary category that holds the pre-approved wording. */
    String CATEGORY_NAME = "EQA Report Comment";

    /** One selectable library entry. */
    record LibraryEntry(String id, String text) {
    }

    /** One comment attached to a cycle, with who attached it and when. */
    record AttachedComment(String id, String libraryEntryId, String text, String attachedBy, String attachedAt) {
    }

    /** Active library entries in their configured order. */
    List<LibraryEntry> getLibrary();

    /** Comments attached to this cycle, oldest first — the order they print in. */
    List<AttachedComment> getComments(Long cycleId);

    /**
     * Attaches each library entry that is not attached already and returns what was
     * added. Re-sending an attached id is a no-op rather than a second identical
     * paragraph on the report.
     *
     * @throws IllegalArgumentException if an id is not an active entry of
     *                                  {@link #CATEGORY_NAME}, or if its wording is
     *                                  too long to store as a note
     */
    List<AttachedComment> attach(Long cycleId, List<String> libraryEntryIds, String currentUserId);

    /**
     * Removes one attached comment.
     *
     * @throws IllegalArgumentException if the comment is not attached to this cycle
     */
    void detach(Long cycleId, String commentId);
}
