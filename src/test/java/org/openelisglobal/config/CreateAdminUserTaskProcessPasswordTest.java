package org.openelisglobal.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Locks the line-ending handling contract for the bundled admin password hash.
 * On Windows checkouts with core.autocrlf=true, adminPassword.txt is checked
 * out with \r\n endings even though the git blob only has \n, which left a
 * trailing \r on the hash and broke isHashedPassword()'s match.
 */
public class CreateAdminUserTaskProcessPasswordTest {

    private final CreateAdminUserTask task = new CreateAdminUserTask();

    @Test
    public void processPassword_trailingLf_isStripped() {
        assertEquals("$2a$12$hash", task.processPassword("$2a$12$hash\n"));
    }

    @Test
    public void processPassword_trailingCrLf_isStripped() {
        assertEquals("$2a$12$hash", task.processPassword("$2a$12$hash\r\n"));
    }

    @Test
    public void processPassword_trailingCr_isStripped() {
        assertEquals("$2a$12$hash", task.processPassword("$2a$12$hash\r"));
    }

    @Test
    public void processPassword_noTrailingLineEnding_isUnchanged() {
        assertEquals("$2a$12$hash", task.processPassword("$2a$12$hash"));
    }

    @Test
    public void processPassword_adminMarker_isStripped() {
        assertEquals("$2a$12$hash", task.processPassword("admin:$2a$12$hash\r\n"));
    }

    @Test
    public void processPassword_legacy2yPrefix_isConvertedTo2a() {
        assertEquals("$2a$12$hash", task.processPassword("$2y$12$hash\r\n"));
    }
}
