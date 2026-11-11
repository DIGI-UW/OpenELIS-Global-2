package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;

public class MicroCaseSearchPageForm {
    public List<MicroCaseSummaryForm> rows = new ArrayList<>();
    public List<IdValuePair> labUnits = new ArrayList<>();
    public long total;
    public int page;
    public int pageSize;
}
