package org.openelisglobal.program.valueholder.pathology;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public class PathologyCaseViewDisplayItem extends PathologyDisplayItem {

    /**
     * The day the specimen reached the laboratory, which is not the day the
     * examination was requested (FR-1). Held without a zone, so rendering it cannot
     * move it onto a neighbouring day.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate receivedDate;

    /**
     * One localized type name per sample item on the case, so two items of one type
     * read as that type twice (FR-1). Empty when the sample carries none.
     */
    private List<String> specimenTypes;

    private String age;

    private String sex;

    private String referringFacility;

    private String department;

    private String requester;

    private Questionnaire programQuestionnaire;

    private QuestionnaireResponse programQuestionnaireResponse;

    private String assignedTechnicianId;

    private String assignedPathologistId;

    private List<PathologyBlock> blocks;

    private List<PathologySlide> slides;

    private List<IdValuePair> techniques;

    private List<RequestView> requests;

    private List<ConclusionEntry> conclusions;

    private String conclusionText;

    private String grossExam;

    private String microscopyExam;

    private String reviewNotes;

    private boolean decalcificationRequired;

    /**
     * The name of every tissue type the case's cassettes hold, by dictionary id, so
     * an entry retired after a cassette recorded it still reads as a name.
     */
    private Map<String, String> tissueTypeNames;

    /**
     * The name of every stain the case's slides carry, by dictionary id, so a stain
     * retired after a slide recorded it still reads as a name.
     */
    private Map<String, String> stainNames;

    private List<PathologyReport> reports;

    /**
     * Who checked each slide's label against its block, and when, by slide id; a
     * slide never checked is absent.
     */
    private Map<Integer, LabelVerificationView> labelVerifications;

    /** Every stage the case entered, oldest first, as recorded by the server. */
    private List<StageEventView> stageEvents;

    /** The processor run written on the case's latest Processing entry. */
    private String runReference;

    /** Whether a case save would write a processor run now. */
    private boolean runReferenceEditable;

    /**
     * Who last recorded each slide's stain status, and when, by slide id; a slide
     * with no status recorded is absent.
     */
    private Map<Integer, SlideStampView> stainStamps;

    /**
     * Who recorded each slide's coverslip check, and when, by slide id; a slide not
     * checked is absent.
     */
    private Map<Integer, SlideStampView> coverslipQcStamps;

    /** The coverslipping batch written on the case's latest Coverslipping entry. */
    private String coverslipRunReference;

    /** Whether a case save would write a coverslipping batch now. */
    private boolean coverslipRunReferenceEditable;

    /**
     * The stage the case would be sent on to, or null; worked out by the server so
     * the screen and the move read the same rule.
     */
    private String nextStage;

    /** The stage the case would be returned to, or null. */
    private String previousStage;

    /**
     * Whether a release would be taken now, by the rule the release itself is
     * refused on.
     */
    private boolean releasable;

    /**
     * The stage at which the findings are written on this deployment (FR-12.1).
     */
    private String readingStage;

    /** Whether a save would take a change to the findings now. */
    private boolean findingsOpen;

    private boolean referredToIhc;

    /** The immunohistochemistry case this case was referred to; null if none. */
    private Integer ihcCaseId;

    /**
     * The tests already referred to immunohistochemistry on the order; never null.
     */
    private List<String> ihcReferredTestIds = List.of();

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }

    public List<String> getSpecimenTypes() {
        return specimenTypes;
    }

    public void setSpecimenTypes(List<String> specimenTypes) {
        this.specimenTypes = specimenTypes;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getReferringFacility() {
        return referringFacility;
    }

    public void setReferringFacility(String referringFacility) {
        this.referringFacility = referringFacility;
    }

    public Questionnaire getProgramQuestionnaire() {
        return programQuestionnaire;
    }

    public void setProgramQuestionnaire(Questionnaire programQuestionnaire) {
        this.programQuestionnaire = programQuestionnaire;
    }

    public QuestionnaireResponse getProgramQuestionnaireResponse() {
        return programQuestionnaireResponse;
    }

    public void setProgramQuestionnaireResponse(QuestionnaireResponse programQuestionnaireResponse) {
        this.programQuestionnaireResponse = programQuestionnaireResponse;
    }

    public List<PathologyBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<PathologyBlock> blocks) {
        this.blocks = blocks;
    }

    public List<PathologySlide> getSlides() {
        return slides;
    }

    public void setSlides(List<PathologySlide> slides) {
        this.slides = slides;
    }

    public List<ConclusionEntry> getConclusions() {
        return conclusions;
    }

    public void setConclusions(List<ConclusionEntry> conclusions) {
        this.conclusions = conclusions;
    }

    public List<IdValuePair> getTechniques() {
        return techniques;
    }

    public void setTechniques(List<IdValuePair> techniques) {
        this.techniques = techniques;
    }

    public String getGrossExam() {
        return grossExam;
    }

    public void setGrossExam(String grossExam) {
        this.grossExam = grossExam;
    }

    public String getMicroscopyExam() {
        return microscopyExam;
    }

    public void setMicroscopyExam(String microscopyExam) {
        this.microscopyExam = microscopyExam;
    }

    public boolean isDecalcificationRequired() {
        return decalcificationRequired;
    }

    public void setDecalcificationRequired(boolean decalcificationRequired) {
        this.decalcificationRequired = decalcificationRequired;
    }

    public Map<String, String> getTissueTypeNames() {
        return tissueTypeNames;
    }

    public void setTissueTypeNames(Map<String, String> tissueTypeNames) {
        this.tissueTypeNames = tissueTypeNames;
    }

    public Map<String, String> getStainNames() {
        return stainNames;
    }

    public void setStainNames(Map<String, String> stainNames) {
        this.stainNames = stainNames;
    }

    public String getConclusionText() {
        return conclusionText;
    }

    public void setConclusionText(String conclusionText) {
        this.conclusionText = conclusionText;
    }

    public List<RequestView> getRequests() {
        return requests;
    }

    public void setRequests(List<RequestView> requests) {
        this.requests = requests;
    }

    public String getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(String assignedTechnicianId) {
        this.assignedTechnicianId = assignedTechnicianId;
    }

    public String getAssignedPathologistId() {
        return assignedPathologistId;
    }

    public void setAssignedPathologistId(String assignedPathologistId) {
        this.assignedPathologistId = assignedPathologistId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public List<PathologyReport> getReports() {
        return reports;
    }

    public void setReports(List<PathologyReport> reports) {
        this.reports = reports;
    }

    public Map<Integer, LabelVerificationView> getLabelVerifications() {
        return labelVerifications;
    }

    public void setLabelVerifications(Map<Integer, LabelVerificationView> labelVerifications) {
        this.labelVerifications = labelVerifications;
    }

    public List<StageEventView> getStageEvents() {
        return stageEvents;
    }

    public void setStageEvents(List<StageEventView> stageEvents) {
        this.stageEvents = stageEvents;
    }

    public String getRunReference() {
        return runReference;
    }

    public void setRunReference(String runReference) {
        this.runReference = runReference;
    }

    public boolean isRunReferenceEditable() {
        return runReferenceEditable;
    }

    public void setRunReferenceEditable(boolean runReferenceEditable) {
        this.runReferenceEditable = runReferenceEditable;
    }

    public Map<Integer, SlideStampView> getStainStamps() {
        return stainStamps;
    }

    public void setStainStamps(Map<Integer, SlideStampView> stainStamps) {
        this.stainStamps = stainStamps;
    }

    public Map<Integer, SlideStampView> getCoverslipQcStamps() {
        return coverslipQcStamps;
    }

    public void setCoverslipQcStamps(Map<Integer, SlideStampView> coverslipQcStamps) {
        this.coverslipQcStamps = coverslipQcStamps;
    }

    public String getCoverslipRunReference() {
        return coverslipRunReference;
    }

    public void setCoverslipRunReference(String coverslipRunReference) {
        this.coverslipRunReference = coverslipRunReference;
    }

    public boolean isCoverslipRunReferenceEditable() {
        return coverslipRunReferenceEditable;
    }

    public void setCoverslipRunReferenceEditable(boolean coverslipRunReferenceEditable) {
        this.coverslipRunReferenceEditable = coverslipRunReferenceEditable;
    }

    public String getNextStage() {
        return nextStage;
    }

    public void setNextStage(String nextStage) {
        this.nextStage = nextStage;
    }

    public String getPreviousStage() {
        return previousStage;
    }

    public void setPreviousStage(String previousStage) {
        this.previousStage = previousStage;
    }

    public boolean isReleasable() {
        return releasable;
    }

    public void setReleasable(boolean releasable) {
        this.releasable = releasable;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public String getReadingStage() {
        return readingStage;
    }

    public void setReadingStage(String readingStage) {
        this.readingStage = readingStage;
    }

    public boolean isFindingsOpen() {
        return findingsOpen;
    }

    public void setFindingsOpen(boolean findingsOpen) {
        this.findingsOpen = findingsOpen;
    }

    public boolean isReferredToIhc() {
        return referredToIhc;
    }

    public void setReferredToIhc(boolean referredToIhc) {
        this.referredToIhc = referredToIhc;
    }

    public Integer getIhcCaseId() {
        return ihcCaseId;
    }

    public void setIhcCaseId(Integer ihcCaseId) {
        this.ihcCaseId = ihcCaseId;
    }

    public List<String> getIhcReferredTestIds() {
        return ihcReferredTestIds;
    }

    public void setIhcReferredTestIds(List<String> ihcReferredTestIds) {
        this.ihcReferredTestIds = ihcReferredTestIds == null ? List.of() : ihcReferredTestIds;
    }

    /**
     * One stage entry as the screen shows it: names and times already rendered, so
     * nothing on the screen can offer the operator or a date as an input.
     */
    public record StageEventView(Integer id, String stage, String direction, String enteredAt, String enteredBy,
            String completedAt, String completedBy, String reason, String runReference) {

        /**
         * Called inside the transaction that loaded the entry, since the operators are
         * associations of the row.
         */
        public static StageEventView of(PathologyStageEvent event) {
            return new StageEventView(event.getId(), event.getStage().name(), event.getDirection().name(),
                    formatted(event.getEnteredAt()), nameOf(event.getEnteredBy()), formatted(event.getCompletedAt()),
                    nameOf(event.getCompletedBy()), event.getReason(), event.getRunReference());
        }

        private static String formatted(Timestamp timestamp) {
            return timestamp == null ? null : DateUtil.convertTimestampToStringDateAndConfiguredHourTime(timestamp);
        }

        private static String nameOf(SystemUser user) {
            return user == null ? null : user.getDisplayName();
        }
    }

    /**
     * A slide label's check against its block: when, formatted as the stage entries
     * are, and who, by name. The slide row itself carries only the user's id.
     */
    public record LabelVerificationView(String verifiedAt, String verifiedBy) {

        /** The check on a slide, or null when its label was never checked. */
        public static LabelVerificationView of(PathologySlide slide, SystemUser verifier) {
            if (slide.getLabelVerifiedAt() == null) {
                return null;
            }
            return new LabelVerificationView(
                    DateUtil.convertTimestampToStringDateAndConfiguredHourTime(slide.getLabelVerifiedAt()),
                    verifier == null ? null : verifier.getDisplayName());
        }
    }

    /**
     * Who recorded something on a slide and when, formatted as the stage entries
     * are; the slide row itself carries only the user's id.
     *
     * <p>
     * The label check keeps its own {@link LabelVerificationView} with the same two
     * values under other names, because existing screens, specs and an outside QA
     * harness already read those names.
     */
    public record SlideStampView(String at, String by) {

        /** The stamp, or null when nothing was recorded. */
        public static SlideStampView of(Timestamp at, SystemUser by) {
            if (at == null) {
                return null;
            }
            return new SlideStampView(DateUtil.convertTimestampToStringDateAndConfiguredHourTime(at),
                    by == null ? null : by.getDisplayName());
        }
    }

    /**
     * One bench request as the requests table shows it (FR-10.1), every name
     * already rendered so the screen never composes one: the block or slide it
     * names and the one that fulfilled it as the bench prints them, retired or not,
     * the stain by name, and who raised it and when, formatted as the slide stamps
     * are. Kind, priority and status are the names of their values. A request on
     * file from before carries only its id, instruction and status.
     *
     * @param instruction what the bench is asked to do: the text as written, or for
     *                    a request on file the name of the entry it was picked by
     *                    (the stored value when no entry carries it)
     */
    public record RequestView(Integer id, String kind, Integer targetBlockId, Integer targetSlideId, String targetName,
            String stainId, String stainName, String priority, String instruction, String status, String cancelReason,
            Integer resultBlockId, Integer resultSlideId, String resultName, String raisedAt, String raisedBy) {
    }
}
