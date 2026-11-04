package org.openelisglobal.microbiology.form;

/** Bean rows consumed by the patient report's microbiology subreport. */
@lombok.Value
public class MicroPatientReportRow {
    String heading;
    String text;
    String agent;
    String raw;
    String interpretation;
    String standard;
    boolean grid;
}
