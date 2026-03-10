package org.openelisglobal.barcode.labeltype;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class FreezerLabelTest {

    @Mock
    private AutowireCapableBeanFactory beanFactory;

    @Mock
    private DefaultConfigurationProperties configurationProperties;

    @Mock
    private MessageSource messageSource;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", beanFactory);
        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
        when(messageSource.getMessage(anyString(), any(), anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MessageUtil.setMessageSource(messageSource);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", null);
    }

    @Test
    public void freezerLabel_rendersConfiguredOptionalFields() {
        when(configurationProperties.getPropertyValue(any(Property.class))).thenAnswer(invocation -> {
            Property property = invocation.getArgument(0);
            switch (property) {
            case FREEZER_LABEL_BARCODE_WIDTH:
            case FREEZER_LABEL_BARCODE_HEIGHT:
                return "2";
            case FREEZER_LABEL_FIELD_PATIENT_ID:
            case FREEZER_LABEL_FIELD_STORAGE_LOCATION:
            case FREEZER_LABEL_FIELD_SPECIMEN_TYPE:
            case FREEZER_LABEL_FIELD_COLLECTION_DATE:
            case FREEZER_LABEL_FIELD_EXPIRY_DATE:
                return "true";
            case MAX_FREEZER_LABEL_PRINTED:
                return "10";
            default:
                return "";
            }
        });

        FreezerLabel label = new FreezerLabel("ACC-1.1", "P-100", "Rack A1", "Blood", "2026-01-01", "2026-02-01");
        List<LabelField> aboveFields = collect(label.getAboveFields());
        List<LabelField> belowFields = collect(label.getBelowFields());

        assertTrue(aboveFields.stream().anyMatch(field -> "P-100".equals(field.getValue())));
        assertTrue(aboveFields.stream().anyMatch(field -> "Rack A1".equals(field.getValue())));
        assertTrue(aboveFields.stream().anyMatch(field -> "Blood".equals(field.getValue())));
        assertTrue(belowFields.stream().anyMatch(field -> "2026-01-01".equals(field.getValue())));
        assertTrue(belowFields.stream().anyMatch(field -> "2026-02-01".equals(field.getValue())));
    }

    @Test
    public void freezerLabel_skipsDisabledOptionalFields() {
        when(configurationProperties.getPropertyValue(any(Property.class))).thenAnswer(invocation -> {
            Property property = invocation.getArgument(0);
            switch (property) {
            case FREEZER_LABEL_BARCODE_WIDTH:
            case FREEZER_LABEL_BARCODE_HEIGHT:
                return "2";
            case FREEZER_LABEL_FIELD_PATIENT_ID:
            case FREEZER_LABEL_FIELD_STORAGE_LOCATION:
            case FREEZER_LABEL_FIELD_COLLECTION_DATE:
            case FREEZER_LABEL_FIELD_EXPIRY_DATE:
                return "true";
            case FREEZER_LABEL_FIELD_SPECIMEN_TYPE:
                return "false";
            case MAX_FREEZER_LABEL_PRINTED:
                return "10";
            default:
                return "";
            }
        });

        FreezerLabel label = new FreezerLabel("ACC-1.1", "P-100", "Rack A1", "Blood", "2026-01-01", "2026-02-01");
        List<LabelField> aboveFields = collect(label.getAboveFields());

        assertFalse(aboveFields.stream().anyMatch(field -> "Blood".equals(field.getValue())));
    }

    private List<LabelField> collect(Iterable<LabelField> fields) {
        List<LabelField> list = new ArrayList<>();
        for (LabelField field : fields) {
            list.add(field);
        }
        return list;
    }
}
