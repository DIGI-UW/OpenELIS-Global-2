package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;

public class MicroCaseReadinessForm {

    public String caseId;
    public boolean finalReleaseReady;
    public List<String> blockers = new ArrayList<>();
    public int astRunsComplete;
    public int astRunsTotal;
    public int significantIsolatesAwaitingAstSetup;
    public int isolatesPendingIdentification;
}
