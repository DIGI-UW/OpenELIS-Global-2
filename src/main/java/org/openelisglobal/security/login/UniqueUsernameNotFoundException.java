package org.openelisglobal.security.login;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class UniqueUsernameNotFoundException extends UsernameNotFoundException {

    private static final long serialVersionUID = -6799172532873720927L;

    public UniqueUsernameNotFoundException(String msg) {
        super(msg);
    }

    public UniqueUsernameNotFoundException(String msg, Throwable t) {
        super(msg, t);
    }
}
