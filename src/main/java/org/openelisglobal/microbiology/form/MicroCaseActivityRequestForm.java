package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroCaseActivityRequestForm {
    public String nextStage;
    public String note;
    public List<MicroLotSelectionRequestForm> lotSelections = new ArrayList<>();
}
