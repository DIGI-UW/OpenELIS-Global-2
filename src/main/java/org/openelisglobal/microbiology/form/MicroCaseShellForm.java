package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;

public class MicroCaseShellForm extends MicroCaseSummaryForm {
    public List<MicroCaseSpecimenForm> samples = new ArrayList<>();
    public List<MicroCaseSpecimenForm> pendingSamples = new ArrayList<>();
    public List<MicroCaseSummaryForm> relatedCases = new ArrayList<>();
    public List<IdValuePair> transferLabUnits = new ArrayList<>();
    public boolean canWrite;
    public boolean canValidate;
}
