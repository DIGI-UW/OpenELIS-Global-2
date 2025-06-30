package org.openelisglobal.dataexchange.fhir.service;

import java.util.List;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.dataexchange.order.action.IOrderInterpreter.InterpreterResults;
import org.openelisglobal.dataexchange.order.action.IOrderInterpreter.OrderType;
import org.openelisglobal.dataexchange.order.action.MessagePatient;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.test.valueholder.Test;

public interface TaskInterpreter {

    public List<String> getUnsupportedTests();

    public List<String> getUnsupportedPanels();

    String getReferringOrderNumber();

    String getMessage();

    MessagePatient getMessagePatient();

    public OrderType getOrderType();

    List<InterpreterResults> getResultStatus();

    Test getTest();

    Panel getPanel();

    OrderPriority getOrderPriority();

    List<InterpreterResults> interpret(Task incomingTask, ServiceRequest incomingServiceRequest,
            Patient incomingPatient);
}
