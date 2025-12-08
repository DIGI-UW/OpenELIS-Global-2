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

import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.hoh.hapi.server.HohServlet;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v251.datatype.CWE;
import ca.uhn.hl7v2.model.v251.message.ACK;
import ca.uhn.hl7v2.model.v251.message.OML_O21;
import ca.uhn.hl7v2.model.v251.message.ORL_O22;
import ca.uhn.hl7v2.model.v251.segment.ERR;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.openelisglobal.dataexchange.order.action.IOrderInterpreter.InterpreterResults;
import org.openelisglobal.dataexchange.order.action.OrderWorker.OrderResult;
import org.openelisglobal.spring.util.SpringContext;

public class OrderServlet extends HohServlet {

    private static final long serialVersionUID = -2572093053734971596L;

    @Override
    public void init(ServletConfig theConfig) throws ServletException {
        setApplication(new OrderApplication());
    }

    /** The application does the actual processing */
    private class OrderApplication implements ReceivingApplication {
        /**
         * processMessage is fired each time a new message arrives.
         *
         * @param message     The message which was received
         * @param theMetadata A map containing additional information about the message,
         *                    where it came from, etc.
         */
        @Override
        public Message processMessage(Message message, Map theMetadata)
                throws ReceivingApplicationException, HL7Exception {
            // LogEvent.logFatal("OrderServlet", "processMessage", message.encode());
            // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "Received
            // message:\n" + message.printStructure());

            // TO DO add message signature verification for authentication of messages

            OrderWorker worker = new OrderWorker(message);

            worker.setInterpreter(SpringContext.getBean(IOrderInterpreter.class));
            worker.setExistanceChecker(SpringContext.getBean(IOrderExistanceChecker.class));
            worker.setPersister(SpringContext.getBean(IOrderPersister.class));

            OrderResult orderResult = worker.handleOrderRequest();

            // ACK response = null;
            ORL_O22 response = null;
            OML_O21 omlMessage;
            try {
                omlMessage = (OML_O21) message;

                if (orderResult == OrderResult.OK) {

                    response = ackToOrlO22((ACK) message.generateACK());

                } else if (orderResult == OrderResult.NON_CANCELABLE_ORDER
                        || orderResult == OrderResult.DUPLICATE_ORDER) {
                    response = ackToOrlO22((ACK) omlMessage.generateACK(AcknowledgmentCode.CR, null));
                    ERR err = createNewERRSegment("207", "Application internal error",
                            orderResult.toString() + " : " + worker.getExistanceCheckResult().toString(), response);
                    response.insertERR(err, 0);
                } else if (orderResult == OrderResult.MESSAGE_ERROR) {
                    response = ackToOrlO22((ACK) omlMessage.generateACK(AcknowledgmentCode.CR, null));

                    List<InterpreterResults> interpreterResults = worker.getMessageErrors();
                    int errorCnt = response.getERRReps();
                    ERR err = null;
                    for (InterpreterResults result : interpreterResults) {
                        switch (result) {
                        case MISSING_ORDER_NUMBER:
                        case MISSING_PATIENT_GUID:
                        case MISSING_PATIENT_DOB:
                        case MISSING_PATIENT_GENDER:
                        case MISSING_PATIENT_IDENTIFIER:
                        case MISSING_TESTS:
                            err = createNewERRSegment("101", "Required field missing", result.toString(), response);
                            response.insertERR(err, errorCnt++);
                            break;
                        case UNSUPPORTED_TESTS:
                            StringBuilder testResponseBuilder = new StringBuilder(result.toString());
                            List<String> unsupportedTests = worker.getUnsupportedTests();
                            if (!unsupportedTests.isEmpty()) {
                                testResponseBuilder.append("[");
                                testResponseBuilder.append(unsupportedTests.get(0));
                                for (int i = 1; i < unsupportedTests.size(); i++) {
                                    testResponseBuilder.append(",");
                                    testResponseBuilder.append(unsupportedTests.get(i));
                                }

                                testResponseBuilder.append("]");
                            }

                            err = createNewERRSegment("207", "Application internal error",
                                    testResponseBuilder.toString(), response);
                            response.insertERR(err, errorCnt++);
                            break;
                        case UNSUPPORTED_PANELS:
                            StringBuilder panelResponseBuilder = new StringBuilder(result.toString());
                            List<String> unsupportedPanels = worker.getUnsupportedPanels();
                            if (!unsupportedPanels.isEmpty()) {
                                panelResponseBuilder.append("[");
                                panelResponseBuilder.append(unsupportedPanels.get(0));
                                for (int i = 1; i < unsupportedPanels.size(); i++) {
                                    panelResponseBuilder.append(",");
                                    panelResponseBuilder.append(unsupportedPanels.get(i));
                                }

                                panelResponseBuilder.append("]");
                            }

                            err = createNewERRSegment("207", "Application internal error",
                                    panelResponseBuilder.toString(), response);
                            response.insertERR(err, errorCnt++);
                            break;
                        case UNKNOWN_REQUEST_TYPE:
                        case OTHER_THAN_PANEL_OR_TEST_REQUESTED:
                            err = createNewERRSegment("103", "Table value not found", result.toString(), response);
                            response.insertERR(err, errorCnt++);
                            break;
                        case INTERPRET_ERROR:
                            err = createNewERRSegment("207", "Application internal error", "Unexpected internal error",
                                    response);
                            response.insertERR(err, errorCnt++);
                        }
                    }
                } else {
                    response = ackToOrlO22((ACK) omlMessage.generateACK(AcknowledgmentCode.AE,
                            new HL7Exception("Unknown result thrown")));
                }

                addOrderInfo(response, omlMessage);
            } catch (IOException e) {
                throw new ReceivingApplicationException(e);
            }
            return response;
        }

