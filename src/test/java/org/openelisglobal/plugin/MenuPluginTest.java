package org.openelisglobal.plugin;

import static org.junit.Assert.assertFalse;

import java.lang.reflect.Field;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.services.PluginMenuService;

/**
 * Unit tests for {@link MenuPlugin}.
 *
 * <p>
 * Verifies the defensive null-check on PluginMenuService.getInstance() returns
 * false when the service is not initialized, rather than propagating a
 * NullPointerException.
 */
public class MenuPluginTest {

    private Object originalInstance;

    @Before
    public void setUp() throws Exception {
        // Save and clear the static INSTANCE field
        Field field = PluginMenuService.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        originalInstance = field.get(null);
        field.set(null, null);
    }

    @After
    public void tearDown() throws Exception {
        // Restore original INSTANCE
        Field field = PluginMenuService.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        field.set(null, originalInstance);
    }

    /**
     * When PluginMenuService.INSTANCE is null (service not yet initialized),
     * connect() should return false without calling insertMenu().
     */
    @Test
    public void connect_shouldReturnFalse_whenPluginMenuServiceNotInitialized() {
        MenuPlugin plugin = new MenuPlugin() {
            @Override
            protected void insertMenu() {
                throw new AssertionError("insertMenu() should not be called when service is null");
            }
        };

        assertFalse("connect() should return false when PluginMenuService is null", plugin.connect());
    }
}
