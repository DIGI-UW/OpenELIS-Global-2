package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroAstOverrideRequestForm {

    public String overrideInterpretation;
    public String overrideReason;
}
