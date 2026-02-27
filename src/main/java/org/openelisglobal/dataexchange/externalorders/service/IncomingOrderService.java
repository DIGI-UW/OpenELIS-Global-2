package org.openelisglobal.dataexchange.externalorders.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.dataexchange.externalorders.dto.ExternalOrderRequest;
import org.openelisglobal.dataexchange.externalorders.valueholder.IncomingOrder;
import org.openelisglobal.sample.form.SamplePatientEntryForm;

public interface IncomingOrderService {

    Integer receiveOrder(ExternalOrderRequest externalOrderRequest, String payloadJson, String receivedSysUserId);

    IncomingOrder receiveOrMergeOrder(ExternalOrderRequest externalOrderRequest, String payloadJson, String receivedSysUserId);

    List<IncomingOrder> getOrders();

    Optional<IncomingOrder> getOrderByExternalOrderNumber(String externalOrderNumber);

    IncomingOrder updateOrderByExternalOrderNumber(String externalOrderNumber, ExternalOrderRequest updatedRequest,
            String payloadJson, String updatedSysUserId);

    void finalizeHolding(String externalOrderNumber);

    void deleteHoldingByExternalOrderNumber(String externalOrderNumber);

    SamplePatientEntryForm buildSamplePatientEntryForm(String externalOrderNumber);
}
