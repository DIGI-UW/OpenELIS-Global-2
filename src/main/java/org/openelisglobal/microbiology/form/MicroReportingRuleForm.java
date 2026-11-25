package org.openelisglobal.microbiology.form;

import java.util.List;

public class MicroReportingRuleForm {
    public String id, antibioticId, antibioticName, organismId, organismName, organismGroup, clinicalContext,
            reportBehavior, cascadeCondition, note, noteWhen;
    public List<String> sampleTypeIds = List.of();
    public List<String> sampleTypeNames = List.of();
    public boolean addAutomatically;
    public Boolean active;
}
