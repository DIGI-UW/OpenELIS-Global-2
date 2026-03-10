package org.openelisglobal.barcode;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.barcode.labeltype.Label;
import org.openelisglobal.barcode.service.BarcodeLabelInfoService;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class BarcodeLabelMakerTest {

    @Mock
    private AutowireCapableBeanFactory beanFactory;

    @Mock
    private DefaultConfigurationProperties configurationProperties;

    @Mock
    private MessageSource messageSource;

    @Mock
    private SampleService sampleService;

    @Mock
    private SampleItemService sampleItemService;

    @Mock
    private PathologySampleService pathologySampleService;

    @Mock
    private BarcodeLabelInfoService barcodeLabelInfoService;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", beanFactory);

        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
        when(beanFactory.getBean(SampleService.class)).thenReturn(sampleService);
        when(beanFactory.getBean(SampleItemService.class)).thenReturn(sampleItemService);
        when(beanFactory.getBean(PathologySampleService.class)).thenReturn(pathologySampleService);
        when(beanFactory.getBean(BarcodeLabelInfoService.class)).thenReturn(barcodeLabelInfoService);
        when(barcodeLabelInfoService.getDataByCode(anyString())).thenReturn(null);

        when(configurationProperties.getPropertyValue(any(Property.class))).thenAnswer(invocation -> {
            Property property = invocation.getArgument(0);
            switch (property) {
            case BAR_CODE_TYPE:
                return "BARCODE";
            case BLOCK_LABEL_BARCODE_WIDTH:
            case BLOCK_LABEL_BARCODE_HEIGHT:
                return "2";
            case BLOCK_LABEL_FIELD_PATIENT_ID:
            case BLOCK_LABEL_FIELD_SPECIMEN_TYPE:
            case BLOCK_LABEL_FIELD_CASE_NUMBER:
                return "false";
            case BLOCK_LABEL_FIELD_BLOCK_ID:
                return "true";
            case MAX_BLOCK_LABEL_PRINTED:
                return "10";
            default:
                return "";
            }
        });

        when(messageSource.getMessage(anyString(), any(), anyString(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("date.format".equals(key) || "date.format.formatKey".equals(key)) {
                return "MM/dd/yyyy";
            }
            if ("dateTime.format".equals(key) || "timestamp.format.formatKey".equals(key)) {
                return "MM/dd/yyyy HH:mm";
            }
            if ("time.format".equals(key) || "time.format.formatKey".equals(key)) {
                return "HH:mm";
            }
            if ("timestamp.format.formatKey.12".equals(key)) {
                return "MM/dd/yyyy hh:mm a";
            }
            return invocation.getArgument(2);
        });
        MessageUtil.setMessageSource(messageSource);

        Sample sample = new Sample();
        sample.setId("S-1");
        sample.setAccessionNumber("ACC-1");
        when(sampleService.getSampleByAccessionNumber("ACC-1")).thenReturn(sample);
        when(sampleService.getPatient(sample)).thenReturn(null);

        PathologyBlock block = new PathologyBlock();
        block.setId(10);
        block.setBlockNumber(1);
        PathologySample pathologySample = new PathologySample();
        pathologySample.setId(99);
        pathologySample.setBlocks(new java.util.ArrayList<>());
        pathologySample.getBlocks().add(block);
        when(pathologySampleService.getAllMatching("sample.id", "S-1"))
                .thenReturn(java.util.Collections.singletonList(pathologySample));

        SampleItem sampleItem = new SampleItem();
        when(sampleItemService.getSampleItemsBySampleId("S-1")).thenReturn(java.util.Collections.singletonList(sampleItem));
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", null);
    }

    @Test
    public void generateLabels_blocksOverMaxWhenOverrideIsFalse() {
        BarcodeLabelMaker labelMaker = new BarcodeLabelMaker();

        labelMaker.generateLabels("ACC-1", "blockOrder", "11", "false");

        assertEquals(0, getQueuedLabels(labelMaker).size());
    }

    @Test
    public void generateLabels_allowsOverMaxWhenOverrideIsTrue() {
        BarcodeLabelMaker labelMaker = new BarcodeLabelMaker();

        labelMaker.generateLabels("ACC-1", "blockOrder", "11", "true");

        assertEquals(1, getQueuedLabels(labelMaker).size());
    }

    @SuppressWarnings("unchecked")
    private ArrayList<Label> getQueuedLabels(BarcodeLabelMaker labelMaker) {
        return (ArrayList<Label>) ReflectionTestUtils.getField(labelMaker, "labels");
    }
}
