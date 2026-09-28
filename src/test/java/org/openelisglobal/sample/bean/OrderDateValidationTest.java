package org.openelisglobal.sample.bean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.sample.form.SamplePatientEntryForm;

/**
 * FR-K9 and OGC-1135: the order's request date accepts today in either the
 * display format or ISO-8601, and a date in the future is refused with a plain
 * message instead of "Invalid date format".
 */
public class OrderDateValidationTest extends BaseWebContextSensitiveTest {

    private ValidatorFactory factory;
    private Validator validator;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        factory = Validation.byDefaultProvider().configure().messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();
        validator = factory.getValidator();
    }

    @After
    public void closeValidator() {
        factory.close();
    }

    @Test
    public void todayInTheDisplayFormatIsAccepted() {
        assertTrue(violations(DateUtil.getCurrentDateAsText()).isEmpty());
    }

    @Test
    public void todayInIsoIsAccepted() {
        assertTrue(violations(LocalDate.now().toString()).isEmpty());
    }

    @Test
    public void aFutureDateIsRefusedAsInTheFuture() {
        Set<ConstraintViolation<SampleOrderItem>> refused = violations(LocalDate.now().plusDays(2).toString());

        assertEquals(1, refused.size());
        assertEquals("Date may not be in the future", refused.iterator().next().getMessage());
    }

    @Test
    public void anUnreadableDateIsStillAFormatError() {
        Set<ConstraintViolation<SampleOrderItem>> refused = violations("26-Sept-2026");

        assertEquals(1, refused.size());
        assertEquals("Invalid date format", refused.iterator().next().getMessage());
    }

    private Set<ConstraintViolation<SampleOrderItem>> violations(String requestDate) {
        SampleOrderItem order = new SampleOrderItem();
        order.setRequestDate(requestDate);
        return validator.validateProperty(order, "requestDate", SamplePatientEntryForm.SamplePatientEntry.class);
    }
}
