package org.openelisglobal.patient.saving;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.sample.util.CI.IProjectFormMapper;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class AccessionerTest {

    @Mock
    private AutowireCapableBeanFactory beanFactory;
    @Mock
    private IStatusService statusService;
    @Mock
    private ReferenceTablesService referenceTablesService;
    @Mock
    private DefaultConfigurationProperties configurationProperties;
    @Mock
    private MessageSource messageSource;
    @Mock
    private PatientService patientService;
    @Mock
    private IProjectFormMapper projectFormMapper;

    private AutowireCapableBeanFactory previousFactory;
    private Object previousMessageUtilInstance;

    @Before
    public void setUp() {
        previousFactory = (AutowireCapableBeanFactory) ReflectionTestUtils.getField(SpringContext.class, "factory");
        previousMessageUtilInstance = ReflectionTestUtils.getField(MessageUtil.class, "instance");
        ReflectionTestUtils.setField(SpringContext.class, "factory", beanFactory);

        ReferenceTables sampleTable = new ReferenceTables();
        sampleTable.setId("1");
        lenient().when(beanFactory.getBean(ReferenceTablesService.class)).thenReturn(referenceTablesService);
        lenient().when(referenceTablesService.getReferenceTableByName("sample")).thenReturn(sampleTable);
        lenient().when(beanFactory.getBean(IStatusService.class)).thenReturn(statusService);
        lenient().when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
        lenient().when(beanFactory.getBean(PatientService.class)).thenReturn(patientService);
        lenient().when(configurationProperties.getPropertyValue(any(ConfigurationProperties.Property.class)))
                .thenReturn("X");
        lenient().when(messageSource.getMessage(anyString(), any(), anyString(), any())).thenReturn("MM/dd/yyyy");
        MessageUtil.setMessageSource(messageSource);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", previousFactory);
        ReflectionTestUtils.setField(MessageUtil.class, "instance", previousMessageUtilInstance);
    }

    @Test
    public void findPatientByIndentifiers_usesNationalIdWhenPatientIdentifierIsPresent() {
        Accessioner accessioner = createAccessioner("SUBJ-100");
        Patient expected = new Patient();
        when(patientService.getPatientByNationalId("SUBJ-100")).thenReturn(expected);

        Patient actual = accessioner.findPatientByIndentifiers();

        assertSame(expected, actual);
        verify(patientService).getPatientByNationalId("SUBJ-100");
        verify(patientService, never()).getPatientByExternalId(any());
    }

    @Test
    public void findPatientByIndentifiers_fallsBackToExternalIdWhenPatientIdentifierIsNull() {
        Accessioner accessioner = createAccessioner(null);
        Patient expected = new Patient();
        when(projectFormMapper.getSiteSubjectNumber()).thenReturn("SITE-200");
        when(patientService.getPatientByExternalId("SITE-200")).thenReturn(expected);

        Patient actual = accessioner.findPatientByIndentifiers();

        assertSame(expected, actual);
        verify(patientService, never()).getPatientByNationalId(any());
        verify(patientService).getPatientByExternalId("SITE-200");
    }

    @Test
    public void findPatientByIndentifiers_fallsBackToExternalIdWhenPatientIdentifierIsNonInternedEmptyString() {
        Accessioner accessioner = createAccessioner(new String(""));
        Patient expected = new Patient();
        when(projectFormMapper.getSiteSubjectNumber()).thenReturn("SITE-200");
        when(patientService.getPatientByExternalId("SITE-200")).thenReturn(expected);

        Patient actual = accessioner.findPatientByIndentifiers();

        assertSame(expected, actual);
        verify(patientService, never()).getPatientByNationalId(any());
        verify(patientService).getPatientByExternalId("SITE-200");
    }

    private Accessioner createAccessioner(String patientIdentifier) {
        Accessioner accessioner = new Accessioner("ACC-1", patientIdentifier, "SITE-200", "1") {
            @Override
            public boolean canAccession() {
                return true;
            }

            @Override
            protected void populateSampleData() {
            }

            @Override
            protected String getActionLabel() {
                return "test";
            }
        };
        accessioner.projectFormMapper = projectFormMapper;
        return accessioner;
    }
}
