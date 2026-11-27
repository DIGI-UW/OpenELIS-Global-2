package org.openelisglobal.program.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.valueholder.pathology.CoverslipQc;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.program.valueholder.pathology.PathologyStageEvent;
import org.openelisglobal.program.valueholder.pathology.StainStatus;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public interface PathologySampleService extends BaseObjectService<PathologySample, Integer> {

    /** Audit verb recorded when a cassette is cut. */
    String PATHOLOGY_BLOCK_CREATED = "PATHOLOGY_BLOCK_CREATED";

    /** Audit verb recorded when a cassette is embedded and becomes a block. */
    String PATHOLOGY_BLOCK_EMBEDDED = "PATHOLOGY_BLOCK_EMBEDDED";

    /** Audit verb recorded when a block is retired. */
    String PATHOLOGY_BLOCK_DEACTIVATED = "PATHOLOGY_BLOCK_DEACTIVATED";

    /** Audit verb recorded when a slide is cut from a block. */
    String PATHOLOGY_SLIDE_CREATED = "PATHOLOGY_SLIDE_CREATED";

    /** Audit verb recorded when a slide is retired. */
    String PATHOLOGY_SLIDE_DEACTIVATED = "PATHOLOGY_SLIDE_DEACTIVATED";

    /** Audit verb recorded when a slide's printed label matches its block. */
    String PATHOLOGY_SLIDE_LABEL_VERIFIED = "PATHOLOGY_SLIDE_LABEL_VERIFIED";

    /**
     * Audit verb recorded when the identifiers scanned for a slide do not match it
     * and its block.
     */
    String PATHOLOGY_SLIDE_LABEL_MISMATCH = "PATHOLOGY_SLIDE_LABEL_MISMATCH";

    /**
     * Audit verb recorded when staining a slide is found good or unsatisfactory;
     * the payload says which.
     */
    String PATHOLOGY_SLIDE_STAINED = "PATHOLOGY_SLIDE_STAINED";

    /** Audit verb recorded when a slide's coverslip check passes or fails. */
    String PATHOLOGY_SLIDE_QC_RECORDED = "PATHOLOGY_SLIDE_QC_RECORDED";

    /** Audit verb recorded when a case is sent on to a later stage. */
    String PATHOLOGY_STAGE_ADVANCED = "PATHOLOGY_STAGE_ADVANCED";

    /** Audit verb recorded when a case is returned to an earlier stage. */
    String PATHOLOGY_STAGE_RETURNED = "PATHOLOGY_STAGE_RETURNED";

    /** Audit verb recorded when the pathologist releases the case. */
    String PATHOLOGY_CASE_SIGNED_OUT = "PATHOLOGY_CASE_SIGNED_OUT";

    /**
     * Audit verb recorded when a signed-out case is reopened; the payload names the
     * stage it went to, the reason and the final report it voided, if any.
     */
    String PATHOLOGY_CASE_REOPENED = "PATHOLOGY_CASE_REOPENED";

    /**
     * Audit verb recorded when a sign-out raises a critical result; the payload
     * names the matching diagnoses and whether the pathologist flagged the case.
     */
    String PATHOLOGY_CRITICAL_RESULT_EMITTED = "PATHOLOGY_CRITICAL_RESULT_EMITTED";

    /**
     * Audit verb recorded when the processor run is written at Processing, or the
     * coverslipping batch at Coverslipping; the payload names the stage.
     */
    String PATHOLOGY_RUN_REFERENCE_RECORDED = "PATHOLOGY_RUN_REFERENCE_RECORDED";

    /** Audit verb recorded when a pathologist raises a bench request. */
    String PATHOLOGY_REQUEST_RAISED = "PATHOLOGY_REQUEST_RAISED";

    /**
     * Audit verb recorded when a bench request is completed, cancelled or reopened.
     */
    String PATHOLOGY_REQUEST_STATUS_CHANGED = "PATHOLOGY_REQUEST_STATUS_CHANGED";

    /**
     * Audit verb recorded on the case when it is sent to the pathologist with bench
     * requests still open, naming them.
     */
    String PATHOLOGY_REQUEST_OVERRIDDEN = "PATHOLOGY_REQUEST_OVERRIDDEN";

    /**
     * Audit verb recorded on the case when a save changes its coded or narrative
     * conclusion; the payload names the codes, never the narrative.
     */
    String PATHOLOGY_CONCLUSION_SAVED = "PATHOLOGY_CONCLUSION_SAVED";

    /**
     * Audit verb recorded on the case when a save refers it to immunohistochemistry
     * or orders more markers on the referral.
     */
    String PATHOLOGY_IHC_REFERRED = "PATHOLOGY_IHC_REFERRED";

    /** Dictionary category a laboratory fills with the tissue types it cuts. */
    String TISSUE_TYPE_CATEGORY = "pathology_tissue_types";

    /** Dictionary category of the coded diagnoses a pathologist may conclude. */
    String CONCLUSION_CATEGORY = "pathologist_conclusions";

    /** Dictionary category of the techniques a case's findings may record. */
    String TECHNIQUE_CATEGORY = "pathology_techniques";

    /**
     * Dictionary category of the stains a slide may carry, shipped by the
     * configuration loader and extended by the laboratory. The code reads the
     * category by this name.
     */
    String STAIN_CATEGORY = "Pathology Stains";

    /**
     * What checking a slide's label against its block found: the slide as it now
     * stands, and whether the identifiers given matched it and its block.
     */
    record LabelVerification(PathologySlide slide, boolean match) {
    }

    List<PathologySample> getWithStatus(List<PathologyStatus> statuses);

    List<PathologySample> searchWithStatusAndTerm(List<PathologyStatus> statuses, String searchTerm);

    void assignTechnician(Integer pathologySampleId, SystemUser systemUser, String curUserId);

    void assignPathologist(Integer pathologySampleId, SystemUser systemUser, String curUserId);

    Long getCountWithStatus(List<PathologyStatus> statuses);

    Long getCountWithOpenRequests();

    /**
     * The case's slides, copied inside a transaction so a caller holding the case
     * outside any session can still iterate them, in the order the case view lists
     * them, so printed labels come out in that order too.
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

    /**
     * Checks a slide's printed label against its block before the slide is
     * sectioned (FR-7.3 to FR-7.5). The block field must carry the parent block's
     * barcode and the slide field the slide's own; where the laboratory has no
     * scanner the identifiers as printed are taken instead. A match stamps the
     * slide with the session user and the server clock.
     *
     * <p>
     * The refusals come first, in this order: a blank field, a retired slide, a
     * slide with no block recorded and a slide of a signed-out case, each with a
     * message id the screen translates. Then the identifiers are compared, whether
     * or not the slide was verified before.
     *
     * <p>
     * A mismatch is not thrown: it is answered as a result with {@code match}
     * false, so the audit entry recording it commits rather than being rolled back
     * with a refusal. That holds for a slide already verified too: a wrong scan is
     * recorded, and the earlier stamp stands. A matching scan of a slide already
     * verified writes nothing.
     *
     * @return the outcome, or empty when no slide carries that id
     */
    Optional<LabelVerification> verifySlideLabel(Integer slideId, String block, String slide, String sysUserId);

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
     * {@link #advance(Integer, PathologyStatus, String)}, confirming the bench
     * requests the page listed as open (FR-10.11). A move onto the pathologist's
     * queue while a request is open goes ahead only when every open request is in
     * {@code acknowledgedRequestIds}; otherwise it is refused, naming each open
     * request's instruction, so a request raised behind an open page is shown
     * before it is passed over. An id that is not open is ignored. A move that goes
     * ahead past open requests records them on the case before the move itself.
     * Every other move is unchanged.
     *
     * @param acknowledgedRequestIds the requests the page showed as open; null is
     *                               none
     */
    PathologyStageEvent advance(Integer pathologySampleId, PathologyStatus from, String sysUserId,
            List<Integer> acknowledgedRequestIds);

    /**
     * Raises a bench request on the case (FR-10.3 to FR-10.7), open, as the session
     * user at the server clock, and audits it. A request may be raised at any stage
     * until the case is signed out.
     *
     * <p>
     * The refusals, in this order, each with a message id the screen translates: no
     * kind or an unknown one, a blank instruction or one longer than its column, an
     * unknown priority, a signed-out case; then by kind, a slide request without a
     * block of the case in use, a stain request without a slide of the case in use,
     * and a stain request without a stain in use on the stain list. Nothing is
     * written before all of them pass. A block request names no target and no
     * stain, and anything posted that the kind does not take is ignored.
     *
     * @return the request as stored, with its id
     */
    PathologyRequest raiseRequest(Integer pathologySampleId, RequestDraft draft, String sysUserId);

    /**
     * Completes, cancels or reopens a bench request (FR-10.8, FR-10.9). An open
     * request may be completed or cancelled and a closed one reopened; a completed
     * one is not cancelled, nor a cancelled one completed, without reopening it
     * first. A completion may name the block or slide that fulfilled it, and a
     * reopening clears that and any reason.
     *
     * <p>
     * The refusals, in this order: no status or an unknown one, a cancellation
     * without a reason or with one longer than its column, a signed-out case, a
     * request not on the case; then a request no longer at {@code from} is a
     * conflict; the status it already holds is answered as it stands with nothing
     * written; then a transition not allowed, and for a completion a result of the
     * wrong kind, not on the case or retired. A request on file, which has no kind,
     * takes no result.
     *
     * @return the request as it now stands
     * @throws PathologyRequestConflictException when the request is no longer at
     *                                           {@code from}
     */
    PathologyRequest changeRequestStatus(Integer pathologySampleId, Integer requestId, RequestStatusChange change,
            String sysUserId);

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

    /**
     * Reopens a signed-out case at the reading stage (S-8.3), voiding the final
     * report in force and recording the reason.
     *
     * @param from the stage the caller saw the case at
     * @return the event recording the case's entry onto the reading stage
     * @throws PathologyStageConflictException when the case no longer stands at
     *                                         {@code from}
     */
    PathologyStageEvent reopen(Integer pathologySampleId, PathologyStatus from, String reason, String sysUserId);

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
     * Whether a case save would take a run reference for the stage now: the case
     * stands at that stage, Processing for the processor run or Coverslipping for
     * the coverslipping batch, on an entry that holds none yet.
     */
    boolean isRunReferenceRecordable(PathologySample pathologySample, PathologyStatus stage);

    /**
     * Records what staining found for the named slides of the case, one slide or a
     * batch alike (FR-8.1, FR-8.2), with the session user and the server clock.
     * Every slide is checked before any is written, so a refusal leaves all of them
     * as they were. A slide already at that status is left alone and audited no
     * further. A change of status clears the slide's coverslip check, since a slide
     * restained or found unsatisfactory is coverslipped and checked again.
     *
     * <p>
     * Marks are taken only while the case is on the bench, at Staining or
     * Coverslipping. The refusals, in this order, each with a message id the screen
     * translates: no slide or no status named, a signed-out case, a case not yet at
     * Staining, a case already with the pathologist, a slide that is not on the
     * case, a retired slide, and a slide marked stained with no stain recorded. A
     * change raises the case's version, so a mark and a stage move landing together
     * cannot both commit.
     *
     * @return the named slides as they now stand, in the order the case view lists
     *         them
     */
    List<PathologySlide> recordStainStatus(Integer pathologySampleId, List<Integer> slideIds, StainStatus status,
            String sysUserId);

    /**
     * Records the coverslip check of the named slides of the case (FR-8.4), on the
     * same rules as {@link #recordStainStatus}, taken only at Coverslipping. Where
     * the laboratory tracks Staining, a slide not marked stained is refused: a
     * check of an unstained slide checks nothing.
     *
     * @return the named slides as they now stand, in the order the case view lists
     *         them
     */
    List<PathologySlide> recordCoverslipQc(Integer pathologySampleId, List<Integer> slideIds, CoverslipQc result,
            String sysUserId);

    /**
     * Whether a release would be taken now: the case stands in the pathologist's
     * queue, or it stands at Completed with no release on record.
     */
    boolean isReleasable(PathologySample pathologySample);

    /** Signed out: on Completed and released, so nothing on the case changes. */
    boolean isSignedOut(PathologySample pathologySample);

    /**
     * The stage at which the findings are written on this deployment: Under
     * Pathologist Review where it uses that stage, else Ready for Pathologist.
     */
    PathologyStatus readingStage();

    /**
     * Whether a save takes a change to the findings now (FR-12.1): the case is
     * under review (also when parked there), stands at the reading stage, or at
     * Completed and never released.
     */
    boolean findingsOpen(PathologySample pathologySample);

    /**
     * Writes the audit entry an event on a row beside the case leaves behind: the
     * verb and its facts travel as a JSON payload against the row's reference
     * table, and never carry anything about the patient.
     */
    void recordRowEvent(String referenceTableName, String referenceId, String activity, Map<String, Object> payload,
            String sysUserId);

    /**
     * The tests the case's order already holds as immunohistochemistry referral
     * analyses, in the order's analysis order; a save does not order these again.
     * Empty when none.
     */
    List<String> ihcReferredTestIds(PathologySample pathologySample);
}
