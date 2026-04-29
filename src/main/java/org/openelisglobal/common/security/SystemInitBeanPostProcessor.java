package org.openelisglobal.common.security;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

/**
 * Sets SystemInitFlag before each bean's @PostConstruct runs and clears it
 * after, allowing @PostConstruct methods to call @PreAuthorize-protected
 * services without an auth context.
 */
@Component
public class SystemInitBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        SystemInitFlag.set();
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        SystemInitFlag.clear();
        return bean;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
