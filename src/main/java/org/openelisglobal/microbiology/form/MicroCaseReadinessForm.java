package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;

public class MicroCaseReadinessForm {

    public java.util.List<String> pendingIncomingIds = new java.util.ArrayList<>();
    public String caseId;
    public boolean finalReleaseReady;
    public List<String> blockers = new ArrayList<>();
    public java.util.Map<String, String> blockerLabels = java.util.Map.of();
    public int astRunsComplete;
    public int astRunsTotal;
    public int significantIsolatesAwaitingAstSetup;
    public int isolatesPendingIdentification;
}
