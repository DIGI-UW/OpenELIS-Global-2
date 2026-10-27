package org.openelisglobal.delivery.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.security.converter.EncryptionConverter;

/**
 * One row per transport the laboratory sends through: Email, SMS and WhatsApp.
 * The secret (SMTP password, gateway token) is encrypted at rest and never
 * leaves the server; the row also remembers the last successful and failed send
 * so the Channels page can say Ready, Not set up or Failing.
 */
@Entity
@Table(name = "delivery_channel", schema = "clinlims")
public class DeliveryChannel extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    public enum Channel {
        EMAIL, SMS, WHATSAPP
    }

    public enum Security {
        STARTTLS, SSL_TLS, NONE
    }

    public enum SmsGateway {
        BMP, SMPP, TEXTIT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, unique = true, length = 16)
    private Channel channel;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "host", length = 255)
    private String host;

    @Column(name = "port")
    private Integer port;

    @Enumerated(EnumType.STRING)
    @Column(name = "security", length = 16)
    private Security security;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "secret", columnDefinition = "text")
    @Convert(converter = EncryptionConverter.class)
    private String secret;

    @Column(name = "sender_address", length = 255)
    private String senderAddress;

    @Column(name = "sender_name", length = 255)
    private String senderName;

    @Column(name = "reply_to", length = 255)
    private String replyTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway", length = 16)
    private SmsGateway gateway;

    @Column(name = "country_code", length = 8)
    private String countryCode;

    @Column(name = "last_success_at")
    private OffsetDateTime lastSuccessAt;

    @Column(name = "last_failure_at")
    private OffsetDateTime lastFailureAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public SmsGateway getGateway() {
        return gateway;
    }

    public void setGateway(SmsGateway gateway) {
        this.gateway = gateway;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public OffsetDateTime getLastSuccessAt() {
        return lastSuccessAt;
    }

    public void setLastSuccessAt(OffsetDateTime lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public OffsetDateTime getLastFailureAt() {
        return lastFailureAt;
    }

    public void setLastFailureAt(OffsetDateTime lastFailureAt) {
        this.lastFailureAt = lastFailureAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
