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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.dataexchange.order.action;

import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notifications.dao.NotificationDAO;
import org.openelisglobal.notifications.entity.Notification;
import org.openelisglobal.patient.service.PatientContactService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.service.PatientIdentityTypeService;
import org.openelisglobal.patientidentitytype.valueholder.PatientIdentityType;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Scope("prototype")
public class DBOrderPersister implements IOrderPersister {

    private String SERVICE_USER_ID;
    private String IDENTITY_GUID_ID;
    private String IDENTITY_STNUMBER_ID;
    private String IDENTITY_OBNUMBER_ID;
    private String IDENTITY_PCNUMBER_ID;
    private String IDENTITY_SUBJECTNUMBER_ID;

    @Autowired
    private ElectronicOrderService eOrderService;
    @Autowired
    private PatientIdentityTypeService identityTypeService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private PatientIdentityService identityService;
    @Autowired
    private PersonService personService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private PatientContactService patientContactService;
    @Autowired
    private PersonAddressService personAddressService;
    @Autowired
    private AddressPartService addressPartService;
    @Autowired
    private NotificationDAO notificationDAO;
    @Autowired
    private UserRoleService userRoleService;

    private Patient patient;

    private String ADDRESS_PART_VILLAGE_ID;
    private String ADDRESS_PART_COMMUNE_ID;
    private String ADDRESS_PART_DEPT_ID;

