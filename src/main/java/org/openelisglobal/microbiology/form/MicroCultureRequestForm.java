package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class MicroCultureRequestForm {
    public String sourceSampleItemId, parentId, subculturePurpose, containerIdentifier;
    public Long mediumItemId, lotId;
    public boolean notTracked;
    public String atmosphereId, durationUnit;
    public BigDecimal temperature, duration, checkIntervalHours, loopVolume;
    public Timestamp inoculatedAt, positiveAt;
    public String readingId, quantityId, note, outcome, reasonId, signal;
    public BigDecimal extendBy;
    public String unit;
    public String proposalId;
}
