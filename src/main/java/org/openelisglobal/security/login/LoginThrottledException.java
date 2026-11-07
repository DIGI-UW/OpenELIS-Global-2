package org.openelisglobal.security.login;

import org.springframework.security.core.AuthenticationException;

/** The login or the client has failed too often within the window. */
public class LoginThrottledException extends AuthenticationException {

    private final long windowMinutes;

    public LoginThrottledException(long windowMinutes) {
        super("too many failed sign-ins; wait " + windowMinutes + " minutes");
        this.windowMinutes = windowMinutes;
    }

    public long getWindowMinutes() {
        return windowMinutes;
    }
}
