package org.openelisglobal.patient.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import org.junit.Test;
import org.openelisglobal.person.valueholder.Person;

/**
 * Unit tests for the GPS String→BigDecimal converter used by both
 * PatientUtil.copyFormBeanToValueHolders and
 * PatientManagementUpdate.copyFormBeanToValueHolders.
 *
 * Locks: parse handles null/empty/whitespace/invalid input cleanly; the Person
 * setter applies parsed BigDecimals; round-trip survives operator typos without
 * throwing (validation surface is upstream).
 */
public class PatientGpsCoordinatesTest {

    @Test
    public void parse_null_returnsNull() {
        assertNull(PatientGpsCoordinates.parse(null));
    }

    @Test
    public void parse_empty_returnsNull() {
        assertNull(PatientGpsCoordinates.parse(""));
    }

    @Test
    public void parse_whitespace_returnsNull() {
        assertNull(PatientGpsCoordinates.parse("   "));
    }

    @Test
    public void parse_invalidString_returnsNull() {
        assertNull(PatientGpsCoordinates.parse("not-a-number"));
    }

    @Test
    public void parse_validNegative_returnsBigDecimal() {
        BigDecimal result = PatientGpsCoordinates.parse("-18.879190");
        assertNotNull(result);
        assertEquals(new BigDecimal("-18.879190"), result);
    }

    @Test
    public void parse_validPositive_returnsBigDecimal() {
        BigDecimal result = PatientGpsCoordinates.parse("47.507905");
        assertNotNull(result);
        assertEquals(new BigDecimal("47.507905"), result);
    }

    @Test
    public void parse_trimsLeadingAndTrailingWhitespace() {
        BigDecimal result = PatientGpsCoordinates.parse("  47.507905  ");
        assertNotNull(result);
        assertEquals(new BigDecimal("47.507905"), result);
    }

    @Test
    public void applyToPerson_withValidValues_setsBigDecimalsOnPerson() {
        Person person = new Person();
        PatientGpsCoordinates.applyToPerson("-18.879190", "47.507905", person);
        assertEquals(new BigDecimal("-18.879190"), person.getGpsLatitude());
        assertEquals(new BigDecimal("47.507905"), person.getGpsLongitude());
    }

    @Test
    public void applyToPerson_withNullValues_setsNullOnPerson() {
        Person person = new Person();
        PatientGpsCoordinates.applyToPerson(null, null, person);
        assertNull(person.getGpsLatitude());
        assertNull(person.getGpsLongitude());
    }

    @Test
    public void applyToPerson_withEmptyStrings_setsNullOnPerson() {
        Person person = new Person();
        PatientGpsCoordinates.applyToPerson("", "", person);
        assertNull(person.getGpsLatitude());
        assertNull(person.getGpsLongitude());
    }

    @Test
    public void applyToPerson_withInvalidStrings_setsNullOnPerson() {
        Person person = new Person();
        PatientGpsCoordinates.applyToPerson("garbage", "also-garbage", person);
        assertNull(person.getGpsLatitude());
        assertNull(person.getGpsLongitude());
    }

    @Test
    public void applyToPerson_withMixedValidAndInvalid_setsValidOnly() {
        Person person = new Person();
        PatientGpsCoordinates.applyToPerson("-18.879190", "garbage", person);
        assertEquals(new BigDecimal("-18.879190"), person.getGpsLatitude());
        assertNull(person.getGpsLongitude());
    }
}
