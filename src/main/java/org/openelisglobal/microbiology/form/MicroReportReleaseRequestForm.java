package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroReportReleaseRequestForm {
    public java.util.List<String> confirmedIncomingIds = java.util.List.of();
}
