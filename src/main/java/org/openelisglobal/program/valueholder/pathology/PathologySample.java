package org.openelisglobal.program.valueholder.pathology;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.program.util.DesignationScheme;
import org.openelisglobal.program.util.DictionaryEntryNames;
import org.openelisglobal.program.util.PathologyDesignations;
import org.openelisglobal.program.util.PathologyRowOrder;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion.ConclusionType;
import org.openelisglobal.program.valueholder.pathology.PathologyTechnique.TechniqueType;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Entity
@Table(name = "pathology_sample")
public class PathologySample extends ProgramSample {

    /**
     * These are the histopathology bench stages a case passes through, in the order
     * they happen (FR-2.1 of the pathology case view FRS).
     *
     * Declaration order is the bench order and is the single source for the
     * Pathology Dashboard stage filter and the PATHOLOGY_STATUS display list
     * (FR-2.6), so constants must stay in bench order.
     *
     * Hibernate stores the constant name, so a name must never be removed without a
     * changeset that maps stored rows onto its replacement: CUTTING was absorbed
     * into GROSSING, SLICING became MICROTOMY, and ADDITIONAL_REQUEST was retired
     * because an outstanding request is a fact about pathology_request rows, not a
     * stage.
     */
    public enum PathologyStatus {
        ACCESSIONED("Accessioned"), GROSSING("Grossing"), DECALCIFICATION("Decalcification"), PROCESSING("Processing"),
        EMBEDDING("Embedding"), MICROTOMY("Microtomy"), STAINING("Staining"), COVERSLIPPING("Coverslipping & QC"),
        READY_PATHOLOGIST("Ready for Pathologist"), UNDER_REVIEW("Under Pathologist Review"), COMPLETED("Completed");

        private String display;

        PathologyStatus(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }

    @Valid
    @OneToOne
    @JoinColumn(name = "technician_id", referencedColumnName = "id")
    private SystemUser technician;

    @Valid
    @OneToOne
    @JoinColumn(name = "pathologist_id", referencedColumnName = "id")
    private SystemUser pathologist;

