package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroCaseNonconformanceRequestForm {
    public String categoryId;
    public String typeId;
    public Integer reportingUnitId;
    public String severity;
    public String title;
    public String description;
    public String immediateAction;
    public String disposition;
    public String eventType;
    public String sourceAstRunId;
    public String astTechnique;
    public List<String> orderedAntibioticIds;
}
