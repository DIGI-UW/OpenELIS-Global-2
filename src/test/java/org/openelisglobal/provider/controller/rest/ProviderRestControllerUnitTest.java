package org.openelisglobal.provider.controller.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hibernate.ObjectNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.service.ProviderTitleService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Standalone MockMvc REST controller unit tests for
 * {@link ProviderRestController}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProviderRestControllerUnitTest {

    @Mock
    private ProviderService providerService;

    @Mock
    private PersonService personService;

    @Mock
    private ProviderTitleService providerTitleService;

    @InjectMocks
    private ProviderRestController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void getProvider_whenProviderExists_returns200AndProviderJson() throws Exception {
        Provider provider = new Provider();
        provider.setId("2");
        provider.setNpi("1234567891");
        provider.setExternalId("EXT123466");
        provider.setActive(true);
        when(providerService.get("2")).thenReturn(provider);

        mockMvc.perform(get("/rest/Provider/raw/2").accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("2")).andExpect(jsonPath("$.npi").value("1234567891"))
                .andExpect(jsonPath("$.externalId").value("EXT123466")).andExpect(jsonPath("$.active").value(true));
    }

    @Test
    public void getProvider_whenObjectNotFoundExceptionThrown_returns404() throws Exception {
        when(providerService.get("99999")).thenThrow(new ObjectNotFoundException("99999", Provider.class.getName()));

        mockMvc.perform(get("/rest/Provider/raw/99999").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getProvider_whenServiceReturnsNull_returns404() throws Exception {
        when(providerService.get("99999")).thenReturn(null);

        mockMvc.perform(get("/rest/Provider/raw/99999").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getPerson_whenPersonExists_returns200AndPersonJson() throws Exception {
        Person person = new Person();
        person.setId("2");
        person.setFirstName("James");
        person.setLastName("Mark");
        person.setCity("Orion");
        when(personService.get("2")).thenReturn(person);

        mockMvc.perform(get("/rest/Provider/Person/2").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("2"))
                .andExpect(jsonPath("$.firstName").value("James")).andExpect(jsonPath("$.lastName").value("Mark"))
                .andExpect(jsonPath("$.city").value("Orion"));
    }

    @Test
    public void getPerson_whenObjectNotFoundExceptionThrown_returns404() throws Exception {
        when(personService.get("99999")).thenThrow(new ObjectNotFoundException("99999", Person.class.getName()));

        mockMvc.perform(get("/rest/Provider/Person/99999").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getPerson_whenServiceReturnsNull_returns404() throws Exception {
        when(personService.get("99999")).thenReturn(null);

        mockMvc.perform(get("/rest/Provider/Person/99999").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }
}
