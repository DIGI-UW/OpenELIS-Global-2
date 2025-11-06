package org.openelisglobal.patient.form;

public interface PatientInfoForm {

    String getFirstName();

    void setFirstName(String firstName);

    void setLastName(String lastName);

    String getLastName();

    void setDob(String birthDateForDisplay);

    String getDob();

    void setGender(String gender);

    String getGender();

    void setNationalId(String object);

    String getNationalId();

    void setSubjectNumber(String subjectNumber);

    String getSubjectNumber();

    void setSt(String identityValue);

    String getSt();
}
