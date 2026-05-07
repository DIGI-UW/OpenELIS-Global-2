package org.openelisglobal.common.services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PhoneNumberServiceTest {

    private static final String MADAGASCAR_FORMAT = "+261 37 XX XXX XX | +261 38 XX XXX XX";
    private static final List<PhoneFormatCase> PHONE_FORMAT_CASES = Arrays
            .asList(new PhoneFormatCase("Madagascar 37/38", MADAGASCAR_FORMAT,
                    Arrays.asList("+261 37 12 345 67", "+261 38 12 345 67", "+261371234567", "+261381234567",
                            "+261-37-12-345-67", "+261-38-99-888-77"),
                    Arrays.asList("+261 33 45 676 98", "+261-39-12-345-67")));

    @Mock
    private AutowireCapableBeanFactory beanFactory;

    @Mock
    private DefaultConfigurationProperties configurationProperties;

    private AutowireCapableBeanFactory previousFactory;
    private PhoneNumberService service;
    private String currentPhoneFormat;

    @Before
    public void setUp() throws Exception {
        Field factoryField = SpringContext.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        previousFactory = (AutowireCapableBeanFactory) factoryField.get(null);
        factoryField.set(null, beanFactory);

        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
        when(configurationProperties.getPropertyValue(Property.VALIDATE_PHONE_FORMAT)).thenReturn("true");
        when(configurationProperties.getPropertyValue(Property.PHONE_FORMAT))
                .thenAnswer(invocation -> currentPhoneFormat);

        service = new PhoneNumberService();
    }

    @After
    public void tearDown() throws Exception {
        Field factoryField = SpringContext.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        factoryField.set(null, previousFactory);
    }

    @Test
    public void validatePhoneNumber_usesConfiguredCountryPhoneFormatCases() {
        for (PhoneFormatCase testCase : PHONE_FORMAT_CASES) {
            currentPhoneFormat = testCase.format;

            for (String validNumber : testCase.validNumbers) {
                assertTrue(testCase.name + " should accept " + validNumber, service.validatePhoneNumber(validNumber));
            }

            for (String invalidNumber : testCase.invalidNumbers) {
                assertFalse(testCase.name + " should reject " + invalidNumber,
                        service.validatePhoneNumber(invalidNumber));
            }
        }
    }

    @Test
    public void validatePhoneFormat_acceptsConfiguredCountryPhoneFormatTemplates() {
        for (PhoneFormatCase testCase : PHONE_FORMAT_CASES) {
            assertTrue(testCase.name + " template should be accepted",
                    PhoneNumberService.validatePhoneFormat(testCase.format));
        }
    }

    private static final class PhoneFormatCase {
        private final String name;
        private final String format;
        private final List<String> validNumbers;
        private final List<String> invalidNumbers;

        private PhoneFormatCase(String name, String format, List<String> validNumbers, List<String> invalidNumbers) {
            this.name = name;
            this.format = format;
            this.validNumbers = validNumbers;
            this.invalidNumbers = invalidNumbers;
        }
    }
}
