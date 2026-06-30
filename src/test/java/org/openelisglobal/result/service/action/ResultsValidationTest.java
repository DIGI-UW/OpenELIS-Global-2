package org.openelisglobal.result.service.action;

import org.junit.Before;
import org.junit.Test;

import org.openelisglobal.BaseWebContextSensitiveTest;

import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.result.action.util.ResultsValidation;

import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.validation.Errors;

import static org.junit.Assert.*;


public class ResultsValidationTest extends BaseWebContextSensitiveTest {



    @Autowired
    private ResultsValidation resultsValidation;

    @Autowired
    private SpringContext springContext;

    private TestResultItem item;

    @Before
    public void setup() {
        assertNotNull("SpringContext should be initialized for ConfigurationProperties.getInstance()", springContext);

        // Enable the rejection-validation branch for this test.
        ConfigurationProperties.getInstance().setPropertyValue(Property.allowResultRejection, "true");

        item = new TestResultItem();
        item.setTestDate("01/01/2024");
    }

    @Test
    public void validateItem_rejectedWithNoReason_shouldHaveError() {
        assertNotNull("ResultsValidation should be injected by Spring", resultsValidation);

        item.setRejected(true);
        item.setRejectReasonId("0");
        Errors errors = resultsValidation.validateItem(item);
        assertNotNull(errors);
        assertTrue(errors.hasErrors());
    }

    @Test
    public void validateItem_invalidDate_shouldHaveError() {
        item.setTestDate("not-a-date");
        item.setRejected(true);
        item.setRejectReasonId("1");
        Errors errors = resultsValidation.validateItem(item);
        assertTrue(errors.hasErrors());
    }

    @Test
    public void validateItem_futureDate_shouldHaveError() {
        item.setTestDate("01/01/2099");
        //item.setRejected(true); working
        item.setRejected(false);
        item.setRejectReasonId("1");
        Errors errors = resultsValidation.validateItem(item);
        assertTrue(errors.hasErrors());
    }

    @Test
    public void validateItem_validRejection_shouldNotHaveRejectionError() {
        item.setRejected(true);
        item.setRejectReasonId("1");
        Errors errors = resultsValidation.validateItem(item);
        assertFalse(errors.hasErrors());
    }
}
