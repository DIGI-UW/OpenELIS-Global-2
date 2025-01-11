package org.openelisglobal.common.util;

import javax.servlet.http.HttpServletRequest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;

public class ControllerUtills {

    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA);
        if (usd == null) {
            usd = (UserSessionData) request.getAttribute(IActionConstants.USER_SESSION_DATA);
            if (usd == null) {
                return null;
            }
        }
        return String.valueOf(usd.getSystemUserId());
    }
}
