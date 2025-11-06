package org.openelisglobal.patient.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.person.valueholder.Person;

public interface PatientService extends BaseObjectService<Patient, String> {

    void getData(Patient patient);

    Patient getData(String patientId);

    Patient getPatientByNationalId(String subjectNumber);

    List<Patient> getPatientsByNationalId(String nationalId);

    Patient getPatientByPerson(Person person);

    List<Patient> getPageOfPatients(int startingRecNo);

    Patient getPatientByExternalId(String externalId);

    List<Patient> getAllPatients();

    boolean externalIDExists(String patientExternalID);

    Patient readPatient(String idString);

    List<String> getPatientIdentityBySampleStatusIdAndProject(List<Integer> inclusiveStatusIdList, String study);

    void persistPatientData(PatientManagementInfo patientInfo, Patient patient, String sysUserId);

    String getGUID(Patient patient);

    String getNationalId(Patient patient);

    String getSTNumber(Patient patient);

    String getSubjectNumber(Patient patient);

    String getFirstName(Patient patient);

    String getLastName(Patient patient);

    String getLastFirstName(Patient patient);

    String getGender(Patient patient);

    String getLocalizedGender(Patient patient);

    Map<String, String> getAddressComponents(Patient patient);

    String getEnteredDOB(Patient patient);

    Timestamp getDOB(Patient patient);

    String getPhone(Patient patient);

    Person getPerson(Patient patient);

    String getPatientId(Patient patient);

    String getBirthdayForDisplay(Patient patient);

    List<PatientIdentity> getIdentityList(Patient patient);

    String getExternalId(Patient patient);

    String getAKA(Patient patient);

    String getMother(Patient patient);

    String getInsurance(Patient patient);

    String getOccupation(Patient patient);

    String getOrgSite(Patient patient);

    String getMothersInitial(Patient patient);

    String getEducation(Patient patient);

    String getMaritalStatus(Patient patient);

    String getHealthDistrict(Patient patient);

    String getHealthRegion(Patient patient);

    String getObNumber(Patient patient);

    String getPCNumber(Patient patient);

    Patient getPatientForGuid(String patientGuid);

    String getNationality(Patient patient);

    String getOtherNationality(Patient patient);

    Patient getPatientBySubjectNumber(String subjectNumber);

    void insertNewPatientAddressInfo(String partId, String value, String type, Patient patient, String sysUserId);

    List<Patient> getAllMissingFhirUuid();

    Patient getByExternalId(String idPart);
}
