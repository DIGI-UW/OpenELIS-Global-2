package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MicroAstRunForm {

    public String id;
    public String isolateId;
    public String panelId;
    public String status;
    public Timestamp startedAt;
    public String startedBy;
    public Timestamp reviewedAt;
    public String reviewedBy;
    public List<MicroAstReadingForm> readings = new ArrayList<>();
}
