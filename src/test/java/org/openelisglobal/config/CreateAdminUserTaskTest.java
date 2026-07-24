package org.openelisglobal.config;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class CreateAdminUserTaskTest {

    private CreateAdminUserTask task;

    @Before
    public void setUp() {
        task = new CreateAdminUserTask();
    }

    @Test
    public void processPassword_withAdminPrefixAndCRLF_stripsMarkerAndConvertsPrefix() {
        String input = "admin:$2y$12$G.ie.iZuxFu7DKVtIkvpyu/jg9dulZIsgfu6EfY5E05OWJnvOhC6W\r\n";
        String expected = "$2a$12$G.ie.iZuxFu7DKVtIkvpyu/jg9dulZIsgfu6EfY5E05OWJnvOhC6W";
        String actual = task.processPassword(input);
        assertEquals(expected, actual);
    }

    @Test
    public void processPassword_withAdminPrefixAndLF_stripsMarkerAndConvertsPrefix() {
        String input = "admin:$2a$12$G.ie.iZuxFu7DKVtIkvpyu/jg9dulZIsgfu6EfY5E05OWJnvOhC6W\n";
        String expected = "$2a$12$G.ie.iZuxFu7DKVtIkvpyu/jg9dulZIsgfu6EfY5E05OWJnvOhC6W";
        String actual = task.processPassword(input);
        assertEquals(expected, actual);
    }

    @Test
    public void processPassword_withoutAdminPrefix_convertsBcryptPrefixAndTrims() {
        String input = "$2y$12$G.ie.iZuxFu7DKVtIkvpyu/jg9dulZIsgfu6EfY5E05OWJnvOhC6W\r\n";
        String expected = "$2a$12$G.ie.iZuxFu7DKVtIkvpyu/jg9dulZIsgfu6EfY5E05OWJnvOhC6W";
        String actual = task.processPassword(input);
        assertEquals(expected, actual);
    }

    @Test
    public void processPassword_nullAndEmptyInput_returnsEmptyString() {
        assertEquals("", task.processPassword(null));
        assertEquals("", task.processPassword("   "));
    }
}
