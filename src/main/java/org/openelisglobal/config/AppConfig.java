package org.openelisglobal.config;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.fhir.springserialization.QuestionnaireDeserializer;
import org.openelisglobal.fhir.springserialization.QuestionnaireResponseDeserializer;
import org.openelisglobal.fhir.springserialization.QuestionnaireResponseSerializer;
import org.openelisglobal.fhir.springserialization.QuestionnaireSerializer;
import org.openelisglobal.interceptor.CommonPageAttributesInterceptor;
import org.openelisglobal.interceptor.UrlErrorsInterceptor;
import org.openelisglobal.internationalization.GlobalLocaleResolver;
import org.openelisglobal.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@EnableWebMvc
@Configuration
@EnableJpaRepositories(basePackages = { "org.itech", "org.ozeki.sms" })
@PropertySource("classpath:application.properties")
@PropertySource("file:/run/secrets/common.properties")
@PropertySource(value = "file:/run/secrets/extra.properties", ignoreResourceNotFound = true)
@PropertySource(value = "classpath:SystemConfiguration.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:/var/lib/openelis-global/properties/TotalSystemConfiguration.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:/var/lib/openelis-global/properties/SystemConfiguration.properties", ignoreResourceNotFound = true)
@ComponentScan(basePackages = { "spring", "org.openelisglobal", "org.itech", "org.ozeki.sms", "oe.plugin" })
public class AppConfig implements WebMvcConfigurer {

    @Autowired
    @Qualifier(value = "ModuleAuthenticationInterceptor")
    HandlerInterceptor moduleAuthenticationInterceptor;

    @Autowired
    UrlErrorsInterceptor urlLocatedErrorsInterceptor;
    @Autowired
    CommonPageAttributesInterceptor pageAttributesInterceptor;
    @Autowired
    LocaleChangeInterceptor localeChangeInterceptor;
    @Autowired
    LocaleResolver localResolver;

    @Bean
    public ViewResolver internalResourceViewResolver() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("");
        viewResolver.setSuffix("");
        viewResolver.setContentType("text/html; charset=UTF-8");
        return viewResolver;
    }

    @Bean(name = "filterMultipartResolver")
    public StandardServletMultipartResolver multipartResolver() {
        StandardServletMultipartResolver multipartResolver = new StandardServletMultipartResolver();
        multipartResolver.setResolveLazily(false);
        return multipartResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor).addPathPatterns("/**");
        registry.addInterceptor(moduleAuthenticationInterceptor).addPathPatterns("/**")
                .excludePathPatterns(SecurityConfig.OPEN_PAGES) //
                .excludePathPatterns(SecurityConfig.LOGIN_PAGES) //
                .excludePathPatterns(SecurityConfig.RESOURCE_PAGES) //
                .excludePathPatterns(SecurityConfig.AUTH_OPEN_PAGES);
        // TO DO ,we need to have a better way to handle user roles for rest controllers
        // .excludePathPatterns(SecurityConfig.REST_CONTROLLERS);
        // .excludePathPatterns(SecurityConfig.CLIENT_CERTIFICATE_PAGES);
        registry.addInterceptor(urlLocatedErrorsInterceptor).addPathPatterns("/**");
        registry.addInterceptor(pageAttributesInterceptor).addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Register resource handlers for css, js, etc.
        registry.addResourceHandler("select2/**").addResourceLocations("classpath:static/select2/");
        registry.addResourceHandler("scripts/**").addResourceLocations("classpath:static/scripts/");
        registry.addResourceHandler("css/**").addResourceLocations("classpath:static/css/");
        registry.addResourceHandler("images/**").addResourceLocations("/static/images/");
        registry.addResourceHandler("favicon/**").addResourceLocations("/static/favicon/");
        registry.addResourceHandler("fontawesome-free-5.13.1-web/**")
                .addResourceLocations("/static/fontawesome-free-5.13.1-web/");
        registry.addResourceHandler("documentation/**").addResourceLocations("classpath:static/documentation/");
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    // prototype scope so we don't have to restart when connection info is edited
    public JavaMailSender getJavaMailSender() {
        ConfigurationProperties configurationProperties = ConfigurationProperties.getInstance();
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        String address = configurationProperties.getPropertyValue(Property.PATIENT_RESULTS_SMTP_ADDRESS);
        if (!GenericValidator.isBlankOrNull(address)) {
            try {
                URI uri = new URI(address);
                mailSender.setHost(uri.getHost());
                mailSender.setPort(uri.getPort());
            } catch (URISyntaxException e) {
                LogEvent.logError(e);
            }
        }

        String username = configurationProperties.getPropertyValue(Property.PATIENT_RESULTS_SMTP_USERNAME);
        String password = configurationProperties.getPropertyValue(Property.PATIENT_RESULTS_SMTP_PASSWORD);
        if (!(GenericValidator.isBlankOrNull(username) || GenericValidator.isBlankOrNull(password))) {
            mailSender.setUsername(username);
            mailSender.setPassword(password);
            props.put("mail.smtp.auth", "true");
        } else {
            props.put("mail.smtp.auth", "false");
        }

        return mailSender;
    }

    @Bean("localeResolver")
    @Primary
    // this belongs in InternationalizationConfig.java, but putting it there breaks
    // functionality
    public LocaleResolver localeResolver() {
        GlobalLocaleResolver localeResolver = new GlobalLocaleResolver();
        return localeResolver;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public MappingJackson2HttpMessageConverter jacksonMessageConverter() {
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();

        ObjectMapper mapper = new ObjectMapper();
        // Registering Hibernate4Module to support lazy objects
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Hibernate5JakartaModule());
        mapper.registerModule(new Jdk8Module());
        mapper.setSerializationInclusion(Include.NON_NULL);

        SimpleModule module = new SimpleModule();
        module.addSerializer(Questionnaire.class, new QuestionnaireSerializer());
        module.addDeserializer(Questionnaire.class, new QuestionnaireDeserializer());
        module.addSerializer(QuestionnaireResponse.class, new QuestionnaireResponseSerializer());
        module.addDeserializer(QuestionnaireResponse.class, new QuestionnaireResponseDeserializer());
        mapper.registerModule(module);

        messageConverter.setObjectMapper(mapper);
        return messageConverter;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Here we add our custom-configured HttpMessageConverter
        converters.add(jacksonMessageConverter());
        // super.configureMessageConverters(converters);
    }
}