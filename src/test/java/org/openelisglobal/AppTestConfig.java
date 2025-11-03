package org.openelisglobal;

import static org.mockito.Mockito.mock;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jasypt.util.text.TextEncryptor;
import org.mockito.Mockito;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.barcode.controller.PrintBarcodeController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.common.services.RequesterService;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.common.util.Versioning;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.externalconnections.service.BasicAuthenticationDataService;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notification.service.AnalysisNotificationConfigService;
import org.openelisglobal.notification.service.TestNotificationConfigService;
import org.openelisglobal.odoo.client.OdooClient;
import org.openelisglobal.odoo.client.OdooConnection;
import org.openelisglobal.odoo.config.TestProductMapping;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.referral.fhir.service.FhirReferralService;
import org.openelisglobal.reports.service.WHONetReportServiceImpl;
import org.openelisglobal.requester.service.RequesterTypeService;
import org.openelisglobal.result.controller.AnalyzerResultsController;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan(basePackages = { "org.openelisglobal.spring", "org.openelisglobal.patient",
        "org.openelisglobal.patientidentity", "org.openelisglobal.gender", "org.openelisglobal.patientidentitytype",
        "org.openelisglobal.patienttype", "org.openelisglobal.address", "org.openelisglobal.dictionary",
        "org.openelisglobal.person", "org.openelisglobal.audittrail", "org.openelisglobal.referencetables",
        "org.openelisglobal.history", "org.openelisglobal.menu", "org.openelisglobal.login",
        "org.openelisglobal.systemusermodule", "org.openelisglobal.rolemodule", "org.openelisglobal.view",
        "org.openelisglobal.search", "org.openelisglobal.common.util", "org.openelisglobal.sample",
        "org.openelisglobal.sampleitem", "org.openelisglobal.analysis", "org.openelisglobal.result",
        "org.openelisglobal.resultlimit", "org.openelisglobal.resultlimits", "org.openelisglobal.typeoftestresult",
        "org.openelisglobal.samplehuman", "org.openelisglobal.provider", "org.openelisglobal.role",
        "org.openelisglobal.organization", "org.openelisglobal.region", "org.openelisglobal.program",
        "org.openelisglobal.note", "org.openelisglobal.requester", "org.openelisglobal.method",
        "org.openelisglobal.sampleorganization", "org.openelisglobal.analyte", "org.openelisglobal.panel",
        "org.openelisglobal.panelitem", "org.openelisglobal.reports", "org.openelisglobal.userrole",
        "org.openelisglobal.unitofmeasure", "org.openelisglobal.testtrailer", "org.openelisglobal.scriptlet",
        "org.openelisglobal.localization", "org.openelisglobal.systemuser", "org.openelisglobal.systemmodule",
        "org.openelisglobal.testdictionary", "org.openelisglobal.dictionarycategory", "org.openelisglobal.sampledomain",
        "org.openelisglobal.sampleproject", "org.openelisglobal.observationhistorytype",
        "org.openelisglobal.statusofsample", "org.openelisglobal.test", "org.openelisglobal.analyzerimport",
        "org.openelisglobal.analyzer", "org.openelisglobal.testanalyte", "org.openelisglobal.observationhistory",
        "org.openelisglobal.systemusersection", "org.openelisglobal.citystatezip", "org.openelisglobal.typeofsample",
        "org.openelisglobal.siteinformation", "org.openelisglobal.config", "org.openelisglobal.image",
        "org.openelisglobal.testresult", "org.openelisglobal.barcode", "org.openelisglobal.referral",
        "org.openelisglobal.qaevent", "org.openelisglobal.project", "org.openelisglobal.sampleqaevent",
        "org.openelisglobal.patientrelation", "org.openelisglobal.inventory", "org.openelisglobal.testcodes",
        "org.openelisglobal.datasubmission", "org.openelisglobal.label", "org.openelisglobal.renametestsection",
        "org.openelisglobal.action", "org.openelisglobal.analysisqaevent", "org.openelisglobal.analysisqaeventaction",
        "org.openelisglobal.dataexchange", "org.openelisglobal.samplepdf", "org.openelisglobal.samplenewborn",
        "org.openelisglobal.sampleqaeventaction", "org.openelisglobal.analyzerresults", "org.openelisglobal.testreflex",
        "org.openelisglobal.county", "org.openelisglobal.sampletracking", "org.openelisglobal.testresultsview",
        "org.openelisglobal.projectorganization", "org.openelisglobal.sourceofsample",
        "org.openelisglobal.testconfiguration", "org.openelisglobal.usertestsection",
        "org.openelisglobal.testcalculated", "org.openelisglobal.odoo", "org.openelisglobal.ocl",
        "org.openelisglobal.storage" }, excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.patient.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.organization.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.sample.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.result.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.login.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.program.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.siteinformation.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.config.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.fhir.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.*.fhir.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.odoo.config.OdooConnectionConfig"),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PrintBarcodeController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WHONetReportServiceImpl.class) })
@EnableWebMvc
public class AppTestConfig implements WebMvcConfigurer {

    @Bean
    @Profile("test")
    public TextEncryptor textEncryptor() {
        return mock(TextEncryptor.class);
    }

    @Bean()
    @Profile("test")
    public PluginAnalyzerService pluginAnalyzerService() {
        return mock(PluginAnalyzerService.class);
    }

    @Bean()
    @Profile("test")
    public FhirPersistanceService fhirPesistence() {
        return mock(FhirPersistanceService.class);
    }

    @Bean()
    @Profile("test")
    public FhirUtil fhirUtil() {
        return mock(FhirUtil.class);
    }

    @Bean()
    @Profile("test")
    public FhirConfig fhirConfig() {
        return mock(FhirConfig.class);
    }

    @Bean()
    @Profile("test")
    public CloseableHttpClient closeableHttpClient() {
        return mock(CloseableHttpClient.class);
    }

    @Bean()
    @Profile("test")
    public FhirContext fhirContext() {
        return mock(FhirContext.class);
    }

    @Bean()
    @Profile("test")
    public FhirTransformService fhirTransformServicehirTransformService() {
        return mock(FhirTransformService.class);
    }

    @Bean()
    @Profile("test")
    public FhirTransformService fhirTransformService() {
        return mock(FhirTransformService.class);
    }

    @Bean()
    @Profile("test")
    public ExternalConnectionService externalConnectService() {
        return mock(ExternalConnectionService.class);
    }

    @Bean()
    @Profile("test")
    public BasicAuthenticationDataService basicAuthDataService() {
        return mock(BasicAuthenticationDataService.class);
    }

    @Bean()
    @Profile("test")
    public BasicAuthenticationDataService basicAuthenticationDataService() {
        return mock(BasicAuthenticationDataService.class);
    }

    @Bean()
    @Profile("test")
    public TestNotificationConfigService testNotificationConfigService() {
        return mock(TestNotificationConfigService.class);
    }

    @Bean()
    @Profile("test")
    public UnsatisfiedDependencyException unsatisfiedDependencyException() {
        return mock(UnsatisfiedDependencyException.class);
    }

    @Bean()
    @Profile("test")
    public AnalysisNotificationConfigService analysisNotificationConfigService() {
        return mock(AnalysisNotificationConfigService.class);
    }

    @Bean()
    @Profile("test")
    public AuditTrailService auditTrailService() {
        return mock(AuditTrailService.class);
    }

    @Bean()
    @Profile("test")
    public DisplayListService displayListService() {
        return mock(DisplayListService.class);
    }

    @Bean()
    @Profile("test")
    public SampleOrderService sampleOrderService() {
        return mock(SampleOrderService.class);
    }

    @Bean()
    @Profile("test")
    public RequesterService requesterService() {
        return mock(RequesterService.class);
    }

    @Bean
    @Profile("test")
    public FhirReferralService fhirReferralService() {
        return Mockito.mock(FhirReferralService.class);
    }

    @Bean()
    @Profile("test")
    public Versioning versioning() {
        return mock(Versioning.class);
    }

    @Bean
    @Profile("test")
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:/languages/message");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        MessageUtil.setMessageSource(messageSource);
        return messageSource;
    }

    @Bean
    public MappingJackson2HttpMessageConverter jsonConverter() {
        List<MediaType> supportedMediaTypes = new ArrayList<>();
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);

        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(builder.build());
        jsonConverter.setSupportedMediaTypes(supportedMediaTypes);
        return jsonConverter;
    }

    @Bean()
    @Profile("test")
    public IStatusService iStatusService() {
        return mock(IStatusService.class);
    }

    @Bean
    @Profile("test")
    public OdooClient odooClient() {
        return mock(OdooClient.class);
    }

    @Bean
    @Profile("test")
    public OdooConnection odooConnection() {
        return mock(OdooConnection.class);
    }

    @Bean
    @Profile("test")
    public TestProductMapping testProductMapping() {
        return mock(TestProductMapping.class);
    }

    @Bean()
    @Profile("Test")
    public RequesterTypeService RequesterTypeService() {
        return mock(RequesterTypeService.class);
    }

    @Bean()
    @Profile("Test")
    public OrganizationTypeService OrganizationTypeService() {
        return mock(OrganizationTypeService.class);
    }

    @Override
    public void configureMessageConverters(@NonNull List<HttpMessageConverter<?>> converters) {
        WebMvcConfigurer.super.configureMessageConverters(converters);
        converters.add(new StringHttpMessageConverter());
        converters.add(jsonConverter());
    }

    @Bean()
    public AnalyzerResultsController analyzerResultsController() {
        return mock(AnalyzerResultsController.class);
    }

}
