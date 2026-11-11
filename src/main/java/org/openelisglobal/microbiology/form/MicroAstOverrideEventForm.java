package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public class MicroAstOverrideEventForm {

    public String id;
    public String readingId;
    public String action;
    public String fromInterpretation;
    public String toInterpretation;
    public String reason;
    public Timestamp performedAt;
    public String performedBy;
    public String performedByDisplay;
}
