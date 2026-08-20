package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MicroAstRunForm {

    public String id;
    public String isolateId;
    public String panelId;
    public Integer panelVersion;
    public String panelProvenance;
    public String panelAdjustmentReason;
    public String breakpointStandardId;
    public String breakpointVersion;
    public String attemptType;
    public String sourceRunId;
    public String attemptReason;
    public String method;
    public String technique;
    public String measurementType;
    public boolean reportable;
    public String status;
    public Timestamp startedAt;
    public String startedBy;
    public Timestamp reviewedAt;
    public String reviewedBy;
    public String analyzerInstrumentId;
    public String analyzerCardId;
    public String analyzerSoftwareVersion;
    public String analyzerOrganismId;
    public String analyzerOrganismName;
    public BigDecimal analyzerOrganismConfidence;
    public String analyzerExpertFlags;
    public String instrumentQcReference;
    public String qcState;
    public String qcOverrideReason;
    public Timestamp qcOverriddenAt;
    public String qcOverriddenBy;
    public Timestamp analyzerFlagsAcknowledgedAt;
    public String analyzerFlagsAcknowledgedBy;
    public String analyzerFlagsAcknowledgementReason;
    public Timestamp analyzerLoadedAt;
    public Timestamp analyzerCompletedAt;
    public String analyzerMessageCodes;
    public String sourceEventId;
    public List<MicroAstRunAntibioticForm> orderedAntibiotics = new ArrayList<>();
    public List<MicroAstReadingForm> readings = new ArrayList<>();
}
