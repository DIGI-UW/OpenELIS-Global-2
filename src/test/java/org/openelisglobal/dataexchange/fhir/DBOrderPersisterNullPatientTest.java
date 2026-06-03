package org.openelisglobal.dataexchange.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-356 (receive side): verifies that an ElectronicOrder can be persisted
 * without a patient (env/vector samples). The patient_id column is nullable, so
 * the DB must accept a null patient.
 */
public class DBOrderPersisterNullPatientTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private PersonService personService;

    @Before
    public void cleanState() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "electronic_order", "patient", "person" });
    }

    @Test
    public void insert_shouldCreateElectronicOrder_whenPatientIsNull() {
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setExternalId("ENV-REF-NULL-PATIENT-001");
        eOrder.setData("{\"resourceType\":\"Task\",\"status\":\"requested\"}");
        eOrder.setStatusId("1");
        eOrder.setOrderTimestamp(DateUtil.getNowAsTimestamp());
        eOrder.setSysUserId("1");
        eOrder.setType(ElectronicOrderType.FHIR);
        eOrder.setPriority(OrderPriority.ROUTINE);
        // Explicitly no patient — this is the OGC-356 fix path
        eOrder.setPatient(null);

        String insertedId = electronicOrderService.insert(eOrder);

        assertNotNull("eOrder should be persisted and have an ID", insertedId);

        List<ElectronicOrder> found = electronicOrderService
                .getElectronicOrdersByExternalId("ENV-REF-NULL-PATIENT-001");
        assertNotNull("should find the persisted order", found);
        assertEquals("should find exactly one order", 1, found.size());
        assertNull("patient should be null for env/vector order", found.get(0).getPatient());
        assertEquals("ENV-REF-NULL-PATIENT-001", found.get(0).getExternalId());
    }

    @Test
    public void insert_shouldCreateElectronicOrderWithPatient_whenPatientIsPresent() {
        // Create a real patient for the regression test
        Person person = new Person();
        person.setFirstName("Test");
        person.setLastName("RegressionPatient");
        person.setSysUserId("1");
        String personId = personService.insert(person);
        assertNotNull("person should be inserted", personId);

        Patient patient = new Patient();
        patient.setPerson(person);
        patient.setGender("M");
        patient.setSysUserId("1");
        String patientId = patientService.insert(patient);
        assertNotNull("patient should be inserted", patientId);

        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setExternalId("CLINICAL-REF-WITH-PATIENT-001");
        eOrder.setData("{\"resourceType\":\"Task\",\"status\":\"requested\"}");
        eOrder.setStatusId("1");
        eOrder.setOrderTimestamp(DateUtil.getNowAsTimestamp());
        eOrder.setSysUserId("1");
        eOrder.setType(ElectronicOrderType.FHIR);
        eOrder.setPriority(OrderPriority.ROUTINE);
        eOrder.setPatient(patient);

        String insertedId = electronicOrderService.insert(eOrder);

        assertNotNull("eOrder should be persisted", insertedId);
        List<ElectronicOrder> found = electronicOrderService
                .getElectronicOrdersByExternalId("CLINICAL-REF-WITH-PATIENT-001");
        assertEquals("should find exactly one order", 1, found.size());
        assertNotNull("patient should be present for clinical order", found.get(0).getPatient());
        assertEquals(patientId, found.get(0).getPatient().getId());
    }
}
