package org.openelisglobal.notification.service;

public record SendOutcome(Status status, int attempts, String errorMessage) {

    public enum Status {
        SUCCESS, FAILED
    }

    public static SendOutcome success(int attempts) {
        return new SendOutcome(Status.SUCCESS, attempts, null);
    }

    public static SendOutcome failed(int attempts, String errorMessage) {
        return new SendOutcome(Status.FAILED, attempts, errorMessage);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
