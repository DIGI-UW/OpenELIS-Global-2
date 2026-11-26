package org.openelisglobal.program.service;

/**
 * A change of a bench request's status from the case view (FR-10.8, FR-10.9).
 * The statuses are the names as posted, so the service refuses one it does not
 * know with a message the screen translates.
 *
 * @param from          the status the page showed, so a page that is out of
 *                      date cannot undo a change made since
 * @param status        the status asked for: OPENED, COMPLETED or CANCELLED
 * @param cancelReason  why the request is cancelled, required for CANCELLED and
 *                      ignored otherwise
 * @param resultBlockId the block that fulfilled a block request, optional
 * @param resultSlideId the slide that fulfilled a slide or stain request,
 *                      optional
 */
public record RequestStatusChange(String from, String status, String cancelReason, Integer resultBlockId,
        Integer resultSlideId) {
}
