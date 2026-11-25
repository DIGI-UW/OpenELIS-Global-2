package org.openelisglobal.program.controller.cytology;

import java.util.List;
import org.openelisglobal.program.service.ClinicalAttachmentUpload;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis;
import org.openelisglobal.program.valueholder.cytology.CytologyReport;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.program.valueholder.cytology.CytologySlide;
import org.openelisglobal.program.valueholder.cytology.CytologySpecimenAdequacy;

public class CytologySampleForm {

    private CytologyStatus status;

    private List<CytologySlideForm> slides;

    private String assignedCytoPathologistId;

    private String assignedTechnicianId;

    private CytologySpecimenAdequacy specimenAdequacy;

    private String systemUserId;

    private Boolean release = false;

    private CytologyDiagnosis diagnosis;

    private List<CytologyReportForm> reports;

    public CytologyStatus getStatus() {
        return status;
    }

    public void setStatus(CytologyStatus status) {
        this.status = status;
    }

    public List<CytologySlideForm> getSlides() {
        return slides;
    }

    public void setSlides(List<CytologySlideForm> slides) {
        this.slides = slides;
    }

    public String getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(String assignedTechnicianId) {
        this.assignedTechnicianId = assignedTechnicianId;
    }

    public String getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(String systemUserId) {
        this.systemUserId = systemUserId;
    }

    public Boolean getRelease() {
        return release;
    }

    public void setRelease(Boolean release) {
        this.release = release;
    }

    public String getAssignedCytoPathologistId() {
        return assignedCytoPathologistId;
    }

    public void setAssignedCytoPathologistId(String assignedCytoPathologistId) {
        this.assignedCytoPathologistId = assignedCytoPathologistId;
    }

    public CytologySpecimenAdequacy getSpecimenAdequacy() {
        return specimenAdequacy;
    }

    public void setSpecimenAdequacy(CytologySpecimenAdequacy specimenAdequacy) {
        this.specimenAdequacy = specimenAdequacy;
    }

    public CytologyDiagnosis getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(CytologyDiagnosis diagnosis) {
        this.diagnosis = diagnosis;
    }

    public List<CytologyReportForm> getReports() {
        return reports;
    }

    public void setReports(List<CytologyReportForm> reports) {
        this.reports = reports;
    }

    public static class CytologySlideForm extends CytologySlide implements ClinicalAttachmentUpload {

        private static final long serialVersionUID = 3142138533368581327L;

        private String base64Image;

        public String getBase64Image() {
            return base64Image;
        }

        public void setBase64Image(String base64Image) {
            this.base64Image = base64Image;
        }
    }

    public static class CytologyReportForm extends CytologyReport implements ClinicalAttachmentUpload {

        private static final long serialVersionUID = 3142138533368581327L;

        private String base64Image;

        public String getBase64Image() {
            return base64Image;
        }

        public void setBase64Image(String base64Image) {
            this.base64Image = base64Image;
        }
    }
}
