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
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class SlideLabelTest {

    @Mock
    private AutowireCapableBeanFactory beanFactory;

    @Mock
    private DefaultConfigurationProperties configurationProperties;

    @Mock
    private MessageSource messageSource;

    private AutowireCapableBeanFactory previousFactory;
    private Object previousMessageUtilInstance;

    @Before
    public void setUp() {
        previousFactory = (AutowireCapableBeanFactory) ReflectionTestUtils.getField(SpringContext.class, "factory");
        previousMessageUtilInstance = ReflectionTestUtils.getField(MessageUtil.class, "instance");
        ReflectionTestUtils.setField(SpringContext.class, "factory", beanFactory);
        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
        when(messageSource.getMessage(anyString(), any(), anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MessageUtil.setMessageSource(messageSource);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", previousFactory);
        ReflectionTestUtils.setField(MessageUtil.class, "instance", previousMessageUtilInstance);
    }

    @Test
    public void slideLabel_includesOptionalFieldsWhenEnabled() {
        when(configurationProperties.getPropertyValue(any(Property.class))).thenAnswer(invocation -> {
            Property property = invocation.getArgument(0);
            switch (property) {
            case SLIDE_LABEL_BARCODE_WIDTH:
            case SLIDE_LABEL_BARCODE_HEIGHT:
                return "2";
            case SLIDE_LABEL_FIELD_PATIENT_ID:
                return "false";
            case SLIDE_LABEL_FIELD_SLIDE_ID:
            case SLIDE_LABEL_FIELD_STAIN_TYPE:
            case SLIDE_LABEL_FIELD_BLOCK_ID:
            case SLIDE_LABEL_FIELD_CASE_NUMBER:
                return "true";
            case MAX_SLIDE_LABEL_PRINTED:
                return "10";
            default:
                return "";
            }
        });

        PathologySlide slide = new PathologySlide();
        slide.setSlideNumber(7);

        SlideLabel label = new SlideLabel(null, new Sample(), new PathologySample(), slide, "ACC-1", "H&E", "B-4", "C-9");
        List<LabelField> fields = collect(label.getAboveFields());

        assertTrue(fields.stream().anyMatch(field -> "7".equals(field.getValue())));
        assertTrue(fields.stream().anyMatch(field -> "H&E".equals(field.getValue())));
        assertTrue(fields.stream().anyMatch(field -> "B-4".equals(field.getValue())));
        assertTrue(fields.stream().anyMatch(field -> "C-9".equals(field.getValue())));
    }

    @Test
    public void slideLabel_omitsOptionalFieldsWhenDisabled() {
        when(configurationProperties.getPropertyValue(any(Property.class))).thenAnswer(invocation -> {
            Property property = invocation.getArgument(0);
            switch (property) {
            case SLIDE_LABEL_BARCODE_WIDTH:
            case SLIDE_LABEL_BARCODE_HEIGHT:
                return "2";
            case SLIDE_LABEL_FIELD_PATIENT_ID:
                return "false";
            case SLIDE_LABEL_FIELD_SLIDE_ID:
                return "true";
            case SLIDE_LABEL_FIELD_STAIN_TYPE:
            case SLIDE_LABEL_FIELD_BLOCK_ID:
            case SLIDE_LABEL_FIELD_CASE_NUMBER:
                return "false";
            case MAX_SLIDE_LABEL_PRINTED:
                return "10";
            default:
                return "";
            }
        });

        PathologySlide slide = new PathologySlide();
        slide.setSlideNumber(7);

        SlideLabel label = new SlideLabel(null, new Sample(), new PathologySample(), slide, "ACC-1", "H&E", "B-4", "C-9");
        List<LabelField> fields = collect(label.getAboveFields());

        assertTrue(fields.stream().anyMatch(field -> "7".equals(field.getValue())));
        assertFalse(fields.stream().anyMatch(field -> "H&E".equals(field.getValue())));
        assertFalse(fields.stream().anyMatch(field -> "B-4".equals(field.getValue())));
        assertFalse(fields.stream().anyMatch(field -> "C-9".equals(field.getValue())));
    }

    private List<LabelField> collect(Iterable<LabelField> fields) {
        List<LabelField> list = new ArrayList<>();
        for (LabelField field : fields) {
            list.add(field);
        }
        return list;
    }
}
