/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.person.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;

@Setter
@Getter
@Entity
@DynamicUpdate
@Table(name = "PERSON")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class Person extends BaseObject<String> {

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    @Id
    @GeneratedValue(generator = "person_seq_gen")
    @GenericGenerator(name = "person_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "person_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @ValidName(nameType = NameType.LAST_NAME)
    @KeywordField(normalizer = "lowercase")
    @Column(name = "LAST_NAME")
    private String lastName;

    @ValidName(nameType = NameType.FIRST_NAME)
    @KeywordField(normalizer = "lowercase")
    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "MIDDLE_NAME")
    private String middleName;

    @Column(name = "MULTIPLE_UNIT")
    private String multipleUnit;

    @Column(name = "STREET_ADDRESS")
    private String streetAddress;

    @Column(name = "CITY")
    private String city;

    @Column(name = "STATE")
    private String state;

    @Column(name = "ZIP_CODE")
    private String zipCode;

    @Column(name = "COUNTRY")
    private String country;

    @Pattern(regexp = ValidationHelper.PHONE_REGEX)
    @Column(name = "WORK_PHONE")
    private String workPhone;

    @Pattern(regexp = ValidationHelper.PHONE_REGEX)
    @Column(name = "HOME_PHONE")
    private String homePhone;

    @Pattern(regexp = ValidationHelper.PHONE_REGEX)
    @Column(name = "CELL_PHONE")
    private String cellPhone;

    @Pattern(regexp = ValidationHelper.PHONE_REGEX)
    @Column(name = "primary_phone")
    private String primaryPhone;

    @Column(name = "FAX")
    private String fax;

    @Email
    @Column(name = "EMAIL")
    private String email;

    @Column(name = "department")
    private String department;

    // OGC-650 (LO-01-01): patient registration GPS coordinates. Toggle-gated by
    // the PATIENT_GPS_CAPTURE_ENABLED config property — only rendered when on.
    @Column(name = "gps_latitude")
    private java.math.BigDecimal gpsLatitude;

    @Column(name = "gps_longitude")
    private java.math.BigDecimal gpsLongitude;

    @JsonIgnore
    @AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "person")))
    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY)
    private Set<Patient> patients = new HashSet<>(0);

    public Person() {
        super();
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public Set getPatients() {
        return this.patients;
    }

    public void setPatients(Set patients) {
        this.patients = patients;
    }

    public void addPatient(Patient patient) {
        patients.add(patient);
        patient.setPerson(this);
    }
}
