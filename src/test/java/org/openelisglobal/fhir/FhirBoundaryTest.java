package org.openelisglobal.fhir;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.After;
import org.junit.Test;
import org.openelisglobal.fhir.providers.*;
import org.openelisglobal.fhir.servlets.FhirRestfulServer;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Exercises the registered production servlet, including HAPI request and
 * response authorization.
 */
public class FhirBoundaryTest {
    private FhirRestfulServer server;
    private final ObjectMapper json = new ObjectMapper();

    @After
    public void cleanUp() {
        SecurityContextHolder.clearContext();
        if (server != null)
            server.destroy();
    }

    private void initialize(IResourceProvider... providers) throws Exception {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(FhirContext.class)).thenReturn(FhirContext.forR4Cached());
        Map<String, IResourceProvider> beans = new LinkedHashMap<>();
        for (IResourceProvider provider : providers)
            beans.put(provider.getResourceType().getSimpleName(), provider);
        when(context.getBeansOfType(IResourceProvider.class)).thenReturn(beans);
        server = new FhirRestfulServer(context);
        server.init(new MockServletConfig(new MockServletContext()));
    }

    private void login(String... permissions) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("boundary-reader",
                null, AuthorityUtils.createAuthorityList(permissions)));
    }

    private MockHttpServletResponse serve(String method, String path, String body) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/fhir" + path.split("\\?")[0]);
        request.setServletPath("/fhir");
        String[] pieces = path.split("\\?", 2);
        request.setPathInfo(pieces[0]);
        if (pieces.length > 1) {
            request.setQueryString(pieces[1]);
            for (String pair : pieces[1].split("&")) {
                String[] parameter = pair.split("=", 2);
                request.addParameter(parameter[0], parameter.length == 1 ? "" : parameter[1]);
            }
        }
        request.addHeader("Accept", "application/fhir+json");
        request.setContentType("application/fhir+json");
        if (body != null)
            request.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        server.service(request, response);
        return response;
    }

    @Test
    public void productionProvidersExposeNoWritesEvenToAdministrator() throws Exception {
        IResourceProvider[] providers = { new PatientProvider(), new ObservationProvider(),
                new DiagnosticReportProvider(), new ServiceRequestProvider(), new SpecimenProvider(),
                new DeviceProvider(), new LocationProvider(), new OrganizationProvider(), new PractitionerProvider() };
        initialize(providers);
        login("ROLE_ADMIN", "ROLE_GLOBAL_ADMIN", "fhir.read.results", "fhir.read.patients", "fhir.read.reference");
        for (IResourceProvider provider : providers) {
            String type = provider.getResourceType().getSimpleName();
            for (String method : List.of("POST", "PUT", "PATCH", "DELETE")) {
                String path = "/" + type + (method.equals("POST") ? "" : "/00000000-0000-0000-0000-000000000001");
                MockHttpServletResponse response = serve(method, path, "{\"resourceType\":\"" + type + "\"}");
                assertTrue(type + " " + method + ": " + response.getContentAsString(),
                        Set.of(400, 405).contains(response.getStatus()));
                assertEquals("OperationOutcome",
                        json.readTree(response.getContentAsString()).path("resourceType").asText());
            }
        }
        for (String kind : List.of("transaction", "batch")) {
            MockHttpServletResponse response = serve("POST", "/", "{\"resourceType\":\"Bundle\",\"type\":\"" + kind
                    + "\",\"entry\":[{\"request\":{\"method\":\"DELETE\",\"url\":\"Patient/p1\"}}]}");
            assertTrue(response.getContentAsString(), Set.of(400, 405).contains(response.getStatus()));
        }
        JsonNode metadata = json.readTree(serve("GET", "/metadata", null).getContentAsString());
        for (JsonNode resource : metadata.path("rest").get(0).path("resource")) {
            for (JsonNode interaction : resource.path("interaction")) {
                assertTrue(interaction.toString(),
                        Set.of("read", "search-type").contains(interaction.path("code").asText()));
            }
        }
    }

    @Test
    public void ordinaryAndAnonymousUsersCannotReadClinicalResources() throws Exception {
        initialize(new Patients(), new Observations());
        for (String path : List.of("/Patient", "/Patient/p1", "/Observation", "/Observation/o1", "/metadata")) {
            SecurityContextHolder.clearContext();
            assertEquals(path, 403, serve("GET", path, null).getStatus());
            login("ROLE_RESULTS");
            assertEquals(path, 403, serve("GET", path, null).getStatus());
        }
    }

    @Test
    public void eachGrantAdmitsOnlyItsResourceTypes() throws Exception {
        initialize(new Patients(), new Observations());
        login("fhir.read.results");
        assertEquals(200, serve("GET", "/Observation/o1", null).getStatus());
        assertEquals(200, serve("GET", "/Observation", null).getStatus());
        assertEquals(403, serve("GET", "/Patient/p1", null).getStatus());
        login("fhir.read.patients");
        assertEquals(200, serve("GET", "/Patient/p1", null).getStatus());
        assertEquals(403, serve("GET", "/Observation/o1", null).getStatus());
        login("fhir.read.reference");
        assertEquals(403, serve("GET", "/Patient/p1", null).getStatus());
        assertEquals(403, serve("GET", "/Observation/o1", null).getStatus());
    }

    @Test
    public void includesCannotLeakResourcesOutsideTheReadersGrant() throws Exception {
        initialize(new Patients(), new Observations());
        login("fhir.read.results");
        MockHttpServletResponse denied = serve("GET", "/Observation?_include=Observation:subject", null);
        assertEquals(403, denied.getStatus());
        assertFalse(denied.getContentAsString().contains("BoundaryPatient"));
        login("fhir.read.results", "fhir.read.patients");
        MockHttpServletResponse allowed = serve("GET", "/Observation?_include=Observation:subject", null);
        assertEquals(200, allowed.getStatus());
        assertTrue(allowed.getContentAsString().contains("BoundaryPatient"));
    }

    public static class Patients implements IResourceProvider {
        public Class<Patient> getResourceType() {
            return Patient.class;
        }

        @Read
        public Patient read(@IdParam IdType id) {
            return patient();
        }

        @Search
        public List<Patient> search() {
            return List.of(patient());
        }

        static Patient patient() {
            Patient patient = new Patient();
            patient.setId("Patient/p1");
            patient.addName().setFamily("BoundaryPatient");
            return patient;
        }
    }

    public static class Observations implements IResourceProvider {
        public Class<Observation> getResourceType() {
            return Observation.class;
        }

        @Read
        public Observation read(@IdParam IdType id) {
            return observation();
        }

        @Search
        public List<IBaseResource> search(@IncludeParam(allow = { "Observation:subject" }) Set<Include> includes) {
            return includes != null && !includes.isEmpty() ? List.of(observation(), Patients.patient())
                    : List.of(observation());
        }

        static Observation observation() {
            Observation observation = new Observation();
            observation.setId("Observation/o1");
            observation.setSubject(new org.hl7.fhir.r4.model.Reference("Patient/p1"));
            return observation;
        }
    }
}
