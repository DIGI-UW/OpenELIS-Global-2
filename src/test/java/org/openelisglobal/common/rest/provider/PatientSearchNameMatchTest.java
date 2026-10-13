package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Order entry's patient search matches names exactly or by prefix (OGC-1443,
 * FR-B6a), while every other caller of the same endpoint keeps the
 * contains-and-sound-alike matching.
 */
public class PatientSearchNameMatchTest extends BaseWebContextSensitiveTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private PatientService patientService;

    @Autowired
    private PersonService personService;

    private MockMvc searchMvc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        ensureReferenceTables("PATIENT", "PERSON", "PATIENT_IDENTITY");
        executeDataSetWithStateManagement("testdata/system-user.xml");
        cleanRowsInCurrentConnection(new String[] { "patient_identity", "patient", "person" });
        PatientIdentityTypeMap.reset();

        createPatient("John", "Smith", "NM-1");
        createPatient("Jane", "Smithson", "NM-2");
        createPatient("John", "TEST-Smith", "NM-3");
        createPatient("Joan", "Smyth", "NM-4");
        createPatient("Peter", "Jones", "NM-5");

        PatientSearchRestController controller = webApplicationContext.getAutowireCapableBeanFactory()
                .createBean(PatientSearchRestController.class);
        searchMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void orderEntryFindsExactAndPrefixMatchesOnly() throws Exception {
        assertEquals(List.of("Smith", "Smithson"), lastNames(search("Smith", "", "prefix")));
    }

    @Test
    public void orderEntryAppliesTheFirstNameByPrefixToo() throws Exception {
        assertEquals(List.of("Smith"), lastNames(search("smi", "Jo", "prefix")));
    }

    @Test
    public void theSharedSearchStillFindsContainedAndSoundAlikeNames() throws Exception {
        assertEquals(List.of("Smith", "Smithson", "Smyth", "TEST-Smith"), lastNames(search("Smith", "", null)));
    }

    private JsonNode search(String lastName, String firstName, String nameMatch) throws Exception {
        var request = get("/rest/patient-search-results").param("lastName", lastName).param("firstName", firstName)
                .param("STNumber", "").param("subjectNumber", "").param("nationalID", "").param("labNumber", "")
                .param("guid", "").param("dateOfBirth", "").param("gender", "").param("suppressExternalSearch", "true");
        if (nameMatch != null) {
            request = request.param("nameMatch", nameMatch);
        }
        MvcResult result = searchMvc.perform(request).andExpect(status().isOk()).andReturn();
        return JSON.readTree(result.getResponse().getContentAsString()).get("patientSearchResults");
    }

    private static List<String> lastNames(JsonNode results) {
        List<String> names = new ArrayList<>();
        results.forEach(result -> names.add(result.get("lastName").asText()));
        Collections.sort(names);
        return names;
    }

    private void createPatient(String firstName, String lastName, String nationalId) throws Exception {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setSysUserId("1");
        personService.save(person);

        Patient patient = new Patient();
        patient.setPerson(person);
        patient.setBirthDate(new Timestamp(new SimpleDateFormat("dd/MM/yyyy").parse("01/02/1990").getTime()));
        patient.setGender("M");
        patient.setNationalId(nationalId);
        patient.setSysUserId("1");
        patientService.insert(patient);
    }
}
