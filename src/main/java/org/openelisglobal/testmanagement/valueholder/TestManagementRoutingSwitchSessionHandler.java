/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.testmanagement.valueholder;

import jakarta.servlet.http.HttpSession;
import org.openelisglobal.common.action.IActionConstants;

/**
 * @author benzd1 bugzilla 2053
 */
public class TestManagementRoutingSwitchSessionHandler implements IActionConstants {

    public static void switchOn(int routingSwitch, HttpSession session) {
        TestManagementRoutingSwitch testManagementRoutingSwitch = null;
        if (session.getAttribute(TEST_MANAGEMENT_ROUTING_SWITCH) != null) {
            testManagementRoutingSwitch = (TestManagementRoutingSwitch) session
                    .getAttribute(TEST_MANAGEMENT_ROUTING_SWITCH);
        } else {
            testManagementRoutingSwitch = new TestManagementRoutingSwitch();
        }
        switch (routingSwitch) {
        case TEST_MANAGEMENT_ROUTING_FROM_RESULTS_ENTRY:
            testManagementRoutingSwitch.setResultsEntrySwitch(true);
            testManagementRoutingSwitch.setQaEntryEntrySwitch(false);
            // bugzilla 2504
            testManagementRoutingSwitch.setQaEntryEntryLineListingSwitch(false);
            break;
        case TEST_MANAGEMENT_ROUTING_FROM_QAEVENTS_ENTRY:
            testManagementRoutingSwitch.setResultsEntrySwitch(false);
            testManagementRoutingSwitch.setQaEntryEntrySwitch(true);
            testManagementRoutingSwitch.setQaEntryEntryLineListingSwitch(false);
            break;
        case TEST_MANAGEMENT_ROUTING_FROM_QAEVENTS_ENTRY_LINELISTING:
            testManagementRoutingSwitch.setResultsEntrySwitch(false);
            testManagementRoutingSwitch.setQaEntryEntrySwitch(false);
            testManagementRoutingSwitch.setQaEntryEntryLineListingSwitch(true);
            break;
        default: // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "An error
            // occurred with QaEventRoutingSwitchSessionHandler
            // switchOn(" + routingSwitch + ")");
        }

        session.setAttribute(TEST_MANAGEMENT_ROUTING_SWITCH, testManagementRoutingSwitch);
    }

    public static void switchOff(int routingSwitch, HttpSession session) {
        TestManagementRoutingSwitch testManagementRoutingSwitch = null;
        if (session.getAttribute(TEST_MANAGEMENT_ROUTING_SWITCH) != null) {
            testManagementRoutingSwitch = (TestManagementRoutingSwitch) session
                    .getAttribute(TEST_MANAGEMENT_ROUTING_SWITCH);
        } else {
            testManagementRoutingSwitch = new TestManagementRoutingSwitch();
        }
        switch (routingSwitch) {
        case TEST_MANAGEMENT_ROUTING_FROM_RESULTS_ENTRY:
            testManagementRoutingSwitch.setResultsEntrySwitch(false);
            break;
        case TEST_MANAGEMENT_ROUTING_FROM_QAEVENTS_ENTRY:
            testManagementRoutingSwitch.setQaEntryEntrySwitch(false);
            break;
        case TEST_MANAGEMENT_ROUTING_FROM_QAEVENTS_ENTRY_LINELISTING:
            testManagementRoutingSwitch.setQaEntryEntryLineListingSwitch(false);
            break;
        default: // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "An error
            // occurred with QaEventRoutingSwitchSessionHandler
            // switchOff(" + routingSwitch + ")");
        }

        session.setAttribute(TEST_MANAGEMENT_ROUTING_SWITCH, testManagementRoutingSwitch);
    }

    public static void switchAllOff(HttpSession session) {
        TestManagementRoutingSwitch testManagementRoutingSwitch = null;
        if (session.getAttribute(TEST_MANAGEMENT_ROUTING_SWITCH) != null) {
            testManagementRoutingSwitch = (TestManagementRoutingSwitch) session
                    .getAttribute(TEST_MANAGEMENT_ROUTING_SWITCH);
        } else {
            testManagementRoutingSwitch = new TestManagementRoutingSwitch();
        }
        testManagementRoutingSwitch.setResultsEntrySwitch(false);
        testManagementRoutingSwitch.setQaEntryEntrySwitch(false);
        testManagementRoutingSwitch.setQaEntryEntryLineListingSwitch(false);

        session.setAttribute(TEST_MANAGEMENT_ROUTING_SWITCH, testManagementRoutingSwitch);
    }

    public static boolean isSwitchOn(int routingSwitch, HttpSession session) {
        TestManagementRoutingSwitch testManagementRoutingSwitch = null;
        if (session.getAttribute(TEST_MANAGEMENT_ROUTING_SWITCH) != null) {
            testManagementRoutingSwitch = (TestManagementRoutingSwitch) session
                    .getAttribute(TEST_MANAGEMENT_ROUTING_SWITCH);
        } else {
            return false;
        }
        switch (routingSwitch) {
        case TEST_MANAGEMENT_ROUTING_FROM_RESULTS_ENTRY:
            if (testManagementRoutingSwitch.isResultsEntrySwitch())
                return true;
            else
                return false;
        case TEST_MANAGEMENT_ROUTING_FROM_QAEVENTS_ENTRY:
            if (testManagementRoutingSwitch.isQaEntryEntrySwitch())
                return true;
            else
                return false;
        case TEST_MANAGEMENT_ROUTING_FROM_QAEVENTS_ENTRY_LINELISTING:
            if (testManagementRoutingSwitch.isQaEntryEntryLineListingSwitch())
                return true;
            else
                return false;
        default: // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "An error
            // occurred with QaEventRoutingSwitchSessionHandler
            // isSwitchOn(" + routingSwitch + ")");
            return false;
        }
    }
}
