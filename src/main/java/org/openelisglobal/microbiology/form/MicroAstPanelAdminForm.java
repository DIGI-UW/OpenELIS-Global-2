package org.openelisglobal.microbiology.form;

import java.util.ArrayList;
import java.util.List;

public class MicroAstPanelAdminForm {
    public String id;
    public String logicalKey;
    public Integer versionNumber;
    public String supersedesPanelId;
    public String name;
    public String workflowType;
    public String organismGroup;
    public String specimenTypeId;
    public boolean active = true;
    public boolean current = true;
    public String publishedBy;
    public String publishedAt;
    public List<MicroAstPanelAntibioticAdminForm> antibiotics = new ArrayList<>();
}
