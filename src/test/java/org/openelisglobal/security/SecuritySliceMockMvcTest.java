package org.openelisglobal.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
public abstract class SecuritySliceMockMvcTest {

    @Autowired
    protected WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;

    @Before
    public void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    /**
     * A do-nothing stub for a {@code @PreAuthorize}-annotated service interface,
     * for use as a collaborator bean in security-slice tests.
     *
     * <p>
     * Mockito (2.x) copies the interface's security annotations onto the generated
     * mock class, so Spring Security's unique-annotation scan finds the same
     * {@code @PreAuthorize} twice (mock method + interface method) and fails with
     * {@code AnnotationConfigurationException} the moment the gate is evaluated. A
     * JDK dynamic proxy carries no copied annotations — the interface stays the
     * single annotation source — so method security evaluates it cleanly. See
     * {@code ClassLevelPreAuthorizeSemanticsTest}.
     *
     * <p>
     * Mirrors Mockito's default answers: empty collections/Optional, defaults for
     * primitives, {@code null} otherwise.
     */
    @SuppressWarnings("unchecked")
    protected static <T> T nullStub(Class<T> iface) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] { iface }, (proxy, method, args) -> {
            Class<?> rt = method.getReturnType();
            if (rt == boolean.class || rt == Boolean.class) {
                return false;
            }
            if (rt == int.class) {
                return 0;
            }
            if (rt == long.class) {
                return 0L;
            }
            if (rt == double.class) {
                return 0.0d;
            }
            if (rt == float.class) {
                return 0.0f;
            }
            if (rt == short.class) {
                return (short) 0;
            }
            if (rt == byte.class) {
                return (byte) 0;
            }
            if (rt == char.class) {
                return (char) 0;
            }
            if (rt == List.class) {
                return Collections.emptyList();
            }
            if (rt == Map.class) {
                return Collections.emptyMap();
            }
            if (rt == Set.class) {
                return Collections.emptySet();
            }
            if (rt == Optional.class) {
                return Optional.empty();
            }
            if (method.getName().equals("toString") && (args == null || args.length == 0)) {
                return "nullStub<" + iface.getSimpleName() + ">";
            }
            if (method.getName().equals("hashCode") && (args == null || args.length == 0)) {
                return System.identityHashCode(proxy);
            }
            if (method.getName().equals("equals") && args != null && args.length == 1) {
                return proxy == args[0];
            }
            return null;
        });
    }
}
