package org.openelisglobal.dataexchange.common;

import java.util.List;
import org.apache.http.HttpStatus;

public abstract class HttpSender implements IExternalSender {

    protected String message;
    protected String url;
    protected int returnStatus = HttpStatus.SC_CREATED;
    String serviceTargetName = "";
    List<String> errors;
    protected String headerName;
    protected String headerValue;
    protected boolean sendAsJson = false;

    public abstract boolean sendMessage();

    @Override
    public void setTargetName(String name) {
        serviceTargetName = name != null ? name : "";
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public void setURI(String url) {
        this.url = url;
    }

    public void setHeader(String name, String value) {
        this.headerName = name;
        this.headerValue = value;
    }

    public void setSendAsJson(boolean sendAsJson) {
        this.sendAsJson = sendAsJson;
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    @Override
    public int getSendResponse() {
        return returnStatus;
    }
}
