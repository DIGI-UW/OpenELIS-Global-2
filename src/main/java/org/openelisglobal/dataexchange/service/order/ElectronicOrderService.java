package org.openelisglobal.dataexchange.service.order;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.order.form.ElectronicOrderViewForm;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder.SortOrder;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ElectronicOrderService extends BaseObjectService<ElectronicOrder, String> {

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<ElectronicOrder> getAllElectronicOrdersOrderedBy(ElectronicOrder.SortOrder order);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<ElectronicOrder> getElectronicOrdersByExternalId(String id);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<ElectronicOrder> getAllElectronicOrdersContainingValueOrderedBy(String searchValue, SortOrder sortOrder);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<ElectronicOrder> getAllElectronicOrdersContainingValuesOrderedBy(String accessionNumber,
            String patientLastName, String patientFirstName, String gender, SortOrder order);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<ElectronicOrder> getElectronicOrdersContainingValueExludedByOrderedBy(String searchValue,
            List<ExternalOrderStatus> excludedStatuses, SortOrder sortOrder);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<ElectronicOrder> getAllElectronicOrdersByDateAndStatus(Date startDate, Date endDate, String statusId,
            SortOrder sortOrder);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    int getCountOfAllElectronicOrdersByDateAndStatus(Date startDate, Date endDate, String statusId);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<ElectronicOrder> getAllElectronicOrdersByTimestampAndStatus(Timestamp startTimestamp, Timestamp endTimestamp,
            String statusId, SortOrder sortOrder);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    int getCountOfElectronicOrdersByTimestampAndStatus(Timestamp startTimestamp, Timestamp endTimestamp,
            String statusId);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    int getCountOfElectronicOrdersByStatusList(List<String> statusIds);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<ElectronicOrder> getAllElectronicOrdersByStatusList(List<String> statusIds, SortOrder sortOrder);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<ElectronicOrder> searchForElectronicOrders(ElectronicOrderViewForm form);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<ElectronicOrder> searchForStudyElectronicOrders(ElectronicOrderViewForm form);
}
