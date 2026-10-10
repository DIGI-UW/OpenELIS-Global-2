package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public class MicroCultureForm {
    public String id, parentId, sourceSampleItemId, subculturePurpose, containerIdentifier;
    public Long mediumItemId, lotId;
    public String mediumName, lotNumber, atmosphereId, atmosphereName, durationUnit, outcome, outcomeBy, positiveSource;
    public boolean notTracked, checkDue, finalReadDue, overdue, canEditInoculatedAt;
    public BigDecimal temperature, duration, checkIntervalHours, loopVolume, timeToPositivityHours;
    public Timestamp inoculatedAt, positiveAt, outcomeAt, incubationEnds, nextCheck;
    public List<Reading> readings;
    public List<Extension> extensions;
    public List<Proposal> proposals;

    public record Reading(String id, String readingId, String readingName, String quantityId, String quantityName,
            String note, BigDecimal incubationDay, String by, Timestamp at) {
    }

    public record Extension(String id, BigDecimal extendBy, String unit, String reasonId, String reasonName,
            String note, String by, Timestamp at) {
    }

    public record Proposal(String id, String signal, Timestamp receivedAt, String confirmedBy, Timestamp confirmedAt) {
    }
}
