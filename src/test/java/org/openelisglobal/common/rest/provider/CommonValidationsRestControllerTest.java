package org.openelisglobal.common.rest.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator;
import org.openelisglobal.common.provider.validation.IAccessionNumberValidator.ValidationResults;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Standalone unit tests for CommonValidationsRestController.
 *
 * Verifies request validation, response mapping, and state isolation across
 * sequential requests.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommonValidationsRestControllerTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private SearchResultsService searchResultsService;

    @Mock
    private AccessionNumberValidatorFactory validatorFactory;

    @Mock
    private IAccessionNumberValidator accessionValidator;

    @Mock
    private IAccessionNumberGenerator accessionGenerator;

    @Mock
    private DefaultConfigurationProperties configProperties;

    @InjectMocks
    private CommonValidationsRestController controller;

    private MockMvc mockMvc;
    private AutowireCapableBeanFactory originalFactory;

    private Object originalMessageUtil;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        originalFactory = (AutowireCapableBeanFactory) ReflectionTestUtils.getField(SpringContext.class, "factory");
        originalMessageUtil = ReflectionTestUtils.getField(MessageUtil.class, "instance");
        AutowireCapableBeanFactory mockFactory = mock(AutowireCapableBeanFactory.class);
        ReflectionTestUtils.setField(SpringContext.class, "factory", mockFactory);

        when(mockFactory.getBean(AccessionNumberValidatorFactory.class)).thenReturn(validatorFactory);
        when(mockFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configProperties);

        when(validatorFactory.getValidator(any(AccessionFormat.class))).thenReturn(accessionValidator);
        when(validatorFactory.getGenerator(any(AccessionFormat.class))).thenReturn(accessionGenerator);

        // Configure internationalized messages
        org.springframework.context.MessageSource messageSource = mock(org.springframework.context.MessageSource.class);
        when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class))).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            Object[] args = invocation.getArgument(1);
            if ("errors.invalid".equals(code)) {
                return "Invalid " + (args != null && args.length > 0 ? args[0] : "");
            }
            if ("phone.number.format.error".equals(code)) {
                return "Invalid phone format: " + (args != null && args.length > 0 ? args[0] : "");
            }
            if ("alert.error".equals(code)) {
                return "Error";
            }
            if ("alert.warning".equals(code)) {
                return "Warning";
            }
            if ("error.duplicate.subjectNumber.warning".equals(code)) {
                return "Duplicate subject number found";
            }
            return code;
        });
        MessageUtil.setMessageSource(messageSource);

        // Default configuration properties
        when(configProperties.getPropertyValue(ConfigurationProperties.Property.PHONE_FORMAT)).thenReturn("ddd-dddd");
        when(configProperties.isPropertyValueEqual(ConfigurationProperties.Property.VALIDATE_PHONE_FORMAT, "false"))
                .thenReturn(false);
        when(configProperties.isPropertyValueEqual(ConfigurationProperties.Property.ALLOW_DUPLICATE_SUBJECT_NUMBERS,
                "true")).thenReturn(false);
        when(configProperties.isPropertyValueEqual(ConfigurationProperties.Property.ALLOW_DUPLICATE_NATIONAL_IDS,
                "true")).thenReturn(false);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", originalFactory);
        ReflectionTestUtils.setField(MessageUtil.class, "instance", originalMessageUtil);
    }

    // -------------------------------------------------------------------------
    // State Isolation Regression Test
    // -------------------------------------------------------------------------

    @Test
    public void stateIsolation_subsequentFailedRequest_doesNotRetainPriorSuccessStatus() throws Exception {
        // First request: programCode found, succeeds
        Project project = new Project();
        project.setProgramCode("HIV");
        when(projectService.getAllProjects()).thenReturn(Collections.singletonList(project));
        when(accessionGenerator.getNextAvailableAccessionNumber(eq("HIV"), anyBoolean())).thenReturn("HIV-0001");

        mockMvc.perform(get("/rest/SampleEntryGenerateScanProvider").param("programCode", "HIV"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.body").value("HIV-0001"));

        // Second request: programCode unknown, fails
        mockMvc.perform(get("/rest/SampleEntryGenerateScanProvider").param("programCode", "NONEXISTENT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.body").value("Invalid program.code"));
    }

    // -------------------------------------------------------------------------
    // Accession Number Validation Endpoint Tests
    // -------------------------------------------------------------------------

    @Test
    public void getAccessionNumberValidation_validFormat_returnsSuccess() throws Exception {
        when(accessionValidator.validFormat("2026-0001", true)).thenReturn(ValidationResults.SUCCESS);

        mockMvc.perform(get("/rest/SampleEntryAccessionNumberValidation")
                .param("accessionNumber", "2026-0001")
                .param("ignoreUsage", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.body").value("Valid accession number"));
    }

    @Test
    public void getAccessionNumberValidation_invalidFormat_returnsFailure() throws Exception {
        when(accessionValidator.validFormat("BAD_ACCESSION", true)).thenReturn(ValidationResults.FORMAT_FAIL);
        when(accessionValidator.getInvalidFormatMessage(ValidationResults.FORMAT_FAIL)).thenReturn("Invalid format message");

        mockMvc.perform(get("/rest/SampleEntryAccessionNumberValidation")
                .param("accessionNumber", "BAD_ACCESSION")
                .param("ignoreUsage", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.body").value("Invalid format message"));
    }

    // -------------------------------------------------------------------------
    // Accession Number Generator Endpoint Tests
    // -------------------------------------------------------------------------

    @Test
    public void accessionNumberGenerator_withoutProgramCode_returnsNextNumber() throws Exception {
        when(accessionGenerator.getNextAvailableAccessionNumber(eq(""), eq(true))).thenReturn("2026-00001");

        mockMvc.perform(get("/rest/SampleEntryGenerateScanProvider"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.body").value("2026-00001"));
    }

    @Test
    public void accessionNumberGenerator_withValidProgramCode_returnsProgramNextNumber() throws Exception {
        Project project = new Project();
        project.setProgramCode("TB");
        when(projectService.getAllProjects()).thenReturn(Collections.singletonList(project));
        when(accessionGenerator.getNextAvailableAccessionNumber(eq("TB"), eq(true))).thenReturn("TB-00001");

        mockMvc.perform(get("/rest/SampleEntryGenerateScanProvider").param("programCode", "TB"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.body").value("TB-00001"));
    }

    @Test
    public void accessionNumberGenerator_withInvalidProgramCode_returnsError() throws Exception {
        when(projectService.getAllProjects()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/rest/SampleEntryGenerateScanProvider").param("programCode", "UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.body").value("Invalid program.code"));
    }

    // -------------------------------------------------------------------------
    // Phone Number Validation Endpoint Tests
    // -------------------------------------------------------------------------

    @Test
    public void getPhoneNumberValidation_validNumber_returnsSuccess() throws Exception {
        mockMvc.perform(get("/rest/PhoneNumberValidationProvider").param("value", "123-4567"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.body").value("Valid phone number"));
    }

    @Test
    public void getPhoneNumberValidation_invalidNumber_returnsFailure() throws Exception {
        mockMvc.perform(get("/rest/PhoneNumberValidationProvider").param("value", "invalid-phone"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.body").value("Invalid phone format: ddd-dddd"));
    }

    // -------------------------------------------------------------------------
    // Subject Number Validation Endpoint Tests
    // -------------------------------------------------------------------------

    @Test
    public void validateSubjectNumber_uniqueSubjectNumber_returnsValid() throws Exception {
        when(searchResultsService.getSearchResultsExact(any(), any(), any(), eq("SUBJ-101"), any(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/rest/subjectNumberValidationProvider")
                .param("numberType", "subjectNumber")
                .param("subjectNumber", "SUBJ-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(true))
                .andExpect(jsonPath("$.body").value("Valid"));
    }

    @Test
    public void validateSubjectNumber_duplicateSubjectNumber_returnsFailure() throws Exception {
        PatientSearchResults result = new PatientSearchResults();
        when(searchResultsService.getSearchResultsExact(any(), any(), any(), eq("SUBJ-DUPE"), any(), any(), any(),
                any(), any(), any())).thenReturn(Collections.singletonList(result));

        mockMvc.perform(get("/rest/subjectNumberValidationProvider").param("numberType", "subjectNumber")
                .param("subjectNumber", "SUBJ-DUPE")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(false))
                .andExpect(jsonPath("$.body").value("fail#Error: Duplicate subject number found"));
    }

    @Test
    public void validateSubjectNumber_withoutNumberType_returnsFailure() throws Exception {
        mockMvc.perform(get("/rest/subjectNumberValidationProvider").param("subjectNumber", "SUBJ-101"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(false));
    }
}
