package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;

public class MicroWhonetPreviewForm {

    public String from;
    public String to;
    public String significance;
    public String dedup;
    public int totalCases;
    public int totalIsolates;
    public int afterSignificance;
    public int afterDeduplication;
    public int exportableIsolates;
    public int exportedRows;
    public int excludedRows;
    public boolean canGenerate;
    public List<MicroWhonetWarningForm> warnings = new ArrayList<>();
    public List<MicroWhonetPreviewRowForm> rows = new ArrayList<>();
}
