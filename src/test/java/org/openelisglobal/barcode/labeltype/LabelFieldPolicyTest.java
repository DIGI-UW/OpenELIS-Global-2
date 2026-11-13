package org.openelisglobal.barcode.labeltype;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.labelpreset.valueholder.LabelFieldKey;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-1218: the legacy labels print what the system preset's content fields
 * say, and fall back to the legacy site_information keys only where the preset
 * gives no answer.
 */
@RunWith(MockitoJUnitRunner.class)
public class LabelFieldPolicyTest {

    @Mock
    private AutowireCapableBeanFactory beanFactory;

    @Mock
    private DefaultConfigurationProperties configurationProperties;

    private AutowireCapableBeanFactory previousFactory;

    @Before
    public void setUp() {
        previousFactory = (AutowireCapableBeanFactory) ReflectionTestUtils.getField(SpringContext.class, "factory");
        ReflectionTestUtils.setField(SpringContext.class, "factory", beanFactory);
        when(beanFactory.getBean(DefaultConfigurationProperties.class)).thenReturn(configurationProperties);
        when(configurationProperties.getPropertyValue(any(Property.class))).thenReturn("true");
    }

    @After
    public void tearDown() {
        LabelFieldPolicy.useLookup(null);
        ReflectionTestUtils.setField(SpringContext.class, "factory", previousFactory);
    }

    @Test
    public void thePresetFieldsDecideWhenThePresetAnswers() {
        LabelFieldPolicy.useLookup(name -> keys("LAB_NUMBER", "PATIENT_ID", "CASE_NUMBER"));

        Set<String> fields = LabelFieldPolicy.printedFields(LabelFieldPolicy.BLOCK);

        assertTrue(LabelFieldPolicy.prints(fields, LabelFieldKey.PATIENT_ID, Property.BLOCK_LABEL_FIELD_PATIENT_ID));
        assertFalse("the legacy key says true, the preset left the field off",
                LabelFieldPolicy.prints(fields, LabelFieldKey.SPECIMEN_TYPE, Property.BLOCK_LABEL_FIELD_SPECIMEN_TYPE));
    }

    @Test
    public void aPresetThatStillCarriesLabNumberAloneLeavesTheLegacyKeysInCharge() {
        LabelFieldPolicy.useLookup(name -> keys("LAB_NUMBER"));
        when(configurationProperties.getPropertyValue(Property.BLOCK_LABEL_FIELD_SPECIMEN_TYPE)).thenReturn("false");

        Set<String> fields = LabelFieldPolicy.printedFields(LabelFieldPolicy.BLOCK);

        assertNull(fields);
        assertTrue(LabelFieldPolicy.prints(fields, LabelFieldKey.PATIENT_ID, Property.BLOCK_LABEL_FIELD_PATIENT_ID));
        assertFalse(
                LabelFieldPolicy.prints(fields, LabelFieldKey.SPECIMEN_TYPE, Property.BLOCK_LABEL_FIELD_SPECIMEN_TYPE));
    }

    @Test
    public void anUnknownPresetOrAFailingLookupFallsBackToTheLegacyKeys() {
        LabelFieldPolicy.useLookup(name -> null);
        assertNull(LabelFieldPolicy.printedFields(LabelFieldPolicy.SLIDE));

        LabelFieldPolicy.useLookup(name -> {
            throw new IllegalStateException("no context");
        });
        assertNull(LabelFieldPolicy.printedFields(LabelFieldPolicy.SLIDE));
        assertTrue(LabelFieldPolicy.prints(null, LabelFieldKey.SLIDE_ID, Property.SLIDE_LABEL_FIELD_SLIDE_ID));
    }

    @Test
    public void withoutAPresetServiceTheContextLookupAnswersNothing() {
        assertNull("the mocked factory has no LabelPresetService bean",
                LabelFieldPolicy.printedFields(LabelFieldPolicy.FREEZER));
    }

    private static Set<String> keys(String... values) {
        return new LinkedHashSet<>(List.of(values));
    }
}
