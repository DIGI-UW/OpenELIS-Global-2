package org.openelisglobal.sample.validator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;

/**
 * maxAccessionNumber is "&lt;accession&gt;-&lt;highest sample item number&gt;".
 * With a dashed accession format such as YEARNUM_DASH_SEVEN (26-0000001) the
 * accession itself contains a dash, so the value must be split at the last
 * dash, not the first.
 */
public class SampleEditFormValidatorDashedAccessionTest {

    private final SampleEditFormValidator validator = new SampleEditFormValidator();
    private Object previousContext;
    private Object previousFactory;

    @Before
    public void setUp() throws Exception {
        previousContext = ReflectionTestUtils.getField(SpringContext.class, "context");
        previousFactory = ReflectionTestUtils.getField(SpringContext.class, "factory");
        IAccessionNumberValidator format = mock(IAccessionNumberValidator.class);
        when(format.validFormat(anyString(), anyBoolean())).thenReturn(ValidationResults.FORMAT_FAIL);
        when(format.validFormat("26-0000001", false)).thenReturn(ValidationResults.SUCCESS);
        when(format.validFormat("DEV01260000000001", false)).thenReturn(ValidationResults.SUCCESS);

        AccessionNumberValidatorFactory factory = mock(AccessionNumberValidatorFactory.class);
        when(factory.getValidator(AccessionFormat.GENERAL)).thenReturn(format);
        when(factory.getGenerator(AccessionFormat.MAIN)).thenReturn(mock(IAccessionNumberGenerator.class));

        // Every other bean the validator looks up through SpringContext is a plain
        // mock.
        AutowireCapableBeanFactory beans = mock(AutowireCapableBeanFactory.class, invocation -> {
            Object type = invocation.getArguments().length == 1 ? invocation.getArgument(0) : null;
            if (!"getBean".equals(invocation.getMethod().getName()) || !(type instanceof Class)) {
                return null;
            }
            return type == AccessionNumberValidatorFactory.class ? factory : mock((Class<?>) type);
        });
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getAutowireCapableBeanFactory()).thenReturn(beans);
        new SpringContext().setApplicationContext(context);
        ReflectionTestUtils.setField(validator, "orderPartiesValidator", mock(SampleOrderPartiesValidator.class));
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "context", previousContext);
        ReflectionTestUtils.setField(SpringContext.class, "factory", previousFactory);
    }

    private BeanPropertyBindingResult validate(String maxAccessionNumber) {
        SampleEditForm form = new SampleEditForm();
        form.setMaxAccessionNumber(maxAccessionNumber);
        form.setSampleXML("<samples></samples>");
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
        validator.validate(form, errors);
        return errors;
    }

    @Test
    public void acceptsAnAccessionThatContainsADash() {
        assertNull(validate("26-0000001-2").getFieldError("maxAccessionNumber"));
    }

    @Test
    public void stillAcceptsAnAccessionWithoutADash() {
        assertNull(validate("DEV01260000000001-2").getFieldError("maxAccessionNumber"));
    }

    @Test
    public void rejectsAMalformedValue() {
        assertNotNull(validate("26-0000001-x").getFieldError("maxAccessionNumber"));
        assertNotNull(validate("26-0000001").getFieldError("maxAccessionNumber"));
        assertNotNull(validate("0000001").getFieldError("maxAccessionNumber"));
    }
}
