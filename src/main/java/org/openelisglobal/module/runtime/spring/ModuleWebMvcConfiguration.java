package org.openelisglobal.module.runtime.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Replaces {@code @EnableWebMvc} on {@code AppConfig}: the same Spring MVC
 * configuration (it is the class that annotation imports, so every
 * {@code WebMvcConfigurer} bean, including {@code AppConfig}, is still
 * applied), with the request mapping created as a
 * {@link ModuleAwareRequestMappingHandlerMapping} so that module controllers
 * can be registered into it at start-up. The bean keeps its name,
 * {@code requestMappingHandlerMapping}.
 */
@Configuration
public class ModuleWebMvcConfiguration extends DelegatingWebMvcConfiguration {

    @Override
    protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
        return new ModuleAwareRequestMappingHandlerMapping();
    }
}
