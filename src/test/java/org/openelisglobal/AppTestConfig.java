package org.openelisglobal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.apache.http.impl.client.CloseableHttpClient;
import org.itech.fhir.dataexport.api.service.DataExportService;
import org.itech.fhir.dataexport.core.dao.DataExportTaskDAO;
import org.itech.fhir.dataexport.core.service.DataExportTaskService;
import org.jasypt.util.text.TextEncryptor;
import org.mockito.Mockito;
import org.openelisglobal.analyzer.AnalyzerTestProfileCatalog;
import org.openelisglobal.analyzer.service.BridgeProfileCatalogService;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.barcode.controller.PrintBarcodeController;
import org.openelisglobal.common.paging.PagingProperties;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.common.services.RequesterService;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.common.util.Versioning;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notification.service.AnalysisNotificationConfigService;
import org.openelisglobal.notification.service.TestNotificationConfigService;
import org.openelisglobal.notification.service.TestNotificationService;
import org.openelisglobal.notification.service.TestNotificationServiceImpl;
import org.openelisglobal.notifications.dao.NotificationDAO;
import org.openelisglobal.odoo.client.OdooClient;
import org.openelisglobal.odoo.client.OdooConnection;
import org.openelisglobal.odoo.config.TestProductMapping;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.referral.fhir.service.FhirReferralService;
import org.openelisglobal.reports.service.WHONetReportServiceImpl;
import org.openelisglobal.requester.service.RequesterTypeService;
import org.openelisglobal.result.controller.AnalyzerResultsController;
import org.openelisglobal.result.controller.rest.AccessionResultsRestController;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.security.certs.service.TruststoreService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.ozeki.sms.service.OzekiMessageOutService;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan(basePackages = { "org.openelisglobal.spring", "org.openelisglobal.common.services",
        "org.openelisglobal.patient", "org.openelisglobal.patientidentity", "org.openelisglobal.gender",
        "org.openelisglobal.patientidentitytype", "org.openelisglobal.patienttype", "org.openelisglobal.address",
        "org.openelisglobal.dictionary", "org.openelisglobal.person", "org.openelisglobal.audittrail",
        "org.openelisglobal.referencetables", "org.openelisglobal.history", "org.openelisglobal.menu",
        "org.openelisglobal.login", "org.openelisglobal.systemusermodule", "org.openelisglobal.rolemodule",
        "org.openelisglobal.view", "org.openelisglobal.search", "org.openelisglobal.common.util",
        "org.openelisglobal.sample", "org.openelisglobal.sampleitem", "org.openelisglobal.sampletyperequest",
        "org.openelisglobal.analysis", "org.openelisglobal.result", "org.openelisglobal.resultlimit",
        "org.openelisglobal.resultlimits", "org.openelisglobal.typeoftestresult", "org.openelisglobal.samplehuman",
        "org.openelisglobal.provider", "org.openelisglobal.role", "org.openelisglobal.organization",
        "org.openelisglobal.region", "org.openelisglobal.program", "org.openelisglobal.note",
        "org.openelisglobal.requester", "org.openelisglobal.method", "org.openelisglobal.sampleorganization",
        "org.openelisglobal.analyte", "org.openelisglobal.panel", "org.openelisglobal.panelitem",
        "org.openelisglobal.reports", "org.openelisglobal.userrole", "org.openelisglobal.unitofmeasure",
        "org.openelisglobal.testtrailer", "org.openelisglobal.scriptlet", "org.openelisglobal.localization",
        "org.openelisglobal.systemuser", "org.openelisglobal.systemmodule", "org.openelisglobal.testdictionary",
        "org.openelisglobal.dictionarycategory", "org.openelisglobal.sampledomain", "org.openelisglobal.sampleproject",
        "org.openelisglobal.observationhistorytype", "org.openelisglobal.statusofsample", "org.openelisglobal.test",
        "org.openelisglobal.testmethod.service", "org.openelisglobal.testmethod.daoimpl",
        "org.openelisglobal.testresultcomponent.service", "org.openelisglobal.testresultcomponent.daoimpl",
        "org.openelisglobal.testresultinterpretation.service", "org.openelisglobal.testresultinterpretation.daoimpl",
        "org.openelisglobal.testactivation.service", "org.openelisglobal.testactivation.daoimpl",
        "org.openelisglobal.testsamplehandling.service", "org.openelisglobal.testsamplehandling.daoimpl",
        "org.openelisglobal.testterminology.service", "org.openelisglobal.testterminology.daoimpl",
        "org.openelisglobal.sampletypeterminology.service", "org.openelisglobal.sampletypeterminology.daoimpl",
        "org.openelisglobal.panelterminology.service", "org.openelisglobal.panelterminology.daoimpl",
        "org.openelisglobal.testreagentlink.service", "org.openelisglobal.testreagentlink.daoimpl",
        "org.openelisglobal.testalertrule", "org.openelisglobal.testcatalog.service",
        "org.openelisglobal.analyzerimport", "org.openelisglobal.analyzer", "org.openelisglobal.plugin",
        "org.openelisglobal.testanalyte", "org.openelisglobal.observationhistory",
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
        "org.openelisglobal.storage", "org.openelisglobal.notebook", "org.openelisglobal.coldstorage",
        "org.openelisglobal.labelpreset", "org.openelisglobal.alert", "org.openelisglobal.notification",
        "org.openelisglobal.shipment", "org.openelisglobal.reportdefinition", "org.openelisglobal.scheduler",
        "org.openelisglobal.sitebranding", "org.openelisglobal.resultvalidation", "org.openelisglobal.plugin",
        "org.openelisglobal.fhir.providers", "org.openelisglobal.common.dao", "org.openelisglobal.report",
        "org.openelisglobal.eqa", "org.openelisglobal.qc", "org.openelisglobal.externalconnections",
        "org.openelisglobal.notifications", "org.openelisglobal.calendar", "org.openelisglobal.qachecklist",
        "org.openelisglobal.esig", "org.openelisglobal.compliance", "org.openelisglobal.vector",
        "org.openelisglobal.sampleacceptance", "org.openelisglobal.sampletyperequest",
        "org.openelisglobal.resultreporting.service", "org.openelisglobal.security",
        "org.openelisglobal.genericsample" }, excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.patient.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.organization.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.sample.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.vector.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.result.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.login.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.program.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.siteinformation.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.config.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.odoo.config.OdooConnectionConfig"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.scheduler.SchedulerConfig"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.security.SecurityConfig"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.security.DaemonUserConfig"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.security.login.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.eqa.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.qc.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.eqa.scheduler.*"),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PrintBarcodeController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WHONetReportServiceImpl.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TestNotificationServiceImpl.class),
                // Nested test-only @Configuration classes must not be picked up by this
                // shared component scan. ComplianceReportReissueSecurityTest.TestConfig
                // registers a mock SampleComplianceStandardDAO @Bean for its isolated
                // MockMvc slice; if scanned here it collides with the real
                // sampleComplianceStandardDAOImpl (NoUniqueBeanDefinitionException) and
                // breaks the shared integration ApplicationContext. Matched by REGEX on
                // its binary name because the nested config is package-private.
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.compliance.controller.rest.ComplianceReportReissueSecurityTest.*") })
@EnableWebMvc
public class AppTestConfig implements WebMvcConfigurer {

    @Bean
    @Primary
    @Profile("test")
    public BridgeProfileCatalogService bridgeProfileCatalogService() {
        return AnalyzerTestProfileCatalog::catalog;
    }

    @Bean
    @Profile("test")
    public TextEncryptor textEncryptor() {
        TextEncryptor encryptor = mock(TextEncryptor.class);
        // Return input unchanged so JPA @Convert works with plain-text test data
        when(encryptor.encrypt(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(encryptor.decrypt(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        return encryptor;
    }

    @Bean
    @Profile("test")
    public AccessionNumberValidatorFactory accessionNumberValidatorFactory() {
        return mock(AccessionNumberValidatorFactory.class);
    }

    @Bean
    @Profile("test")
    public TruststoreService truststoreService() {
        return mock(TruststoreService.class);
    }

    @Bean()
    @Profile("test")
    public PluginAnalyzerService pluginAnalyzerService() {
        return mock(PluginAnalyzerService.class);
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
    public TestNotificationConfigService testNotificationConfigService() {
        return mock(TestNotificationConfigService.class);
    }

    @Bean()
    @Profile("test")
    public TestNotificationService testNotificationService() {
        return mock(TestNotificationService.class);
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
    public NotificationDAO notificationDAO() {
        return mock(NotificationDAO.class);
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

    @Bean()
    @Profile("test")
    public JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }

    @Bean()
    @Profile("test")
    public OzekiMessageOutService ozekiMessageOutService() {
        return mock(OzekiMessageOutService.class);
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
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        builder.modules(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return builder.build();
    }

    @Bean
    public MappingJackson2HttpMessageConverter jsonConverter() {
        List<MediaType> supportedMediaTypes = new ArrayList<>();
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        // Production (AppConfig.jacksonMessageConverter) uses a raw ObjectMapper,
        // which REJECTS unknown JSON fields; Spring's builder default silently
        // accepts them. Tests must exercise the production contract — a lenient
        // test mapper is exactly how an un-PUT-able GET representation shipped
        // in the alert-rule endpoints (OGC-949).
        builder.failOnUnknownProperties(true);

        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(builder.build());
        jsonConverter.setSupportedMediaTypes(supportedMediaTypes);
        return jsonConverter;
    }

    // Removed IStatusService mock - integration tests need the real StatusService
    // to properly load status maps from the database. Unit tests use @Mock
    // directly.

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
    public void extendMessageConverters(@NonNull List<HttpMessageConverter<?>> converters) {
        // Add custom converters while keeping default converters
        // (including ResourceHttpMessageConverter for serving files)
        converters.add(new StringHttpMessageConverter());
        // index 0, like production AppConfig — otherwise Spring's default
        // (lenient) Jackson converter handles JSON and the strict contract
        // is never exercised
        converters.add(0, jsonConverter());
    }

    @Bean()
    public AnalyzerResultsController analyzerResultsController(TypeOfSampleService typeOfSampleService) {
        return new AnalyzerResultsController(typeOfSampleService);
    }

    @Bean
    public AccessionResultsRestController accessionResultsRestController(RoleService roleService) {
        return new AccessionResultsRestController(roleService);
    }

    @Bean
    public org.openelisglobal.result.controller.rest.ResultEntryRestController resultEntryRestController() {
        return new org.openelisglobal.result.controller.rest.ResultEntryRestController();
    }

    /**
     * Explicit bean (the testcatalog.controller package is not scanned — a sibling
     * controller's class init breaks the test context) so MockMvc can exercise the
     * editor endpoints' real JSON binding: direct controller invocation bypasses
     * Jackson, which is how the un-PUT-able alert-rule GET representation shipped
     * unnoticed.
     */
    /** See testCatalogEditorRestController — same MockMvc rationale. */
    @Bean
    public org.openelisglobal.common.management.controller.rest.SampleTypeManagementRestController sampleTypeManagementRestController() {
        return new org.openelisglobal.common.management.controller.rest.SampleTypeManagementRestController();
    }

    @Bean
    public org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController testCatalogEditorRestController(
            org.openelisglobal.test.service.TestService testService,
            org.openelisglobal.testresultcomponent.service.TestResultComponentService componentService,
            org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService interpretationService,
            org.openelisglobal.testresult.service.TestResultService testResultService,
            org.openelisglobal.resultlimit.service.ResultLimitService resultLimitService,
            org.openelisglobal.testcatalog.service.RangeCoverageValidationService coverageService,
            org.openelisglobal.testsamplehandling.service.TestSampleHandlingService handlingService,
            org.openelisglobal.analyzer.service.AnalyzerService analyzerService,
            org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService analyzerTestMappingService,
            org.openelisglobal.typeofsample.service.TypeOfSampleService typeOfSampleService,
            org.openelisglobal.typeofsample.service.TypeOfSampleTestService typeOfSampleTestService,
            org.openelisglobal.testterminology.service.TestTerminologyMappingService terminologyService,
            org.openelisglobal.panel.service.PanelService panelService,
            org.openelisglobal.panelitem.service.PanelItemService panelItemService) {
        return new org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController(testService,
                componentService, interpretationService, testResultService, resultLimitService, coverageService,
                handlingService, analyzerService, analyzerTestMappingService, typeOfSampleService,
                typeOfSampleTestService, terminologyService, panelService, panelItemService);
    }

    @Bean
    public org.openelisglobal.result.controller.rest.LogbookResultsRestController logbookResultsRestController(
            org.openelisglobal.referral.service.ReferralTypeService referralTypeService) {
        return new org.openelisglobal.result.controller.rest.LogbookResultsRestController(referralTypeService);
    }

    @Bean
    public org.openelisglobal.eqa.controller.rest.EQAAlertRestController eqaAlertRestController() {
        return new org.openelisglobal.eqa.controller.rest.EQAAlertRestController();
    }

    @Bean
    public org.openelisglobal.login.controller.ChangePasswordLoginController changePasswordLoginController() {
        return new org.openelisglobal.login.controller.ChangePasswordLoginController();
    }

    @Bean
    @Profile("test")
    public PagingProperties pagingProperties() {
        return new PagingProperties();
    }

    @Bean("daemonSystemUser")
    @Profile("test")
    public org.openelisglobal.systemuser.valueholder.SystemUser daemonSystemUser() {
        org.openelisglobal.systemuser.valueholder.SystemUser user = new org.openelisglobal.systemuser.valueholder.SystemUser();
        user.setId("1");
        user.setLoginName("daemon");
        user.setFirstName("System");
        user.setLastName("Daemon");
        return user;
    }

    @Bean("daemonSysUserId")
    @Profile("test")
    public String daemonSysUserId() {
        return "1";
    }

    @Bean
    @Profile("test")
    public DataExportService dataExportService() {
        return mock(DataExportService.class);
    }

    @Bean
    @Profile("test")
    public DataExportTaskService dataExportTaskService() {
        return mock(DataExportTaskService.class);
    }

    @Bean
    @Profile("test")
    public DataExportTaskDAO dataExportTaskDAO() {
        return mock(DataExportTaskDAO.class);
    }
}
