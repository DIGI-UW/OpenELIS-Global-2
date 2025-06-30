package org.openelisglobal.audittrail.action.workers;

import java.util.List;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.bean.SampleOrderItem;

public interface AuditTrailViewWorker {

    public List<AuditTrailItem> getAuditTrail() throws IllegalStateException;

    public void setAccessionNumber(String accessionNumber);

    public SampleOrderItem getSampleOrderSnapshot();

    public PatientManagementInfo getPatientSnapshot();
}
