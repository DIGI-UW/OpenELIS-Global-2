package org.openelisglobal.sample.event;

import java.time.OffsetDateTime;
import lombok.Getter;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.springframework.context.ApplicationEvent;

@Getter
public class SamplePatientUpdateDataCreatedEvent extends ApplicationEvent {
    private final SamplePatientUpdateData updateData;
    private final PatientManagementInfo patientInfo;
    private final SamplePatientEntryForm form;
    private final OffsetDateTime createdAt;

    public SamplePatientUpdateDataCreatedEvent(Object source, SamplePatientUpdateData updateData,
            PatientManagementInfo patientInfo, SamplePatientEntryForm form) {
        super(source);
        this.updateData = updateData;
        this.patientInfo = patientInfo;
        this.form = form;
        this.createdAt = OffsetDateTime.now();
    }
}
