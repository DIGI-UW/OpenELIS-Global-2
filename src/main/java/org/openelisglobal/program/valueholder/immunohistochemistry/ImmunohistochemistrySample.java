package org.openelisglobal.program.valueholder.immunohistochemistry;

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
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Entity
@Table(name = "immunohistochemistry_sample")
public class ImmunohistochemistrySample extends ProgramSample {

    private static final long serialVersionUID = -9073029687643009937L;

    public enum ImmunohistochemistryStatus {
        IN_PROGRESS("In Progress"), READY_PATHOLOGIST("Ready for Pathologist"), COMPLETED("Completed");

        private String display;

        ImmunohistochemistryStatus(String display) {
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
    private ImmunohistochemistryStatus status = ImmunohistochemistryStatus.IN_PROGRESS;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "immunohistochemistry_sample_id")
    private List<ImmunohistochemistrySampleReport> reports;

    @Valid
    @OneToOne
    @JoinColumn(name = "pathology_sample_id", referencedColumnName = "id")
    private PathologySample pathologySample;

    @Column(name = "reffered")
    private Boolean reffered = false;

    public ImmunohistochemistryStatus getStatus() {
        return status;
    }

    public void setStatus(ImmunohistochemistryStatus status) {
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

    public List<ImmunohistochemistrySampleReport> getReports() {
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

    public void setReports(List<ImmunohistochemistrySampleReport> reports) {
        this.reports = reports;
    }

    public PathologySample getPathologySample() {
        return pathologySample;
    }

    public void setPathologySample(PathologySample pathologySample) {
        this.pathologySample = pathologySample;
    }

    public Boolean getReffered() {
        return reffered;
    }

    public void setReffered(Boolean reffered) {
        this.reffered = reffered;
    }
}
