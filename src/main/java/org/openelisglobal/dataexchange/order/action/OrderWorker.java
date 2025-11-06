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
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.order.action.IOrderExistanceChecker.CheckResult;
import org.openelisglobal.dataexchange.order.action.IOrderInterpreter.InterpreterResults;
import org.openelisglobal.dataexchange.order.action.IOrderInterpreter.OrderType;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.spring.util.SpringContext;

public class OrderWorker {
    public enum OrderResult {
        OK, DUPLICATE_ORDER, NON_CANCELABLE_ORDER, MESSAGE_ERROR
    }

    private final Message orderMessage;
    private IOrderInterpreter interpreter;
    private IOrderExistanceChecker existanceChecker;
    private IOrderPersister persister;
    private IStatusService statusService;
    private List<InterpreterResults> interpretResults;
    private CheckResult checkResult;

    public OrderWorker(Message message) {
        orderMessage = message;
    }

    public void setInterpreter(IOrderInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    public void setExistanceChecker(IOrderExistanceChecker orderExistanceChecker) {
        existanceChecker = orderExistanceChecker;
    }

    public void setPersister(IOrderPersister orderPersister) {
        persister = orderPersister;
    }

    private IStatusService getStatusService() {
        if (statusService == null) {
            statusService = SpringContext.getBean(IStatusService.class);
        }
        return statusService;
    }

    public void setStatusService(IStatusService statusService) {
        this.statusService = statusService;
    }

    public List<InterpreterResults> getMessageErrors() {
        return interpretResults;
    }

    public List<String> getUnsupportedTests() {
        return interpreter.getUnsupportedTests();
    }

    public List<String> getUnsupportedPanels() {
        return interpreter.getUnsupportedPanels();
    }

    public CheckResult getExistanceCheckResult() {
        return checkResult;
    }

    public OrderResult handleOrderRequest() throws IllegalStateException {
        if (interpreter == null || persister == null || existanceChecker == null) {
            throw new IllegalStateException("Interpreter, existanceChecker or persister have not been set");
        }

        interpretResults = interpreter.interpret(orderMessage);

        if (interpretResults.get(0) == InterpreterResults.OK) {
            String referringOrderNumber = interpreter.getReferringOrderNumber();
            String message = interpreter.getMessage();
            OrderType orderType = interpreter.getOrderType();
            MessagePatient patient = interpreter.getMessagePatient();

            checkResult = existanceChecker.check(referringOrderNumber);

            switch (checkResult) {
            case ORDER_FOUND_QUEUED:
                if (orderType == OrderType.CANCEL) {
                    cancelOrder(referringOrderNumber);
                    return OrderResult.OK;
                } else {
                    return OrderResult.DUPLICATE_ORDER;
                }
            case ORDER_FOUND_INPROGRESS:
                return orderType == OrderType.CANCEL ? OrderResult.NON_CANCELABLE_ORDER : OrderResult.DUPLICATE_ORDER;
            case NOT_FOUND:
                if (orderType == OrderType.CANCEL) {
                    return OrderResult.NON_CANCELABLE_ORDER;
                } else {
                    insertNewOrder(referringOrderNumber, message, patient);
                }
                break;
            case ORDER_FOUND_CANCELED:
                if (orderType == OrderType.CANCEL) {
                    return OrderResult.NON_CANCELABLE_ORDER;
                } else {
                    insertNewOrder(referringOrderNumber, message, patient);
                }
                break;
            }

        } else {
            return OrderResult.MESSAGE_ERROR;
        }

        return OrderResult.OK;
    }

    private void cancelOrder(String referringOrderNumber) {
        persister.cancelOrder(referringOrderNumber);
    }

    private void insertNewOrder(String referringOrderNumber, String message, MessagePatient patient) {
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setExternalId(referringOrderNumber);
        eOrder.setData(message);
        eOrder.setStatusId(getStatusService().getStatusID(ExternalOrderStatus.Entered));
        eOrder.setOrderTimestamp(DateUtil.getNowAsTimestamp());
        eOrder.setSysUserId(persister.getServiceUserId());
        eOrder.setPriority(OrderPriority.ROUTINE);
        eOrder.setType(ElectronicOrderType.HL7_V2);

        persister.persist(patient, eOrder);
    }
}
