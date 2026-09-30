package org.openelisglobal.sample.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;

/**
 * OGC-1407: a save of an existing order that asks to add its patient again
 * keeps the patient the order already has when it is the same person, instead
 * of adding one more patient record per save.
 */
@RunWith(MockitoJUnitRunner.class)
public class SamplePatientEntryOrderPatientReuseTest {

    private static final String ORDER_ID = "172";

    @Mock
    private SampleService sampleService;
    @Mock
    private SampleHumanService sampleHumanService;

    @InjectMocks
    private SamplePatientEntryRestController controller;

    @Test
    public void addingTheSameNationalIdAgainKeepsTheOrdersPatient() {
        orderHolds(patient("115", "QA1407N1", "Qadup", "Nia", "05/03/1988", "F"));
        PatientManagementInfo sent = added("qa1407n1 ", "Qadup", "Nia", "05/03/1988", "F");

        controller.reuseOrderPatient(existingOrder(), sent);

        assertEquals("115", sent.getPatientPK());
        assertEquals(PatientUpdateStatus.NO_ACTION, sent.getPatientUpdateStatus());
    }

    @Test
    public void aDifferentNationalIdIsAnotherPersonAndIsStillAdded() {
        orderHolds(patient("115", "QA1407N1", "Qadup", "Nia", "05/03/1988", "F"));
        PatientManagementInfo sent = added("QA1407N2", "Qadup", "Nia", "05/03/1988", "F");

        controller.reuseOrderPatient(existingOrder(), sent);

        assertNull(sent.getPatientPK());
        assertEquals(PatientUpdateStatus.ADD, sent.getPatientUpdateStatus());
    }

    @Test
    public void withoutNationalIdsTheSameNameBirthDateAndSexIsTheSamePerson() {
        orderHolds(patient("115", null, "Qadup", "Nia", "05/03/1988", "F"));
        PatientManagementInfo sent = added("", "qadup", "NIA", "05/03/1988", "F");

        controller.reuseOrderPatient(existingOrder(), sent);

        assertEquals("115", sent.getPatientPK());
        assertEquals(PatientUpdateStatus.NO_ACTION, sent.getPatientUpdateStatus());
    }

    @Test
    public void withoutNationalIdsAnotherBirthDateIsAnotherPerson() {
        orderHolds(patient("115", null, "Qadup", "Nia", "05/03/1988", "F"));
        PatientManagementInfo sent = added("", "Qadup", "Nia", "06/03/1988", "F");

        controller.reuseOrderPatient(existingOrder(), sent);

        assertNull(sent.getPatientPK());
        assertEquals(PatientUpdateStatus.ADD, sent.getPatientUpdateStatus());
    }

    @Test
    public void aNationalIdOnOnlyOneSideIsAnotherPerson() {
        orderHolds(patient("115", null, "Qadup", "Nia", "05/03/1988", "F"));
        PatientManagementInfo sent = added("QA1407N1", "Qadup", "Nia", "05/03/1988", "F");

        controller.reuseOrderPatient(existingOrder(), sent);

        assertNull(sent.getPatientPK());
        assertEquals(PatientUpdateStatus.ADD, sent.getPatientUpdateStatus());
    }

    @Test
    public void anOrderWithoutAPatientAddsTheOneSent() {
        Sample order = new Sample();
        order.setId(ORDER_ID);
        when(sampleService.get(ORDER_ID)).thenReturn(order);
        when(sampleHumanService.getPatientForSample(order)).thenReturn(null);
        PatientManagementInfo sent = added("QA1407N1", "Qadup", "Nia", "05/03/1988", "F");

        controller.reuseOrderPatient(existingOrder(), sent);

        assertNull(sent.getPatientPK());
        assertEquals(PatientUpdateStatus.ADD, sent.getPatientUpdateStatus());
    }

    @Test
    public void aNewOrderIsUntouched() {
        PatientManagementInfo sent = added("QA1407N1", "Qadup", "Nia", "05/03/1988", "F");

        controller.reuseOrderPatient(new SampleOrderItem(), sent);

        assertEquals(PatientUpdateStatus.ADD, sent.getPatientUpdateStatus());
        verify(sampleService, never()).get(anyString());
    }

    @Test
    public void aSaveThatAlreadyNamesItsPatientIsUntouched() {
        PatientManagementInfo sent = added("QA1407N1", "Qadup", "Nia", "05/03/1988", "F");
        sent.setPatientPK("115");
        sent.setPatientUpdateStatus(PatientUpdateStatus.UPDATE);

        controller.reuseOrderPatient(existingOrder(), sent);

        assertEquals("115", sent.getPatientPK());
        assertEquals(PatientUpdateStatus.UPDATE, sent.getPatientUpdateStatus());
        verify(sampleService, never()).get(anyString());
    }

    private void orderHolds(Patient patient) {
        Sample order = new Sample();
        order.setId(ORDER_ID);
        when(sampleService.get(ORDER_ID)).thenReturn(order);
        when(sampleHumanService.getPatientForSample(order)).thenReturn(patient);
    }

    private SampleOrderItem existingOrder() {
        SampleOrderItem order = new SampleOrderItem();
        order.setSampleId(ORDER_ID);
        order.setLabNo("DEV01260000000000552");
        return order;
    }

    /**
     * The stored patient. Its birth date is answered directly: the value object's
     * setter parses the date through the configured date format, which needs the
     * application context.
     */
    private Patient patient(String id, String nationalId, String lastName, String firstName, String birthDate,
            String gender) {
        Patient patient = new Patient() {
            @Override
            public String getBirthDateForDisplay() {
                return birthDate;
            }
        };
        patient.setId(id);
        patient.setNationalId(nationalId);
        patient.setGender(gender);
        Person person = new Person();
        person.setLastName(lastName);
        person.setFirstName(firstName);
        patient.setPerson(person);
        return patient;
    }

    private PatientManagementInfo added(String nationalId, String lastName, String firstName, String birthDate,
            String gender) {
        PatientManagementInfo sent = new PatientManagementInfo();
        sent.setPatientUpdateStatus(PatientUpdateStatus.ADD);
        sent.setNationalId(nationalId);
        sent.setLastName(lastName);
        sent.setFirstName(firstName);
        sent.setBirthDateForDisplay(birthDate);
        sent.setGender(gender);
        return sent;
    }
}
