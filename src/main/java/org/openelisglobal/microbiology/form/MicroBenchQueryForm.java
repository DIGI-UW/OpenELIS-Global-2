package org.openelisglobal.microbiology.form;

import java.util.List;

public class MicroBenchQueryForm extends MicroCaseSearchForm {
    public String cultureFilter;
    public boolean needsAttention;
    public List<String> labUnitIds;
}
