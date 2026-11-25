package org.openelisglobal.barcode.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.labelpreset.service.LabelQuantityRefusedException;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * A refused label request names the limit it broke, so the person printing
 * knows how far to reduce it.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class LabelQuantityPolicyTest {

    @Mock
    private AutowireCapableBeanFactory beanFactory;
    @Mock
    private DefaultConfigurationProperties configurationProperties;

    private Object previousFactory;

    @Before
    public void setUp() {
        previousFactory = ReflectionTestUtils.getField(SpringContext.class, "factory");
        ReflectionTestUtils.setField(SpringContext.class, "factory", beanFactory);
        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(SpringContext.class, "factory", previousFactory);
    }

    @Test
    public void aQuantityAboveTheConfiguredLimitIsRefusedWithThatLimit() {
        when(configurationProperties.getPropertyValue(Property.MAX_REQUEST_LABEL_QUANTITY)).thenReturn("12");

        LabelQuantityPolicy.require(12, 1);
        LabelQuantityRefusedException refusal = assertThrows(LabelQuantityRefusedException.class,
                () -> LabelQuantityPolicy.require(13, 1));

        assertEquals(LabelQuantityRefusedException.ABOVE_LIMIT, refusal.getMessageKey());
        assertEquals(Map.of("max", 12), refusal.getMessageArgs());
    }

    @Test
    public void aJobAboveTheDefaultTotalIsRefusedWithThatTotal() {
        LabelQuantityPolicy.requireTotal(1000);
        LabelQuantityRefusedException refusal = assertThrows(LabelQuantityRefusedException.class,
                () -> LabelQuantityPolicy.requireTotal(1001));

        assertEquals(LabelQuantityRefusedException.JOB_TOO_LARGE, refusal.getMessageKey());
        assertEquals(Map.of("max", 1000), refusal.getMessageArgs());
    }
}
