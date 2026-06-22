package org.openelisglobal.externalconnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.externalconnections.service.ExternalConnectionContactService;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.ProgrammedConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionContact;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;

public class ExternalConnectionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ExternalConnectionService externalConnectionService;
    @Autowired
    private ExternalConnectionContactService contactService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/external-connection.xml");
    }

    @Test
    public void createNewExternalConnection_shouldCreateExternalConnection() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "person", "external_connection_contact",
                "basic_authentication_data", "external_connection" });

        BasicAuthenticationData basicAuthData = new BasicAuthenticationData();
        basicAuthData.setSysUserId("1");
        basicAuthData.setUsername("test-user");
        basicAuthData.setPassword("test-password");

        Map<AuthType, ExternalConnectionAuthenticationData> authDataMap = Map.of(AuthType.BASIC, basicAuthData);

        Person pat = new Person();
        pat.setFirstName("John");
        pat.setLastName("Moe");

        ExternalConnectionContact contact = new ExternalConnectionContact();
        contact.setPerson(pat);
        List<ExternalConnectionContact> contacts = new ArrayList<>();
        contacts.add(contact);

        ExternalConnection newCon = new ExternalConnection();
        newCon.setSysUserId("1");
        newCon.setActive(true);
        newCon.setProgrammedConnection(ProgrammedConnection.BMP_SMS_SERVER);
        newCon.setActiveAuthenticationType(AuthType.BASIC);
        newCon.setUri(new URI("http://example.com"));

        externalConnectionService.createNewExternalConnection(authDataMap, contacts, newCon);

        List<ExternalConnection> connections = externalConnectionService.getAll();

        assertNotNull(connections);
        assertEquals(1, connections.size());
        assertNotNull(connections.get(0).getId());

        List<ExternalConnectionContact> savedContacts = contactService.getAll();
        assertTrue(savedContacts.stream().anyMatch(c -> "John".equals(c.getPerson().getFirstName())));

    }

    @Test
    public void updateExternalConnection_shouldUpdateExternalConnection() throws Exception {

        ExternalConnection existingConnection = externalConnectionService.get(1);
        existingConnection.setProgrammedConnection(ProgrammedConnection.INFO_HIGHWAY);
        BasicAuthenticationData basicAuthData = new BasicAuthenticationData();
        basicAuthData.setSysUserId("1");
        basicAuthData.setUsername("test-user");
        basicAuthData.setPassword("test-password");
        ExternalConnectionContact existingContact = contactService.get(1);
        existingContact.getPerson().setFirstName("Jane");
        Map<AuthType, ExternalConnectionAuthenticationData> authDataMap = Map.of(AuthType.BASIC, basicAuthData);
        List<ExternalConnectionContact> contacts = List.of(existingContact);
        externalConnectionService.updateExternalConnection(authDataMap, contacts, existingConnection);
        assertEquals(ProgrammedConnection.INFO_HIGHWAY, externalConnectionService.get(1).getProgrammedConnection());
        assertEquals("Jane", contactService.get(1).getPerson().getFirstName());
    }

    @Test
    public void updateExternalConnectionFields_shouldUpdateGivenArguments() throws Exception {
        externalConnectionService.updateExternalConnectionFields(1, "1", true, ProgrammedConnection.INFO_HIGHWAY,
                AuthType.BASIC, new URI("http://example.com"), "Test Connection", "Test Description", "test-user",
                "test-password");
        assertEquals(ProgrammedConnection.INFO_HIGHWAY, externalConnectionService.get(1).getProgrammedConnection());

    }

    @Test
    public void getAll_shouldReturnAllExternalConnections() throws Exception {
        List<ExternalConnection> connections = externalConnectionService.getAll();
        assertNotNull(connections);
        assertEquals(ProgrammedConnection.SMTP_SERVER, connections.get(0).getProgrammedConnection());
        assertEquals(ProgrammedConnection.SMPP_SERVER, connections.get(1).getProgrammedConnection());
        assertEquals(ProgrammedConnection.BMP_SMS_SERVER, connections.get(2).getProgrammedConnection());
        assertEquals(ProgrammedConnection.INFO_HIGHWAY, connections.get(3).getProgrammedConnection());
    }

    @Test
    public void save_shouldSaveExternalConnection() throws Exception {
        ExternalConnection newCon = new ExternalConnection();
        newCon.setSysUserId("1");
        newCon.setActive(true);
        newCon.setProgrammedConnection(ProgrammedConnection.BMP_SMS_SERVER);
        newCon.setActiveAuthenticationType(AuthType.BASIC);
        newCon.setUri(new URI("http://example.com"));
        ExternalConnection savedConnnection = externalConnectionService.save(newCon);
        assertEquals(ProgrammedConnection.BMP_SMS_SERVER, savedConnnection.getProgrammedConnection());
    }

    @Test
    public void saveAll_shouldSaveAllExternalConnections() throws Exception {
        ExternalConnection newCon1 = new ExternalConnection();
        newCon1.setSysUserId("1");
        newCon1.setActive(true);
        newCon1.setProgrammedConnection(ProgrammedConnection.BMP_SMS_SERVER);
        newCon1.setActiveAuthenticationType(AuthType.BASIC);
        newCon1.setUri(new URI("http://example.com"));

        ExternalConnection newCon2 = new ExternalConnection();
        newCon2.setSysUserId("1");
        newCon2.setActive(true);
        newCon2.setProgrammedConnection(ProgrammedConnection.INFO_HIGHWAY);
        newCon2.setActiveAuthenticationType(AuthType.BASIC);
        newCon2.setUri(new URI("http://example.com"));

        List<ExternalConnection> savedConnections = externalConnectionService.saveAll(List.of(newCon1, newCon2));
        assertEquals(2, savedConnections.size());
        assertEquals(ProgrammedConnection.BMP_SMS_SERVER, savedConnections.get(0).getProgrammedConnection());
        assertEquals(ProgrammedConnection.INFO_HIGHWAY, savedConnections.get(1).getProgrammedConnection());
    }

    @Test
    public void update_shouldUpdateExternalConnection() throws Exception {
        ExternalConnection existingConnection = externalConnectionService.get(1);
        existingConnection.setProgrammedConnection(ProgrammedConnection.INFO_HIGHWAY);
        ExternalConnection updatedConnection = externalConnectionService.update(existingConnection);
        assertEquals(ProgrammedConnection.INFO_HIGHWAY, updatedConnection.getProgrammedConnection());
    }

    @Test
    public void updateAll_shouldUpdateAllExternalConnections() throws Exception {
        ExternalConnection existingConnection1 = externalConnectionService.get(1);
        existingConnection1.setProgrammedConnection(ProgrammedConnection.INFO_HIGHWAY);

        ExternalConnection existingConnection2 = externalConnectionService.get(2);
        existingConnection2.setProgrammedConnection(ProgrammedConnection.BMP_SMS_SERVER);

        List<ExternalConnection> updatedConnections = externalConnectionService
                .updateAll(List.of(existingConnection1, existingConnection2));
        assertEquals(2, updatedConnections.size());
        assertEquals(ProgrammedConnection.INFO_HIGHWAY, updatedConnections.get(0).getProgrammedConnection());
        assertEquals(ProgrammedConnection.BMP_SMS_SERVER, updatedConnections.get(1).getProgrammedConnection());
    }

    @Test
    public void insert_shouldInsertExternalConnection() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "external_connection" });
        ExternalConnection newCon = new ExternalConnection();
        newCon.setSysUserId("1");
        newCon.setActive(true);
        newCon.setProgrammedConnection(ProgrammedConnection.BMP_SMS_SERVER);
        newCon.setActiveAuthenticationType(AuthType.BASIC);
        newCon.setUri(new URI("http://example.com"));
        int insertedConnection = externalConnectionService.insert(newCon);
        assertNotNull(insertedConnection);
        assertEquals(ProgrammedConnection.BMP_SMS_SERVER,
                externalConnectionService.get(insertedConnection).getProgrammedConnection());
    }

    @Test
    public void insertAll_shouldInsertAllExternalConnections() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "external_connection" });
        ExternalConnection newCon1 = new ExternalConnection();
        newCon1.setSysUserId("1");
        newCon1.setActive(true);
        newCon1.setProgrammedConnection(ProgrammedConnection.BMP_SMS_SERVER);
        newCon1.setActiveAuthenticationType(AuthType.BASIC);
        newCon1.setUri(new URI("http://example.com"));

        ExternalConnection newCon2 = new ExternalConnection();
        newCon2.setSysUserId("1");
        newCon2.setActive(true);
        newCon2.setProgrammedConnection(ProgrammedConnection.INFO_HIGHWAY);
        newCon2.setActiveAuthenticationType(AuthType.BASIC);
        newCon2.setUri(new URI("http://example.com"));

        List<Integer> insertedConnections = externalConnectionService.insertAll(List.of(newCon1, newCon2));
        assertEquals(2, insertedConnections.size());
        assertNotNull(insertedConnections.get(0));
        assertNotNull(insertedConnections.get(1));
        assertEquals(ProgrammedConnection.BMP_SMS_SERVER,
                externalConnectionService.get(insertedConnections.get(0)).getProgrammedConnection());
        assertEquals(ProgrammedConnection.INFO_HIGHWAY,
                externalConnectionService.get(insertedConnections.get(1)).getProgrammedConnection());
    }

    @Test
    public void delete_shouldDeleteExternalConnection() throws Exception {
        cleanRowsInCurrentConnection(
                new String[] { "person", "external_connection_contact", "basic_authentication_data",

                });
        ExternalConnection connection = externalConnectionService.get(1);
        assertNotNull(connection);
        externalConnectionService.delete(connection);
        List<ExternalConnection> connections = externalConnectionService.getAll();
        assertTrue(connections.stream().noneMatch(c -> c.getId() == 1));
    }

    @Test
    public void delete_shouldDeleteExternalConnectionById() throws Exception {
        cleanRowsInCurrentConnection(
                new String[] { "person", "external_connection_contact", "basic_authentication_data",

                });
        ExternalConnection connection = externalConnectionService.get(1);
        assertNotNull(connection);
        externalConnectionService.delete(1, "");
        List<ExternalConnection> connections = externalConnectionService.getAll();
        assertTrue(connections.stream().noneMatch(c -> c.getId() == 1));
    }

    @Test
    public void deleteAll_shouldDeleteAllExternalConnections() throws Exception {
        cleanRowsInCurrentConnection(
                new String[] { "person", "external_connection_contact", "basic_authentication_data",

                });
        ExternalConnection connection1 = externalConnectionService.get(1);
        ExternalConnection connection2 = externalConnectionService.get(2);
        List<ExternalConnection> connections = new ArrayList<>();
        connections.add(connection1);
        connections.add(connection2);
        assertNotNull(connections);
        assertTrue(connections.size() > 0);
        externalConnectionService.deleteAll(connections);
        List<ExternalConnection> deletedConnections = externalConnectionService.getAll();
        assertTrue(deletedConnections.stream().noneMatch(c -> c.getId() == 1));
        assertTrue(deletedConnections.stream().noneMatch(c -> c.getId() == 2));

    }

    @Test
    public void deleteAll_shouldDeleteAllExternalConnectionsByIds() throws Exception {
        cleanRowsInCurrentConnection(
                new String[] { "person", "external_connection_contact", "basic_authentication_data",

                });
        List<Integer> idsToDelete = List.of(1, 2);
        externalConnectionService.deleteAll(idsToDelete, "");
        List<ExternalConnection> deletedConnections = externalConnectionService.getAll();
        assertTrue(deletedConnections.stream().noneMatch(c -> c.getId() == 1));
        assertTrue(deletedConnections.stream().noneMatch(c -> c.getId() == 2));
    }

    @Test
    public void get_shouldReturnExternalConnectionById() throws Exception {
        ExternalConnection connection = externalConnectionService.get(1);
        assertNotNull(connection);
        assertEquals("1", String.valueOf(connection.getId()));
        assertEquals(ProgrammedConnection.SMTP_SERVER, connection.getProgrammedConnection());
    }

    @Test
    public void getPage_shouldReturnExternalConnectionsPage() throws Exception {
        List<ExternalConnection> connections = externalConnectionService.getPage(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));

        assertTrue(connections.size() <= expectedPages);
        assertTrue(connections.stream()
                .anyMatch(c -> ProgrammedConnection.SMTP_SERVER.equals(c.getProgrammedConnection())));
        assertTrue(connections.stream()
                .anyMatch(c -> ProgrammedConnection.SMPP_SERVER.equals(c.getProgrammedConnection())));
    }

    @Test
    public void getAllMaching_shouldReturnAllMatchingGivenMap() throws Exception {
        Map<String, Object> filterMap = Map.of("id", "1");
        List<ExternalConnection> connections = externalConnectionService.getAllMatching(filterMap);
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());

    }

    @Test
    public void getAllMatching_shouldReturnAllMatchingGivenArguments() {
        List<ExternalConnection> connections = externalConnectionService.getAllMatching("id", "1");
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());

    }

    @Test
    public void getAllOrdered_shouldReturnOrderedExternalConnectionGivenArguments() {
        List<ExternalConnection> connections = externalConnectionService.getAllOrdered("id", false);
        assertEquals(ProgrammedConnection.SMTP_SERVER, connections.get(0).getProgrammedConnection());
        assertEquals(ProgrammedConnection.SMPP_SERVER, connections.get(1).getProgrammedConnection());
        assertEquals(ProgrammedConnection.BMP_SMS_SERVER, connections.get(2).getProgrammedConnection());
        assertEquals(ProgrammedConnection.INFO_HIGHWAY, connections.get(3).getProgrammedConnection());

    }

    @Test
    public void getAllOrdered_shouldReturnAllOrderedGivenList() {
        List<String> filterIds = List.of("id");
        List<ExternalConnection> connections = externalConnectionService.getAllOrdered(filterIds, false);
        assertEquals(ProgrammedConnection.SMTP_SERVER, connections.get(0).getProgrammedConnection());
        assertEquals(ProgrammedConnection.SMPP_SERVER, connections.get(1).getProgrammedConnection());
        assertEquals(ProgrammedConnection.BMP_SMS_SERVER, connections.get(2).getProgrammedConnection());
        assertEquals(ProgrammedConnection.INFO_HIGHWAY, connections.get(3).getProgrammedConnection());

    }

    @Test
    public void getAllMatchingOrdered_shouldRetrunMatchingOrderedGivenMap() {
        Map<String, Object> filterMap = Map.of("id", "1");
        List<ExternalConnection> connections = externalConnectionService.getAllMatchingOrdered(filterMap, "id", false);
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnAllMatchingOrderedGivenListAndMap() {
        List<String> filterIds = List.of("id");
        Map<String, Object> filterMap = Map.of("id", "1");
        List<ExternalConnection> connections = externalConnectionService.getAllMatchingOrdered(filterMap, filterIds,
                false);
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnAllMatchingOrderedGivenList() {

        List<String> filterIds = List.of("id");
        List<ExternalConnection> connections = externalConnectionService.getAllMatchingOrdered("id", "1", filterIds,
                false);
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnAllMatchingOrderedGivenArguments() {
        List<ExternalConnection> connections = externalConnectionService.getAllMatchingOrdered("id", "1", "id", false);
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());
    }

    @Test
    public void getMatchingPage_shouldReturnMatchingPageGivenMap() {
        Map<String, Object> filterMap = Map.of("id", "1");
        List<ExternalConnection> connections = externalConnectionService.getMatchingPage(filterMap, 1);
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());
    }

    @Test
    public void getMatchingPage_shouldReturnAllMatchingGivenArguments() {
        List<ExternalConnection> connections = externalConnectionService.getMatchingPage("id", "1", 1);
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());

    }

    @Test
    public void getOrderedPage_shouldReturnOrderedExternalConnectionGivenArguments() {
        List<ExternalConnection> connections = externalConnectionService.getOrderedPage("id", false, 1);
        assertEquals(ProgrammedConnection.SMTP_SERVER, connections.get(0).getProgrammedConnection());
        assertEquals(ProgrammedConnection.SMPP_SERVER, connections.get(1).getProgrammedConnection());
        assertEquals(ProgrammedConnection.BMP_SMS_SERVER, connections.get(2).getProgrammedConnection());
        assertEquals(ProgrammedConnection.INFO_HIGHWAY, connections.get(3).getProgrammedConnection());

    }

    @Test
    public void getOrderedPage_shouldReturnAllOrderedGivenList() {
        List<String> filterIds = List.of("id");
        List<ExternalConnection> connections = externalConnectionService.getOrderedPage(filterIds, false, 1);
        assertEquals(ProgrammedConnection.SMTP_SERVER, connections.get(0).getProgrammedConnection());
        assertEquals(ProgrammedConnection.SMPP_SERVER, connections.get(1).getProgrammedConnection());
        assertEquals(ProgrammedConnection.BMP_SMS_SERVER, connections.get(2).getProgrammedConnection());
        assertEquals(ProgrammedConnection.INFO_HIGHWAY, connections.get(3).getProgrammedConnection());

    }

    @Test
    public void getMatchingOrderedPage_shouldRetrunMatchingOrderedGivenMap() {
        Map<String, Object> filterMap = Map.of("id", "1");
        List<ExternalConnection> connections = externalConnectionService.getMatchingOrderedPage(filterMap, "id", false,
                1);
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());
    }

    @Test
    public void getMatchingOrderedPage_shouldReturnAllMatchingOrderedGivenListAndMap() {
        List<String> filterIds = List.of("id");
        Map<String, Object> filterMap = Map.of("id", "1");
        List<ExternalConnection> connections = externalConnectionService.getMatchingOrderedPage(filterMap, filterIds,
                false, 1);
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());
    }

    @Test
    public void getMatchingOrderedPage_shouldReturnAllMatchingOrderedGivenList() {

        List<String> filterIds = List.of("id");
        List<ExternalConnection> connections = externalConnectionService.getMatchingOrderedPage("id", "1", filterIds,
                false, 1);
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());
    }

    @Test
    public void getMatchingOrderedPage_shouldReturnAllMatchingOrderedGivenArguments() {
        List<ExternalConnection> connections = externalConnectionService.getMatchingOrderedPage("id", "1", "id", false,
                1);
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());
    }

    @Test
    public void getCount_shouldReturnTotalExternalConnectionCount() {
        List<ExternalConnection> connections = externalConnectionService.getAll();
        int count = externalConnectionService.getCount();
        assertTrue(count == connections.size());
    }

    @Test
    public void getCountMatching_shouldReturnTotalExternalConnectionsThatMatch() {
        Map<String, Object> matchMap = Map.of("id", "1");
        int count = externalConnectionService.getCountMatching(matchMap);
        List<ExternalConnection> connections = externalConnectionService.getAllMatching(matchMap);
        assertTrue(count == connections.size());
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());

    }

    @Test
    public void getCountMatching_shouldReturnTotalExternalConnectionsCountGivenArguments() {
        int count = externalConnectionService.getCountMatching("uri", "https://openmrs.example.org/api");
        List<ExternalConnection> connections = externalConnectionService.getAllMatching("id", "1");
        assertTrue(count == connections.size());
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());

    }

    @Test
    public void getCountLike_shouldReturnLikeExternalConnectionsGivenMap() {
        Map<String, String> matchMap = Map.of("uri", "https://openmrs.example.org/api");
        int count = externalConnectionService.getCountLike(matchMap);
        List<ExternalConnection> connections = externalConnectionService.getAllMatching("uri",
                "https://openmrs.example.org/api");
        assertTrue(count == connections.size());
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());

    }

    @Test
    public void getCountLike_shouldReturnLikeExternalConnectionsCountGivenArguments() {
        int count = externalConnectionService.getCountLike("uri", "https://openmrs.example.org/api");
        List<ExternalConnection> connections = externalConnectionService.getAllMatching("uri",
                "https://openmrs.example.org/api");
        assertTrue(count == connections.size());
        assertEquals("1", String.valueOf(connections.getFirst().getId()));
        assertEquals(AuthType.BASIC, connections.getFirst().getActiveAuthenticationType());
    }

    @Test
    public void getNext_shouldReturnNextExternalConnection() {
        ExternalConnection nextExternalConnection = externalConnectionService.getNext("1");
        assertEquals("2", String.valueOf(nextExternalConnection.getId()));
    }

    @Test
    public void getPrevious_shouldReturnPreviousExternalConnection() {
        ExternalConnection prevExternalConnection = externalConnectionService.getPrevious("2");
        assertEquals("1", String.valueOf(prevExternalConnection.getId()));

    }

    @Test
    public void getMatch_shouldReturnAllMatchingGivenMap() throws Exception {
        Map<String, Object> filterMap = Map.of("id", "1");
        Optional<ExternalConnection> connection = externalConnectionService.getMatch(filterMap);
        assertEquals("1", String.valueOf(connection.get().getId()));
        assertEquals(AuthType.BASIC, connection.get().getActiveAuthenticationType());

    }

    @Test
    public void getMatch_shouldReturnAllMatchingGivenArguments() {
        Optional<ExternalConnection> connection = externalConnectionService.getMatch("id", "1");
        assertEquals("1", String.valueOf(connection.get().getId()));
        assertEquals(AuthType.BASIC, connection.get().getActiveAuthenticationType());

    }

}