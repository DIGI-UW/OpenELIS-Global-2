package org.openelisglobal.patient.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PatientService extends BaseObjectService<Patient, String> {

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    void getData(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    Patient getData(String patientId);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    Patient getPatientByNationalId(String subjectNumber);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    List<Patient> getPatientsByNationalId(String nationalId);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    Patient getPatientByPerson(Person person);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    List<Patient> getPageOfPatients(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    Patient getPatientByExternalId(String externalId);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    List<Patient> getAllPatients();

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    boolean externalIDExists(String patientExternalID);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    Patient readPatient(String idString);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    List<String> getPatientIdentityBySampleStatusIdAndProject(List<Integer> inclusiveStatusIdList, String study);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_CREATE')")
    void persistPatientData(PatientManagementInfo patientInfo, Patient patient, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getGUID(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getNationalId(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getSTNumber(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getSubjectNumber(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getFirstName(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getLastName(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getLastFirstName(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getGender(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getLocalizedGender(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    Map<String, String> getAddressComponents(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getEnteredDOB(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    Timestamp getDOB(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getPhone(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    Person getPerson(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getPatientId(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getBirthdayForDisplay(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    List<PatientIdentity> getIdentityList(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getExternalId(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getAKA(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getMother(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getInsurance(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getOccupation(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getOrgSite(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getMothersInitial(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getEducation(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getMaritalStatus(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getHealthDistrict(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getHealthRegion(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getObNumber(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getPCNumber(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    Patient getPatientForGuid(String patientGuid);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getNationality(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    String getOtherNationality(Patient patient);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    Patient getPatientBySubjectNumber(String subjectNumber);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_EDIT')")
    void insertNewPatientAddressInfo(String partId, String value, String type, Patient patient, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    List<Patient> getAllMissingFhirUuid();

    @PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
    Patient getByExternalId(String idPart);
}
