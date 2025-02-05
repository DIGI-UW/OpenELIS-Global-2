package org.openelisglobal;

import static org.mockito.Mockito.mock;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.apache.http.impl.client.CloseableHttpClient;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.citystatezip.service.CityStateZipService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.util.Versioning;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.externalconnections.service.BasicAuthenticationDataService;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.localization.dao.LocalizationDAO;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.notification.service.AnalysisNotificationConfigService;
import org.openelisglobal.notification.service.TestNotificationConfigService;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.program.service.ImmunohistochemistrySampleService;
import org.openelisglobal.referral.service.ReferralResultService;
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.service.ReferralSetService;
import org.openelisglobal.requester.service.RequesterTypeService;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.sampleorganization.service.SampleOrganizationService;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.systemusersection.service.SystemUserSectionService;
import org.openelisglobal.test.dao.TestDAO;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.userrole.service.UserRoleService;
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
        "org.openelisglobal.person", "org.openelisglobal.dictionary.controller.rest",
        "org.openelisglobal.dictionary.service", "org.openelisglobal.dictionarycategory.service",
        "org.openelisglobal.dictionary.daoimpl", "org.openelisglobal.dictionarycategory.daoimpl",
        "org.openelisglobal.audittrail.daoimpl", "org.openelisglobal.referencetables.service",
        "org.openelisglobal.referencetables.daoimpl", "org.openelisglobal.history.service",
        "org.openelisglobal.menu.service", "org.openelisglobal.menu.daoimpl", "org.openelisglobal.login.daoimpl",
        "org.openelisglobal.systemusermodule.service", "org.openelisglobal.rolemodule.service",
        "org.openelisglobal.systemusermodule.daoimpl", "org.openelisglobal.systemusermodule.service",
        "org.openelisglobal.login.service", "org.openelisglobal.view", "org.openelisglobal.search.service",
        "org.openelisglobal.sample.daoimpl", "org.openelisglobal.common.util", "org.openelisglobal.login.service",
        "org.openelisglobal.view", "org.openelisglobal.search.service", "org.openelisglobal.sample",
        "org.openelisglobal.sampleitem.", "org.openelisglobal.analysis", "org.openelisglobal.result.service",
        "org.openelisglobal.result.daoimpl", "org.openelisglobal.resultlimit", "org.openelisglobal.resultlimits",
        "org.openelisglobal.typeoftestresult", "org.openelisglobal.samplehuman", "org.openelisglobal.provider",
        "org.openelisglobal.role", "org.openelisglobal.organization", "org.openelisglobal.region.service",
        "org.openelisglobal.region.dao", "org.openelisglobal.program.service", "org.openelisglobal.program.dao",
        "org.openelisglobal.systemuser.service", "org.openelisglobal.systemuser.daoimpl" }, excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.patient.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.provider.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.organization.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.sample.controller.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.dictionary.controller.*.java"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.config.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.fhir.*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org.openelisglobal.*.fhir.*") })
@EnableWebMvc
public class AppTestConfig implements WebMvcConfigurer {

    // mock Beans
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
    public LocalizationServiceImpl localise() {
        return mock(LocalizationServiceImpl.class);
    }

    @Bean()
    @Profile("test")
    public FhirTransformService fhirTransformService() {
        return mock(FhirTransformService.class);
    }

    @Bean()
    @Profile("test")
    public PanelService panelService() {
        return mock(PanelService.class);
    }

    @Bean()
    @Profile("test")
    public PanelItemService panelItemService() {
        return mock(PanelItemService.class);
    }

    @Bean()
    @Profile("test")
    public SystemUserSectionService stemUserSectionService() {
        return mock(SystemUserSectionService.class);
    }

    @Bean()
    @Profile("test")
    public TestAnalyteService testAnalyteService() {
        return mock(TestAnalyteService.class);
    }

