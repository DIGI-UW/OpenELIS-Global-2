package org.openelisglobal.microbiology.form;

/** A requested specimen; never an eligible target for clinical result entry. */
public class MicroCaseRequestedSpecimenForm {
    public Integer requestId;
    public String sampleTypeId;
    public String specimenType;
    public String bodySite;
    public String containerType;
    public Integer cultureSetNumber;
    public java.sql.Timestamp collectionDate;
    public boolean collectedInSets;
}
