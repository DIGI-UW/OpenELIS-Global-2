package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroCaseWorkflowChangeRequestForm {
    public String workflowType;
    public String cultureMethodId;
    public String reason;
    public boolean preserveExistingWorkConfirmed;
}
