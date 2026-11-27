package org.openelisglobal.patient.controller.rest;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;

/**
 * OGC-1376: a patient screen opened before someone else saved the patient got a
 * raw 500 ("Row was updated or deleted by another transaction") and no message.
 * The save is refused first with a 409 naming who saved and when, and nothing
 * is written.
 */
public class PatientManagementStaleSaveTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientService patientService;

    private PatientManagementRestController controller;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/facade-patient.xml");
        controller = webApplicationContext.getAutowireCapableBeanFactory()
                .createBean(PatientManagementRestController.class);
    }

    private PatientManagementInfo editFrom(Patient stored, Person person) {
        PatientManagementInfo info = new PatientManagementInfo();
        info.setPatientPK(stored.getId());
        info.setPatientLastUpdated(stored.getLastupdated().toString());
        info.setPersonLastUpdated(person.getLastupdated().toString());
        info.setEmail("stale.edit@example.org");
        return info;
    }

    @Test
    public void aSaveFromAScreenLoadedBeforeThePersonChangedIsRefused() throws Exception {
        Patient stored = patientService.get("1");
        Person person = patientService.getPerson(stored);
        PatientManagementInfo info = editFrom(stored, person);
        info.setPersonLastUpdated(new java.sql.Timestamp(person.getLastupdated().getTime() - 60_000).toString());

        ResponseEntity<Map<String, Object>> response = controller.savepatient(new MockHttpServletRequest(), info,
                new BeanPropertyBindingResult(info, "patientInfo"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("error.patient.staleSave", response.getBody().get("messageKey"));
        assertEquals("john@gmail.com", patientService.getPerson(patientService.get("1")).getEmail());
    }

    @Test
    public void aSaveFromAScreenLoadedBeforeThePatientChangedIsRefused() throws Exception {
        Patient stored = patientService.get("1");
        PatientManagementInfo info = editFrom(stored, patientService.getPerson(stored));
        info.setPatientLastUpdated(new java.sql.Timestamp(stored.getLastupdated().getTime() - 60_000).toString());

        ResponseEntity<Map<String, Object>> response = controller.savepatient(new MockHttpServletRequest(), info,
                new BeanPropertyBindingResult(info, "patientInfo"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals("error.patient.staleSave", response.getBody().get("messageKey"));
    }
}
