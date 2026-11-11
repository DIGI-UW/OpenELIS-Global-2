package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroCriticalCommunicationRequestForm {

    public String targetType;
    public String targetId;
    public String recipient;
    public String recipientContact;
    public String communicationMethod;
    public String message;
    public boolean followUpNeeded;
    public String resolutionNote;
}
