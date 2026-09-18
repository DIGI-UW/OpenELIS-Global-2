package org.openelisglobal.common.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

public class SystemInitFlagTest {

    @After
    public void tearDown() {
        SystemInitFlag.clear();
    }

    @Test
    public void isSet_returnsFalse_byDefault() {
        assertFalse(SystemInitFlag.isSet());
    }

    @Test
    public void isSet_returnsTrue_afterSet() {
        SystemInitFlag.set();
        assertTrue(SystemInitFlag.isSet());
    }

    @Test
    public void isSet_returnsFalse_afterClear() {
        SystemInitFlag.set();
        SystemInitFlag.clear();
        assertFalse(SystemInitFlag.isSet());
    }

    @Test
    public void flag_isThreadLocal() throws InterruptedException {
        SystemInitFlag.set();
        assertTrue(SystemInitFlag.isSet());

        boolean[] otherThreadSaw = { true };
        Thread t = new Thread(() -> otherThreadSaw[0] = SystemInitFlag.isSet());
        t.start();
        t.join();

        assertFalse("Flag set on main thread must not be visible on another thread", otherThreadSaw[0]);
        assertTrue("Flag must still be set on original thread", SystemInitFlag.isSet());
    }
}
