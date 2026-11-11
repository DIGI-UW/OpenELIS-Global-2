package org.openelisglobal.externalconnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.externalconnections.service.ExternalConnectionContactService;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionContact;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;

public class ExternalConnectionContactServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ExternalConnectionService externalConnectionService;

    @Autowired
    private ExternalConnectionContactService contactService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/externalconnection-contact.xml");
    }

    @Test
    public void getAll_shouldReturnAllExternalConnectionsContacts() {
        List<ExternalConnectionContact> externalConnectionContacts = contactService.getAll();
        assertTrue(externalConnectionContacts.size() > 0);

        assertEquals("John", externalConnectionContacts.get(0).getPerson().getFirstName());
        assertEquals("James", externalConnectionContacts.get(1).getPerson().getFirstName());
    }

    @Test
    public void insert_shouldInsertNewConnectionContact() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "person" });

        Person pat = new Person();
        pat.setFirstName("John");
        pat.setLastName("Moe");

        ExternalConnection con = externalConnectionService.get(1);

        ExternalConnectionContact contact = new ExternalConnectionContact();
        contact.setExternalConnection(con);
        contact.setPerson(pat);

        int insertedContactId = contactService.insert(contact);
        ExternalConnectionContact savedContact = contactService.get(insertedContactId);

        assertEquals("John", savedContact.getPerson().getFirstName());
        assertEquals("Moe", savedContact.getPerson().getLastName());
    }

    @Test
    public void save_shouldSaveNewExternalContact() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "person" });

        Person pat = new Person();
        pat.setFirstName("John");
        pat.setLastName("Moe");

        ExternalConnection con = externalConnectionService.get(1);

        ExternalConnectionContact contact = new ExternalConnectionContact();
        contact.setExternalConnection(con);
        contact.setPerson(pat);

        ExternalConnectionContact saveContact = contactService.save(contact);
        ExternalConnectionContact savedContact = contactService.get(saveContact.getId());

        assertEquals("John", savedContact.getPerson().getFirstName());
        assertEquals("Moe", savedContact.getPerson().getLastName());
    }

    @Test
    public void update_shouldUpdateExternalConnectionContact() throws Exception {
        ExternalConnectionContact contact = contactService.get(1);
        contact.getPerson().setFirstName("Jesca");
        contact.getPerson().setLastName("Winnie");
        ExternalConnectionContact saveContact = contactService.update(contact);
        ExternalConnectionContact savedContact = contactService.get(saveContact.getId());

        assertEquals("Jesca", savedContact.getPerson().getFirstName());
        assertEquals("Winnie", savedContact.getPerson().getLastName());
    }

    @Test
    public void saveAll_shouldSaveAllExternalConnnectionContacts() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "person" });

        Person pat = new Person();
        pat.setFirstName("Jesca");
        pat.setLastName("May");

        ExternalConnection con = externalConnectionService.get(1);

        ExternalConnectionContact contact1 = new ExternalConnectionContact();
        contact1.setExternalConnection(con);
        contact1.setPerson(pat);

        ExternalConnectionContact contact2 = new ExternalConnectionContact();
        contact2.setExternalConnection(con);
        contact2.setPerson(pat);

        List<ExternalConnectionContact> contacts = new ArrayList<>();
        contacts.add(contact1);
        contacts.add(contact2);

        contactService.saveAll(contacts);

        List<ExternalConnectionContact> saved = contactService.getAll();
        assertTrue(saved.size() > 0);
        assertEquals("Jesca", saved.get(saved.size() - 1).getPerson().getFirstName());
        assertEquals("May", saved.get(saved.size() - 1).getPerson().getLastName());
    }

    @Test
    public void insertAll_shouldInsertAllExternalConnectionContacts() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "person" });

        Person pat = new Person();
        pat.setFirstName("Jesca");
        pat.setLastName("May");

        ExternalConnection con = externalConnectionService.get(1);

        ExternalConnectionContact contact1 = new ExternalConnectionContact();
        contact1.setExternalConnection(con);
        contact1.setPerson(pat);

        ExternalConnectionContact contact2 = new ExternalConnectionContact();
        contact2.setExternalConnection(con);
        contact2.setPerson(pat);

        List<ExternalConnectionContact> contacts = new ArrayList<>();
        contacts.add(contact1);
        contacts.add(contact2);

        contactService.insertAll(contacts);

        List<ExternalConnectionContact> saved = contactService.getAll();
        assertTrue(saved.size() > 0);
        assertEquals("Jesca", saved.get(saved.size() - 1).getPerson().getFirstName());
        assertEquals("May", saved.get(saved.size() - 1).getPerson().getLastName());
    }

    @Test
    public void updateAll_shouldUpdateAllExternalConnectionsContacts() {
        ExternalConnectionContact contact = contactService.get(1);
        contact.getPerson().setFirstName("Jesca");
        contact.getPerson().setLastName("Winnie");

        ExternalConnectionContact contact2 = contactService.get(3);
        contact2.getPerson().setFirstName("Jordan");
        contact2.getPerson().setLastName("Mugume");

        List<ExternalConnectionContact> contacts = new ArrayList<>();
        contacts.add(contact);
        contacts.add(contact2);

        contactService.updateAll(contacts);

        ExternalConnectionContact contact3 = contactService.get(1);
        ExternalConnectionContact contact4 = contactService.get(3);

        assertEquals("Jesca", contact3.getPerson().getFirstName());
        assertEquals("Winnie", contact3.getPerson().getLastName());

        assertEquals("Jordan", contact4.getPerson().getFirstName());
        assertEquals("Mugume", contact4.getPerson().getLastName());
    }

    @Test
    public void getNext_shouldReturnNextExternalConnectionContact() throws Exception {
        ExternalConnectionContact contact = contactService.get(1);
        ExternalConnectionContact nextContact = contactService.getNext(String.valueOf(contact.getId()));

        assertEquals("3", String.valueOf(nextContact.getId()));
    }

    @Test
    public void getPrevious_shouldReturnPreviousExternalConnectionContact() {
        ExternalConnectionContact contact = contactService.get(3);
        ExternalConnectionContact previousContact = contactService.getPrevious(String.valueOf(contact.getId()));

        assertEquals("1", String.valueOf(previousContact.getId()));
    }

    @Test
    public void getCount_shouldReturnTotolExternalConnectionCotacts() {
        int count = contactService.getCount();
        List<ExternalConnectionContact> contacts = contactService.getAll();

        assertEquals(count, contacts.size());
    }

    @Test
    public void get_shouldReturnExternalConnectionContactGivenId() {
        ExternalConnectionContact contact = contactService.get(1);

        assertEquals("John", contact.getPerson().getFirstName());
    }

    @Test
    public void getPage_shouldReturnPageExternalConnectionContacts() {
        List<ExternalConnectionContact> contacts = contactService.getPage(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));

        assertTrue(contacts.size() <= expectedPages);
        assertTrue(contacts.stream().anyMatch(c -> "John".equals(c.getPerson().getFirstName())));
    }

    @Test
    public void getMatchingOrderedPageGivenMapAndList_shouldReturnMatchingOrderedList() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("person.lastName", "Doe");

        List<String> orderProperties = new ArrayList<>();
        orderProperties.add("person.lastName");
        orderProperties.add("person.firstName");

        List<ExternalConnectionContact> results = contactService.getMatchingOrderedPage(filters, orderProperties, false,
                1);

        assertTrue(results.size() > 0);
        assertEquals("Doe", results.get(0).getPerson().getLastName());
        assertEquals("John", results.get(0).getPerson().getFirstName());
    }

    @Test
    public void getOrderedPage_shouldReturnOrderedPageOfExternalConnectionContacts() {
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add("id");

        List<ExternalConnectionContact> results = contactService.getOrderedPage(orderProperties, false, 1);

        assertTrue(results.size() > 0);
        assertEquals("John", results.get(0).getPerson().getFirstName());
        assertEquals("James", results.get(1).getPerson().getFirstName());
    }

    @Test
    public void getOrderedPage_shouldReturnOrderedPageOfExternalConnectionContactsByPersonLastName() {
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add("person.lastName");
        orderProperties.add("person.firstName");

        List<ExternalConnectionContact> results = contactService.getOrderedPage(orderProperties, false, 1);

        assertTrue(results.size() > 0);
        assertTrue(results.get(0).getPerson().getLastName() != null);
    }

    @Test
    public void getMatching_shouldReturnMatchingExternalConnectionContactsGivenMap() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("id", 1);

        List<ExternalConnectionContact> results = contactService.getAllMatching(filters);

        assertTrue(results.size() > 0);
        assertEquals("John", results.get(0).getPerson().getFirstName());
    }

    @Test
    public void getMatching_shouldReturnMatchingExternalConnectionContactsGivenNestedMap() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("person.lastName", "Doe"); // change if dataset uses another last name

        List<ExternalConnectionContact> results = contactService.getAllMatching(filters);

        assertTrue(results.size() > 0);
        assertEquals("Doe", results.get(0).getPerson().getLastName());
        assertEquals("John", results.get(0).getPerson().getFirstName());
    }

    @Test
    public void getAllMatching_shouldReturnAllMatchingExternalConnectionContactsGivenList() {
        List<ExternalConnectionContact> results = contactService.getAllMatching("id", "1");

        assertTrue(results.size() > 0);
        assertEquals("John", results.get(0).getPerson().getFirstName());
    }

    @Test
    public void getAllMatching_shouldReturnAllMatchingExternalConnectionContactsGivenNestedProperty() {
        List<ExternalConnectionContact> results = contactService.getAllMatching("person.lastName", "Doe");

        assertTrue(results.size() > 0);
        assertEquals("Doe", results.get(0).getPerson().getLastName());
        assertEquals("John", results.get(0).getPerson().getFirstName());
    }

    @Test
    public void getAllOrdered_shouldReturnAllExternalConnectionContactsOrderedByGivenList() {
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add("id");

        List<ExternalConnectionContact> results = contactService.getAllOrdered(orderProperties, false);

        assertTrue(results.size() > 0);
        assertEquals("John", results.get(0).getPerson().getFirstName());
        assertEquals("James", results.get(1).getPerson().getFirstName());
    }

    @Test
    public void getAllOrdered_shouldReturnAllExternalConnectionContactsOrderedByNestedPropertyList() {
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add("person.lastName");
        orderProperties.add("person.firstName");

        List<ExternalConnectionContact> results = contactService.getAllOrdered(orderProperties, false);

        assertTrue(results.size() > 0);
        assertTrue(results.get(0).getPerson().getLastName() != null);
    }

    @Test
    public void getAllOrdered_shouldReturnAllExternalConnectionContactsOrderedByGivenProperty() {
        List<ExternalConnectionContact> results = contactService.getAllOrdered("id", false);

        assertTrue(results.size() > 0);
        assertEquals("John", results.get(0).getPerson().getFirstName());
        assertEquals("James", results.get(1).getPerson().getFirstName());
    }

    @Test
    public void getAllOrdered_shouldReturnAllExternalConnectionContactsOrderedByNestedProperty() {
        List<ExternalConnectionContact> results = contactService.getAllOrdered("person.lastName", false);

        assertTrue(results.size() > 0);
        assertTrue(results.get(0).getPerson().getLastName() != null);
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnMatchingOrderedExternalConnectionContactsGivenMapAndList() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("person.lastName", "Doe");

        List<String> orderProperties = new ArrayList<>();
        orderProperties.add("person.lastName");
        orderProperties.add("person.firstName");

        List<ExternalConnectionContact> results = contactService.getAllMatchingOrdered(filters, orderProperties, false);

        assertTrue(results.size() > 0);
        assertEquals("Doe", results.get(0).getPerson().getLastName());
        assertEquals("John", results.get(0).getPerson().getFirstName());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnMatchingOrderedExternalConnectionContactsGivenMapAndProperty() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("person.lastName", "Doe");

        List<ExternalConnectionContact> results = contactService.getAllMatchingOrdered(filters, "person.lastName",
                false);

        assertTrue(results.size() > 0);
        assertEquals("Doe", results.get(0).getPerson().getLastName());
        assertEquals("John", results.get(0).getPerson().getFirstName());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnMatchingOrderedExternalConnectionContactsGivenPropertyAndList() {
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add("person.lastName");
        orderProperties.add("person.firstName");

        List<ExternalConnectionContact> results = contactService.getAllMatchingOrdered("person.lastName", "Doe",
                orderProperties, false);

        assertTrue(results.size() > 0);
        assertEquals("Doe", results.get(0).getPerson().getLastName());
        assertEquals("John", results.get(0).getPerson().getFirstName());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnMatchingOrderedExternalConnectionContactsGivenProperty() {
        List<ExternalConnectionContact> results = contactService.getAllMatchingOrdered("person.lastName", "Doe",
                "person.lastName", false);

        assertTrue(results.size() > 0);
        assertEquals("Doe", results.get(0).getPerson().getLastName());
        assertEquals("John", results.get(0).getPerson().getFirstName());
    }

    @Test
    public void getMatchingOrderedPage_shouldReturnMatchingOrderedPageOfExternalConnectionContactsGivenListAndPropertyValue() {
        List<String> orderProperties = new ArrayList<>();
        orderProperties.add("person.lastName");
        orderProperties.add("person.firstName");

        List<ExternalConnectionContact> results = contactService.getMatchingOrderedPage("person.lastName", "Doe",
                orderProperties, false, 1);

        assertTrue(results.size() > 0);
        assertEquals("Doe", results.get(0).getPerson().getLastName());
        assertEquals("John", results.get(0).getPerson().getFirstName());
    }

    @Test
    public void getMatchingOrderedPage_shouldReturnMatchingOrderedPageGivenMapandorderProperties() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("person.lastName", "Doe");

        List<ExternalConnectionContact> results = contactService.getMatchingOrderedPage(filters, "person.lastName",
                false, 1);

        assertTrue(results.size() > 0);
        assertEquals("Doe", results.get(0).getPerson().getLastName());
        assertEquals("John", results.get(0).getPerson().getFirstName());
    }

    @Test
    public void delete_shouldDeleteExternalConnectionContact() throws Exception {
        ExternalConnectionContact contact = contactService.get(1);
        contactService.delete(contact);

        List<ExternalConnectionContact> contacts = contactService.getAll();
        assertTrue(contacts.size() > 0);
        assertTrue(contacts.stream().noneMatch(c -> c.getId() == 1));
    }

    @Test
    public void delete_shouldDeleteGivenId() throws Exception {
        contactService.delete(1, "");

        List<ExternalConnectionContact> contacts = contactService.getAll();
        assertTrue(contacts.size() > 0);
        assertTrue(contacts.stream().noneMatch(c -> c.getId() == 1));
    }

    @Test
    public void deleteAll_shouldDeleteAllExternalConnectionContacts() throws Exception {
        List<ExternalConnectionContact> contacts = contactService.getAll();
        contactService.deleteAll(contacts);

        List<ExternalConnectionContact> deletedContacts = contactService.getAll();
        assertTrue(deletedContacts.isEmpty());
    }

    @Test
    public void deleteAll_shouldDeleteAllGivenIds() {
        List<Integer> ids = new ArrayList<>();
        ids.add(1);
        ids.add(3);

        contactService.deleteAll(ids, "");

        List<ExternalConnectionContact> contacts = contactService.getAll();
        assertTrue(contacts.size() > 0);
        assertTrue(contacts.stream().noneMatch(c -> c.getId() == 1));
        assertTrue(contacts.stream().noneMatch(c -> c.getId() == 3));
    }

    @Test
    public void getCountLike_shouldReturnCountOfExternalConnectionContactsMatchingGivenPropertyAndValue() {
        int count = contactService.getCountLike("person.firstName", "John");

        assertTrue(count > 0);
    }

    @Test
    public void getCountLike_shouldReturnCountOfExternalConnectionContactsMatchingGivenNestedLastName() {
        int count = contactService.getCountLike("person.lastName", "Doe");

        assertTrue(count > 0);
    }

    @Test
    public void getCountLike_shouldReturnCountOfExternalConnectionContactsMatchingGivenMap() {
        Map<String, String> filters = new HashMap<>();
        filters.put("person.firstName", "John");

        int count = contactService.getCountLike(filters);

        assertTrue(count > 0);
    }

    @Test
    public void getCountLike_shouldReturnCountOfExternalConnectionContactsMatchingGivenNestedMap() {
        Map<String, String> filters = new HashMap<>();
        filters.put("person.lastName", "Doe");

        int count = contactService.getCountLike(filters);

        assertTrue(count > 0);
    }

    @Test
    public void getCountMatching_shouldReturnCountOfExternalConnectionContactsMatchingGivenPropertyAndValue() {
        int count = contactService.getCountMatching("person.firstName", "John");

        assertEquals(1, count);
    }

    @Test
    public void getCountMatching_shouldReturnCountOfExternalConnectionContactsMatchingGivenNestedLastName() {
        int count = contactService.getCountMatching("person.lastName", "Doe");

        assertTrue(count > 0);
    }

    @Test
    public void getCountMatching_shouldReturnCountOfExternalConnectionContactsMatchingGivenMap() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("person.firstName", "John");

        int count = contactService.getCountMatching(filters);

        assertEquals(1, count);
    }

    @Test
    public void getCountMatching_shouldReturnCountOfExternalConnectionContactsMatchingGivenNestedMap() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("person.lastName", "Doe");

        int count = contactService.getCountMatching(filters);

        assertTrue(count > 0);
    }
}