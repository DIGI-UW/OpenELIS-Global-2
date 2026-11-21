package org.openelisglobal.program.service;

/**
 * A bench request as the pathologist raises it from the case view (FR-10.2 to
 * FR-10.7). Kind and priority are the names as posted, so the service refuses
 * one it does not know with a message the screen translates rather than the
 * request failing to bind. A target or stain the kind does not take is ignored.
 *
 * @param kind          BLOCK, SLIDE or STAIN
 * @param targetBlockId the block a slide request is cut from
 * @param targetSlideId the slide a stain request is applied to
 * @param stainId       the stain a stain request asks for, a dictionary id
 * @param priority      NORMAL, URGENT or STAT; blank is NORMAL
 * @param instruction   what the bench is asked to do
 */
public record RequestDraft(String kind, Integer targetBlockId, Integer targetSlideId, String stainId, String priority,
        String instruction) {
}
