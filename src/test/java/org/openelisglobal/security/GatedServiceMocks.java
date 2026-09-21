package org.openelisglobal.security;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.mockito.Mockito;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.springframework.test.util.AopTestUtils;

/**
 * Mockito mocks of {@code @PreAuthorize}-gated service interfaces that are safe
 * to publish as Spring context {@code @Bean}s.
 *
 * <p>
 * <b>Why this exists.</b> Mockito's generated subclass inherits the interface's
 * {@code @PreAuthorize} — at class level and method level alike. Spring
 * Security's {@code AuthorizationAnnotationUtils.findUniqueAnnotation} then
 * finds the annotation twice (once on the mock, once on the interface; it skips
 * only <em>synthetic</em> methods, and the mock's are real) and throws
 * {@code AnnotationConfigurationException} the moment a gate is evaluated.
 * {@code withSettings().withoutAnnotations()} does <em>not</em> prevent this on
 * the Mockito version this project pins: it compiles and runs, but
 * {@code getDeclaredAnnotations()} on the generated class still returns the
 * annotation, and so does
 * {@code AnnotatedElementUtils.findAllMergedAnnotations}, which is what Spring
 * Security actually calls. Verified by reflection on Mockito 2.21.0; do not
 * reintroduce {@code withoutAnnotations()} as the fix.
 *
 * <p>
 * <b>What this does.</b> Wraps the Mockito mock in a JDK dynamic proxy. The
 * proxy implements only the interface and carries no annotations of its own, so
 * the interface stays the single annotation source and method security
 * evaluates it cleanly — which {@code ClassLevelPreAuthorizeSemanticsTest}
 * pins. Every call still forwards to the mock, so stubbing and verification
 * work as usual.
 *
 * <p>
 * <b>Stubbing.</b> Stub and verify against {@link #mockBehind(Object)}, never
 * against the bean itself: an {@code @Autowired} field holds Spring's
 * method-security AOP proxy around the JDK proxy around the mock, and calling
 * {@code when(bean.foo())} runs the {@code @PreAuthorize} interceptor with no
 * SecurityContext — failing on authentication before Mockito sees the call.
 * Inside a {@code @Bean} method it is simplest to create the mock, stub it, and
 * return {@link #asGatedBean(Object)}.
 *
 * <p>
 * Use {@code SecuritySliceMockMvcTest#nullStub} instead when the collaborator
 * only needs to exist.
 */
public final class GatedServiceMocks {

    private GatedServiceMocks() {
    }

    /**
     * Implemented by the JDK proxies this class produces for interfaces, so
     * {@link #mockBehind(Object)} can reach the Mockito mock. The generated
     * subclass for a class deliberately does NOT implement it: if the stub
     * implemented any interface, Spring's method-security auto-proxy would pick a
     * JDK proxy exposing only that interface, and injection into a field typed as
     * the class would fail with BeanNotOfRequiredTypeException. Class stubs carry
     * the delegate in {@link #DELEGATE_FIELD} instead, so the auto-proxy sees an
     * interface-less target and subclasses it with CGLIB.
     */
    public interface GatedStub {
        Object gatedMockDelegate();
    }

    /** Public field on generated class stubs holding the Mockito mock. */
    public static final String DELEGATE_FIELD = "__gatedMockDelegate";

    /**
     * A fresh Mockito mock of {@code type}, wrapped for publication as a bean.
     * Interfaces get a JDK proxy; classes get an annotation-free generated subclass
     * — see {@link #asGatedBean(Object, Class)}.
     */
    public static <T> T stubbableMock(Class<T> type) {
        return asGatedBean(Mockito.mock(type), type);
    }

    /**
     * Wraps an existing Mockito mock for publication as a bean. Requires the mock
     * to have been created from an interface (which is the only shape a gated
     * service takes in this codebase).
     */
    @SuppressWarnings("unchecked")
    public static <T> T asGatedBean(T mock) {
        Class<?> mockClass = mock.getClass();
        // Interface mock: Mockito's class implements the interface plus its own marker.
        for (Class<?> candidate : mockClass.getInterfaces()) {
            if (!candidate.getName().startsWith("org.mockito.")) {
                return (T) asGatedBean(mock, candidate);
            }
        }
        // Class mock: Mockito's class extends the mocked class.
        Class<?> superclass = mockClass.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            return (T) asGatedBean(mock, superclass);
        }
        throw new IllegalArgumentException("asGatedBean needs a Mockito mock, got " + mockClass);
    }

    /**
     * For an interface, a JDK proxy. For a class, a ByteBuddy subclass built
     * WITHOUT the method/type attribute appenders Mockito opts into — those
     * appenders are exactly what copies {@code @PreAuthorize} onto Mockito's
     * generated type, so leaving them out yields an override with no annotation of
     * its own, and Spring Security's unique-annotation scan finds the superclass
     * method's annotation exactly once. The instance is created with Objenesis so
     * the class's constructor (typically {@code @Autowired}) never runs. Every
     * method forwards to the mock, as with the proxy.
     */
    @SuppressWarnings("unchecked")
    private static <T> T asGatedBean(T mock, Class<?> type) {
        ForwardingHandler handler = new ForwardingHandler(mock);
        if (type.isInterface()) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type, GatedStub.class }, handler);
        }
        try {
            Class<?> generated = new ByteBuddy()
                    // Mirror the superclass constructors (never run — see Objenesis below) so
                    // Spring's CGLIB auto-proxy has a visible constructor to subclass.
                    .subclass(type, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                    .defineField(DELEGATE_FIELD, Object.class, Visibility.PUBLIC).method(ElementMatchers.any())
                    .intercept(InvocationHandlerAdapter.of(handler)).make()
                    .load(type.getClassLoader(), ClassLoadingStrategy.UsingLookup
                            .of(MethodHandles.privateLookupIn(type, MethodHandles.lookup())))
                    .getLoaded();
            Object stub = OBJENESIS.newInstance(generated);
            generated.getField(DELEGATE_FIELD).set(stub, mock);
            return (T) stub;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException("Cannot generate a gated stub for " + type, e);
        }
    }

    private static final Objenesis OBJENESIS = new ObjenesisStd();

    /**
     * The Mockito mock behind a bean produced by this class — the object to hand to
     * {@code when(...)}, {@code verify(...)} or {@code reset(...)}. Peels Spring's
     * AOP proxy first, then the JDK proxy. Returns the argument's ultimate target
     * unchanged if it is not one of ours, so it is safe to apply to any
     * collaborator.
     */
    @SuppressWarnings("unchecked")
    public static <T> T mockBehind(T bean) {
        Object target = AopTestUtils.getUltimateTargetObject(bean);
        if (target instanceof GatedStub stub) {
            return (T) stub.gatedMockDelegate();
        }
        try {
            return (T) target.getClass().getField(DELEGATE_FIELD).get(target);
        } catch (NoSuchFieldException e) {
            return (T) target; // not one of ours
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Forwards every call to the wrapped Mockito mock. */
    private static final class ForwardingHandler implements InvocationHandler {

        private final Object delegate;

        ForwardingHandler(Object delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == GatedStub.class) {
                return delegate;
            }
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                // Unwrap so a stubbed thenThrow surfaces as the real exception rather
                // than as a reflection wrapper.
                throw e.getCause();
            }
        }
    }
}
