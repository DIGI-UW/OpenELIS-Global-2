package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties.OEProperties;
import org.openelisglobal.common.util.DefaultConfigurationProperties.PropertyHolder;

public class ConfigurationPropertyIsolationTest extends BaseWebContextSensitiveTest {

    @Test
    public void restoringASettingPreservesItsTranslationsForTheNextConfigurationWrite() {
        ConfigurationProperties configuration = ConfigurationProperties.getInstance();
        OEProperties properties = configuration.finalProperties;
        PropertyHolder original = properties.getPropertyHolder(Property.BILLING_REFERENCE_NUMBER_LABEL);
        assertNotNull(original);
        Properties writableBefore = properties.getPropertiesForWriting();

        configuration.setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL, "-1");
        assertEquals("-1", configuration.getPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL));

        restoreConfigurationProperties();

        assertSame(original, properties.getPropertyHolder(Property.BILLING_REFERENCE_NUMBER_LABEL));
        assertEquals(writableBefore, configuration.finalProperties.getPropertiesForWriting());
    }

    @Test
    public void aTestOnlyOverrideIsRemovedBeforeTheNextTest() {
        ConfigurationProperties configuration = ConfigurationProperties.getInstance();
        String propertyName = "test.configuration.isolation.only";
        assertFalse(configuration.finalProperties.containsKey(propertyName));

        configuration.finalProperties.setPropertyValue(propertyName, "temporary");
        assertTrue(configuration.finalProperties.containsKey(propertyName));

        restoreConfigurationProperties();

        assertFalse(configuration.finalProperties.containsKey(propertyName));
    }
}
