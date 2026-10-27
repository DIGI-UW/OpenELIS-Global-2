package org.openelisglobal.delivery.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * One Sent Messages row; the rendered text and attempts come with the detail.
 */
public class SentMessageDTO {

    public static class ChannelDTO {
        private String channel;
        private String status;
        private String errorMessage;
        private int attempts;
        private String lastAttemptedAt;
        private String nextAttemptAt;
        private String gatewayMessageId;

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }

        public String getLastAttemptedAt() {
            return lastAttemptedAt;
        }

        public void setLastAttemptedAt(String lastAttemptedAt) {
            this.lastAttemptedAt = lastAttemptedAt;
        }

        public String getNextAttemptAt() {
            return nextAttemptAt;
        }

        public void setNextAttemptAt(String nextAttemptAt) {
            this.nextAttemptAt = nextAttemptAt;
        }

        public String getGatewayMessageId() {
            return gatewayMessageId;
        }

        public void setGatewayMessageId(String gatewayMessageId) {
            this.gatewayMessageId = gatewayMessageId;
        }
    }

    public static class AttemptDTO {
        private String channel;
        private String attemptedAt;
        private boolean succeeded;
        private String errorMessage;

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getAttemptedAt() {
            return attemptedAt;
        }

        public void setAttemptedAt(String attemptedAt) {
            this.attemptedAt = attemptedAt;
        }

        public boolean isSucceeded() {
            return succeeded;
        }

        public void setSucceeded(boolean succeeded) {
            this.succeeded = succeeded;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    private Long id;
    private String product;
    private String eventCode;
    private String ruleName;
    private String firedAt;
    private String sentBy;
    private boolean automatic;
    private String recipientType;
    private String recipientDisplayName;
    private String recipientEmail;
    private String recipientPhone;
    private String labNumber;
    private String workflow;
    private String status;
    private Long resentFromId;
    private Long reportVersionId;
    private String reportFormat;
    private List<ChannelDTO> channels = new ArrayList<>();
    private String renderedSubject;
    private String renderedMessage;
    private List<AttemptDTO> attempts = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getFiredAt() {
        return firedAt;
    }

    public void setFiredAt(String firedAt) {
        this.firedAt = firedAt;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    public boolean isAutomatic() {
        return automatic;
    }

    public void setAutomatic(boolean automatic) {
        this.automatic = automatic;
    }

    public String getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(String recipientType) {
        this.recipientType = recipientType;
    }

    public String getRecipientDisplayName() {
        return recipientDisplayName;
    }

    public void setRecipientDisplayName(String recipientDisplayName) {
        this.recipientDisplayName = recipientDisplayName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getLabNumber() {
        return labNumber;
    }

    public void setLabNumber(String labNumber) {
        this.labNumber = labNumber;
    }

    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getResentFromId() {
        return resentFromId;
    }

    public void setResentFromId(Long resentFromId) {
        this.resentFromId = resentFromId;
    }

    public Long getReportVersionId() {
        return reportVersionId;
    }

    public void setReportVersionId(Long reportVersionId) {
        this.reportVersionId = reportVersionId;
    }

    public String getReportFormat() {
        return reportFormat;
    }

    public void setReportFormat(String reportFormat) {
        this.reportFormat = reportFormat;
    }

    public List<ChannelDTO> getChannels() {
        return channels;
    }

    public void setChannels(List<ChannelDTO> channels) {
        this.channels = channels;
    }

    public String getRenderedSubject() {
        return renderedSubject;
    }

    public void setRenderedSubject(String renderedSubject) {
        this.renderedSubject = renderedSubject;
    }

    public String getRenderedMessage() {
        return renderedMessage;
    }

    public void setRenderedMessage(String renderedMessage) {
        this.renderedMessage = renderedMessage;
    }

    public List<AttemptDTO> getAttempts() {
        return attempts;
    }

    public void setAttempts(List<AttemptDTO> attempts) {
        this.attempts = attempts;
    }
}
