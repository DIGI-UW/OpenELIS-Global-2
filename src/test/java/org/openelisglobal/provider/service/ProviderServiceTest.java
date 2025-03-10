package org.openelisglobal.provider.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;

public class ProviderServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private PersonService personServive;

    private static final UUID FH_UUID1 = UUID.fromString("f2cdeff8-8d5b-4023-bd7c-932b4b98b6d2");
    private static final UUID FH_UUID2 = UUID.fromString("f1cdeff8-8d5b-4023-bd7c-932b4b98b6d2");
    private static final String PERSON1_FIRSTNAME = "Henry";
    private static final String PERSON1_MIDDLENAME = "Mutton";
    private static final String PERSON1_LASTNAME = "Stanley";

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/provider.xml");
    }

    @Test
    public void providerInThDataBase() {
        List<Provider> providers = providerService.getAll();
        providers.forEach(provider -> System.out.print(provider.getProviderType() + " "));
    }

    @Test
    public void getAllProviders_shouldReturnAllProvders() {
        List<Provider> providers = providerService.getAllProviders();
        assertNotNull("should return providers", providers);
        assertEquals("should return 2 providers", 2, providers.size());

    }

    @Test
    public void getProviderByPerson_shouldReturnProviderGivenThePerson() {
        Person person1 = personServive.getPersonByLastName("Doe");
        Person person2 = personServive.getPersonByLastName("Mulizi");
        Provider provider1 = providerService.getProviderByPerson(person1);
        Provider provider2 = providerService.getProviderByPerson(person2);
        assertNotNull("Should have return privider1", provider1);
        assertNotNull("Should have return privider2", provider2);
        assertEquals("provider1 should have an External id EXT123456", "EXT123456", provider1.getExternalId());
        assertEquals("provider2 should have an External id EXT123466", "EXT123466", provider2.getExternalId());
    }

    @Test
    public void getProviderByFhirUuid_shouldReturnTheProviderGivenFhirUUID() {

        Provider provider1 = providerService.getProviderByFhirId(FH_UUID1);
        Provider provider2 = providerService.getProviderByFhirId(FH_UUID2);
        assertNotNull("should return provider", provider1);
        assertNotNull("should return provider", provider2);
        assertEquals("provider1 should have an External id EXT123456", "EXT123456", provider1.getExternalId());
        assertEquals("provider2 should have an External id EXT123466", "EXT123466", provider2.getExternalId());
        assertEquals("provider1 should have an provider type G", "G", provider1.getProviderType());
        assertEquals("provider2 should have an provider type s", "S", provider2.getProviderType());
    }

    @Test
    public void getProviderPages_shouldReturnPagesWithProviders() {
        List<Provider> providersPage = providerService.getPageOfProviders(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(providersPage.size() <= expectedPages);
    }

    @Test
    public void getPagesOfSearchedProviders_shouldReturnPagesOfSearchedProvider() {
        Provider provider1 = providerService.getProviderByFhirId(FH_UUID1);
        String providerType = provider1.getProviderType();
        List<Provider> providersSearched = providerService.getPagesOfSearchedProviders(1, providerType);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(providersSearched.size() <= expectedPages);

    }

    @Test
    public void deactivateProviders_shouldSetAllActiveFalse() {
        List<Provider> providers = providerService.getAllProviders();
        providerService.deactivateProviders(providers);
        List<Provider> providersDeactivated = providerService.getAllProviders();
        providersDeactivated.forEach(provider -> assertFalse("should have false on active", provider.getActive()));

    }

    @Test
    public void insertOrUpdateProviderByFhirUuid_shouldUpdateProviderGivenTheFhirUUID() {
        Provider provider1 = new Provider();
        Person person1 = personServive.get("3");

        provider1.setPerson(person1);

        Provider provider2 = providerService.insertOrUpdateProviderByFhirUuid(FH_UUID1, provider1);

        assertNotNull("should return a provider", provider2);
        assertNotNull("provider person should not be null", provider2.getPerson());
        assertEquals("provider person first name should match", PERSON1_FIRSTNAME,
                provider2.getPerson().getFirstName());
        assertEquals("provider person middle name should match", PERSON1_MIDDLENAME,
                provider2.getPerson().getMiddleName());
        assertEquals("provider person first name should match", PERSON1_LASTNAME, provider2.getPerson().getLastName());
    }

    @Test
    public void getData_shouldReturnDataForProvider() {
        Provider provider1 = providerService.getProviderByFhirId(FH_UUID1);
        providerService.getData(provider1);
        assertEquals("Person last name should be Doe", "Doe", provider1.getPerson().getLastName());
        assertEquals("Person last name should be Doe", "Joddy", provider1.getPerson().getMiddleName());
        assertEquals("Person last name should be Doe", "John", provider1.getPerson().getFirstName());

    }

    @Test
    public void getAllActiveProviders_shouldReturnActiveproviders() {
        List<Provider> providers = providerService.getAllActiveProviders();
        providers.forEach(provider -> assertTrue("should all returned have active true", provider.getActive()));
    }

    @Test
    public void getProviderIdByFhirId_shouldReturnProviderIdGiveFhirId() {
        String provider1ID = providerService.getProviderIdByFhirId(FH_UUID1);
        String provider2ID = providerService.getProviderIdByFhirId(FH_UUID2);
        assertEquals("Provider1 id should be 1", "1", provider1ID);
        assertEquals("Provider2 id should be 2", "2", provider2ID);
    }

    @Test
    public void deactivateAllProviders_shouldImplicitlyDeactivateAllProviders() {
        providerService.deactivateAllProviders();
        List<Provider> providers = providerService.getAll();
        providers.forEach(provider -> assertFalse("should all active be false", provider.getActive()));
    }

    @Test
    public void getTotalSearchedProviderCount_shouldGetCountForSearchedProviders() {
        Provider provider1 = providerService.get("1");
        String providerType = provider1.getProviderType();
        int totalCount = providerService.getTotalSearchedProviderCount(providerType);
        assertTrue("should be a value greater than or equal to zero", totalCount >= 0);

    }

}