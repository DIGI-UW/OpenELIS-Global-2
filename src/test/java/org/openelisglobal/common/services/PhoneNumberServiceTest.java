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

    private static final String MADAGASCAR_FORMAT = "+261 (37|38) XX XXX XX | +(37|38) XX XXX XX | (37|38) XX XXX XX";
    private static final String MADAGASCAR_SLASH_FORMAT = "+261 37/38 XX XXX XX | +37/38 XX XXX XX | 37/38 XX XXX XX";
    private static final String E164_INTERNATIONAL_VALIDATION = "E164";
    private static final List<PhoneFormatCase> PHONE_FORMAT_CASES = Arrays
            .asList(new PhoneFormatCase("Madagascar 37/38", MADAGASCAR_FORMAT, E164_INTERNATIONAL_VALIDATION,
                    Arrays.asList("+261 37 12 345 67", "+261 38 12 345 67", "+261371234567", "+261381234567",
                            "+261-37-12-345-67", "+261-38-99-888-77", "37 12 345 67", "381234567", "+37 12 345 67",
                            "+38-99-888-77", "37-12-345-67", "+33 6 12 34 56 78", "+12025550123", "+44-20-7183-8750"),
                    Arrays.asList("+261 33 45 676 98", "+261-39-12-345-67", "33 12 345 67", "391234567", "+0123456789",
                            "+1234567", "+1234567890123456", "12025550123", "+1 (202) 555-0123")));

    @Mock
    private AutowireCapableBeanFactory beanFactory;

    @Mock
    private DefaultConfigurationProperties configurationProperties;

    private AutowireCapableBeanFactory previousFactory;
    private PhoneNumberService service;
    private String currentPhoneFormat;
    private String currentInternationalPhoneValidation;

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
        when(configurationProperties.getPropertyValue(Property.PHONE_INTERNATIONAL_VALIDATION))
                .thenAnswer(invocation -> currentInternationalPhoneValidation);

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
            currentInternationalPhoneValidation = testCase.internationalValidation;

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
    public void validatePhoneNumber_rejectsInternationalFallbackWhenNotConfigured() {
        currentPhoneFormat = MADAGASCAR_FORMAT;
        currentInternationalPhoneValidation = "";

        assertFalse("International fallback should require explicit configuration",
                service.validatePhoneNumber("+33 6 12 34 56 78"));
    }

    @Test
    public void validatePhoneNumber_acceptsSlashDelimitedConfiguredPrefixes() {
        currentPhoneFormat = MADAGASCAR_SLASH_FORMAT;
        currentInternationalPhoneValidation = E164_INTERNATIONAL_VALIDATION;

        assertTrue("Slash-delimited prefixes should accept 37", service.validatePhoneNumber("+261 37 12 345 67"));
        assertTrue("Slash-delimited prefixes should accept 38", service.validatePhoneNumber("+261381234567"));
        assertFalse("Slash-delimited prefixes should reject unconfigured prefixes",
                service.validatePhoneNumber("+261 33 45 676 98"));
    }

    @Test
    public void validatePhoneFormat_acceptsConfiguredCountryPhoneFormatTemplates() {
        for (PhoneFormatCase testCase : PHONE_FORMAT_CASES) {
            assertTrue(testCase.name + " template should be accepted",
                    PhoneNumberService.validatePhoneFormat(testCase.format));
            assertTrue(testCase.name + " international validation setting should be accepted",
                    PhoneNumberService.validateInternationalPhoneValidation(testCase.internationalValidation));
        }
        assertTrue("Slash-delimited Madagascar template should be accepted",
                PhoneNumberService.validatePhoneFormat(MADAGASCAR_SLASH_FORMAT));
    }

    private static final class PhoneFormatCase {
        private final String name;
        private final String format;
        private final String internationalValidation;
        private final List<String> validNumbers;
        private final List<String> invalidNumbers;

        private PhoneFormatCase(String name, String format, String internationalValidation, List<String> validNumbers,
                List<String> invalidNumbers) {
            this.name = name;
            this.format = format;
            this.internationalValidation = internationalValidation;
            this.validNumbers = validNumbers;
            this.invalidNumbers = invalidNumbers;
        }
    }
}