    @Bean()
    @Profile("test")
    public LocalizationDAO localiseDao() {
        return mock(LocalizationDAO.class);
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
    public NoteService noteServiceoteService() {
        return mock(NoteService.class);
    }

    @Bean()
    @Profile("test")
    public SampleQaEventService sampleQaEventService() {
        return mock(SampleQaEventService.class);
    }

    @Bean()
    @Profile("test")
    public SampleRequesterService sampleRequesterService() {
        return mock(SampleRequesterService.class);
    }

    @Bean()
    @Profile("test")
    public RequesterTypeService requesterTypeService() {
        return mock(RequesterTypeService.class);
    }

    @Bean()
    @Profile("test")
    public BasicAuthenticationDataService basicAuthenticationDataService() {
        return mock(BasicAuthenticationDataService.class);
    }

    @Bean()
    @Profile("test")
    public ObservationHistoryService observationHistoryService() {
        return mock(ObservationHistoryService.class);
    }

    @Bean()
    @Profile("test")
    public TestSectionService testSectionService() {
        return mock(TestSectionService.class);
    }

    @Bean()
    @Profile("test")
    public UserRoleService userRoleService() {
        return mock(UserRoleService.class);
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
    public ElectronicOrderService electronicOrderService() {
        return mock(ElectronicOrderService.class);
    }

    @Bean()
    @Profile("test")
    public AnalysisNotificationConfigService analysisNotificationConfigService() {
        return mock(AnalysisNotificationConfigService.class);
    }

    @Bean()
    @Profile("test")
    public ImmunohistochemistrySampleService immunohistochemistrySampleService() {
        return mock(ImmunohistochemistrySampleService.class);
    }

    @Bean()
    @Profile("test")
    public ObservationHistoryTypeService observationHistoryTypeService() {
        return mock(ObservationHistoryTypeService.class);
    }

    @Bean()
    @Profile("test")
    public SampleOrganizationService sampleOrganizationService() {
        return mock(SampleOrganizationService.class);
    }

    @Bean()
    @Profile("test")
    public ReferralResultService ReferralResultService() {
        return mock(ReferralResultService.class);
    }

    @Bean()
    @Profile("test")
    public CityStateZipService cityStateZipService() {
        return mock(CityStateZipService.class);
    }

    @Bean()
    @Profile("test")
    public ReferralService referralService() {
        return mock(ReferralService.class);
    }

    @Bean()
    @Profile("test")
    public ReferralSetService ReferralSetService() {
        return mock(ReferralSetService.class);
    }

    @Bean()
    @Profile("test")
    public TestServiceImpl testServiceImpl() {
        return mock(TestServiceImpl.class);
    }

    @Bean()
    @Profile("test")
    public AuditTrailService auditTrailService() {
        return mock(AuditTrailService.class);
    }

    @Bean()
    @Profile("test")
    public TestDAO testDao() {
        return mock(TestDAO.class);
    }

    @Bean()
    @Profile("test")
    public TestResultService testResultService() {
        return mock(TestResultService.class);
    }

    @Bean()
    @Profile("test")
    public TypeOfSampleTestService typeOfSampleTestService() {
        return mock(TypeOfSampleTestService.class);
    }

    @Bean()
    @Profile("test")
    public TypeOfSampleService typeOfSample() {
        return mock(TypeOfSampleService.class);
    }

    @Bean()
    @Profile("test")
    public Versioning versioning() {
        return mock(Versioning.class);
    }

    @Bean()
    @Profile("test")
    public SiteInformationService siteInformationService() {
        return mock(SiteInformationService.class);
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

    @Bean()
    @Profile("test")
    public StatusOfSampleService statusOfSampleService() {
        return mock(StatusOfSampleService.class);
    }

    @Override
    public void configureMessageConverters(@NonNull List<HttpMessageConverter<?>> converters) {
        WebMvcConfigurer.super.configureMessageConverters(converters);
        converters.add(new StringHttpMessageConverter());
        converters.add(jsonConverter());
    }
}
