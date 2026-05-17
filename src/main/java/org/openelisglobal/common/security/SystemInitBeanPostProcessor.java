package org.openelisglobal.common.security;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Sets SystemInitFlag before any bean is instantiated (via BeanFactoryPostProcessor,
 * which runs before @PostConstruct methods) and clears it once the context has
 * fully refreshed (via ContextRefreshedEvent). This covers both the production
 * servlet context and the test Spring context, so @PostConstruct methods can
 * call @PreAuthorize-protected services without an auth context during startup.
 */
@Component
public class SystemInitBeanPostProcessor
        implements BeanFactoryPostProcessor, ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        SystemInitFlag.set();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        SystemInitFlag.clear();
    }
}
