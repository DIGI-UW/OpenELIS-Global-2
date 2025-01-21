package org.openelisglobal.address;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPK;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;

public class PersonAdressServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    PersonAddressService pAddressService;

    @Autowired
    AddressPartService partService;

    @Autowired
    PersonService personService;

    @Before
    public void init() {
        pAddressService.deleteAll(pAddressService.getAll());
        partService.deleteAll(partService.getAll());
        personService.deleteAll(personService.getAll());
    }

    @After
    @Before
    public void tearDown() {
        pAddressService.deleteAll(pAddressService.getAll());
        partService.deleteAll(partService.getAll());
        personService.deleteAll(personService.getAll());
    }

    @Test
    public void createPersonAddress_shouldCreatePersonAddress() throws Exception {

        Person person = new Person();
        String personId = personService.insert(person);

        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        String partId = partService.insert(part);

        PersonAddress personAddress = new PersonAddress();
        personAddress.setAddressPartId(partId);
        personAddress.setPersonId(personId);
        personAddress.setType("B");
        personAddress.setValue("123");

        Assert.assertEquals(0, pAddressService.getAll().size());

        pAddressService.save(personAddress);
        Assert.assertEquals(1, pAddressService.getAll().size());
        Assert.assertEquals("123", personAddress.getValue());
        Assert.assertEquals("B", personAddress.getType());
    }

    @Test
    public void updatePersonAddress_shouldUpdatePersonAdress() throws Exception {

        Person person = new Person();
        String personId = personService.insert(person);

        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        String partId = partService.insert(part);

        PersonAddress personAddress = new PersonAddress();
        personAddress.setAddressPartId(partId);
        personAddress.setPersonId(personId);
        personAddress.setType("B");
        personAddress.setValue("123");

        Assert.assertEquals(0, pAddressService.getAll().size());

        AddressPK savedPAID = pAddressService.insert(personAddress);
        PersonAddress address = pAddressService.get(savedPAID);
        address.setValue("124");
        pAddressService.save(address);

        Assert.assertEquals("124", address.getValue());
        Assert.assertEquals("B", address.getType());
    }

    @Test
    public void deletePersonAddress_shouldDeletePersonAddress() throws Exception {

        Person person = new Person();
        String personId = personService.insert(person);

        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        String partId = partService.insert(part);

        PersonAddress personAddress = new PersonAddress();
        personAddress.setAddressPartId(partId);
        personAddress.setPersonId(personId);
        personAddress.setType("B");
        personAddress.setValue("123");

        PersonAddress pAddress = pAddressService.save(personAddress);
        pAddressService.delete(pAddress);

        Assert.assertEquals(0, pAddressService.getAll().size());
    }

    @Test
    public void insert_shouldInsertPersonAdress() throws Exception {

        Person person = new Person();
        String personId = personService.insert(person);

        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        String partId = partService.insert(part);

        PersonAddress personAddress = new PersonAddress();
        personAddress.setAddressPartId(partId);
        personAddress.setPersonId(personId);
        personAddress.setType("B");
        personAddress.setValue("123");

        Assert.assertEquals(0, pAddressService.getAll().size());

        AddressPK savedPAID = pAddressService.insert(personAddress);
        PersonAddress address = pAddressService.get(savedPAID);

        Assert.assertEquals("123", address.getValue());
        Assert.assertEquals("B", address.getType());
    }

    @Test
    public void getAddressPartsByPersonId_shouldAddressPartsByPersonId() throws Exception {

        Person person = new Person();
        String personId = personService.insert(person);

        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        String partId = partService.insert(part);

        PersonAddress personAddress = new PersonAddress();
        personAddress.setAddressPartId(partId);
        personAddress.setPersonId(personId);
        personAddress.setType("B");
        personAddress.setValue("123");

        Assert.assertEquals(0, pAddressService.getAll().size());

        pAddressService.insert(personAddress);

        Assert.assertEquals(1, pAddressService.getAddressPartsByPersonId(personId).size());
    }

    @Test
    public void getByPersonIdAndPartId_shouldReturnPersonAdressByPersonIdAndPartId() throws Exception {

        Person person = new Person();
        String personId = personService.insert(person);

        AddressPart part = new AddressPart();
        part.setPartName("PartName");
        part.setDisplayOrder("022");

        String partId = partService.insert(part);

        PersonAddress personAddress = new PersonAddress();
        personAddress.setAddressPartId(partId);
        personAddress.setPersonId(personId);
        personAddress.setType("B");
        personAddress.setValue("123");

        Assert.assertEquals(0, pAddressService.getAll().size());

        pAddressService.insert(personAddress);
        PersonAddress address = pAddressService.getByPersonIdAndPartId(personId, partId);

        Assert.assertEquals("123", address.getValue());
        Assert.assertEquals("B", address.getType());
    }
}
