package org.openelisglobal.dataexchange.fhir;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dataexchange.fhir.service.TaskInterpreter;
import org.openelisglobal.dataexchange.order.action.IOrderInterpreter.InterpreterResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * OGC-356 (receive side): verifies that TaskInterpreterImpl handles null
 * Patient (env/vector samples) without throwing NPE.
 */
public class TaskInterpreterImplNullPatientTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private org.openelisglobal.dataexchange.fhir.FhirConfig fhirConfig;

    private Task task;
    private ServiceRequest serviceRequest;

    @Before
    public void setUp() throws Exception {
        // Set up mocked FhirContext to use a real JSON parser
        IParser jsonParser = FhirContext.forR4().newJsonParser();
        when(fhirContext.newJsonParser()).thenReturn(jsonParser);
        when(fhirConfig.getOeFhirSystem()).thenReturn("http://openelis-global.org");

        task = new Task();
        task.setId(UUID.randomUUID().toString());
        task.setStatus(Task.TaskStatus.REQUESTED);
        task.setAuthoredOn(new Date());

        serviceRequest = new ServiceRequest();
        serviceRequest.setId(UUID.randomUUID().toString());
        serviceRequest.addIdentifier(new Identifier().setValue("ENV-REF-001"));
        // Use a LOINC code that likely doesn't exist in test DB — that's fine,
        // the test targets the patient null-guard, not test/panel lookup
        serviceRequest.setCode(
                new CodeableConcept().addCoding(new Coding("http://loinc.org", "00000-0", "Nonexistent test")));
    }

    @Test
    public void interpret_shouldNotThrowNPE_whenPatientIsNull() {
        TaskInterpreter interpreter = applicationContext.getBean(TaskInterpreter.class);

        // This must not throw NPE — prior to the fix, createPatientFromFHIR()
        // would dereference null patient at patient.getIdElement().getIdPart()
        List<InterpreterResults> results = interpreter.interpret(task, serviceRequest, null);

        assertNull("messagePatient should be null for env/vector sample", interpreter.getMessagePatient());
        assertFalse("should not contain MISSING_PATIENT_GENDER",
                results.contains(InterpreterResults.MISSING_PATIENT_GENDER));
        assertFalse("should not contain MISSING_PATIENT_DOB", results.contains(InterpreterResults.MISSING_PATIENT_DOB));
        assertFalse("should not contain MISSING_PATIENT_IDENTIFIER",
                results.contains(InterpreterResults.MISSING_PATIENT_IDENTIFIER));
    }

    @Test
    public void interpret_shouldReturnPatientValidationErrors_whenPatientHasMissingFields() {
        TaskInterpreter interpreter = applicationContext.getBean(TaskInterpreter.class);

        // Patient with no gender and no identifiers — should trigger validation
        Patient incompletePatient = new Patient();
        incompletePatient.setId(UUID.randomUUID().toString());

        List<InterpreterResults> results = interpreter.interpret(task, serviceRequest, incompletePatient);

        // Regression: non-null Patient with missing data should still produce
        // patient validation errors (confirming the guard only skips for null)
        assertTrue("should contain MISSING_PATIENT_GENDER for incomplete patient",
                results.contains(InterpreterResults.MISSING_PATIENT_GENDER));
        assertTrue("should contain MISSING_PATIENT_IDENTIFIER for incomplete patient",
                results.contains(InterpreterResults.MISSING_PATIENT_IDENTIFIER));
    }

    @Test
    public void interpret_shouldReturnNonPatientResults_whenPatientIsNull() {
        TaskInterpreter interpreter = applicationContext.getBean(TaskInterpreter.class);

        List<InterpreterResults> results = interpreter.interpret(task, serviceRequest, null);

        // UNSUPPORTED_TESTS is expected (the LOINC code doesn't exist in test DB),
        // but no MISSING_PATIENT_* should appear — that's the OGC-356 fix
        boolean hasPatientError = results.stream().anyMatch(r -> r == InterpreterResults.MISSING_PATIENT_GENDER
                || r == InterpreterResults.MISSING_PATIENT_DOB || r == InterpreterResults.MISSING_PATIENT_IDENTIFIER
                || r == InterpreterResults.MISSING_PATIENT_GUID);
        assertFalse("null patient should not produce patient validation errors", hasPatientError);
    }
}
