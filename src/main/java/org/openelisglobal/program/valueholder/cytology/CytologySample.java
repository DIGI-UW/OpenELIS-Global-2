package org.openelisglobal.program.valueholder.cytology;

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
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Entity
@Table(name = "cytology_sample")
public class CytologySample extends ProgramSample {

    public enum CytologyStatus {
        PREPARING_SLIDES("Preparing slides"), SCREENING("Screening"),
        READY_FOR_CYTOPATHOLOGIST("Ready for Cytopathologist"), COMPLETED("Completed");

        private String display;

        CytologyStatus(String display) {
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
    @JoinColumn(name = "cytopathologist_id", referencedColumnName = "id")
    private SystemUser cytoPathologist;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "status")
    private CytologyStatus status = CytologyStatus.PREPARING_SLIDES;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cytology_sample_id")
    private List<CytologySlide> slides;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "specimen_adequacy_id", referencedColumnName = "id")
    private CytologySpecimenAdequacy specimenAdequacy;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cytology_diagnosis_id", referencedColumnName = "id")
    private CytologyDiagnosis diagnosis;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cytology_sample_id")
    private List<CytologyReport> reports;

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

    public CytologyStatus getStatus() {
        return status;
    }

    public void setStatus(CytologyStatus status) {
        this.status = status;
    }

    public List<CytologySlide> getSlides() {
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

    public void setSlides(List<CytologySlide> slides) {
        this.slides = slides;
    }

    public SystemUser getCytoPathologist() {
        return cytoPathologist;
    }

    public String getCytoPathologist_Audit() {
        if (cytoPathologist == null) {
            return null;
        } else {
            return cytoPathologist.getDisplayName();
        }
    }

    public void setCytoPathologist(SystemUser cytoPathologist) {
        this.cytoPathologist = cytoPathologist;
    }

    public CytologySpecimenAdequacy getSpecimenAdequacy() {
        return specimenAdequacy;
    }

    public String getSpecimenAdequacy_Audit() {
        if (specimenAdequacy == null) {
            return null;
        } else {
            return "Result Type: " + specimenAdequacy.getResultType() + ", Satisfaction: "
                    + specimenAdequacy.getSatisfaction() == null ? "" : specimenAdequacy.getSatisfaction().getDisplay();
        }
    }

    public void setSpecimenAdequacy(CytologySpecimenAdequacy specimenAdequacy) {
        this.specimenAdequacy = specimenAdequacy;
    }

    public CytologyDiagnosis getDiagnosis() {
        return diagnosis;
    }

    public String getDiagnosis_Audit() {
        if (diagnosis == null) {
            return null;
        } else {
            return "Negative Diagnosis: " + diagnosis.getNegativeDiagnosis();
        }
    }

    public void setDiagnosis(CytologyDiagnosis diagnosis) {
        this.diagnosis = diagnosis;
    }

    public List<CytologyReport> getReports() {
        return reports;
    }

    public String getReports_Audit() {
        if (reports == null) {
            return null;
        } else {
            return StringUtils.join(reports.stream()
                    .map(e -> "File Type: " + e.getFileType() + ", Report Type: "
                            + (e.getReportType() == null ? "" : e.getReportType().getDisplay()))
                    .collect(Collectors.toList()), "; ");
        }
    }

    public void setReports(List<CytologyReport> reports) {
        this.reports = reports;
    }
}
