package org.openelisglobal.delivery.dto;

/**
 * A channel as the Channels page sees it. The secret is never in here: only
 * whether one is stored. A PUT with a blank secret keeps the stored one.
 */
public class DeliveryChannelDTO {

    private String channel;
    private boolean active = true;
    /** READY, NOT_SET_UP or FAILING. */
    private String status;
    private String host;
    private Integer port;
    private String security;
    private String username;
    private boolean secretSet;
    private String secret;
    private String senderAddress;
    private String senderName;
    private String replyTo;
    private String gateway;
    private String countryCode;
    private String lastSuccessAt;
    private String lastFailureAt;
    private String lastError;
    /** True when the channel's fields are edited on this page. */
    private boolean editable;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isSecretSet() {
        return secretSet;
    }

    public void setSecretSet(boolean secretSet) {
        this.secretSet = secretSet;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getLastSuccessAt() {
        return lastSuccessAt;
    }

    public void setLastSuccessAt(String lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public String getLastFailureAt() {
        return lastFailureAt;
    }

    public void setLastFailureAt(String lastFailureAt) {
        this.lastFailureAt = lastFailureAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}
