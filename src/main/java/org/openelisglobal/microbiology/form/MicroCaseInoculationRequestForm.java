package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroCaseInoculationRequestForm {
    public String sourceInoculationId;
    public String containerIdentifier;
    public String media;
    public String incubation;
    public String atmosphere;
}
