package org.openelisglobal.patient.service;

import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.gender.service.GenderService;
import org.openelisglobal.gender.valueholder.Gender;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.dao.PatientDAO;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.service.PatientIdentityTypeService;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;
import org.openelisglobal.patienttype.service.PatientPatientTypeService;
import org.openelisglobal.patienttype.util.PatientTypeMap;
import org.openelisglobal.patienttype.valueholder.PatientPatientType;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientServiceImpl extends AuditableBaseObjectServiceImpl<Patient, String> implements PatientService {

    public static final String ADDRESS_STREET = "Street";
    public static final String ADDRESS_STATE = "State";
    public static final String ADDRESS_VILLAGE = "village";
    public static final String ADDRESS_DEPT = "department";
    public static final String ADDRESS_COMMUNE = "commune";
    public static final String ADDRESS_ZIP = "zip";
    public static final String ADDRESS_COUNTRY = "Country";
    public static final String ADDRESS_CITY = "City";

    // These have getters
    private static String PATIENT_ST_IDENTITY;
    private static String PATIENT_SUBJECT_IDENTITY;
    private static String PATIENT_HEALTH_DISTRICT_IDENTITY;
    private static String PATIENT_HEALTH_REGION_IDENTITY;

    // these are only used in this class
    private static String PATIENT_GUID_IDENTITY;
    private static String PATIENT_NATIONAL_IDENTITY;
    private static String PATIENT_AKA_IDENTITY;
    private static String PATIENT_MOTHER_IDENTITY;
    private static String PATIENT_INSURANCE_IDENTITY;
    private static String PATIENT_OCCUPATION_IDENTITY;
    private static String PATIENT_ORG_SITE_IDENTITY;
    private static String PATIENT_MOTHERS_INITIAL_IDENTITY;
    private static String PATIENT_EDUCATION_IDENTITY;
    private static String PATIENT_MARITAL_IDENTITY;
    private static String PATIENT_OB_NUMBER_IDENTITY;
    private static String PATIENT_PC_NUMBER_IDENTITY;
    private static String PATIENT_NATIONALITY;
    private static String PATIENT_OTHER_NATIONALITY;

    @Autowired
    private PatientDAO baseObjectDAO;

    @Autowired
    private PatientIdentityService patientIdentityService;
    @Autowired
    private GenderService genderService;
    @Autowired
    private PatientIdentityTypeService identityTypeService;
    @Autowired
    private AddressPartService addressPartService;
    @Autowired
    private PersonAddressService personAddressService;
    @Autowired
    private PatientPatientTypeService patientPatientTypeService;
    @Autowired
    private PersonService personService;
    @Autowired
    protected FhirPersistanceService fhirPersistanceService;

    @Autowired
    private PatientContactService patientContactService;

    @PostConstruct
    public void initializeGlobalVariables() {

        PatientIdentityType patientType = identityTypeService.getNamedIdentityType("GUID");
        if (patientType != null) {
            PATIENT_GUID_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("SUBJECT");
        if (patientType != null) {
            PATIENT_SUBJECT_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("NATIONAL");
        if (patientType != null) {
            PATIENT_NATIONAL_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("ST");
        if (patientType != null) {
            PATIENT_ST_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("AKA");
        if (patientType != null) {
            PATIENT_AKA_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("MOTHER");
        if (patientType != null) {
            PATIENT_MOTHER_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("INSURANCE");
        if (patientType != null) {
            PATIENT_INSURANCE_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("OCCUPATION");
        if (patientType != null) {
            PATIENT_OCCUPATION_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("ORG_SITE");
        if (patientType != null) {
            PATIENT_ORG_SITE_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("MOTHERS_INITIAL");
        if (patientType != null) {
            PATIENT_MOTHERS_INITIAL_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("EDUCATION");
        if (patientType != null) {
            PATIENT_EDUCATION_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("MARITIAL");
        if (patientType != null) {
            PATIENT_MARITAL_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("HEALTH DISTRICT");
        if (patientType != null) {
            PATIENT_HEALTH_DISTRICT_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("HEALTH REGION");
        if (patientType != null) {
            PATIENT_HEALTH_REGION_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("OB_NUMBER");
        if (patientType != null) {
            PATIENT_OB_NUMBER_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("PC_NUMBER");
        if (patientType != null) {
            PATIENT_PC_NUMBER_IDENTITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("NATIONALITY");
        if (patientType != null) {
            PATIENT_NATIONALITY = patientType.getId();
        }

        patientType = identityTypeService.getNamedIdentityType("OTHER NATIONALITY");
        if (patientType != null) {
            PATIENT_OTHER_NATIONALITY = patientType.getId();
        }
    }

    PatientServiceImpl() {
        super(Patient.class);
        this.auditTrailLog = true;
    }

    @Override
    protected PatientDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public String insert(Patient patient) {
        if (patient.getFhirUuid() == null) {
            patient.setFhirUuid(UUID.randomUUID());
        }
        return super.insert(patient);
    }

    public static String getPatientSTIdentity() {
        return PATIENT_ST_IDENTITY;
    }

    public static String getPatientSubjectIdentity() {
        return PATIENT_SUBJECT_IDENTITY;
    }

    public static String getPatientHealthDistrictIdentity() {
        return PATIENT_HEALTH_DISTRICT_IDENTITY;
    }

    public static String getPatientHealthRegionIdentity() {
        return PATIENT_HEALTH_REGION_IDENTITY;
    }

    @Override
    public Patient getPatientForGuid(String guid) {
        List<PatientIdentity> identites = patientIdentityService.getPatientIdentitiesByValueAndType(guid,
                PATIENT_GUID_IDENTITY);
        if (identites.isEmpty()) {
            return null;
        }

        return baseObjectDAO.getData(identites.get(0).getPatientId());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getGUID()
     */
    @Override
    @Transactional(readOnly = true)
    public String getGUID(Patient patient) {
        return getIdentityInfo(patient, PATIENT_GUID_IDENTITY);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getNationalId()
     */
    @Override
    @Transactional(readOnly = true)
    public String getNationalId(Patient patient) {
        if (patient == null) {
            return "";
        }

        if (!GenericValidator.isBlankOrNull(patient.getNationalId())) {
            return patient.getNationalId();
        } else {
            return getIdentityInfo(patient, PATIENT_NATIONAL_IDENTITY);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getSTNumber()
     */
    @Override
    @Transactional(readOnly = true)
    public String getSTNumber(Patient patient) {
        return getIdentityInfo(patient, PATIENT_ST_IDENTITY);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getSubjectNumber()
     */
    @Override
    @Transactional(readOnly = true)
    public String getSubjectNumber(Patient patient) {
        return getIdentityInfo(patient, PATIENT_SUBJECT_IDENTITY);
    }

    private String getIdentityInfo(Patient patient, String identityId) {
        if (patient == null || GenericValidator.isBlankOrNull(identityId)) {
            return "";
        }

        PatientIdentity identity = patientIdentityService.getPatitentIdentityForPatientAndType(patient.getId(),
                identityId);

        if (identity != null) {
            return identity.getIdentityData();
        } else {
            return "";
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getFirstName()
     */
    @Override
    @Transactional(readOnly = true)
    public String getFirstName(Patient patient) {
        return personService.getFirstName(patient.getPerson());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getLastName()
     */
    @Override
    @Transactional(readOnly = true)
    public String getLastName(Patient patient) {
        return personService.getLastName(patient.getPerson());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getLastFirstName()
     */
    @Override
    @Transactional(readOnly = true)
    public String getLastFirstName(Patient patient) {
        return personService.getLastFirstName(patient.getPerson());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getGender()
     */
    @Override
    @Transactional(readOnly = true)
    public String getGender(Patient patient) {
        return patient != null ? patient.getGender() : "";
    }

    @Override
    @Transactional(readOnly = true)
    public String getLocalizedGender(Patient patient) {
        String genderType = getGender(patient);

        if (genderType.length() > 0) {
            Gender gender = genderService.getGenderByType(genderType);
            if (gender != null) {
                return gender.getLocalizedName();
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getAddressComponents(
     * )
     */
    @Override
    public Map<String, String> getAddressComponents(Patient patient) {
        return personService.getAddressComponents(patient.getPerson());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getEnteredDOB()
     */
    @Override
    @Transactional(readOnly = true)
    public String getEnteredDOB(Patient patient) {
        return patient != null ? patient.getBirthDateForDisplay() : "";
    }

    @Override
    @Transactional(readOnly = true)
    public Timestamp getDOB(Patient patient) {
        return patient != null ? patient.getBirthDate() : null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getPhone()
     */
    @Override
    @Transactional(readOnly = true)
    public String getPhone(Patient patient) {
        return personService.getPhone(patient.getPerson());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getPerson()
     */
    @Override
    @Transactional(readOnly = true)
    public Person getPerson(Patient patient) {
        return patient.getPerson();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getPatientId()
     */
    @Override
    @Transactional(readOnly = true)
    public String getPatientId(Patient patient) {
        return patient == null ? null : patient.getId();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getBirthdayForDisplay
     * ()
     */
    @Override
    @Transactional(readOnly = true)
    public String getBirthdayForDisplay(Patient patient) {
        return patient != null ? DateUtil.convertTimestampToStringDate(patient.getBirthDate()) : "";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openelisglobal.common.services.IPatientService#getIdentityList()
     */
    @Override
    @Transactional(readOnly = true)
    public List<PatientIdentity> getIdentityList(Patient patient) {
        return patient != null ? PatientUtil.getIdentityListForPatient(patient) : new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public String getExternalId(Patient patient) {
        return patient == null ? "" : patient.getExternalId();
    }

    @Override
    @Transactional(readOnly = true)
    public String getAKA(Patient patient) {
        return getIdentityInfo(patient, PATIENT_AKA_IDENTITY);
    }

    @Override
    @Transactional(readOnly = true)
    public String getMother(Patient patient) {
        return getIdentityInfo(patient, PATIENT_MOTHER_IDENTITY);
    }

    @Override
    @Transactional(readOnly = true)
    public String getInsurance(Patient patient) {
        return getIdentityInfo(patient, PATIENT_INSURANCE_IDENTITY);
    }

    @Override
    @Transactional(readOnly = true)
    public String getOccupation(Patient patient) {
        return getIdentityInfo(patient, PATIENT_OCCUPATION_IDENTITY);
    }

    @Override
    @Transactional(readOnly = true)
    public String getOrgSite(Patient patient) {
        return getIdentityInfo(patient, PATIENT_ORG_SITE_IDENTITY);
    }

    @Override
    @Transactional(readOnly = true)
    public String getMothersInitial(Patient patient) {
        return getIdentityInfo(patient, PATIENT_MOTHERS_INITIAL_IDENTITY);
    }

    @Override
    @Transactional(readOnly = true)
    public String getEducation(Patient patient) {
        return getIdentityInfo(patient, PATIENT_EDUCATION_IDENTITY);
    }

    @Override
    @Transactional(readOnly = true)
    public String getMaritalStatus(Patient patient) {
        return getIdentityInfo(patient, PATIENT_MARITAL_IDENTITY);
    }

    @Override
    @Transactional(readOnly = true)
    public String getHealthDistrict(Patient patient) {
        return getIdentityInfo(patient, PATIENT_HEALTH_DISTRICT_IDENTITY);
    }

    @Override
    @Transactional(readOnly = true)
    public String getHealthRegion(Patient patient) {
        return getIdentityInfo(patient, PATIENT_HEALTH_REGION_IDENTITY);
    }

    @Override
    @Transactional(readOnly = true)
    public String getObNumber(Patient patient) {
        return getIdentityInfo(patient, PATIENT_OB_NUMBER_IDENTITY);
    }

    @Override
    @Transactional(readOnly = true)
    public String getPCNumber(Patient patient) {
        return getIdentityInfo(patient, PATIENT_PC_NUMBER_IDENTITY);
    }

    @Override
    @Transactional(readOnly = true)
    public String getNationality(Patient patient) {
        return getIdentityInfo(patient, PATIENT_NATIONALITY);
    }

    @Override
    @Transactional(readOnly = true)
    public String getOtherNationality(Patient patient) {
        return getIdentityInfo(patient, PATIENT_OTHER_NATIONALITY);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Patient patient) {
        getBaseObjectDAO().getData(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public Patient getData(String patientId) {
        return getBaseObjectDAO().getData(patientId);
    }

    @Override
    @Transactional(readOnly = true)
    public Patient getPatientByNationalId(String nationalId) {
        return getBaseObjectDAO().getPatientByNationalId(nationalId);
    }

    @Override
    @Transactional(readOnly = true)
    public Patient getPatientBySubjectNumber(String subjectNumber) {
        return getBaseObjectDAO().getPatientBySubjectNumber(subjectNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> getPatientsByNationalId(String nationalId) {
        return getBaseObjectDAO().getPatientsByNationalId(nationalId);
    }

    @Override
    @Transactional(readOnly = true)
    public Patient getPatientByPerson(Person person) {
        return getBaseObjectDAO().getPatientByPerson(person);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> getPageOfPatients(int startingRecNo) {
        return getBaseObjectDAO().getPageOfPatients(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public Patient getPatientByExternalId(String externalId) {
        return getBaseObjectDAO().getPatientByExternalId(externalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> getAllPatients() {
        return getBaseObjectDAO().getAllPatients();
    }

    @Override
    public boolean externalIDExists(String patientExternalID) {
        return getBaseObjectDAO().externalIDExists(patientExternalID);
    }

    @Override
    public Patient readPatient(String idString) {
        return getBaseObjectDAO().readPatient(idString);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getPatientIdentityBySampleStatusIdAndProject(List<Integer> inclusiveStatusIdList,
            String study) {
        return getBaseObjectDAO().getPatientIdentityBySampleStatusIdAndProject(inclusiveStatusIdList, study);
    }

    @Override
    @Transactional
    public void persistPatientData(PatientManagementInfo patientInfo, Patient patient, String sysUserId)
            throws LIMSRuntimeException {
        if (patientInfo.getPatientUpdateStatus() == PatientUpdateStatus.ADD) {
            personService.insert(patient.getPerson());
        } else if (patientInfo.getPatientUpdateStatus() == PatientUpdateStatus.UPDATE) {
            personService.update(patient.getPerson());
        }
        patient.setPerson(patient.getPerson());

        if (patientInfo.getPatientUpdateStatus() == PatientUpdateStatus.ADD) {
            UUID uuid = UUID.randomUUID();
            // patientInfo.setFhirUuid(uuid);
            patientInfo.setGuid(uuid.toString());
            patient.setFhirUuid(uuid);
            insert(patient);
        } else if (patientInfo.getPatientUpdateStatus() == PatientUpdateStatus.UPDATE) {
            update(patient);
        }

        persistContact(patientInfo, patient);
        persistPatientRelatedInformation(patientInfo, patient, sysUserId);
        patientInfo.setPatientPK(patient.getId());
    }

    private void persistContact(PatientManagementInfo patientInfo, Patient patient) {
        if (GenericValidator.isBlankOrNull(patientInfo.getPatientContact().getId())) {
            PatientContact contact = patientInfo.getPatientContact();
            Person contactPerson = patientInfo.getPatientContact().getPerson();
            contact.setPatientId(patient.getId());
            contact.setSysUserId(patientInfo.getPatientContact().getSysUserId());
            contactPerson.setSysUserId(contact.getSysUserId());

            personService.insert(contactPerson);
            patientContactService.insert(contact);
        } else {
            Person newContactPerson = patientInfo.getPatientContact().getPerson();
            PatientContact contact = patientContactService.get(patientInfo.getPatientContact().getId());
            Person oldContactPerson = contact.getPerson();
            oldContactPerson.setEmail(newContactPerson.getEmail());
            oldContactPerson.setLastName(newContactPerson.getLastName());
            oldContactPerson.setFirstName(newContactPerson.getFirstName());
            oldContactPerson.setPrimaryPhone(newContactPerson.getPrimaryPhone());
            contact.setSysUserId(patientInfo.getPatientContact().getSysUserId());
            oldContactPerson.setSysUserId(patientInfo.getPatientContact().getSysUserId());
        }
    }

    protected void persistPatientRelatedInformation(PatientManagementInfo patientInfo, Patient patient,
            String sysUserId) {
        persistIdentityTypes(patientInfo, patient, sysUserId);
        persistExtraPatientAddressInfo(patientInfo, patient, sysUserId);
        persistPatientType(patientInfo, patient, sysUserId);
    }

    protected void persistIdentityTypes(PatientManagementInfo patientInfo, Patient patient, String sysUserId) {

        persistIdentityType(patientInfo.getSTnumber(), "ST", patientInfo, patient, sysUserId);
        persistIdentityType(patientInfo.getMothersName(), "MOTHER", patientInfo, patient, sysUserId);
        persistIdentityType(patientInfo.getAka(), "AKA", patientInfo, patient, sysUserId);
        persistIdentityType(patientInfo.getInsuranceNumber(), "INSURANCE", patientInfo, patient, sysUserId);
        persistIdentityType(patientInfo.getOccupation(), "OCCUPATION", patientInfo, patient, sysUserId);
        persistIdentityType(patientInfo.getSubjectNumber(), "SUBJECT", patientInfo, patient, sysUserId);
        persistIdentityType(patientInfo.getMothersInitial(), "MOTHERS_INITIAL", patientInfo, patient, sysUserId);
        persistIdentityType(patientInfo.getEducation(), "EDUCATION", patientInfo, patient, sysUserId);
        persistIdentityType(patientInfo.getMaritialStatus(), "MARITIAL", patientInfo, patient, sysUserId);
        persistIdentityType(patientInfo.getNationality(), "NATIONALITY", patientInfo, patient, sysUserId);
        persistIdentityType(patientInfo.getHealthDistrict(), "HEALTH DISTRICT", patientInfo, patient, sysUserId);
        persistIdentityType(patientInfo.getHealthRegion(), "HEALTH REGION", patientInfo, patient, sysUserId);
        persistIdentityType(patientInfo.getOtherNationality(), "OTHER NATIONALITY", patientInfo, patient, sysUserId);
        persistIdentityType(patientInfo.getGuid(), "GUID", patientInfo, patient, sysUserId);
    }

    private void persistExtraPatientAddressInfo(PatientManagementInfo patientInfo, Patient patient, String sysUserId) {
        String ADDRESS_PART_DEPT_ID = "";
        String ADDRESS_PART_COMMUNE_ID = "";
        String ADDRESS_PART_VILLAGE_ID = "";

        List<AddressPart> partList = addressPartService.getAll();
        for (AddressPart addressPart : partList) {
            if ("department".equals(addressPart.getPartName())) {
                ADDRESS_PART_DEPT_ID = addressPart.getId();
            } else if ("commune".equals(addressPart.getPartName())) {
                ADDRESS_PART_COMMUNE_ID = addressPart.getId();
            } else if ("village".equals(addressPart.getPartName())) {
                ADDRESS_PART_VILLAGE_ID = addressPart.getId();
            }
        }

        PersonAddress village = null;
        PersonAddress commune = null;
        PersonAddress dept = null;
        List<PersonAddress> personAddressList = personAddressService
                .getAddressPartsByPersonId(patient.getPerson().getId());

        for (PersonAddress address : personAddressList) {
            if (address.getAddressPartId().equals(ADDRESS_PART_COMMUNE_ID)) {
                commune = address;
                commune.setValue(patientInfo.getCommune());
                commune.setSysUserId(sysUserId);
                personAddressService.update(commune);
            } else if (address.getAddressPartId().equals(ADDRESS_PART_VILLAGE_ID)) {
                village = address;
                village.setValue(patientInfo.getCity());
                village.setSysUserId(sysUserId);
                personAddressService.update(village);
            } else if (address.getAddressPartId().equals(ADDRESS_PART_DEPT_ID)) {
                dept = address;
                if (!GenericValidator.isBlankOrNull(patientInfo.getAddressDepartment())
                        && !patientInfo.getAddressDepartment().equals("0")) {
                    dept.setValue(patientInfo.getAddressDepartment());
                    dept.setType("D");
                    dept.setSysUserId(sysUserId);
                    personAddressService.update(dept);
                }
            }
        }

        if (commune == null) {
            insertNewPatientAddressInfo(ADDRESS_PART_COMMUNE_ID, patientInfo.getCommune(), "T", patient, sysUserId);
        }

        if (village == null) {
            insertNewPatientAddressInfo(ADDRESS_PART_VILLAGE_ID, patientInfo.getCity(), "T", patient, sysUserId);
        }

        if (dept == null && patientInfo.getAddressDepartment() != null
                && !patientInfo.getAddressDepartment().equals("0")) {
            insertNewPatientAddressInfo(ADDRESS_PART_DEPT_ID, patientInfo.getAddressDepartment(), "D", patient,
                    sysUserId);
        }
    }

    @Override
    public void insertNewPatientAddressInfo(String partId, String value, String type, Patient patient,
            String sysUserId) {
        PersonAddress address;
        address = new PersonAddress();
        address.setPersonId(patient.getPerson().getId());
        address.setAddressPartId(partId);
        address.setType(type);
        address.setValue(value);
        address.setSysUserId(sysUserId);
        personAddressService.insert(address);
    }

    public void persistIdentityType(String paramValue, String type, PatientManagementInfo patientInfo, Patient patient,
            String sysUserId) throws LIMSRuntimeException {

        Boolean newIdentityNeeded = true;
        String typeID = PatientIdentityTypeMap.getInstance().getIDForType(type);

        if (patientInfo.getPatientUpdateStatus() == PatientUpdateStatus.UPDATE) {

            for (PatientIdentity listIdentity : patientInfo.getPatientIdentities()) {
                if (listIdentity.getIdentityTypeId().equals(typeID)) {

                    newIdentityNeeded = false;

                    if ((listIdentity.getIdentityData() == null && !GenericValidator.isBlankOrNull(paramValue))
                            || (listIdentity.getIdentityData() != null
                                    && !listIdentity.getIdentityData().equals(paramValue))) {
                        listIdentity.setIdentityData(paramValue);
                        patientIdentityService.update(listIdentity);
                    }

                    break;
                }
            }
        }

        if (newIdentityNeeded && !GenericValidator.isBlankOrNull(paramValue)) {
            // either a new patient or a new identity item
            PatientIdentity identity = new PatientIdentity();
            identity.setPatientId(patient.getId());
            identity.setIdentityTypeId(typeID);
            identity.setSysUserId(sysUserId);
            identity.setIdentityData(paramValue);
            identity.setLastupdatedFields();
            patientIdentityService.insert(identity);
        }
    }

    protected void persistPatientType(PatientManagementInfo patientInfo, Patient patient, String sysUserId) {
        String typeName = null;

        try {
            typeName = patientInfo.getPatientType();
        } catch (RuntimeException e) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "persistPatientType", "ignoring exception");
        }

        if (!GenericValidator.isBlankOrNull(typeName) && !"0".equals(typeName)) {
            String typeID = PatientTypeMap.getInstance().getIDForType(typeName);

            PatientPatientType patientPatientType = patientPatientTypeService
                    .getPatientPatientTypeForPatient(patient.getId());

            if (patientPatientType == null) {
                patientPatientType = new PatientPatientType();
                patientPatientType.setSysUserId(sysUserId);
                patientPatientType.setPatientId(patient.getId());
                patientPatientType.setPatientTypeId(typeID);
                patientPatientTypeService.insert(patientPatientType);
            } else {
                patientPatientType.setSysUserId(sysUserId);
                patientPatientType.setPatientTypeId(typeID);
                patientPatientTypeService.update(patientPatientType);
            }
        }
    }

    @Override
    public List<Patient> getAllMissingFhirUuid() {
        return baseObjectDAO.getAllMissingFhirUuid();
    }

    @Override
    public Patient getByExternalId(String id) {
        return baseObjectDAO.getByExternalId(id);
    }
}
