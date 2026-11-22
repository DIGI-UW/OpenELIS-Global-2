package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;

public class MicroWorklistPageForm {

    public List<MicroWorklistRowForm> rows = new ArrayList<>();
    public List<MicroWorklistRecentActivityForm> recentActivity = new ArrayList<>();
    public MicroWorklistSummaryForm summary = new MicroWorklistSummaryForm();
    public MicroWhonetFilterOptionsForm filterOptions = new MicroWhonetFilterOptionsForm();
    public int total;
    public int page;
    public int pageSize;
}
