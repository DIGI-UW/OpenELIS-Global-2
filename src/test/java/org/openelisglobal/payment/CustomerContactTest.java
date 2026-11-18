package org.openelisglobal.payment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openelisglobal.payment.gateway.CustomerContact;

public class CustomerContactTest {

    @Test
    public void phoneLosesSpacesDashesDotsAndBrackets() {
        assertEquals("+919876543210", CustomerContact.of("+91 98765-43210", null).phone());
        assertEquals("02212345678", CustomerContact.of("(022) 1234.5678", null).phone());
    }

    @Test
    public void phoneWithLettersOrWrongLengthIsDropped() {
        assertNull(CustomerContact.of("call me", null).phone());
        assertNull(CustomerContact.of("1234567", null).phone());
        assertNull(CustomerContact.of("1234567890123456", null).phone());
        assertNull(CustomerContact.of("98765x43210", null).phone());
    }

    @Test
    public void emailIsTrimmedAndMustLookLikeAnAddress() {
        assertEquals("pat@example.org", CustomerContact.of(null, "  pat@example.org ").email());
        assertNull(CustomerContact.of(null, "pat.example.org").email());
        assertNull(CustomerContact.of(null, "pat@localhost").email());
        assertNull(CustomerContact.of(null, "pat @example.org").email());
    }

    @Test
    public void blankInputsGiveAnEmptyContact() {
        assertTrue(CustomerContact.of("", "  ").isEmpty());
        assertTrue(CustomerContact.of(null, null).isEmpty());
        assertTrue(CustomerContact.NONE.isEmpty());
    }

    @Test
    public void eitherValidValueMakesTheContactUsable() {
        assertFalse(CustomerContact.of("9876543210", null).isEmpty());
        assertFalse(CustomerContact.of(null, "pat@example.org").isEmpty());
    }
}
