package org.openelisglobal.program.controller.immunohistochemistry;

import java.util.Base64;
import java.util.List;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample.ImmunohistochemistryStatus;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySampleReport;

public class ImmunohistochemistrySampleForm {

    private ImmunohistochemistryStatus status;
    private String assignedPathologistId;
    private String assignedTechnicianId;
    private String systemUserId;
    private Boolean release = false;

    private List<ImmunohistochemistryReportForm> reports;

    public ImmunohistochemistryStatus getStatus() {
        return status;
    }

    public void setStatus(ImmunohistochemistryStatus status) {
        this.status = status;
    }

    public String getAssignedPathologistId() {
        return assignedPathologistId;
    }

    public void setAssignedPathologistId(String assignedPathologistId) {
        this.assignedPathologistId = assignedPathologistId;
    }

    public String getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(String assignedTechnicianId) {
        this.assignedTechnicianId = assignedTechnicianId;
    }

    public Boolean getRelease() {
        return release;
    }

    public void setRelease(Boolean release) {
        this.release = release;
    }

    public String getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(String systemUserId) {
        this.systemUserId = systemUserId;
    }

    public List<ImmunohistochemistryReportForm> getReports() {
        return reports;
    }

    public void setReports(List<ImmunohistochemistryReportForm> reports) {
        this.reports = reports;
    }

    public static class ImmunohistochemistryReportForm extends ImmunohistochemistrySampleReport {
        private static final long serialVersionUID = 3142138533368581327L;
        private String base64Image;

        public String getBase64Image() {
            return base64Image;
        }

        public void setBase64Image(String base64Image) {
            this.base64Image = base64Image;
            String[] imageInfo = base64Image.split(";base64,", 2);

            setFileType(imageInfo[0]);
            setImage(Base64.getDecoder().decode(imageInfo[1]));
        }
    }
}
