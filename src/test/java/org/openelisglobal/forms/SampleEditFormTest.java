package org.openelisglobal.forms;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.validator.SampleEditFormValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

public class SampleEditFormTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleEditFormValidator validator;

    private SampleEditForm form;
    private Errors errors;

    @Before
    public void setUp() throws Exception {

        form = new SampleEditForm();
        errors = new BeanPropertyBindingResult(form, "form");

        AccessionNumberValidatorFactory mockFactory = Mockito.mock(AccessionNumberValidatorFactory.class);
        IAccessionNumberValidator mockValidator = Mockito.mock(IAccessionNumberValidator.class);
        IAccessionNumberGenerator mockGenerator = Mockito.mock(IAccessionNumberGenerator.class);

        Mockito.when(mockValidator.validFormat(Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(ValidationResults.SUCCESS);

        Mockito.when(mockValidator.getMaxAccessionLength()).thenReturn(10);
        Mockito.when(mockValidator.getMinAccessionLength()).thenReturn(5);

        Mockito.when(mockGenerator.getChangeableLength()).thenReturn(5);
        Mockito.when(mockGenerator.getInvarientLength()).thenReturn(5);

        Mockito.when(mockFactory.getValidator(Mockito.any())).thenReturn(mockValidator);
        Mockito.when(mockFactory.getGenerator(Mockito.any())).thenReturn(mockGenerator);

        // inject static field
        Field field = AccessionNumberUtil.class.getDeclaredField("accessionNumberValidatorFactory");
        field.setAccessible(true);
        field.set(null, mockFactory);
    }

    @Test
    public void shouldPassValidation_whenValidDataProvided() {

        form.setNoSampleFound(false);
        form.setIsConfirmationSample(false);
        form.setIsEditable(true);

        form.setPatientName("John Doe");
        form.setGender("M");
        form.setNationalId("12345678");

        // REQUIRED FORM FIELDS FOR VALIDATION
        form.setMaxAccessionNumber("ABC-123");

        form.setEditableAccession(5);
        form.setNonEditableAccession(5);
        form.setMaxAccessionLength(10);

        String sampleXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><samples>"
                + "<sample sampleID='2' date='01/01/2026' time='00:00' collector='test' quantity='1' uom='1' "
                + "tests='1' testSectionMap='' testSampleTypeMap='' panels='' rejected='false' rejectReasonId='' "
                + "initialConditionIds='' numOrderLabels='4' numSpecimenLabels='3'/>"
                + "<sample sampleID='3' date='01/01/2026' time='00:00' collector='test' quantity='1' uom='1' "
                + "tests='2' testSectionMap='' testSampleTypeMap='' panels='' rejected='false' rejectReasonId='' "
                + "initialConditionIds='' numOrderLabels='4' numSpecimenLabels='5'/>" + "</samples>";

        form.setSampleXML(sampleXml);

        validator.validate(form, errors);

        if (errors.hasErrors()) {
            fail(formatErrors(errors));
        }

        assertFalse(errors.hasErrors());
    }

    private String formatErrors(Errors errors) {
        if (!errors.hasErrors()) {
            return "";
        }

        // Log full details with stacktrace
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Validation failed with ").append(errors.getErrorCount()).append(" error(s)\n");

        for (Object error : errors.getAllErrors()) {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                logMessage.append("  - Field: '").append(fieldError.getField()).append("'")
                        .append(", Rejected value: '").append(fieldError.getRejectedValue()).append("'")
                        .append(", Message: ").append(fieldError.getDefaultMessage()).append("\n");
            } else {
                logMessage.append("  - ").append(error.toString()).append("\n");
            }
        }

        // Log with stacktrace
        LogEvent.logError(this.getClass().getSimpleName(), "formatErrors", logMessage.toString());

        // Return formatted message for exception
        return errors.getAllErrors().stream().map(e -> {
            if (e instanceof FieldError) {
                FieldError fe = (FieldError) e;
                return fe.getField() + ": " + fe.getDefaultMessage();
            }
            return e.getDefaultMessage();
        }).filter(Objects::nonNull).collect(Collectors.joining("; "));
    }

}