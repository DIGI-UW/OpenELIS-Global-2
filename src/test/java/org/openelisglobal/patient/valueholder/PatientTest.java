package org.openelisglobal.patient.valueholder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;

public class PatientTest {

    private Patient patient;

    @BeforeClass
    public static void configureDateUtilDependencies() {
        DefaultConfigurationProperties configuration = mock(DefaultConfigurationProperties.class);
        when(configuration.getPropertyValue(Property.AmbiguousDateHolder)).thenReturn("x");
        when(configuration.getPropertyValue(Property.AmbiguousDateValue)).thenReturn("01");
        when(configuration.getPropertyValue(Property.DEFAULT_DATE_LOCALE)).thenReturn("en-US");
        when(configuration.getPropertyValue(Property.DEFAULT_LANG_LOCALE)).thenReturn("en-US");
        when(configuration.getPropertyValue(Property.StringContext)).thenReturn(null);

        AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configuration);

        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getAutowireCapableBeanFactory()).thenReturn(beanFactory);

        new SpringContext().setApplicationContext(applicationContext);

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("languages/message");
        messageSource.setDefaultEncoding("UTF-8");
        MessageUtil.setMessageSource(messageSource);
    }

    @Before
    public void setUp() {
        patient = new Patient();
    }

    @Test
    public void newPatient_ShouldExposeSafeDefaults() {
        assertNull("A new patient should not have an id", patient.getId());
        assertNull("A new patient should not have a person", patient.getPerson());
        assertFalse("A new patient must not be marked as merged", patient.getIsMerged());
        assertEquals("A missing FHIR UUID should render as an empty string", "", patient.getFhirUuidAsString());
    }

    @Test
    public void setBirthDate_ShouldUpdateStoredTimestampAndDisplayValue() {
        Timestamp birthDate = Timestamp.valueOf("2024-03-05 14:30:00");

        patient.setBirthDate(birthDate);

        assertEquals("Birth date should be stored exactly", birthDate, patient.getBirthDate());
        assertEquals("Birth date should be formatted for display", "03/05/2024", patient.getBirthDateForDisplay());
    }

    @Test
    public void setBirthDateForDisplay_ShouldParseDisplayDateIntoTimestamp() {
        patient.setBirthDateForDisplay("03/05/2024");

        assertEquals("Display value should be preserved", "03/05/2024", patient.getBirthDateForDisplay());
        assertEquals("Parsed timestamp should keep the same calendar day", LocalDate.of(2024, 3, 5),
                patient.getBirthDate().toLocalDateTime().toLocalDate());
    }

    @Test
    public void setBirthDateForDisplay_WithAmbiguousDate_ShouldNormalizeMissingMonthAndDay() {
        patient.setBirthDateForDisplay("xx/xx/2024");

        assertEquals("Original ambiguous display value should be preserved", "xx/xx/2024",
                patient.getBirthDateForDisplay());
        assertEquals("Ambiguous month and day should normalize to configured placeholder values",
                LocalDate.of(2024, 1, 1),
                patient.getBirthDate().toLocalDateTime().toLocalDate());
    }

    @Test
    public void setBirthTime_ShouldUpdateStoredDateAndDisplayValue() {
        Date birthTime = Date.valueOf("2024-03-05");

        patient.setBirthTime(birthTime);

        assertEquals("Birth time should be stored exactly", birthTime, patient.getBirthTime());
        assertEquals("Birth time should be formatted for display", "03/05/2024", patient.getBirthTimeForDisplay());
    }

    @Test
    public void setBirthTimeForDisplay_ShouldParseDisplayDateIntoSqlDate() {
        patient.setBirthTimeForDisplay("03/05/2024");

        assertEquals("Display value should be preserved", "03/05/2024", patient.getBirthTimeForDisplay());
        assertEquals("Parsed SQL date should match the display date", Date.valueOf("2024-03-05"), patient.getBirthTime());
    }

    @Test
    public void setDeathDate_ShouldUpdateStoredDateAndDisplayValue() {
        Date deathDate = Date.valueOf("2024-04-06");

        patient.setDeathDate(deathDate);

        assertEquals("Death date should be stored exactly", deathDate, patient.getDeathDate());
        assertEquals("Death date should be formatted for display", "04/06/2024", patient.getDeathDateForDisplay());
    }

    @Test
    public void setDeathDateForDisplay_ShouldParseDisplayDateIntoSqlDate() {
        patient.setDeathDateForDisplay("04/06/2024");

        assertEquals("Display value should be preserved", "04/06/2024", patient.getDeathDateForDisplay());
        assertEquals("Parsed SQL date should match the display date", Date.valueOf("2024-04-06"), patient.getDeathDate());
    }

    @Test
    public void setFhirUuid_ShouldExposeCanonicalUuidString() {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        patient.setFhirUuid(uuid);

        assertSame("FHIR UUID getter should return the same UUID instance", uuid, patient.getFhirUuid());
        assertEquals("FHIR UUID string should use canonical format", "550e8400-e29b-41d4-a716-446655440000",
                patient.getFhirUuidAsString());
    }

    @Test
    public void setPerson_ShouldReplaceCurrentPersonAssociation() {
        Person first = new Person();
        Person second = new Person();
        second.setId("person-2");

        patient.setPerson(first);
        patient.setPerson(second);

        assertSame("Patient should expose the most recently assigned person", second, patient.getPerson());
        assertEquals("Replacement person details should remain accessible", "person-2", patient.getPerson().getId());
    }

    @Test
    public void mergeFields_ShouldRetainMergeStateMetadata() {
        Timestamp mergeDate = Timestamp.valueOf("2024-05-01 09:15:00");

        patient.setIsMerged(true);
        patient.setMergedIntoPatientId("99");
        patient.setMergeDate(mergeDate);

        assertTrue("Merged patients should report merged state", patient.getIsMerged());
        assertEquals("Merged target patient id should be retained", "99", patient.getMergedIntoPatientId());
        assertEquals("Merge date should be retained", mergeDate, patient.getMergeDate());
    }
}
