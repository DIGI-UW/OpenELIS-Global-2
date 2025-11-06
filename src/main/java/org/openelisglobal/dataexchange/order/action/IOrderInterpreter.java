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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.dataexchange.order.action;

import ca.uhn.hl7v2.model.Message;
import java.util.List;
import org.openelisglobal.test.valueholder.Test;

public interface IOrderInterpreter {

    public enum InterpreterResults {
        OK, UNKNOWN_REQUEST_TYPE, INTERPRET_ERROR, MISSING_ORDER_NUMBER, MISSING_PATIENT_GUID, MISSING_PATIENT_DOB,
        MISSING_PATIENT_GENDER, MISSING_PATIENT_IDENTIFIER, MISSING_TESTS, UNSUPPORTED_TESTS, UNSUPPORTED_PANELS,
        OTHER_THAN_PANEL_OR_TEST_REQUESTED
    }

    public enum OrderType {
        REQUEST("NW"), CANCEL("CA"), UNKNOWN("");

        private String tag;

        OrderType(String tag) {
            this.tag = tag;
        }

        public String getIdentifier() {
            return tag;
        }
    }

    public List<InterpreterResults> interpret(Message orderMessage);

    public String getReferringOrderNumber();

    public String getMessage();

    public MessagePatient getMessagePatient();

    public List<InterpreterResults> getResultStatus();

    public OrderType getOrderType();

    public List<String> getUnsupportedTests();

    public List<String> getUnsupportedPanels();

    public Test getTest();
}
