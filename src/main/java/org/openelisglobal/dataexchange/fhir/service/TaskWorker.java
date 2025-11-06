package org.openelisglobal.dataexchange.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.order.action.IOrderExistanceChecker;
import org.openelisglobal.dataexchange.order.action.IOrderExistanceChecker.CheckResult;
import org.openelisglobal.dataexchange.order.action.IOrderInterpreter.InterpreterResults;
import org.openelisglobal.dataexchange.order.action.IOrderInterpreter.OrderType;
import org.openelisglobal.dataexchange.order.action.IOrderPersister;
import org.openelisglobal.dataexchange.order.action.MessagePatient;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;

public class TaskWorker {

    @Autowired
    private FhirContext fhirContext;

    public enum TaskResult {
        OK, DUPLICATE_ORDER, NON_CANCELABLE_ORDER, MESSAGE_ERROR
    }

    private String message = "";
    private Task task = new Task();
    private ServiceRequest serviceRequest = null;
    private Patient patient = new Patient();
    private TaskInterpreter interpreter;
    private IOrderExistanceChecker existanceChecker;
    private IOrderPersister persister;
    private IStatusService statusService;
    private List<InterpreterResults> interpretResults;
    private CheckResult checkResult;

    public TaskWorker(Task incomingTask, String incomingMessage, ServiceRequest incomingServiceRequest,
            Patient incomingPatient) {
        task = incomingTask;
        message = incomingMessage;
        serviceRequest = incomingServiceRequest;
        patient = incomingPatient;
    }

    public void setInterpreter(TaskInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    public void setExistanceChecker(IOrderExistanceChecker orderExistanceChecker) {
        existanceChecker = orderExistanceChecker;
    }

    public void setPersister(IOrderPersister taskPersister) {
        persister = taskPersister;
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

    public TaskResult handleOrderRequest() throws IllegalStateException {
        if (interpreter == null || persister == null || existanceChecker == null) {
            throw new IllegalStateException("Interpreter, existanceChecker or persister have not been set");
        }

        interpretResults = interpreter.interpret(task, serviceRequest, patient);
        String referringOrderNumber = interpreter.getReferringOrderNumber();
        OrderType orderType = interpreter.getOrderType();
        OrderPriority priority = interpreter.getOrderPriority();
        MessagePatient patient = interpreter.getMessagePatient();
        checkResult = existanceChecker.check(referringOrderNumber);

        if (interpretResults.get(0) == InterpreterResults.OK) {

            // checkResult = existanceChecker.check(referringOrderNumber);
            switch (checkResult) {
            case ORDER_FOUND_QUEUED:
                if (orderType == OrderType.CANCEL) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                            "cancelling order: " + referringOrderNumber);
                    cancelOrder(referringOrderNumber);
                    return TaskResult.OK;
                } else {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                            "duplicate order found: " + referringOrderNumber);
                    return TaskResult.DUPLICATE_ORDER;
                }
            case ORDER_FOUND_INPROGRESS:
                LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                        "order: " + referringOrderNumber + " is duplicate or already in progress");
                return orderType == OrderType.CANCEL ? TaskResult.NON_CANCELABLE_ORDER : TaskResult.DUPLICATE_ORDER;
            case NOT_FOUND:
                if (orderType == OrderType.CANCEL) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                            "cant cancel order not found");
                    return TaskResult.NON_CANCELABLE_ORDER;
                } else {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                            "no order found, entering order: " + referringOrderNumber);
                    insertNewOrder(referringOrderNumber, message, patient, priority, ExternalOrderStatus.Entered);
                    return TaskResult.OK;
                }
            case ORDER_FOUND_CANCELED:
                if (orderType == OrderType.CANCEL) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                            "can't cancel already cancelled order: " + referringOrderNumber);
                    return TaskResult.NON_CANCELABLE_ORDER;
                } else {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                            "order found cancelled, entering order: " + referringOrderNumber);
                    insertNewOrder(referringOrderNumber, message, patient, priority, ExternalOrderStatus.Entered);
                    return TaskResult.OK;
                }
            default:
                LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                        "undetermined issue in correctly interpreted request: " + interpretResults.get(0).toString()
                                + " check result: " + checkResult + " for: " + referringOrderNumber + " " + orderType
                                + " ");
                insertNewOrder(referringOrderNumber, message, patient, priority, ExternalOrderStatus.NonConforming);
                return TaskResult.MESSAGE_ERROR;
            }

        } else if (interpretResults.get(0) == InterpreterResults.UNSUPPORTED_TESTS
                && checkResult == CheckResult.ORDER_FOUND_QUEUED) {
            if (orderType == OrderType.CANCEL) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                        "cancelling order: " + referringOrderNumber + " despite wrong test specified");
                cancelOrder(referringOrderNumber);
                return TaskResult.OK;
            } else {
                LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                        "order: " + referringOrderNumber + " already entered");
                return TaskResult.DUPLICATE_ORDER;
            }
        } else if (interpretResults.get(0) == InterpreterResults.UNSUPPORTED_TESTS) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest",
                    "TaskWorker:unsupported tests: " + referringOrderNumber + orderType);
            insertNewOrder(referringOrderNumber, message, patient, priority, ExternalOrderStatus.NonConforming);
            return TaskResult.MESSAGE_ERROR;
        } else if (interpretResults.get(0) == InterpreterResults.MISSING_PATIENT_GUID
                || interpretResults.get(0) == InterpreterResults.MISSING_PATIENT_DOB
                || interpretResults.get(0) == InterpreterResults.MISSING_PATIENT_GENDER
                || interpretResults.get(0) == InterpreterResults.MISSING_PATIENT_IDENTIFIER) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest", "missing patient info: "
                    + interpretResults.get(0).toString() + "for" + referringOrderNumber + " " + orderType + " ");
            insertNewOrder(referringOrderNumber, message, patient, priority, ExternalOrderStatus.NonConforming);
            return TaskResult.MESSAGE_ERROR;
        } else {
            LogEvent.logDebug(this.getClass().getSimpleName(), "handleOrderRequest", "undetermined issue: "
                    + interpretResults.get(0).toString() + " for: " + referringOrderNumber + " " + orderType + " ");
            insertNewOrder(referringOrderNumber, message, patient, priority, ExternalOrderStatus.NonConforming);
            return TaskResult.MESSAGE_ERROR;
        }
    }

    private void cancelOrder(String referringOrderNumber) {
        LogEvent.logDebug(this.getClass().getSimpleName(), "cancelOrder", "cancelOrder: ");
        persister.cancelOrder(referringOrderNumber);
    }

    private void insertNewOrder(String referringOrderNumber, String message, MessagePatient patient,
            OrderPriority orderPriority, ExternalOrderStatus eoStatus) {
        LogEvent.logDebug(this.getClass().getSimpleName(), "insertNewOrder",
                "TaskWorker:insertNewOrder: " + referringOrderNumber);
        ElectronicOrder eOrder = new ElectronicOrder();
        eOrder.setExternalId(referringOrderNumber);
        eOrder.setData(message);
        eOrder.setStatusId(getStatusService().getStatusID(eoStatus));
        eOrder.setOrderTimestamp(DateUtil.getNowAsTimestamp());
        eOrder.setSysUserId(persister.getServiceUserId());
        eOrder.setType(ElectronicOrderType.FHIR);
        eOrder.setPriority(orderPriority);

        persister.persist(patient, eOrder);
    }
}
