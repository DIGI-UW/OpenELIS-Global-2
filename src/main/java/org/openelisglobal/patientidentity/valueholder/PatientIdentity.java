package org.openelisglobal.patientidentity.valueholder;

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
@Table(name = "PATIENT_IDENTITY")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class PatientIdentity extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "patient_identity_seq_gen")
    @GenericGenerator(name = "patient_identity_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "patient_identity_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "IDENTITY_TYPE_ID", precision = 10, scale = 0)
    private String identityTypeId;

    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "PATIENT_ID", precision = 10, scale = 0, nullable = false)
    private String patientId;

    @Column(name = "IDENTITY_DATA")
    private String identityData;

}
