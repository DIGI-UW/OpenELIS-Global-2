package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;

public class MicroCultureOptionsForm {
    public boolean requireTrackedMedia;
    public List<IdValuePair> atmospheres, readings, quantities, extensionReasons;
    public List<Source> sources;
    public IdValuePair gramStainTest;
    public List<String> gramStainSampleTypeIds;

    public record Source(String sampleItemId, String label, String sampleTypeId, String specimenType) {
    }

    public List<Medium> media;
    public List<MediaLink> mediaLinks;

    public record Medium(Long id, String name, boolean trackLots, String atmosphereId, BigDecimal temperature,
            List<Lot> lots) {
    }

    public record Lot(Long id, String lotNumber) {
    }

    public record MediaLink(Long mediumItemId, String sampleTypeId, BigDecimal duration, String durationUnit,
            BigDecimal checkIntervalHours, BigDecimal loopVolume, String atmosphereId, BigDecimal temperature) {
    }
}
