package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public class MicroCaseSpecimenForm {
    public String id;
    public String sampleItemId;
    public String label;
    public String sampleTypeId;
    public String specimenType;
    public String bodySite;
    public Integer cultureSetNumber;
    public String containerType;
    public String containerPopulation;
    public boolean collectedInSets;
    public Timestamp collectionDate;
}
