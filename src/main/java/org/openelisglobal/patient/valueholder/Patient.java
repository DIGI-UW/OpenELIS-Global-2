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

import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.person.valueholder.Person;

@Indexed
public class Patient extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @GenericField
    private String id;

    private String race;

    @GenericField
    private String gender;

    private Timestamp birthDate;

    @GenericField
    private String birthDateForDisplay;

    private String epiFirstName;

    private String epiMiddleName;

    private String epiLastName;

    private Date birthTime;

    private String birthTimeForDisplay;

    private Date deathDate;

    private String deathDateForDisplay;

    @KeywordField(normalizer = "lowercase")
    private String nationalId;

    private String ethnicity;

    private String schoolAttend;

    private String medicareId;

    private String medicaidId;

    private String birthPlace;

    @IndexedEmbedded(structure = ObjectStructure.NESTED)
    private ValueHolderInterface person;

    @KeywordField(normalizer = "lowercase")
    private String externalId;

    private String upidCode;

    private String selectedPersonId;

    private String chartNumber;

    private UUID fhirUuid;

    public String getChartNumber() {
        return chartNumber;
    }

    public void setChartNumber(String chartNumber) {
        this.chartNumber = chartNumber;
    }

    public Patient() {
        super();
        person = new ValueHolder();
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public Timestamp getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Timestamp birthDate) {
        this.birthDate = birthDate;
        birthDateForDisplay = DateUtil.convertTimestampToStringDate(birthDate);
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public Date getBirthTime() {
        return birthTime;
    }

    public void setBirthTime(Date birthTime) {
        this.birthTime = birthTime;
        birthTimeForDisplay = DateUtil.convertSqlDateToStringDate(birthTime);
    }

    public Date getDeathDate() {
        return deathDate;
    }

    public void setDeathDate(Date deathDate) {
        this.deathDate = deathDate;
        deathDateForDisplay = DateUtil.convertSqlDateToStringDate(deathDate);
    }

    public String getEpiFirstName() {
        return epiFirstName;
    }

    public void setEpiFirstName(String epiFirstName) {
        this.epiFirstName = epiFirstName;
    }

    public String getEpiLastName() {
        return epiLastName;
    }

    public void setEpiLastName(String epiLastName) {
        this.epiLastName = epiLastName;
    }

    public String getEpiMiddleName() {
        return epiMiddleName;
    }

    public void setEpiMiddleName(String epiMiddleName) {
        this.epiMiddleName = epiMiddleName;
    }

    public String getEthnicity() {
        return ethnicity;
    }

    public void setEthnicity(String ethnicity) {
        this.ethnicity = ethnicity;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMedicaidId() {
        return medicaidId;
    }

    public void setMedicaidId(String medicaidId) {
        this.medicaidId = medicaidId;
    }

    public String getMedicareId() {
        return medicareId;
    }

    public void setMedicareId(String medicareId) {
        this.medicareId = medicareId;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public Person getPerson() {
        return (Person) this.person.getValue();
    }

    protected ValueHolderInterface getPersonHolder() {
        return this.person;
    }

    public void setPerson(Person person) {
        this.person.setValue(person);
    }

    protected void setPersonHolder(ValueHolderInterface person) {
        this.person = person;
    }

    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public String getSchoolAttend() {
        return schoolAttend;
    }

    public void setSchoolAttend(String schoolAttend) {
        this.schoolAttend = schoolAttend;
    }

    public String getBirthDateForDisplay() {
        return birthDateForDisplay;
    }

    public void setBirthDateForDisplay(String birthDateForDisplay) {
        this.birthDateForDisplay = birthDateForDisplay;

        if (birthDateForDisplay != null) {
            birthDate = DateUtil.convertAmbiguousStringDateToTimestamp(birthDateForDisplay);
        }
    }

    public String getBirthTimeForDisplay() {
        return birthTimeForDisplay;
    }

    public void setBirthTimeForDisplay(String birthTimeForDisplay) {
        this.birthTimeForDisplay = birthTimeForDisplay;
        this.birthTime = DateUtil.convertStringDateToSqlDate(birthTimeForDisplay);
    }

    public void setDeathDateForDisplay(String deathDateForDisplay) {
        this.deathDateForDisplay = deathDateForDisplay;
        this.deathDate = DateUtil.convertStringDateToSqlDate(deathDateForDisplay);
    }

    public String getDeathDateForDisplay() {
        return deathDateForDisplay;
    }

    public void setSelectedPersonId(String selectedPersonId) {
        this.selectedPersonId = selectedPersonId;
    }

    public String getSelectedPersonId() {
        return this.selectedPersonId;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public String getFhirUuidAsString() {
        return fhirUuid == null ? "" : fhirUuid.toString();
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public String getUpidCode() {
        return upidCode;
    }

    public void setUpidCode(String upidCode) {
        this.upidCode = upidCode;
    }
}
