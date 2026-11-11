package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroCaseNoteRequestForm {
    public String targetType;
    public String targetId;
    public String text;
    public boolean sendWithResult;
}
