package org.openelisglobal.pathology;

import java.util.EnumMap;
import java.util.Map;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.util.PathologyStages;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;

/**
 * Switches every optional pathology stage on for a test and puts each switch
 * back as it was on close, so a test that switches one off cannot change what
 * the next one reads.
 */
final class StageSwitches implements AutoCloseable {

    private final Map<Property, String> before = new EnumMap<>(Property.class);

    private StageSwitches() {
    }

    static StageSwitches allOn() {
        StageSwitches switches = new StageSwitches();
        for (PathologyStatus stage : PathologyStatus.values()) {
            PathologyStages.enablementProperty(stage).ifPresent(property -> {
                switches.before.put(property, ConfigurationProperties.getInstance().getPropertyValue(property));
                ConfigurationProperties.getInstance().setPropertyValue(property, "true");
            });
        }
        return switches;
    }

    @Override
    public void close() {
        before.forEach((property, value) -> ConfigurationProperties.getInstance().setPropertyValue(property,
                value == null ? "true" : value));
    }
}
