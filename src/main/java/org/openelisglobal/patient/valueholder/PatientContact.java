package org.openelisglobal.patient.valueholder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.form.SamplePatientEntryForm.SamplePatientEntryBatch;

public class PatientContact extends BaseObject<String> {

    private static final long serialVersionUID = 7772701778973733042L;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class })
    private String id;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class })
    private String patientId;

    @Valid
    private Person person;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}
