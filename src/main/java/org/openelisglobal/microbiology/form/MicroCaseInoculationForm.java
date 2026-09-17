package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public class MicroCaseInoculationForm {
    public String id;
    public String caseId;
    public String sourceInoculationId;
    public String methodId;
    public String containerIdentifier;
    public String media;
    public String incubation;
    public String atmosphere;
    public Timestamp occurredAt;
    public String performedBy;
}
