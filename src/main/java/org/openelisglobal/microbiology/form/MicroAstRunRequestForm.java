package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroAstRunRequestForm {

    public String isolateId;
    public String panelId;
    public String breakpointStandardId;
}
