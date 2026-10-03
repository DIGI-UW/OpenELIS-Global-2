package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroCaseInoculationRequestForm {
    public String sourceInoculationId;
    public String containerIdentifier;
    public String media;
    public String incubation;
    public String atmosphere;
    public List<MicroLotSelectionRequestForm> lotSelections = new ArrayList<>();
}