    @PostConstruct
    public void initializeGlobalVariables() {
        SystemUser serviceUser = systemUserService.getDataForLoginUser("serviceUser");
        SERVICE_USER_ID = serviceUser == null ? null : serviceUser.getId();

        IDENTITY_GUID_ID = getIdentityType(identityTypeService, "GUID");
        IDENTITY_STNUMBER_ID = getIdentityType(identityTypeService, "ST");
        IDENTITY_OBNUMBER_ID = getIdentityType(identityTypeService, "OB_NUMBER");
        IDENTITY_PCNUMBER_ID = getIdentityType(identityTypeService, "PC_NUMBER");
        IDENTITY_SUBJECTNUMBER_ID = getIdentityType(identityTypeService, "SUBJECT");
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

    private String getIdentityType(PatientIdentityTypeService identityTypeService, String name) {
        PatientIdentityType type = identityTypeService.getNamedIdentityType(name);
        return type == null ? null : type.getId();
    }

    private void persist(MessagePatient orderPatient) {
        patient = patientService.getPatientForGuid(orderPatient.getGuid());
        patient = patient == null ? patientService.getPatientByExternalId(orderPatient.getExternalId()) : patient;
        if (patient == null) {
            createNewPatient(orderPatient);
        } else {
            updatePatient(orderPatient, patient);
        }
    }

    private void persistContact(MessagePatient orderPatient, Patient patient) {
        PatientContact contact = new PatientContact();
        Person contactPerson = new Person();
        contactPerson.setFirstName(orderPatient.getContactFirstName());
        contactPerson.setLastName(orderPatient.getContactLastName());
        contactPerson.setEmail(orderPatient.getContactEmail());
        contactPerson.setPrimaryPhone(orderPatient.getContactPhone());

        contact.setPatientId(patient.getId());
        contact.setSysUserId(SERVICE_USER_ID);
        contactPerson.setSysUserId(SERVICE_USER_ID);

        contactPerson.setId(personService.insert(contactPerson));
        contact.setPerson(contactPerson);
        patientContactService.insert(contact);
    }

    private void createNewPatient(MessagePatient orderPatient) {
        Person person = new Person();
        person.setFirstName(orderPatient.getFirstName());
        person.setLastName(orderPatient.getLastName());
        person.setStreetAddress(orderPatient.getAddressStreet());
        person.setCity(orderPatient.getAddressVillage());
        person.setState(orderPatient.getAddressDepartment());
        person.setCountry(orderPatient.getAddressCountry());
        person.setEmail(orderPatient.getEmail());
        person.setPrimaryPhone(orderPatient.getMobilePhone());
        if (GenericValidator.isBlankOrNull(person.getPrimaryPhone())) {
            person.setPrimaryPhone(orderPatient.getWorkPhone());
        }
        person.setSysUserId(SERVICE_USER_ID);

        patient = new Patient();
        patient.setBirthDateForDisplay(orderPatient.getDisplayDOB());
        patient.setGender(orderPatient.getGender());
        patient.setNationalId(orderPatient.getNationalId());
        patient.setPerson(person);
        patient.setSysUserId(SERVICE_USER_ID);
        patient.setExternalId(orderPatient.getExternalId());
        if (!GenericValidator.isBlankOrNull(orderPatient.getFhirUuid())) {
            patient.setFhirUuid(UUID.fromString(orderPatient.getFhirUuid()));
        }

        if (GenericValidator.isBlankOrNull(orderPatient.getGuid())) {
            orderPatient.setGuid(java.util.UUID.randomUUID().toString());
        }

        List<PatientIdentity> identities = new ArrayList<>();
        if (!GenericValidator.isBlankOrNull(orderPatient.getGuid())) {
            addIdentityIfAppropriate(IDENTITY_GUID_ID, orderPatient.getGuid(), identities);
        } else {
            addIdentityIfAppropriate(IDENTITY_GUID_ID, orderPatient.getExternalId(), identities);
        }

        addIdentityIfAppropriate(IDENTITY_STNUMBER_ID, orderPatient.getStNumber(), identities);
        addIdentityIfAppropriate(IDENTITY_OBNUMBER_ID, orderPatient.getObNumber(), identities);
        addIdentityIfAppropriate(IDENTITY_PCNUMBER_ID, orderPatient.getPcNumber(), identities);
        addIdentityIfAppropriate(IDENTITY_SUBJECTNUMBER_ID, orderPatient.getSubjectNumber(), identities);

        personService.insert(person);
        patientService.insert(patient);

        for (PatientIdentity identity : identities) {
            identity.setPatientId(patient.getId());
            identityService.insert(identity);
        }

        persistContact(orderPatient, patient);
        insertPatientAddress(orderPatient, patient);
    }

    private void insertPatientAddress(MessagePatient orderPatient, Patient patient) {
        insertNewPatientInfo(ADDRESS_PART_COMMUNE_ID, orderPatient.getAddressCommune(), "T",
                patient.getPerson().getId());
    }

    private void addIdentityIfAppropriate(String typeId, String value, List<PatientIdentity> identities) {
        if (typeId != null && value != null) {
            PatientIdentity identity = new PatientIdentity();
            identity.setIdentityData(value);
            identity.setIdentityTypeId(typeId);
            identity.setSysUserId(SERVICE_USER_ID);
            identities.add(identity);
        }
    }

    private void updatePatient(MessagePatient orderPatient, Patient patient) {
        Person person = patientService.getPerson(patient);

        updatePersonIfNeeded(orderPatient, patient, person);
        updatePatientIfNeeded(orderPatient, patient);

        List<PatientIdentity> identityList = patientService.getIdentityList(patient);
        updateIdentityIfNeeded(IDENTITY_OBNUMBER_ID, orderPatient.getObNumber(), patient.getId(), identityList,
                identityService);
        updateIdentityIfNeeded(IDENTITY_STNUMBER_ID, orderPatient.getStNumber(), patient.getId(), identityList,
                identityService);
        updateIdentityIfNeeded(IDENTITY_PCNUMBER_ID, orderPatient.getPcNumber(), patient.getId(), identityList,
                identityService);
        updateIdentityIfNeeded(IDENTITY_GUID_ID, orderPatient.getGuid(), patient.getId(), identityList,
                identityService);
        updateIdentityIfNeeded(IDENTITY_SUBJECTNUMBER_ID, orderPatient.getSubjectNumber(), patient.getId(),
                identityList, identityService);

        updateAddressPartsIfNeeded(orderPatient, person.getId());
    }

    private void updateAddressPartsIfNeeded(MessagePatient orderPatient, String personId) {
        List<PersonAddress> personAddressList = personAddressService.getAddressPartsByPersonId(personId);
        for (PersonAddress address : personAddressList) {
            if (address.getAddressPartId().equals(ADDRESS_PART_COMMUNE_ID)) {
                address.setValue(orderPatient.getAddressCommune());
                address.setSysUserId(SERVICE_USER_ID);
                personAddressService.update(address);
            }
        }
    }

    private void updateIdentityIfNeeded(String identityTypeId, String newIdentityValue, String patientId,
            List<PatientIdentity> identityList, PatientIdentityService identityService) {

        if (!GenericValidator.isBlankOrNull(newIdentityValue)) {
            boolean assigned = false;
            for (PatientIdentity identity : identityList) {
                if (identity.getIdentityTypeId().equals(identityTypeId)) {
                    if (!newIdentityValue.equals(identity.getIdentityData())) {
                        identity.setIdentityData(newIdentityValue);
                        identity.setSysUserId(SERVICE_USER_ID);
                        identityService.update(identity);
                    }
                    assigned = true;
                    break;
                }
            }

            if (!assigned) {
                PatientIdentity identity = new PatientIdentity();
                identity.setIdentityTypeId(identityTypeId);
                identity.setIdentityData(newIdentityValue);
                identity.setPatientId(patientId);
                identity.setSysUserId(SERVICE_USER_ID);
                identityService.insert(identity);
            }
        }
    }

    private void updatePatientIfNeeded(MessagePatient orderPatient, Patient patient) {
        boolean updatePatient = false;

        if (needsUpdating(orderPatient.getDisplayDOB(), patientService.getBirthdayForDisplay(patient))) {
            patient.setBirthDateForDisplay(orderPatient.getDisplayDOB());
            updatePatient = true;
        }

        if (needsUpdating(orderPatient.getGender(), patientService.getGender(patient))) {
            patient.setGender(orderPatient.getGender());
            updatePatient = true;
        }

        if (needsUpdating(orderPatient.getNationalId(), patientService.getNationalId(patient))) {
            patient.setNationalId(orderPatient.getNationalId());
            updatePatient = true;
        }

        if (updatePatient) {
            patient.setSysUserId(SERVICE_USER_ID);
            patientService.update(patient);
        }
    }

    private void updatePersonIfNeeded(MessagePatient orderPatient, Patient patient, Person person) {
        boolean updatePerson = false;

        if (needsUpdating(orderPatient.getFirstName(), patientService.getFirstName(patient))) {
            person.setFirstName(orderPatient.getFirstName());
            updatePerson = true;
        }

        if (needsUpdating(orderPatient.getLastName(), patientService.getLastName(patient))) {
            person.setLastName(orderPatient.getLastName());
            updatePerson = true;
        }
        if (needsUpdating(orderPatient.getAddressStreet(), patientService.getPerson(patient).getStreetAddress())) {
            person.setStreetAddress(orderPatient.getAddressStreet());
            updatePerson = true;
        }
        if (needsUpdating(orderPatient.getAddressVillage(), patientService.getPerson(patient).getCity())) {
            person.setCity(orderPatient.getAddressVillage());
            updatePerson = true;
        }
        if (needsUpdating(orderPatient.getAddressDepartment(), patientService.getPerson(patient).getState())) {
            person.setState(orderPatient.getAddressDepartment());
            updatePerson = true;
        }
        if (needsUpdating(orderPatient.getAddressCountry(), patientService.getPerson(patient).getCountry())) {
            person.setCountry(orderPatient.getAddressCountry());
            updatePerson = true;
        }
        if (needsUpdating(orderPatient.getEmail(), patientService.getPerson(patient).getEmail())) {
            person.setEmail(orderPatient.getEmail());
            updatePerson = true;
        }
        if (needsUpdating(orderPatient.getWorkPhone(), patientService.getPerson(patient).getPrimaryPhone())) {
            person.setPrimaryPhone(orderPatient.getWorkPhone());
            updatePerson = true;
        }
        if (needsUpdating(orderPatient.getMobilePhone(), patientService.getPerson(patient).getPrimaryPhone())) {
            person.setPrimaryPhone(orderPatient.getMobilePhone());
            updatePerson = true;
        }

        if (updatePerson) {
            person.setSysUserId(SERVICE_USER_ID);
            personService.update(person);
        }
    }

    private boolean needsUpdating(String orderPatientValue, String currentPatientValue) {
        return !GenericValidator.isBlankOrNull(orderPatientValue)
                && StringUtil.compareWithNulls(currentPatientValue, orderPatientValue) != 0;
    }

    private void insertNewPatientInfo(String partId, String value, String type, String personId) {
        PersonAddress address;
        address = new PersonAddress();
        address.setPersonId(personId);
        address.setAddressPartId(partId);
        address.setType(type);
        address.setValue(value);
        address.setSysUserId(SERVICE_USER_ID);
        personAddressService.insert(address);
    }

    @Override
    @Transactional
    public void persist(MessagePatient orderPatient, ElectronicOrder eOrder) {
        try {
            persist(orderPatient);
            eOrder.setPatient(patient);
            eOrderService.insert(eOrder);
            if (eOrder.getPriority().equals(OrderPriority.STAT)) {
                String message = MessageUtil.getMessage("notification.eorder.stat", eOrder.getExternalId());
                List<String> systemUserIds = userRoleService.getUserIdsForRole(Constants.ROLE_RECEPTION);
                for (String userId : systemUserIds) {
                    try {
                        Notification notification = new Notification();
                        notification.setMessage(message);
                        notification.setUser(systemUserService.getUserById(userId));
                        notification.setCreatedDate(OffsetDateTime.now());
                        notification.setReadAt(null);
                        notificationDAO.save(notification);

                    } catch (Exception e) {
                    }
                }
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw e;
        }
    }

    @Override
    public String getServiceUserId() {
        return SERVICE_USER_ID;
    }

    @Override
    public void cancelOrder(String referringOrderNumber) {
        if (!GenericValidator.isBlankOrNull(referringOrderNumber)) {
            List<ElectronicOrder> eOrders = eOrderService.getElectronicOrdersByExternalId(referringOrderNumber);

            if (eOrders != null && !eOrders.isEmpty()) {
                ElectronicOrder eOrder = eOrders.get(eOrders.size() - 1);
                eOrder.setStatusId(
                        SpringContext.getBean(IStatusService.class).getStatusID(ExternalOrderStatus.Cancelled));
                eOrder.setSysUserId(SERVICE_USER_ID);
                try {
                    eOrderService.update(eOrder);
                } catch (RuntimeException e) {
                    LogEvent.logError(e);
                }
            }
        }
    }
}
