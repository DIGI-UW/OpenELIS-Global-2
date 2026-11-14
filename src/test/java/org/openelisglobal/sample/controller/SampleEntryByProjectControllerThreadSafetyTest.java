package org.openelisglobal.sample.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.Test;
import org.openelisglobal.sample.form.SampleEntryByProjectForm;

public class SampleEntryByProjectControllerThreadSafetyTest {

    private static final Set<String> FORBIDDEN_STATE_FIELDS = Set.of(
            "task",
            "requesterPerson",
            "collector",
            "referringOrganization",
            "location",
            "serviceRequest",
            "specimen",
            "fhirPatient",
            "encounter"
    );

    @Test
    public void testSampleEntryByProjectControllerHasNoPerRequestStateFields() {
        List<Field> stateFields = Arrays.stream(SampleEntryByProjectController.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .filter(f -> FORBIDDEN_STATE_FIELDS.contains(f.getName()))
                .collect(Collectors.toList());

        assertTrue("SampleEntryByProjectController must not have per-request mutable FHIR fields: " + stateFields,
                stateFields.isEmpty());
    }

    @Test
    public void testSampleTbEntryControllerHasNoPerRequestStateFields() {
        List<Field> stateFields = Arrays.stream(SampleTbEntryController.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .filter(f -> FORBIDDEN_STATE_FIELDS.contains(f.getName()))
                .collect(Collectors.toList());

        assertTrue("SampleTbEntryController must not have per-request mutable FHIR fields: " + stateFields,
                stateFields.isEmpty());
    }

    @Test
    public void testLoadDataInFormWithNullOptionalResources() {
        SampleEntryByProjectController controller = new SampleEntryByProjectController();
        SampleEntryByProjectForm form = new SampleEntryByProjectForm();
        Patient fhirPatient = new Patient();

        try {
            controller.loadDataInForm(form, fhirPatient, null, null, null);
        } catch (Exception e) {
            org.junit.Assert.fail("loadDataInForm must not throw exception when task, encounter, or requesterPerson are null: " + e.getMessage());
        }

        assertNotNull(form.getObservations());
        assertNotNull(form.getProjectData());
        assertTrue(form.getProjectData().getViralLoadTest());
    }

    @Test
    public void testLoadDataInFormWithRequesterPerson() {
        SampleEntryByProjectController controller = new SampleEntryByProjectController();
        SampleEntryByProjectForm form = new SampleEntryByProjectForm();
        Patient fhirPatient = new Patient();
        Practitioner requester = new Practitioner();
        HumanName name = requester.addName();
        name.setFamily("Doe");
        name.addGiven("Jane");

        try {
            controller.loadDataInForm(form, fhirPatient, null, null, requester);
        } catch (Exception e) {
            org.junit.Assert.fail("loadDataInForm must not throw exception: " + e.getMessage());
        }

        assertNotNull(form.getObservations());
        assertEquals("Doe Jane", form.getObservations().getNameOfDoctor());
    }
}