    @Enumerated(EnumType.STRING)
    @NotNull
    private PathologyStatus status = PathologyStatus.ACCESSIONED;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "pathology_sample_id")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<PathologyBlock> blocks;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "pathology_sample_id")
    private List<PathologySlide> slides;

    // A request is raised once and changes status in place, so it keeps its id,
    // its author and its history; no writer takes one out of the collection, and a
    // request is never deleted with it.
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "pathology_sample_id")
    private List<PathologyRequest> requests;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pathology_sample_id")
    private List<PathologyTechnique> techniques;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pathology_sample_id")
    private List<PathologyConclusion> conclusions;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pathology_sample_id")
    private List<PathologyReport> reports;

    @Column(name = "gross_exam")
    private String grossExam;

    @Column(name = "microscopy_exam")
    private String microscopyExam;

    /**
     * Whether the specimen needs decalcifying before processing, which is what
     * makes the DECALCIFICATION stage apply to this case.
     */
    @Column(name = "decalcification_required", nullable = false)
    private boolean decalcificationRequired;

    public List<PathologyRequest> getRequests() {
        return requests;
    }

    /**
     * Names each request by its instruction, with what it asks for, the block or
     * slide it names, the stain, the priority, the status, why it was cancelled and
     * what fulfilled it. Who raised it and when are left out: a change of status is
     * the history row's own user and time.
     */
    public String getRequests_Audit() {
        if (requests == null) {
            return null;
        }
        DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
        return StringUtils
                .join(PathologyRowOrder
                        .requests(requests).stream().map(
                                e -> "Request: " + StringUtils.defaultString(e.instruction(dictionaryService))
                                        + ", Kind: " + (e.getKind() == null ? "" : e.getKind().name()) + ", Target: "
                                        + nameOfBlock(e.getTargetBlockId()).or(() -> nameOfSlide(e.getTargetSlideId()))
                                                .orElse("")
                                        + ", Stain: " + dictionaryName(e.getStainId()) + ", Priority: "
                                        + (e.getPriority() == null ? "" : e.getPriority().name()) + ", Status: "
                                        + (e.getStatus() == null ? "" : e.getStatus().getDisplay())
                                        + ", Cancel reason: " + StringUtils.defaultString(e.getCancelReason())
                                        + ", Fulfilled by: " + nameOfBlock(e.getResultBlockId())
                                                .or(() -> nameOfSlide(e.getResultSlideId())).orElse(""))
                        .collect(Collectors.toList()), "; ");
    }

    /**
     * The block of this case under that id, retired or not; empty for no id or an
     * id the case holds no block under.
     */
    public Optional<PathologyBlock> blockById(Integer blockId) {
        if (blockId == null || blocks == null) {
            return Optional.empty();
        }
        return blocks.stream().filter(block -> blockId.equals(block.getId())).findFirst();
    }

    /**
     * The slide of this case under that id, retired or not; empty for no id or an
     * id the case holds no slide under.
     */
    public Optional<PathologySlide> slideById(Integer slideId) {
        if (slideId == null || slides == null) {
            return Optional.empty();
        }
        return slides.stream().filter(slide -> slideId.equals(slide.getId())).findFirst();
    }

    /** A block of this case as the bench names it ("A1"), retired or not. */
    public Optional<String> nameOfBlock(Integer blockId) {
        return blockById(blockId).map(PathologyBlock::displayIdentifier);
    }

    /**
     * A slide of this case as its label and the bench name it, block first
     * ("A1.1"), retired or not.
     */
    public Optional<String> nameOfSlide(Integer slideId) {
        return slideById(slideId)
                .map(slide -> PathologyDesignations.slideOnBlockName(blockById(slide.getBlockId()).orElse(null), slide,
                        DesignationScheme.fromConfiguration()));
    }

    public void setRequests(List<PathologyRequest> requests) {
        this.requests = requests;
    }

    public List<PathologyTechnique> getTechniques() {
        return techniques;
    }

    public String getTechniques_Audit() {
        if (techniques == null) {
            return null;
        } else {
            return StringUtils.join(techniques.stream()
                    .map(e -> "Value: " + (e.getType() == TechniqueType.DICTIONARY
                            ? SpringContext.getBean(DictionaryService.class).get(e.getValue()).getDisplayValue()
                            : "" + e.getValue()))
                    .collect(Collectors.toList()), "; ");
        }
    }

    public void setTechniques(List<PathologyTechnique> techniques) {
        this.techniques = techniques;
    }

    public List<PathologyConclusion> getConclusions() {
        return conclusions;
    }

    public String getConclusions_Audit() {
        if (conclusions == null) {
            return null;
        } else {
            return StringUtils.join(conclusions.stream()
                    .map(e -> "Value: " + (e.getType() == ConclusionType.DICTIONARY
                            ? SpringContext.getBean(DictionaryService.class).get(e.getValue()).getDisplayValue()
                            : "" + e.getValue()))
                    .collect(Collectors.toList()), "; ");
        }
    }

    public void setConclusions(List<PathologyConclusion> conclusions) {
        this.conclusions = conclusions;
    }

    public PathologyStatus getStatus() {
        return status;
    }

    public void setStatus(PathologyStatus status) {
        this.status = status;
    }

    public SystemUser getTechnician() {
        return technician;
    }

    public String getTechnician_Audit() {
        if (technician == null) {
            return null;
        } else {
            return technician.getDisplayName();
        }
    }

    public void setTechnician(SystemUser technician) {
        this.technician = technician;
    }

    public SystemUser getPathologist() {
        return pathologist;
    }

    public String getPathologist_Audit() {
        if (pathologist == null) {
            return null;
        } else {
            return pathologist.getDisplayName();
        }
    }

    public void setPathologist(SystemUser pathologist) {
        this.pathologist = pathologist;
    }

    public List<PathologyBlock> getBlocks() {
        return blocks;
    }

    /** Names each block by the identifier written on it and the tissue it holds. */
    public String getBlocks_Audit() {
        if (blocks == null) {
            return null;
        } else {
            return StringUtils.join(blocks.stream()
                    .map(e -> "Block: " + e.displayIdentifier() + ", Cassette State: "
                            + (e.getCassetteState() == null ? "" : e.getCassetteState().name()) + ", Location: "
                            + StringUtils.defaultString(e.getLocation()) + ", Tissue: "
                            + dictionaryName(e.getTissueTypeId()) + ", Active: " + e.isActive())
                    .collect(Collectors.toList()), "; ");
        }
    }

    /**
     * A dictionary entry by its name, so a history row says what a cassette held or
     * how a slide was stained rather than a bare id; the id stands in for a missing
     * entry.
     */
    private static String dictionaryName(String dictionaryId) {
        return DictionaryEntryNames.localizedNameOf(SpringContext.getBean(DictionaryService.class), dictionaryId)
                .orElse(StringUtils.defaultString(dictionaryId));
    }

    public void setBlocks(List<PathologyBlock> blocks) {
        this.blocks = blocks;
    }

    public List<PathologySlide> getSlides() {
        return slides;
    }

    /**
     * Names each slide by the identifier written on it and by its parent block,
     * with the level it was cut at, the stain it carries and what staining and the
     * coverslip check found. Who recorded those and when is left out: those are the
     * history row's own user and time.
     */
    public String getSlides_Audit() {
        if (slides == null) {
            return null;
        } else {
            return StringUtils.join(slides.stream().map(e -> "Slide: " + e.displayIdentifier() + ", Block: "
                    + (e.getBlockId() == null ? "" : e.getBlockId()) + ", Location: "
                    + StringUtils.defaultString(e.getLocation()) + ", Level: " + StringUtils.defaultString(e.getLevel())
                    + ", Stain: " + dictionaryName(e.getStainId()) + ", Active: " + e.isActive() + ", File Type: "
                    + StringUtils.defaultString(e.getFileType()) + ", Label verified: "
                    + (e.getLabelVerifiedAt() == null ? ""
                            : DateUtil.convertTimestampToStringDateAndConfiguredHourTime(e.getLabelVerifiedAt()))
                    + ", Stain status: " + (e.getStainStatus() == null ? "" : e.getStainStatus().name())
                    + ", Coverslip QC: " + (e.getCoverslipQc() == null ? "" : e.getCoverslipQc().name()))
                    .collect(Collectors.toList()), "; ");
        }
    }

    public void setSlides(List<PathologySlide> slides) {
        this.slides = slides;
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

    public List<PathologyReport> getReports() {
        return reports;
    }

    public String getReports_Audit() {
        if (reports == null) {
            return null;
        } else {
            return StringUtils.join(reports.stream()
                    .map(e -> "File Type : " + e.getFileType() + ", Report Type: "
                            + (e.getReportType() == null ? "" : e.getReportType().getDisplay()))
                    .collect(Collectors.toList()), "; ");
        }
    }

    public void setReports(List<PathologyReport> reports) {
        this.reports = reports;
    }
}
