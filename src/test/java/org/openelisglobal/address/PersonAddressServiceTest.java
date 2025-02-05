package org.openelisglobal.address;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPK;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;

public class PersonAddressServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    PersonAddressService pAddressService;

    @Autowired
    PersonService personService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/personaddress.xml");
    }

    @Test
    public void createPersonAddress_shouldCreatePersonAddress() throws Exception {

        Person person = new Person();
        person.setFirstName("john");
        person.setLastName("Doe");
        String personId = personService.insert(person);

        PersonAddress personAddress = new PersonAddress();
        personAddress.setAddressPartId("3");
        personAddress.setPersonId(personId);
        personAddress.setType("D");
        personAddress.setValue("123");

        Assert.assertEquals(3, pAddressService.getAll().size());

        pAddressService.save(personAddress);
        Assert.assertEquals(4, pAddressService.getAll().size());
        Assert.assertEquals("123", personAddress.getValue());
        Assert.assertEquals("D", personAddress.getType());
    }

    @Test
    public void updatePersonAddress_shouldUpdatePersonAdress() throws Exception {
        PersonAddress address = pAddressService.getByPersonIdAndPartId("1", "5");
        address.setValue("124");
        pAddressService.save(address);

        Assert.assertEquals("124", address.getValue());
        Assert.assertEquals("P", address.getType());
    }

    @Test
    public void deletePersonAddress_shouldDeletePersonAddress() throws Exception {
        PersonAddress address = pAddressService.getByPersonIdAndPartId("2", "6");

        Assert.assertEquals(3, pAddressService.getAll().size());

        pAddressService.delete(address);

        Assert.assertEquals(2, pAddressService.getAll().size());
    }

    @Test
    public void insert_shouldInsertPersonAdress() throws Exception {

        Person person = new Person();
        person.setFirstName("john");
        person.setLastName("Doe");
        String personId = personService.insert(person);

        PersonAddress personAddress = new PersonAddress();
        personAddress.setAddressPartId("5");
        personAddress.setPersonId(personId);
        personAddress.setType("F");
        personAddress.setValue("123");

        Assert.assertEquals(3, pAddressService.getAll().size());

        AddressPK savedPAID = pAddressService.insert(personAddress);
        PersonAddress address = pAddressService.get(savedPAID);

        Assert.assertEquals("123", address.getValue());
        Assert.assertEquals("F", address.getType());
        Assert.assertEquals(4, pAddressService.getAll().size());
    }

    @Test
    public void getAddressPartsByPersonId_shouldAddressPartsByPersonId() throws Exception {
        List<PersonAddress> pAddresses = pAddressService.getAddressPartsByPersonId("1");

        Assert.assertEquals(2, pAddresses.size());
        Assert.assertEquals("The first element should be tulla", pAddresses.get(0).getValue(), "Tulla");
        Assert.assertEquals("The first element should be 12345678", pAddresses.get(1).getValue(), "12345678");
    }

    @Test
    public void getByPersonIdAndPartId_shouldReturnPersonAdressByPersonIdAndPartId() throws Exception {
        PersonAddress address = pAddressService.getByPersonIdAndPartId("1", "3");

        Assert.assertEquals("Tulla", address.getValue());
        Assert.assertEquals("V", address.getType());
    }
}
