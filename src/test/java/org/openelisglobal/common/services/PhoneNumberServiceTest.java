package org.openelisglobal.common.services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
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

    @Mock
    private AutowireCapableBeanFactory beanFactory;

    @Mock
    private DefaultConfigurationProperties configurationProperties;

    private AutowireCapableBeanFactory previousFactory;
    private PhoneNumberService service;

    @Before
    public void setUp() throws Exception {
        Field factoryField = SpringContext.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        previousFactory = (AutowireCapableBeanFactory) factoryField.get(null);
        factoryField.set(null, beanFactory);

        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
        when(configurationProperties.getPropertyValue(Property.VALIDATE_PHONE_FORMAT)).thenReturn("true");
        when(configurationProperties.getPropertyValue(Property.PHONE_FORMAT)).thenReturn(MADAGASCAR_FORMAT);

        service = new PhoneNumberService();
    }

    @After
    public void tearDown() throws Exception {
        Field factoryField = SpringContext.class.getDeclaredField("factory");
        factoryField.setAccessible(true);
        factoryField.set(null, previousFactory);
    }

    @Test
    public void validatePhoneNumber_acceptsMadagascar37And38WithFlexibleSeparators() {
        assertTrue(service.validatePhoneNumber("+261 37 12 345 67"));
        assertTrue(service.validatePhoneNumber("+261371234567"));
        assertTrue(service.validatePhoneNumber("+261-38-99-888-77"));
    }

    @Test
    public void validatePhoneNumber_rejectsOtherMadagascarPrefixes() {
        assertFalse(service.validatePhoneNumber("+261 33 45 676 98"));
        assertFalse(service.validatePhoneNumber("+261-39-12-345-67"));
    }

    @Test
    public void validatePhoneFormat_acceptsAlternateTemplateSeparator() {
        assertTrue(PhoneNumberService.validatePhoneFormat(MADAGASCAR_FORMAT));
    }
}
