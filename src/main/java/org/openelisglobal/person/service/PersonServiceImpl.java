package org.openelisglobal.person.service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.person.dao.PersonDAO;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@DependsOn({ "springContext" })
public class PersonServiceImpl extends AuditableBaseObjectServiceImpl<Person, String> implements PersonService {

    private Map<String, String> addressPartIdToNameMap;

    @Autowired
    protected PersonDAO baseObjectDAO;

    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private AddressPartService addressPartService;
    @Autowired
    private PersonAddressService personAddressService;

    @PostConstruct
    private void initializeGlobalVariables() {
        addressPartIdToNameMap = new HashMap<>();
        List<AddressPart> parts = addressPartService.getAll();

        for (AddressPart part : parts) {
            addressPartIdToNameMap.put(part.getId(), part.getPartName());
        }
    }

    public PersonServiceImpl() {
        super(Person.class);
        this.auditTrailLog = true;
    }

    @Override
    protected PersonDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public String getFirstName(Person person) {
        return person != null ? person.getFirstName() : "";
    }

    @Override
    @Transactional(readOnly = true)
    public String getLastName(Person person) {
        return person != null ? person.getLastName() : "";
    }

    @Override
    @Transactional(readOnly = true)
    public String getLastFirstName(Person person) {
        String lastName = getLastName(person);
        String firstName = getFirstName(person);
        if (!GenericValidator.isBlankOrNull(lastName) && !GenericValidator.isBlankOrNull(firstName)) {
            lastName += ", ";
        }

        lastName += firstName;

        return lastName;
    }

    @Override
    public Map<String, String> getAddressComponents(Person person) {
        String value;
        Map<String, String> addressMap = new HashMap<>();

        if (person == null) {
            return addressMap;
        }

        List<PersonAddress> addressParts = personAddressService.getAddressPartsByPersonId(person.getId());

        for (PersonAddress parts : addressParts) {
            if ("D".equals(parts.getType()) && !GenericValidator.isBlankOrNull(parts.getValue())) {
                addressMap.put(addressPartIdToNameMap.get(parts.getAddressPartId()),
                        dictionaryService.getDataForId(parts.getValue()).getDictEntry());
            } else {
                value = parts.getValue();
                addressMap.put(addressPartIdToNameMap.get(parts.getAddressPartId()), value == null ? "" : value.trim());
            }
        }

        value = person.getCity();
        addressMap.put("City", value == null ? "" : value.trim());
        value = person.getCountry();
        addressMap.put("Country", value == null ? "" : value.trim());
        value = person.getState();
        addressMap.put("State", value == null ? "" : value.trim());
        value = person.getStreetAddress();
        addressMap.put("Street", value == null ? "" : value.trim());
        value = person.getZipCode();
        addressMap.put("Zip", value == null ? "" : value.trim());

        return addressMap;
    }

    @Override
    @Transactional(readOnly = true)
    public String getPhone(Person person) {
        if (person == null) {
            return "";
        }

        String phone = person.getPrimaryPhone();

        if (GenericValidator.isBlankOrNull(phone)) {
            phone = person.getHomePhone();
        }

        if (GenericValidator.isBlankOrNull(phone)) {
            phone = person.getCellPhone();
        }

        if (GenericValidator.isBlankOrNull(phone)) {
            phone = person.getWorkPhone();
        }

        return phone;
    }

    @Override
    @Transactional(readOnly = true)
    public String getWorkPhone(Person person) {
        return person.getWorkPhone();
    }

    @Override
    @Transactional(readOnly = true)
    public String getCellPhone(Person person) {
        return person.getCellPhone();
    }

    @Transactional(readOnly = true)
    public String getHomePhone(Person person) {
        return person.getHomePhone();
    }

    @Override
    @Transactional(readOnly = true)
    public String getFax(Person person) {
        return person.getFax();
    }

    @Override
    @Transactional(readOnly = true)
    public String getEmail(Person person) {
        return person.getEmail();
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Person person) {
        getBaseObjectDAO().getData(person);
    }

    @Override
    @Transactional(readOnly = true)
    public Person getPersonByLastName(String lastName) {
        return getBaseObjectDAO().getPersonByLastName(lastName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Person> getPageOfPersons(int startingRecNo) {
        return getBaseObjectDAO().getPageOfPersons(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Person> getAllPersons() {
        return getBaseObjectDAO().getAllPersons();
    }

    @Override
    @Transactional(readOnly = true)
    public Person getPersonById(String personId) {
        return getBaseObjectDAO().getPersonById(personId);
    }
}
