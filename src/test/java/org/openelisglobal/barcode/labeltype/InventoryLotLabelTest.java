package org.openelisglobal.barcode.labeltype;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class InventoryLotLabelTest {

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

    private void stubLabelStock(String width, String height) {
        when(configurationProperties.getPropertyValue(any(Property.class))).thenAnswer(invocation -> {
            switch ((Property) invocation.getArgument(0)) {
            case STORAGE_LOCATION_LABEL_BARCODE_WIDTH:
                return width;
            case STORAGE_LOCATION_LABEL_BARCODE_HEIGHT:
                return height;
            default:
                return "";
            }
        });
    }

    @Test
    public void lotLabel_usesTheConfiguredLabelStockSize() {
        stubLabelStock("4", "2");

        InventoryLotLabel label = new InventoryLotLabel("Test Reagent A", "LOT-2025-001", "2099-12-31", "LOT-BC-1000");

        assertEquals(4.0f, label.getWidth(), 0.001f);
        assertEquals(2.0f, label.getHeight(), 0.001f);
    }

    @Test
    public void lotLabel_fallsBackTo3x1_whenNoSizeIsConfigured() {
        stubLabelStock("", null);

        InventoryLotLabel label = new InventoryLotLabel("Test Reagent A", "LOT-2025-001", "2099-12-31", "LOT-BC-1000");

        assertEquals(3.0f, label.getWidth(), 0.001f);
        assertEquals(1.0f, label.getHeight(), 0.001f);
    }
}
