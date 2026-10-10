package org.openelisglobal.program.util;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DefaultConfigurationProperties;
import org.openelisglobal.common.util.DefaultConfigurationProperties.OEProperties;
import org.openelisglobal.program.util.DesignationScheme.PartScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * How a laboratory names its cassettes, blocks and slides is a deployment's own
 * choice (FR-9.3), and a case named under the wrong scheme cannot be renamed
 * afterwards, since its labels are already printed. These tests read the scheme
 * back through the configuration layer the application really uses, so what a
 * deployment sets is what the bench gets, and a deployment that has set nothing
 * still names its cases.
 *
 * <p>
 * The four settings are restored after every test: the configuration store is a
 * singleton shared by the whole test JVM, so a value left behind here would
 * reach every class that runs after it.
 */
public class DesignationSchemeTest extends BaseWebContextSensitiveTest {

    private static final Property[] IDENTIFIER_PROPERTIES = { Property.PATHOLOGY_IDENTIFIER_PART_SCHEME,
            Property.PATHOLOGY_IDENTIFIER_BLOCK_FORMAT, Property.PATHOLOGY_IDENTIFIER_SLIDE_FORMAT,
            Property.PATHOLOGY_IDENTIFIER_SEPARATOR };

    @Autowired
    private DefaultConfigurationProperties defaultConfigurationProperties;

    private final Map<Property, String> valuesBefore = new LinkedHashMap<>();

    @Before
    public void rememberTheDeploymentsOwnSettings() {
        for (Property property : IDENTIFIER_PROPERTIES) {
            valuesBefore.put(property, ConfigurationProperties.getInstance().getPropertyValue(property));
        }
    }

    @After
    public void putTheDeploymentsOwnSettingsBack() {
        valuesBefore
                .forEach((property, value) -> ConfigurationProperties.getInstance().setPropertyValue(property, value));
    }

    @Test
    public void fromConfiguration_readsTheFourProperties() {
        set(Property.PATHOLOGY_IDENTIFIER_PART_SCHEME, "NUMERIC");
        set(Property.PATHOLOGY_IDENTIFIER_BLOCK_FORMAT, "{part}-{n}");
        set(Property.PATHOLOGY_IDENTIFIER_SLIDE_FORMAT, "S{n}");
        set(Property.PATHOLOGY_IDENTIFIER_SEPARATOR, "-");

        DesignationScheme scheme = DesignationScheme.fromConfiguration();

        assertEquals("the part scheme must come from the deployment's own setting", PartScheme.NUMERIC,
                scheme.getPartScheme());
        assertEquals("the block format must come from the deployment's own setting", "{part}-{n}",
                scheme.getBlockFormat());
        assertEquals("the slide format must come from the deployment's own setting", "S{n}", scheme.getSlideFormat());
        assertEquals("the separator must come from the deployment's own setting", "-", scheme.getSeparator());
        assertEquals("a numbered scheme names its first part 1", "1", PathologyDesignations.firstPart(scheme));
    }

    @Test
    public void fromConfiguration_fallsBackToAlphaForAnUnknownPartScheme() {
        set(Property.PATHOLOGY_IDENTIFIER_PART_SCHEME, "letters");

        assertEquals("a scheme nobody has implemented must not leave a case unnameable", PartScheme.ALPHA,
                DesignationScheme.fromConfiguration().getPartScheme());

        set(Property.PATHOLOGY_IDENTIFIER_PART_SCHEME, "numeric");

        assertEquals("a value that names a real scheme is honoured whatever its case", PartScheme.NUMERIC,
                DesignationScheme.fromConfiguration().getPartScheme());
    }

    @Test
    public void fromConfiguration_fallsBackToDefaultsForBlankValues() {
        for (Property property : IDENTIFIER_PROPERTIES) {
            set(property, "");
        }

        DesignationScheme scheme = DesignationScheme.fromConfiguration();

        assertEquals("a blank setting must not name every block the same",
                DesignationScheme.defaults().getBlockFormat(), scheme.getBlockFormat());
        assertEquals("a blank setting must not name every slide the same",
                DesignationScheme.defaults().getSlideFormat(), scheme.getSlideFormat());
        assertEquals("a blank setting must not run the segments of a barcode together",
                DesignationScheme.defaults().getSeparator(), scheme.getSeparator());
        assertEquals("a blank setting must not leave the parts of a case unnamed",
                DesignationScheme.defaults().getPartScheme(), scheme.getPartScheme());
    }

    @Test
    public void fromConfiguration_fallsBackToTheDefaultFormatWithoutANumberPlaceholder() {
        set(Property.PATHOLOGY_IDENTIFIER_BLOCK_FORMAT, "{part}");
        set(Property.PATHOLOGY_IDENTIFIER_SLIDE_FORMAT, "S");

        DesignationScheme scheme = DesignationScheme.fromConfiguration();

        assertEquals(
                "a block format that could never number a cassette gives way to the default,"
                        + " so a typo on the admin page does not stop every case save",
                "{part}{n}", scheme.getBlockFormat());
        assertEquals("and so does a slide format without the number placeholder", "{n}", scheme.getSlideFormat());
    }

    @Test
    public void defaults_matchTheShippedConfiguration() {
        DesignationScheme defaults = DesignationScheme.defaults();

        assertEquals("parts are lettered unless a deployment says otherwise", PartScheme.ALPHA,
                defaults.getPartScheme());
        assertEquals("a block is named for its part and its number", "{part}{n}", defaults.getBlockFormat());
        assertEquals("a slide is named for its number within its block", "{n}", defaults.getSlideFormat());
        assertEquals("a dot separates the segments of a barcode", ".", defaults.getSeparator());
    }

    /**
     * A deployment whose rows are missing must name its cases exactly as
     * {@link DesignationScheme#defaults()} says, or the same case would be named
     * one way before an administrator visits the admin page and another way after.
     */
    @Test
    public void theHardcodedDefaults_areTheSameSchemeAsDefaults() {
        OEProperties hardcoded = (OEProperties) ReflectionTestUtils.invokeMethod(defaultConfigurationProperties,
                "loadHardcodedProperties");
        DesignationScheme defaults = DesignationScheme.defaults();

        Map<Property, String> expected = new LinkedHashMap<>();
        expected.put(Property.PATHOLOGY_IDENTIFIER_PART_SCHEME, defaults.getPartScheme().name());
        expected.put(Property.PATHOLOGY_IDENTIFIER_BLOCK_FORMAT, defaults.getBlockFormat());
        expected.put(Property.PATHOLOGY_IDENTIFIER_SLIDE_FORMAT, defaults.getSlideFormat());
        expected.put(Property.PATHOLOGY_IDENTIFIER_SEPARATOR, defaults.getSeparator());

        expected.forEach((property, value) -> assertEquals(
                property + " must ship with the value this class treats as the default", value,
                hardcoded.getPropertyHolder(property).getValue()));
        assertEquals("all four identifier settings carry a hardcoded default", IDENTIFIER_PROPERTIES.length,
                expected.size());
    }

    // helpers

    private static void set(Property property, String value) {
        ConfigurationProperties.getInstance().setPropertyValue(property, value);
    }
}
