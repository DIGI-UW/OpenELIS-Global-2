package org.openelisglobal.audittrail.form;

import jakarta.validation.Valid;
import java.util.List;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.validation.annotations.SafeHtml;

// used for viewing only, does not need validation
public class AuditTrailViewForm extends BaseForm {

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String accessionNumberSearch = "";

    @Valid
    private List<AuditTrailItem> log;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String accessionNumber = "";

    @Valid
    private SampleOrderItem sampleOrderItems;

    @Valid
    private PatientManagementInfo patientProperties;

    public AuditTrailViewForm() {
        setFormName("AuditTrailViewForm");
    }

    public String getAccessionNumberSearch() {
        return accessionNumberSearch;
    }

    public void setAccessionNumberSearch(String accessionNumberSearch) {
        this.accessionNumberSearch = accessionNumberSearch;
    }

    public List<AuditTrailItem> getLog() {
        return log;
    }

    public void setLog(List<AuditTrailItem> log) {
        this.log = log;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public SampleOrderItem getSampleOrderItems() {
        return sampleOrderItems;
    }

    public void setSampleOrderItems(SampleOrderItem sampleOrderItems) {
        this.sampleOrderItems = sampleOrderItems;
    }

    public PatientManagementInfo getPatientProperties() {
        return patientProperties;
    }

    public void setPatientProperties(PatientManagementInfo patientProperties) {
        this.patientProperties = patientProperties;
    }
}
