package org.openelisglobal.odoo;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.odoo.client.OdooClient;
import org.openelisglobal.odoo.client.RealOdooClient;

@RunWith(MockitoJUnitRunner.class)
public class RealOdooClientTest {

    @Mock
    private OdooClient odooClient;

    private RealOdooClient realOdooClient;

    // ─── startup success ───────────────────────────────────────────────────────

    @Test
    public void constructor_whenOdooReachable_isAvailableTrue() throws Exception {
        doNothing().when(odooClient).init();

        realOdooClient = new RealOdooClient(odooClient);

        assertTrue(realOdooClient.isAvailable());
    }

    @Test
    public void constructor_whenOdooUnreachable_isAvailableFalse() throws Exception {
        doThrow(new RuntimeException("connection refused")).when(odooClient).init();

        realOdooClient = new RealOdooClient(odooClient);

        // isAvailable() will retry init — stub second call to also fail
        doThrow(new RuntimeException("still down")).when(odooClient).init();

        assertFalse(realOdooClient.isAvailable());
    }

    // ─── runtime reconnection ─────────────────────────────────────────────────

    @Test
    public void isAvailable_whenPreviouslyUnavailable_attemptsReconnect() throws Exception {
        // Startup fails
        doThrow(new RuntimeException("down at startup")).when(odooClient).init();
        realOdooClient = new RealOdooClient(odooClient);

        // Now Odoo recovers — next init() call succeeds
        doNothing().when(odooClient).init();

        boolean result = realOdooClient.isAvailable();

        assertTrue(result);
        // init() called once at construction + once during isAvailable() reconnect
        verify(odooClient, times(2)).init();
    }

    @Test
    public void isAvailable_whenPreviouslyAvailable_doesNotReconnect() throws Exception {
        doNothing().when(odooClient).init();
        realOdooClient = new RealOdooClient(odooClient);

        realOdooClient.isAvailable();

        // init() only called once at construction — no extra call
        verify(odooClient, times(1)).init();
    }

    @Test
    public void isAvailable_whenReconnectFails_returnsFalse() throws Exception {
        // All init() calls fail
        doThrow(new RuntimeException("always down")).when(odooClient).init();
        realOdooClient = new RealOdooClient(odooClient);

        boolean result = realOdooClient.isAvailable();

        assertFalse(result);
    }

    // ─── create — marks unavailable on failure ────────────────────────────────

    @Test
    public void create_whenAvailableAndSucceeds_returnsId() throws Exception {
        doNothing().when(odooClient).init();
        realOdooClient = new RealOdooClient(odooClient);
        when(odooClient.create(anyString(), anyList())).thenReturn(42);

        Integer result = realOdooClient.create("account.move", List.of(Map.of("key", "val")));

        assertEquals(Integer.valueOf(42), result);
    }

    @Test(expected = IllegalStateException.class)
    public void create_whenUnavailableAndReconnectFails_throwsIllegalState() throws Exception {
        doThrow(new RuntimeException("down")).when(odooClient).init();
        realOdooClient = new RealOdooClient(odooClient);

        realOdooClient.create("account.move", List.of());
    }

    @Test
    public void create_whenCallFails_marksUnavailable() throws Exception {
        doNothing().when(odooClient).init();
        realOdooClient = new RealOdooClient(odooClient);
        when(odooClient.create(anyString(), anyList()))
                .thenThrow(new RuntimeException("Odoo crashed"));

        try {
            realOdooClient.create("account.move", List.of());
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            // expected
        }

        // Now stub init to fail so reconnect also fails
        doThrow(new RuntimeException("still down")).when(odooClient).init();

        // isAvailable should now return false
        assertFalse(realOdooClient.isAvailable());
    }

    @Test
    public void create_afterFailureAndRecovery_succeedsAgain() throws Exception {
        doNothing().when(odooClient).init();
        realOdooClient = new RealOdooClient(odooClient);

        // First call fails and marks unavailable
        when(odooClient.create(anyString(), anyList()))
                .thenThrow(new RuntimeException("temporary failure"))
                .thenReturn(99);

        try {
            realOdooClient.create("account.move", List.of());
        } catch (RuntimeException e) {
            // expected first failure
        }

        // Odoo recovers — init() succeeds again
        doNothing().when(odooClient).init();

        Integer result = realOdooClient.create("account.move", List.of());

        assertEquals(Integer.valueOf(99), result);
    }

    // ─── searchAndRead — marks unavailable on failure ─────────────────────────

    @Test
    public void searchAndRead_whenAvailableAndSucceeds_returnsResult() throws Exception {
        doNothing().when(odooClient).init();
        realOdooClient = new RealOdooClient(odooClient);
        Object[] expected = new Object[]{Map.of("id", 1)};
        when(odooClient.searchAndRead(anyString(), anyList(), anyList())).thenReturn(expected);

        Object[] result = realOdooClient.searchAndRead("res.partner", List.of(), List.of("id"));

        assertArrayEquals(expected, result);
    }

    @Test(expected = IllegalStateException.class)
    public void searchAndRead_whenUnavailable_throwsIllegalState() throws Exception {
        doThrow(new RuntimeException("down")).when(odooClient).init();
        realOdooClient = new RealOdooClient(odooClient);

        realOdooClient.searchAndRead("res.partner", List.of(), List.of());
    }

    @Test
    public void searchAndRead_whenCallFails_marksUnavailable() throws Exception {
        doNothing().when(odooClient).init();
        realOdooClient = new RealOdooClient(odooClient);
        when(odooClient.searchAndRead(anyString(), anyList(), anyList()))
                .thenThrow(new RuntimeException("Odoo crashed"));

        try {
            realOdooClient.searchAndRead("res.partner", List.of(), List.of("id"));
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            // expected
        }

        // stub reconnect to fail
        doThrow(new RuntimeException("still down")).when(odooClient).init();

        assertFalse(realOdooClient.isAvailable());
    }

    // ─── volatile flag thread-safety sanity check ─────────────────────────────

    @Test
    public void isAvailable_calledMultipleTimes_whenAvailable_staysTrue() throws Exception {
        doNothing().when(odooClient).init();
        realOdooClient = new RealOdooClient(odooClient);

        for (int i = 0; i < 10; i++) {
            assertTrue(realOdooClient.isAvailable());
        }

        // init() should only be called once at startup — not on every isAvailable() call
        verify(odooClient, times(1)).init();
    }

    @Test
    public void isAvailable_whenUnavailableAndReconnectKeepsFailing_retriesEachTime() throws Exception {
        doThrow(new RuntimeException("always down")).when(odooClient).init();
        realOdooClient = new RealOdooClient(odooClient);

        // Call isAvailable 3 times — each should attempt reconnect
        assertFalse(realOdooClient.isAvailable());
        assertFalse(realOdooClient.isAvailable());
        assertFalse(realOdooClient.isAvailable());

        // init() called: 1 (constructor) + 3 (isAvailable reconnect attempts)
        verify(odooClient, times(4)).init();
    }
}
