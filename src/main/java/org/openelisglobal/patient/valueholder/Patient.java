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
package org.openelisglobal.patient.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.person.valueholder.Person;

@Indexed
@Setter
@Getter
@Entity
@DynamicUpdate
@Table(name = "PATIENT")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class Patient extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GenericField
    @GeneratedValue(generator = "patient_seq_gen")
    @GenericGenerator(name = "patient_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "patient_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;

    @Column(name = "RACE", length = 5)
    private String race;

    @GenericField
    @Column(name = "GENDER", length = 1)
    private String gender;

    @Column(name = "BIRTH_DATE", length = 7)
    private Timestamp birthDate;

    @GenericField
    @Column(name = "entered_birth_date", length = 10)
    private String birthDateForDisplay;

    @Column(name = "EPI_FIRST_NAME", length = 25)
    private String epiFirstName;

    @Column(name = "EPI_MIDDLE_NAME", length = 25)
    private String epiMiddleName;

    @Column(name = "EPI_LAST_NAME", length = 240)
    private String epiLastName;

    @Column(name = "BIRTH_TIME", length = 7)
    private Date birthTime;

    @Transient
    private String birthTimeForDisplay;

    @Column(name = "DEATH_DATE", length = 7)
    private Date deathDate;

    @Transient
    private String deathDateForDisplay;

    @KeywordField(normalizer = "lowercase")
    @Column(name = "NATIONAL_ID")
    private String nationalId;

    @Column(name = "ETHNICITY", length = 1)
    private String ethnicity;

    @Column(name = "SCHOOL_ATTEND", length = 240)
    private String schoolAttend;

    @Column(name = "MEDICARE_ID", length = 240)
    private String medicareId;

    @Column(name = "MEDICAID_ID", length = 240)
    private String medicaidId;

    @Column(name = "BIRTH_PLACE")
    private String birthPlace;

    @IndexedEmbedded(structure = ObjectStructure.NESTED)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PERSON_ID", nullable = false)
    private Person person;

    @KeywordField(normalizer = "lowercase")
    @Column(name = "EXTERNAL_ID")
    private String externalId;

    @Column(name = "upid_code")
    private String upidCode;

    @Transient
    private String selectedPersonId;

    @Column(name = "CHART_NUMBER", length = 20)
    private String chartNumber;

    @Column(name = "fhir_uuid", length = 20)
    private UUID fhirUuid;

    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "merged_into_patient_id", precision = 10, scale = 0)
    private String mergedIntoPatientId;

    @Column(name = "is_merged", nullable = false)
    private Boolean isMerged = false;

    @Column(name = "merge_date", length = 7)
    private Timestamp mergeDate;

    public Patient() {
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

    public void setBirthDate(Timestamp birthDate) {
        this.birthDate = birthDate;
        birthDateForDisplay = DateUtil.convertTimestampToStringDate(birthDate);
    }

    public void setBirthTime(Date birthTime) {
        this.birthTime = birthTime;
        birthTimeForDisplay = DateUtil.convertSqlDateToStringDate(birthTime);
    }

    public void setDeathDate(Date deathDate) {
        this.deathDate = deathDate;
        deathDateForDisplay = DateUtil.convertSqlDateToStringDate(deathDate);
    }

    protected Person getPersonHolder() {
        return this.person;
    }

    protected void setPersonHolder(Person person) {
        this.person = person;
    }

    public void setBirthDateForDisplay(String birthDateForDisplay) {
        this.birthDateForDisplay = birthDateForDisplay;

        if (birthDateForDisplay != null) {
            birthDate = DateUtil.convertAmbiguousStringDateToTimestamp(birthDateForDisplay);
        }
    }

    public void setBirthTimeForDisplay(String birthTimeForDisplay) {
        this.birthTimeForDisplay = birthTimeForDisplay;
        this.birthTime = DateUtil.convertStringDateToSqlDate(birthTimeForDisplay);
    }

    public void setDeathDateForDisplay(String deathDateForDisplay) {
        this.deathDateForDisplay = deathDateForDisplay;
        this.deathDate = DateUtil.convertStringDateToSqlDate(deathDateForDisplay);
    }

    public String getFhirUuidAsString() {
        return fhirUuid == null ? "" : fhirUuid.toString();
    }
}
