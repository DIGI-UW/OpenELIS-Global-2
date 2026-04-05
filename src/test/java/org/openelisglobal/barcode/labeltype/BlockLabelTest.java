package org.openelisglobal.barcode.labeltype;

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
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class BlockLabelTest {

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
        when(configurationProperties.getPropertyValue(any(Property.class))).thenAnswer(invocation -> {
            Property property = invocation.getArgument(0);
            switch (property) {
            case BLOCK_LABEL_BARCODE_WIDTH:
            case BLOCK_LABEL_BARCODE_HEIGHT:
                return "2";
            case BLOCK_LABEL_FIELD_PATIENT_ID:
                return "false";
            case BLOCK_LABEL_FIELD_BLOCK_ID:
            case BLOCK_LABEL_FIELD_SPECIMEN_TYPE:
            case BLOCK_LABEL_FIELD_CASE_NUMBER:
                return "true";
            case MAX_BLOCK_LABEL_PRINTED:
                return "10";
            default:
                return "";
            }
        });
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
    public void blockLabel_includesConfiguredSpecimenTypeAndCaseNumber() {
        PathologySample pathologySample = new PathologySample();
        pathologySample.setId(55);

        PathologyBlock block = new PathologyBlock();
        block.setBlockNumber(3);

        BlockLabel label = new BlockLabel(null, new Sample(), pathologySample, block, "ACC-1", "Biopsy");

        List<LabelField> fields = collect(label.getAboveFields());
        assertTrue(fields.stream().anyMatch(field -> "Biopsy".equals(field.getValue())));
        assertTrue(fields.stream().anyMatch(field -> "55".equals(field.getValue())));
        assertTrue(fields.stream().anyMatch(field -> "3".equals(field.getValue())));
    }

    private List<LabelField> collect(Iterable<LabelField> fields) {
        List<LabelField> list = new ArrayList<>();
        for (LabelField field : fields) {
            list.add(field);
        }
        return list;
    }
}
