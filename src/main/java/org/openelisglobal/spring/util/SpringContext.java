package org.openelisglobal.spring.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringContext implements ApplicationContextAware {

    private static ApplicationContext context;
    private static AutowireCapableBeanFactory factory;

    /**
     * Returns the Spring managed bean instance of the given class type (if it
     * exists). Returns null otherwise.
     *
     * @param beanClass
     * @return
     */
    public static <T extends Object> T getBean(Class<T> beanClass) {
        return factory.getBean(beanClass);
    }

    /**
     * Returns the Spring managed bean instance of the given class type (if it
     * exists). Returns null otherwise.
     *
     * @param beanClass
     * @return
     */
    public static Object getBean(String beanName) {
        return factory.getBean(beanName);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Object> T createBean(Class<T> beanClass) {
        return (T) factory.createBean(beanClass, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
    }

    public static void fillBean(Object bean) {
        factory.autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {

        // store ApplicationContext reference to access required beans later on
        SpringContext.context = context;
        SpringContext.factory = context.getAutowireCapableBeanFactory();
    }
}
