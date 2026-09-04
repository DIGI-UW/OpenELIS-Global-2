package org.openelisglobal.patient.valueholder;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.form.SamplePatientEntryForm.SamplePatientEntryBatch;

@Setter
@Getter
@Entity
@DynamicUpdate
@Table(name = "PATIENT_CONTACT")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class PatientContact extends BaseObject<String> {

    private static final long serialVersionUID = 7772701778973733042L;

    @Id
    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class })
    @GeneratedValue(generator = "patient_contact_seq_gen")
    @GenericGenerator(name = "patient_contact_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "patient_contact_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SamplePatientEntryForm.SamplePatientEntry.class,
            SamplePatientEntryBatch.class })
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "patient_id")
    private String patientId;

    @Valid
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "person_id")
    private Person person;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

}
