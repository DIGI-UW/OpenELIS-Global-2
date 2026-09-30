package org.openelisglobal.common.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.OptimisticLockException;
import java.sql.Timestamp;
import org.hibernate.StaleObjectStateException;
import org.junit.Test;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * The version check shared by Modify Order, the Test Catalog editor and Patient
 * Management (OGC-1376).
 */
public class StaleSaveGuardTest {

    private static final Timestamp STORED = Timestamp.valueOf("2026-09-29 10:15:30.123");

    @Test
    public void theStoredVersionIsNotStaleInEitherTokenForm() {
        assertFalse(StaleSaveGuard.isStale(String.valueOf(STORED.getTime()), STORED));
        assertFalse(StaleSaveGuard.isStale(STORED.toString(), STORED));
    }

    @Test
    public void anOlderVersionIsStaleInEitherTokenForm() {
        Timestamp older = new Timestamp(STORED.getTime() - 60_000);
        assertTrue(StaleSaveGuard.isStale(String.valueOf(older.getTime()), STORED));
        assertTrue(StaleSaveGuard.isStale(older.toString(), STORED));
    }

    @Test
    public void aMissingTokenIsNotStaleSoOlderCallersKeepWorking() {
        assertFalse(StaleSaveGuard.isStale(null, STORED));
        assertFalse(StaleSaveGuard.isStale("  ", STORED));
        assertFalse(StaleSaveGuard.isStale("1", null));
    }

    @Test
    public void anUnreadableTokenIsStale() {
        assertTrue(StaleSaveGuard.isStale("yesterday", STORED));
    }

    @Test
    public void theTokenIsTheVersionInEpochMillis() {
        assertEquals(String.valueOf(STORED.getTime()), StaleSaveGuard.token(STORED));
        assertNull(StaleSaveGuard.token(null));
    }

    @Test
    public void optimisticLockFailuresAreRecognisedThroughWrappers() {
        assertTrue(StaleSaveGuard.isOptimisticLockFailure(new LIMSRuntimeException("save failed",
                new OptimisticLockException(new StaleObjectStateException("Person", "575")))));
        assertTrue(
                StaleSaveGuard.isOptimisticLockFailure(new ObjectOptimisticLockingFailureException("Person", "575")));
        assertFalse(StaleSaveGuard.isOptimisticLockFailure(new LIMSRuntimeException("duplicate national id")));
    }
}
