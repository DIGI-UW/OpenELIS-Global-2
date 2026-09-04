package org.openelisglobal.patientidentitytype.valueholder;

import jakarta.persistence.*;
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
@Table(name = "PATIENT_IDENTITY_TYPE")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class PatientIdentityType extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "patient_identity_type_seq_gen")
    @GenericGenerator(name = "patient_identity_type_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "patient_identity_type_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Column(name = "IDENTITY_TYPE", length = 30)
    private String identityType;

    @Column(name = "DESCRIPTION", length = 400)
    private String description;

}