        // add the order info from the original request into the response message
        private void addOrderInfo(ORL_O22 response, OML_O21 request) throws HL7Exception {
            response.getRESPONSE().getPATIENT().getPID().parse((request.getPATIENT().getPID().encode()));
            response.getRESPONSE().getPATIENT().getORDER().getORC().parse(request.getORDER().getORC().encode());
            response.getRESPONSE().getPATIENT().getORDER().getOBSERVATION_REQUEST().getOBR()
                    .parse(request.getORDER().getOBSERVATION_REQUEST().getOBR().encode());
        }

        // convert an ACK message to orl_o22 message
        private ORL_O22 ackToOrlO22(ACK ack) throws HL7Exception, IOException {
            ORL_O22 orl = new ORL_O22();
            orl.initQuickstart("ORL", "O22", "P");
            orl.getMSA().parse(ack.getMSA().encode());
            int errorCnt = 0;
            for (ERR err : ack.getERRAll()) {
                orl.insertERR(err, errorCnt++);
            }

            return orl;
        }

        private ERR createNewERRSegment(String HL70357Identifier, String HL70357Msg, String detail, ORL_O22 response)
                throws DataTypeException {
            ERR err = new ERR(response.getParent(), response.getModelClassFactory());
            CWE cwe = err.getHL7ErrorCode();
            cwe.getIdentifier().setValue(HL70357Identifier);
            cwe.getText().setValue(HL70357Msg);
            cwe.getOriginalText().setValue(detail);
            err.getSeverity().setValue("E");
            return err;
        }

        /*
         * private ERR createNewERRSegment(String HL70357Identifier, String HL70357Msg,
         * String detail, ACK response) throws DataTypeException{ ERR err = new
         * ERR(response.getParent(), response.getModelClassFactory()); CWE cwe =
         * err.getHL7ErrorCode(); cwe.getIdentifier().setValue(HL70357Identifier);
         * cwe.getText().setValue(HL70357Msg); cwe.getOriginalText().setValue(detail);
         * err.getSeverity().setValue("E"); return err; }
         */

        /** {@inheritDoc} */
        @Override
        public boolean canProcess(Message theMessage) {
            return true;
        }
    }
}
