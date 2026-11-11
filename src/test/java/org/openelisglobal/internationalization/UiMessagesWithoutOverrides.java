package org.openelisglobal.internationalization;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 * For unit tests that read the interface catalogue without a Spring context:
 * the catalogue asks the configuration whether translation overrides are on,
 * and here they are off. {@link #restore()} puts back whichever context an
 * earlier test left, so later tests are unaffected.
 */
public final class UiMessagesWithoutOverrides {

    private static Object previousContext;
    private static Object previousFactory;

    private UiMessagesWithoutOverrides() {
    }

    public static void install() throws ReflectiveOperationException {
        previousContext = springField("context").get(null);
        previousFactory = springField("factory").get(null);
        AutowireCapableBeanFactory factory = mock(AutowireCapableBeanFactory.class);
        when(factory.getBean(DefaultConfigurationProperties.class))
                .thenReturn(mock(DefaultConfigurationProperties.class));
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getAutowireCapableBeanFactory()).thenReturn(factory);
        new SpringContext().setApplicationContext(context);
    }

    public static void restore() throws ReflectiveOperationException {
        springField("context").set(null, previousContext);
        springField("factory").set(null, previousFactory);
    }

    private static Field springField(String name) throws NoSuchFieldException {
        Field field = SpringContext.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
