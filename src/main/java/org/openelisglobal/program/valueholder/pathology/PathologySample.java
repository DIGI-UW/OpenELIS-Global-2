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
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion.ConclusionType;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestType;
import org.openelisglobal.program.valueholder.pathology.PathologyTechnique.TechniqueType;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Entity
@Table(name = "pathology_sample")
public class PathologySample extends ProgramSample {

    public enum PathologyStatus {
        GROSSING("Grossing"), CUTTING("Cutting"), PROCESSING("Processing"), SLICING("Slicing for Slides"),
        STAINING("Staining"), READY_PATHOLOGIST("Ready for Pathologist"),
        ADDITIONAL_REQUEST("Additional Pathologist Request"), COMPLETED("Completed");

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
    private PathologyStatus status = PathologyStatus.GROSSING;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pathology_sample_id")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<PathologyBlock> blocks;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pathology_sample_id")
    private List<PathologySlide> slides;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
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

    public List<PathologyRequest> getRequests() {
        return requests;
    }

    public String getRequests_Audit() {
        if (requests == null) {
            return null;
        } else {
            return StringUtils.join(requests.stream()
                    .map(e -> "Status: " + (e.getStatus() == null ? "" : e.getStatus().getDisplay()) + ", Value "
                            + (e.getType() == RequestType.DICTIONARY
                                    ? SpringContext.getBean(DictionaryService.class).get(e.getValue()).getDisplayValue()
                                    : e.getValue()))
                    .collect(Collectors.toList()), "; ");
        }
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

    public String getBlocks_Audit() {
        if (blocks == null) {
            return null;
        } else {
            return StringUtils.join(
                    blocks.stream().map(e -> "Block Number: " + e.getBlockNumber() + ", Location: " + e.getLocation())
                            .collect(Collectors.toList()),
                    "; ");
        }
    }

    public void setBlocks(List<PathologyBlock> blocks) {
        this.blocks = blocks;
    }

    public List<PathologySlide> getSlides() {
        return slides;
    }

    public String getSlides_Audit() {
        if (slides == null) {
            return null;
        } else {
            return StringUtils
                    .join(slides.stream().map(e -> "File Type: " + e.getFileType() + ", Location: " + e.getLocation())
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
