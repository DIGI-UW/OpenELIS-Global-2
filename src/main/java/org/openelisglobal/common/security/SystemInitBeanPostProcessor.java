package org.openelisglobal.common.security;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Clears the SystemInitFlag once the Spring application context has finished
 * refreshing (i.e., all beans are fully initialised). The flag is set earlier,
 * in AnnotationWebAppInitializer.onStartup(), scoped to the startup phase
 * rather than to individual bean creation cycles.
 */
@Component
public class SystemInitBeanPostProcessor implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        SystemInitFlag.clear();
    }
}
