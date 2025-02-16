package org.openelisglobal.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;

public class ProviderServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ProviderService providerService; // Inject the providerService

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/provider.xml"); // Ensure the dataset is correctly loaded
    }

    @Test
    public void providerInThDataBase() {
        List<Provider> providers = providerService.getAll();
        providers.forEach(provider -> System.out.print(provider.getProviderType() + " ")); // Print out the provider
                                                                                           // types
    }

    @Test
    public void getAllProviders_shouldReturnAllProvders() {
        List<Provider> providers = providerService.getAllProviders();
        assertNotNull("should return providers", providers);
        assertEquals("should return 2 providers", 2, providers.size());

    }
}
