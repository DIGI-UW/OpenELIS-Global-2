package org.openelisglobal.fhir;

import static org.junit.Assert.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.fhir.providers.DeviceProvider;
import org.openelisglobal.fhir.providers.DiagnosticReportProvider;
import org.openelisglobal.fhir.providers.LocationProvider;
import org.openelisglobal.fhir.providers.ObservationProvider;
import org.openelisglobal.fhir.providers.OrganizationProvider;
import org.openelisglobal.fhir.providers.PatientProvider;
import org.openelisglobal.fhir.providers.PractitionerProvider;
import org.openelisglobal.fhir.providers.ServiceRequestProvider;
import org.openelisglobal.fhir.providers.SpecimenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

/**
 * {@code _lastUpdated} and {@code meta.lastUpdated} describe when a record was
 * last modified, so reading or searching through the facade must leave every
 * row's lastupdated untouched. Guards the write-on-read causes fixed with the
 * facade search work: the Patient entered-birth-date setter re-deriving
 * {@code birth_date} during hydration, the Provider active getter normalising
 * null to false, and the list converter turning an empty list into a non-null
 * column value.
 */
public class FacadeReadsPreserveLastUpdatedTest extends BaseWebContextSensitiveTest {

    private static final String PATIENT_UUID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String PROVIDER_UUID = "550e8400-e29b-41d4-a716-446655441004";
    private static final String ANALYSIS_UUID = "f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01";
    private static final String SAMPLE_ITEM_UUID = "68438220-5cef-44c4-9e6f-9f88e6b93270";
    private static final String RESULT_UUID = "550e8400-e29b-41d4-a716-446655440003";
    private static final String ANALYZER_UUID = "2d335c87-1def-42e9-a610-2748b9872a1c";
    private static final String ROOM_UUID = "f2cdeff8-8d5b-4023-bd7c-932b4b98b6d3";

    @Autowired
    private PatientProvider patientProvider;
    @Autowired
    private PractitionerProvider practitionerProvider;
    @Autowired
    private OrganizationProvider organizationProvider;
    @Autowired
    private SpecimenProvider specimenProvider;
    @Autowired
    private ServiceRequestProvider serviceRequestProvider;
    @Autowired
    private ObservationProvider observationProvider;
    @Autowired
    private DiagnosticReportProvider diagnosticReportProvider;
    @Autowired
    private DeviceProvider deviceProvider;
    @Autowired
    private LocationProvider locationProvider;

    private RestfulServer fhirServlet;
    private EntityManager entityManager;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/facade-device.xml");
        executeDataSetWithStateManagement("testdata/facade-location.xml");
        executeDataSetWithStateManagement("testdata/result-facade.xml");

        fhirServlet = new RestfulServer(FhirContext.forR4());
        fhirServlet.setResourceProviders(Arrays.asList(patientProvider, practitionerProvider, organizationProvider,
                specimenProvider, serviceRequestProvider, observationProvider, diagnosticReportProvider, deviceProvider,
                locationProvider));
        MockServletConfig servletConfig = new MockServletConfig(new MockServletContext());
        servletConfig.addInitParameter("name", "FhirServlet");
        fhirServlet.init(servletConfig);
        entityManager = SharedEntityManagerCreator
                .createSharedEntityManager(applicationContext.getBean(EntityManagerFactory.class));
    }

    @Test
    public void searchesAndReads_doNotTouchLastUpdated() throws Exception {
        Map<String, Object> before = snapshot();

        for (String path : new String[] { "/Patient",
                "/Patient?_id=" + PATIENT_UUID + "&_revinclude=Observation:patient", "/Patient/" + PATIENT_UUID,
                "/Practitioner?_revinclude=Observation:performer", "/Practitioner/" + PROVIDER_UUID, "/Organization",
                "/Specimen?_include=Specimen:patient", "/Specimen/" + SAMPLE_ITEM_UUID,
                "/ServiceRequest?_include=ServiceRequest:patient&_include=ServiceRequest:requester",
                "/ServiceRequest/" + ANALYSIS_UUID, "/Observation?_include=Observation:performer",
                "/Observation/" + RESULT_UUID, "/DiagnosticReport?_include=DiagnosticReport:result",
                "/DiagnosticReport/" + ANALYSIS_UUID, "/Device", "/Device/" + ANALYZER_UUID,
                "/Location?_revinclude=Location:partof", "/Location/" + ROOM_UUID }) {
            get(path);
        }

        Map<String, Object> after = snapshot();
        before.forEach((row, lastUpdated) -> assertEquals(row + " was rewritten by a read", String.valueOf(lastUpdated),
                String.valueOf(after.get(row))));
    }

    private Map<String, Object> snapshot() {
        Map<String, Object> rows = new LinkedHashMap<>();
        rows.put("patient 1", single("select p.lastupdated from Patient p where p.id = '1'"));
        rows.put("provider 1", single("select p.lastupdated from Provider p where p.id = '1'"));
        rows.put("organization 3", single("select o.lastupdated from Organization o where o.id = '3'"));
        rows.put("sample item 601", single("select s.lastupdated from SampleItem s where s.id = '601'"));
        rows.put("analysis 1", single("select a.lastupdated from Analysis a where a.id = '1'"));
        rows.put("result 3", single("select r.lastupdated from Result r where r.id = '3'"));
        rows.put("analyzer 1", single("select a.lastupdated from Analyzer a where a.id = '1'"));
        rows.put("storage room 1", single("select r.lastupdated from StorageRoom r where r.id = 1"));
        return rows;
    }

    private Object single(String jpql) {
        return entityManager.createQuery(jpql).getSingleResult();
    }

    private void get(String path) throws Exception {
        String query = path.contains("?") ? path.substring(path.indexOf('?') + 1) : null;
        String pathInfo = path.contains("?") ? path.substring(0, path.indexOf('?')) : path;
        MockHttpServletRequest request = buildFhirRequest("GET", pathInfo);
        if (query != null) {
            request.setQueryString(query);
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                request.addParameter(kv[0], kv.length > 1 ? kv[1] : "");
            }
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        fhirServlet.service(request, response);
        assertEquals(path + " -> " + response.getContentAsString(), 200, response.getStatus());
    }
}
