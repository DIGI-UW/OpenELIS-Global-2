package org.openelisglobal.dataexchange.aggregatereporting.form;

import java.util.Collection;
import org.openelisglobal.common.form.BaseForm;

public class TestUsageConfigurationForm extends BaseForm {

    private String enableSending = "enable";

    private String sendHour = "";

    private String sendMin = "";

    private Collection hourList;

    private Collection minList;

    private String url = "";

    private String serviceUserName = "";

    private String servicePassword;

    private String lastSent = "Never";

    private String lastAttemptToSend = "Never";

    private String sendStatus = "";

    public TestUsageConfigurationForm() {
        setFormName("TestUsageConfigurationForm");
    }

    public String getEnableSending() {
        return enableSending;
    }

    public void setEnableSending(String enableSending) {
        this.enableSending = enableSending;
    }

    public String getSendHour() {
        return sendHour;
    }

    public void setSendHour(String sendHour) {
        this.sendHour = sendHour;
    }

    public String getSendMin() {
        return sendMin;
    }

    public void setSendMin(String sendMin) {
        this.sendMin = sendMin;
    }

    public Collection getHourList() {
        return hourList;
    }

    public void setHourList(Collection hourList) {
        this.hourList = hourList;
    }

    public Collection getMinList() {
        return minList;
    }

    public void setMinList(Collection minList) {
        this.minList = minList;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceUserName() {
        return serviceUserName;
    }

    public void setServiceUserName(String serviceUserName) {
        this.serviceUserName = serviceUserName;
    }

    public String getServicePassword() {
        return servicePassword;
    }

    public void setServicePassword(String servicePassword) {
        this.servicePassword = servicePassword;
    }

    public String getLastSent() {
        return lastSent;
    }

    public void setLastSent(String lastSent) {
        this.lastSent = lastSent;
    }

    public String getLastAttemptToSend() {
        return lastAttemptToSend;
    }

    public void setLastAttemptToSend(String lastAttemptToSend) {
        this.lastAttemptToSend = lastAttemptToSend;
    }

    public String getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(String sendStatus) {
        this.sendStatus = sendStatus;
    }
}
