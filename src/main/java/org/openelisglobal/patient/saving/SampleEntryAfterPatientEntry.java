package org.openelisglobal.patient.saving;

import static org.openelisglobal.common.services.StatusService.RecordStatus.NotRegistered;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.services.StatusService.RecordStatus;
import org.openelisglobal.patient.saving.form.IAccessionerForm;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
public class SampleEntryAfterPatientEntry extends SampleEntry implements ISampleEntryAfterPatientEntry {

    public SampleEntryAfterPatientEntry(IAccessionerForm form, String sysUserId, HttpServletRequest request) {
        this();
        super.setFieldsFromForm(form);
        super.setSysUserId(sysUserId);
        super.setRequest(request);

        newPatientStatus = null; // leave it be
        newSampleStatus = RecordStatus.InitialRegistration;
    }

    public SampleEntryAfterPatientEntry() {
        super();
        newPatientStatus = null; // leave it be
        newSampleStatus = RecordStatus.InitialRegistration;
    }

    @Override
    public boolean canAccession() {
        return (NotRegistered == statusSet.getSampleRecordStatus());
    }

    /**
     * Find existing sampleHuman, so we can update it with our new patient when we
     * fill in all IDs when we persist.
     *
     * @see org.openelisglobal.patient.saving.PatientEntry#populateSampleHuman()
     */
    @Override
    protected void populateSampleHuman() {
        sampleHuman = new SampleHuman();
        sampleHuman.setSampleId(statusSet.getSampleId());
        sampleHumanService.getDataBySample(sampleHuman);
    }
}
