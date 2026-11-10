package org.openelisglobal.program.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.program.valueholder.pathology.PathologyStageEvent;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public interface PathologySampleService extends BaseObjectService<PathologySample, Integer> {

    /** Audit verb recorded when a cassette is cut. */
    String PATHOLOGY_BLOCK_CREATED = "PATHOLOGY_BLOCK_CREATED";

    /** Audit verb recorded when a cassette is embedded and becomes a block. */
    String PATHOLOGY_BLOCK_EMBEDDED = "PATHOLOGY_BLOCK_EMBEDDED";

    /** Audit verb recorded when a block is retired. */
    String PATHOLOGY_BLOCK_DEACTIVATED = "PATHOLOGY_BLOCK_DEACTIVATED";

    /** Audit verb recorded when a slide is retired. */
    String PATHOLOGY_SLIDE_DEACTIVATED = "PATHOLOGY_SLIDE_DEACTIVATED";

    /** Audit verb recorded when a case is sent on to a later stage. */
    String PATHOLOGY_STAGE_ADVANCED = "PATHOLOGY_STAGE_ADVANCED";

    /** Audit verb recorded when a case is returned to an earlier stage. */
    String PATHOLOGY_STAGE_RETURNED = "PATHOLOGY_STAGE_RETURNED";

    /** Audit verb recorded when the pathologist releases the case. */
    String PATHOLOGY_CASE_SIGNED_OUT = "PATHOLOGY_CASE_SIGNED_OUT";

    /** Audit verb recorded when the processor run is written at Processing. */
    String PATHOLOGY_RUN_REFERENCE_RECORDED = "PATHOLOGY_RUN_REFERENCE_RECORDED";

    List<PathologySample> getWithStatus(List<PathologyStatus> statuses);

    List<PathologySample> searchWithStatusAndTerm(List<PathologyStatus> statuses, String searchTerm);

    void assignTechnician(Integer pathologySampleId, SystemUser systemUser, String curUserId);

    void assignPathologist(Integer pathologySampleId, SystemUser systemUser, String curUserId);

    Long getCountWithStatus(List<PathologyStatus> statuses);

    Long getCountWithOpenRequests();

    /**
     * The case's slides, copied inside a transaction so a caller holding the case
     * outside any session can still iterate them.
     */
    List<PathologySlide> getSlides(Integer pathologySampleId);

    Long getCountWithStatusBetweenDates(List<PathologyStatus> statuses, Timestamp from, Timestamp to);

    void updateWithFormValues(Integer pathologySampleId, PathologySampleForm form);

    /**
     * Retires a block, keeping the row and the designation it carries. A block is
     * held for years after its case closes and a label printed from it must still
     * resolve, so a block is taken out of use rather than deleted.
     *
     * @return the block, or empty when no block carries that id
     */
    Optional<PathologyBlock> deactivateBlock(Integer blockId, String reason, String sysUserId);

    /**
     * Retires a slide, keeping the row and the designation it carries, on the same
     * reasoning as {@link #deactivateBlock}.
     *
     * @return the slide, or empty when no slide carries that id
     */
    Optional<PathologySlide> deactivateSlide(Integer slideId, String reason, String sysUserId);

    /**
     * Embeds a cassette, so the same row becomes the block slides are cut from.
     *
     * @return the row, unchanged when it is already a block, or empty when no block
     *         carries that id
     * @throws PathologyCaseRuleException when the row has been retired
     */
    Optional<PathologyBlock> embedBlock(Integer blockId, String sysUserId);

    /** The case a block belongs to, null when no case holds it. */
    PathologySample getCaseOfBlock(Integer blockId);

    /** The case a slide belongs to, null when no case holds it. */
    PathologySample getCaseOfSlide(Integer slideId);

    /**
     * Sends the case on to the next stage this deployment uses (FR-2.4), recording
     * who moved it and when. A stage left unfinished, or a case with no bench stage
     * after it, is refused with a message id the screen translates.
     *
     * @param from the stage the caller saw the case at
     * @return the event recording the case's entry onto its new stage
     * @throws PathologyStageConflictException when the case no longer stands at
     *                                         {@code from}
     */
    PathologyStageEvent advance(Integer pathologySampleId, PathologyStatus from, String sysUserId);

    /**
     * Returns the case to the nearest earlier stage that applies to it (FR-2.5),
     * recording the reason.
     *
     * @param from the stage the caller saw the case at
     * @return the event recording the case's entry onto the earlier stage
     * @throws PathologyStageConflictException when the case no longer stands at
     *                                         {@code from}
     */
    PathologyStageEvent returnToPreviousStage(Integer pathologySampleId, PathologyStatus from, String reason,
            String sysUserId);

    /** Every stage the case entered, oldest first. */
    List<PathologyStageEvent> getStageEvents(Integer pathologySampleId);

    /**
     * The stage {@link #advance} would send the case to, on this deployment's
     * switches and the case's decalcification flag; empty when the bench has
     * nowhere to send it.
     */
    Optional<PathologyStatus> nextStage(PathologySample pathologySample);

    /**
     * The stage {@link #returnToPreviousStage} would send the case back to, on the
     * same rules; empty when there is none.
     */
    Optional<PathologyStatus> previousStage(PathologySample pathologySample);

    /**
     * Whether a case save would take a processor run now: the case stands at
     * Processing on an entry that holds none yet.
     */
    boolean isRunReferenceRecordable(PathologySample pathologySample);

    /**
     * Whether a release would be taken now: the case stands in the pathologist's
     * queue, or it stands at Completed with no release on record.
     */
    boolean isReleasable(PathologySample pathologySample);
}
