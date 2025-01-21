package org.openelisglobal.sample.service;

import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.patient.action.IPatientUpdate;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.validator.ValidatePatientInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.patienttype.service.PatientPatientTypeService;
import org.openelisglobal.patienttype.util.PatientTypeMap;
import org.openelisglobal.patienttype.valueholder.PatientPatientType;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

@Service
@Scope("prototype")
public class PatientManagementUpdate extends ControllerUtills implements IPatientUpdate {

    private String currentUserId;
    protected Patient patient;
    protected Person person;
    private List<PatientIdentity> patientIdentities;
    private String patientID = "";
    @Autowired
    private PatientIdentityService identityService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private PersonAddressService personAddressService;
    @Autowired
    private PersonService personService;
    @Autowired
    private AddressPartService addressPartService;
    @Autowired
    private PatientPatientTypeService patientPatientTypeService;
    protected PatientUpdateStatus patientUpdateStatus = PatientUpdateStatus.NO_ACTION;

    private String ADDRESS_PART_VILLAGE_ID;
    private String ADDRESS_PART_COMMUNE_ID;
    private String ADDRESS_PART_DEPT_ID;

    @PostConstruct
    public void initializeGlobalVariables() {
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
    }

    public void setSysUserIdFromRequest(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA);
        currentUserId = String.valueOf(usd.getSystemUserId());
    }

    private void initMembers() {
        patient = new Patient();
        person = new Person();
        patientIdentities = new ArrayList<>();
    }

    private void loadForUpdate(PatientManagementInfo patientInfo) {

        patientID = patientInfo.getPatientPK();
        patient = patientService.readPatient(patientID);
        person = patient.getPerson();

        patientIdentities = identityService.getPatientIdentitiesForPatient(patient.getId());
    }

    private void copyFormBeanToValueHolders(PatientManagementInfo patientInfo)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        PropertyUtils.copyProperties(patient, patientInfo);
        PropertyUtils.copyProperties(person, patientInfo);
    }

    private void setSystemUserID(String currentUserId) {
        patient.setSysUserId(currentUserId);
        person.setSysUserId(currentUserId);

        for (PatientIdentity identity : patientIdentities) {
            identity.setSysUserId(currentUserId);
        }
    }

    private void setLastUpdatedTimeStamps(PatientManagementInfo patientInfo) {
        String patientUpdate = patientInfo.getPatientLastUpdated();
        if (!GenericValidator.isBlankOrNull(patientUpdate)) {
            Timestamp timeStamp = Timestamp.valueOf(patientUpdate);
            patient.setLastupdated(timeStamp);
        }

        String personUpdate = patientInfo.getPersonLastUpdated();
        if (!GenericValidator.isBlankOrNull(personUpdate)) {
            Timestamp timeStamp = Timestamp.valueOf(personUpdate);
            person.setLastupdated(timeStamp);
        }
    }

    protected void persistPatientRelatedInformation(PatientManagementInfo patientInfo) {
        persistIdentityTypes(patientInfo);
        persistExtraPatientAddressInfo(patientInfo);
        persistPatientType(patientInfo);
    }

    protected void persistIdentityTypes(PatientManagementInfo patientInfo) {

        persistIdentityType(patientInfo.getSTnumber(), "ST");
        persistIdentityType(patientInfo.getMothersName(), "MOTHER");
        persistIdentityType(patientInfo.getAka(), "AKA");
        persistIdentityType(patientInfo.getInsuranceNumber(), "INSURANCE");
        persistIdentityType(patientInfo.getOccupation(), "OCCUPATION");
        persistIdentityType(patientInfo.getSubjectNumber(), "SUBJECT");
        persistIdentityType(patientInfo.getMothersInitial(), "MOTHERS_INITIAL");
        persistIdentityType(patientInfo.getEducation(), "EDUCATION");
        persistIdentityType(patientInfo.getMaritialStatus(), "MARITIAL");
        persistIdentityType(patientInfo.getNationality(), "NATIONALITY");
        persistIdentityType(patientInfo.getHealthDistrict(), "HEALTH DISTRICT");
        persistIdentityType(patientInfo.getHealthRegion(), "HEALTH REGION");
        persistIdentityType(patientInfo.getOtherNationality(), "OTHER NATIONALITY");
        persistIdentityType(patientInfo.getGuid(), "GUID");
    }

    private void persistExtraPatientAddressInfo(PatientManagementInfo patientInfo) {
        PersonAddress village = null;
        PersonAddress commune = null;
        PersonAddress dept = null;
        List<PersonAddress> personAddressList = personAddressService.getAddressPartsByPersonId(person.getId());

        for (PersonAddress address : personAddressList) {
            if (address.getAddressPartId().equals(ADDRESS_PART_COMMUNE_ID)) {
                commune = address;
                commune.setValue(patientInfo.getCommune());
                commune.setSysUserId(currentUserId);
                personAddressService.update(commune);
            } else if (address.getAddressPartId().equals(ADDRESS_PART_VILLAGE_ID)) {
                village = address;
                village.setValue(patientInfo.getCity());
                village.setSysUserId(currentUserId);
                personAddressService.update(village);
            } else if (address.getAddressPartId().equals(ADDRESS_PART_DEPT_ID)) {
                dept = address;
                if (!GenericValidator.isBlankOrNull(patientInfo.getAddressDepartment())
                        && !patientInfo.getAddressDepartment().equals("0")) {
                    dept.setValue(patientInfo.getAddressDepartment());
                    dept.setType("D");
                    dept.setSysUserId(currentUserId);
                    personAddressService.update(dept);
                }
            }
        }

        if (commune == null) {
            insertNewPatientInfo(ADDRESS_PART_COMMUNE_ID, patientInfo.getCommune(), "T");
        }

        if (village == null) {
            insertNewPatientInfo(ADDRESS_PART_VILLAGE_ID, patientInfo.getCity(), "T");
        }

        if (dept == null && patientInfo.getAddressDepartment() != null
                && !patientInfo.getAddressDepartment().equals("0")) {
            insertNewPatientInfo(ADDRESS_PART_DEPT_ID, patientInfo.getAddressDepartment(), "D");
        }
    }

    private void insertNewPatientInfo(String partId, String value, String type) {
        PersonAddress address;
        address = new PersonAddress();
        address.setPersonId(person.getId());
        address.setAddressPartId(partId);
        address.setType(type);
        address.setValue(value);
        address.setSysUserId(currentUserId);
        personAddressService.insert(address);
    }

    public void persistIdentityType(String paramValue, String type) throws LIMSRuntimeException {

        Boolean newIdentityNeeded = true;
        String typeID = PatientIdentityTypeMap.getInstance().getIDForType(type);

        if (patientUpdateStatus == PatientUpdateStatus.UPDATE) {

            for (PatientIdentity listIdentity : patientIdentities) {
                if (listIdentity.getIdentityTypeId().equals(typeID)) {

                    newIdentityNeeded = false;

                    if ((listIdentity.getIdentityData() == null && !GenericValidator.isBlankOrNull(paramValue))
                            || (listIdentity.getIdentityData() != null
                                    && !listIdentity.getIdentityData().equals(paramValue))) {
                        listIdentity.setIdentityData(paramValue);
                        identityService.update(listIdentity);
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
            identity.setSysUserId(currentUserId);
            identity.setIdentityData(paramValue);
            identity.setLastupdatedFields();
            identityService.insert(identity);
        }
    }

    protected void persistPatientType(PatientManagementInfo patientInfo) {

        String typeName = null;

        try {
            typeName = patientInfo.getPatientType();
        } catch (RuntimeException e) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "persistPatientType", "typeName ignored");
        }

        if (!GenericValidator.isBlankOrNull(typeName) && !"0".equals(typeName)) {
            String typeID = PatientTypeMap.getInstance().getIDForType(typeName);

            PatientPatientType patientPatientType = patientPatientTypeService
                    .getPatientPatientTypeForPatient(patient.getId());

            if (patientPatientType == null) {
                patientPatientType = new PatientPatientType();
                patientPatientType.setSysUserId(currentUserId);
                patientPatientType.setPatientId(patient.getId());
                patientPatientType.setPatientTypeId(typeID);
                patientPatientTypeService.insert(patientPatientType);
            } else {
                patientPatientType.setSysUserId(currentUserId);
                patientPatientType.setPatientTypeId(typeID);
                patientPatientTypeService.update(patientPatientType);
            }
        }
    }

    @Override
    public Errors preparePatientData(HttpServletRequest request, PatientManagementInfo patientInfo)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Errors errors = new BaseErrors();
        ValidatePatientInfo.validatePatientInfo(errors, patientInfo);
        if (errors.hasErrors()) {
            return errors;
        }

        initMembers();

        if (patientUpdateStatus == PatientUpdateStatus.UPDATE) {
            loadForUpdate(patientInfo);
        }

        copyFormBeanToValueHolders(patientInfo);

        setSystemUserID(getSysUserId(request));

        setLastUpdatedTimeStamps(patientInfo);

        return errors;
    }

    @Override
    public void setPatientUpdateStatus(PatientManagementInfo patientInfo) {
        patientUpdateStatus = patientInfo.getPatientUpdateStatus();
        /*
         * String status = patientInfo.getPatientProcessingStatus();
         *
         * if ("noAction".equals(status)) { patientUpdateStatus =
         * PatientUpdateStatus.NO_ACTION; } else if ("update".equals(status)) {
         * patientUpdateStatus = PatientUpdateStatus.UPDATE; } else {
         * patientUpdateStatus = PatientUpdateStatus.ADD; }
         */
    }

    @Override
    public PatientUpdateStatus getPatientUpdateStatus() {
        return patientUpdateStatus;
    }

    @Override
    public void persistPatientData(PatientManagementInfo patientInfo) throws LIMSRuntimeException {

        if (patientUpdateStatus == PatientUpdateStatus.ADD) {
            personService.insert(person);
        } else if (patientUpdateStatus == PatientUpdateStatus.UPDATE) {
            personService.update(person);
        }
        patient.setPerson(person);

        if (patientUpdateStatus == PatientUpdateStatus.ADD) {
            UUID uuid = UUID.randomUUID();
            // patientInfo.setFhirUuid(uuid);
            patientInfo.setGuid(uuid.toString());
            patient.setFhirUuid(uuid);
            patientService.insert(patient);
        } else if (patientUpdateStatus == PatientUpdateStatus.UPDATE) {
            patientService.update(patient);
        }

        persistPatientRelatedInformation(patientInfo);
        patientID = patient.getId();
        patientInfo.setPatientPK(patientID);
    }

    @Override
    public String getPatientId(SamplePatientEntryForm form) {
        return GenericValidator.isBlankOrNull(patientID) ? form.getPatientProperties().getPatientPK() : patientID;
    }
}
