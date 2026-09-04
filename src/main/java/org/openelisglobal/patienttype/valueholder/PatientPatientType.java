package org.openelisglobal.patienttype.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Setter
@Getter
@Entity
@DynamicUpdate
@Table(name = "PATIENT_PATIENT_TYPE")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class PatientPatientType extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "patient_patient_type_seq_gen")
    @GenericGenerator(name = "patient_patient_type_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "patient_patient_type_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "PATIENT_ID", precision = 10, scale = 0)
    private String patientId;

    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "PATIENT_TYPE_ID", precision = 10, scale = 0, nullable = false)
    private String patientTypeId;

}
