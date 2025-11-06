package org.openelisglobal.dataexchange.service.order;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.order.dao.ElectronicOrderDAO;
import org.openelisglobal.dataexchange.order.form.ElectronicOrderViewForm;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder.SortOrder;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ElectronicOrderServiceImpl extends AuditableBaseObjectServiceImpl<ElectronicOrder, String>
        implements ElectronicOrderService {
    @Autowired
    protected ElectronicOrderDAO baseObjectDAO;
    @Autowired
    protected IStatusService statusService;
    @Autowired
    protected OrganizationService organizationService;
    @Autowired
    protected TestService testService;
    @Autowired
    protected FhirUtil fhirUtil;
    @Autowired
    private FhirConfig fhirConfig;

    ElectronicOrderServiceImpl() {
        super(ElectronicOrder.class);
    }

    @Override
    protected ElectronicOrderDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicOrder> getAllElectronicOrdersOrderedBy(SortOrder order) {
        return getBaseObjectDAO().getAllElectronicOrdersOrderedBy(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicOrder> getElectronicOrdersByExternalId(String id) {
        return getBaseObjectDAO().getElectronicOrdersByExternalId(id);
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersContainingValueOrderedBy(String searchValue, SortOrder order) {

        List<ElectronicOrder> searchResult = getBaseObjectDAO()
                .getAllElectronicOrdersContainingValueOrderedBy(searchValue, order);

        if (searchResult != null && searchResult.size() > 0) {
            return searchResult;
        }
        // this is done in case sample lab number was used to search instead of the
        // order lab number
        if (searchValue != null && searchValue.contains(".")) {
            searchValue = searchValue.substring(0, searchValue.indexOf('.'));
        }
        return getBaseObjectDAO().getAllElectronicOrdersContainingValueOrderedBy(searchValue, order);
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersContainingValuesOrderedBy(String accessionNumber,
            String patientLastName, String patientFirstName, String gender, SortOrder order) {
        return getBaseObjectDAO().getAllElectronicOrdersContainingValuesOrderedBy(accessionNumber, patientLastName,
                patientFirstName, gender, order);
    }

    @Override
    public List<ElectronicOrder> getElectronicOrdersContainingValueExludedByOrderedBy(String searchValue,
            List<ExternalOrderStatus> excludedStatuses, SortOrder sortOrder) {
        List<Integer> exludedStatusIds = new ArrayList<>();
        for (ExternalOrderStatus status : excludedStatuses) {
            String statusId = statusService.getStatusID(status);
            if (!GenericValidator.isBlankOrNull(statusId)) {
                exludedStatusIds.add(Integer.parseInt(statusId));
            }
        }

        return getBaseObjectDAO().getElectronicOrdersContainingValueExludedByOrderedBy(searchValue, exludedStatusIds,
                sortOrder);
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersByDateAndStatus(Date startDate, Date endDate, String statusId,
            SortOrder sortOrder) {
        return getBaseObjectDAO().getAllElectronicOrdersByDateAndStatus(startDate, endDate, statusId, sortOrder);
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersByTimestampAndStatus(Timestamp startTimestamp,
            Timestamp endTimestamp, String statusId, SortOrder sortOrder) {
        return getBaseObjectDAO().getAllElectronicOrdersByTimestampAndStatus(startTimestamp, endTimestamp, statusId,
                sortOrder);
    }

    @Override
    public int getCountOfElectronicOrdersByTimestampAndStatus(Timestamp startTimestamp, Timestamp endTimestamp,
            String statusId) {
        return getBaseObjectDAO().getCountOfElectronicOrdersByTimestampAndStatus(startTimestamp, endTimestamp,
                statusId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicOrder> searchForElectronicOrders(ElectronicOrderViewForm form) {
        switch (form.getSearchType()) {
        case IDENTIFIER:
            IGenericClient fhirClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());
            Bundle searchBundle = fhirClient.search().forResource(ServiceRequest.class)
                    .where(ServiceRequest.IDENTIFIER.exactly().code(form.getSearchValue())).returnBundle(Bundle.class)
                    .execute();

            List<String> identifierValues = new ArrayList<>(searchBundle.getEntry().size() + 1);
            identifierValues.add(form.getSearchValue());
            for (BundleEntryComponent bundleEntry : searchBundle.getEntry()) {
                if (bundleEntry.hasResource()
                        && ResourceType.ServiceRequest.equals(bundleEntry.getResource().getResourceType())) {
                    identifierValues.add(bundleEntry.getResource().getIdElement().getIdPart());
                }
            }
            String nameValue = form.getSearchValue();

            List<ElectronicOrder> eOrders = baseObjectDAO.getAllElectronicOrdersMatchingAnyValue(identifierValues,
                    nameValue, SortOrder.LAST_UPDATED_ASC);

            return eOrders;
        case DATE_STATUS:
            String startDate = form.getStartDate();
            String endDate = form.getEndDate();
            if (GenericValidator.isBlankOrNull(startDate) && !GenericValidator.isBlankOrNull(endDate)) {
                startDate = endDate;
            }
            if (GenericValidator.isBlankOrNull(endDate) && !GenericValidator.isBlankOrNull(startDate)) {
                endDate = startDate;
            }
            java.sql.Timestamp startTimestamp = GenericValidator.isBlankOrNull(startDate) ? null
                    : DateUtil.convertStringDateStringTimeToTimestamp(startDate, "00:00:00.0");
            java.sql.Timestamp endTimestamp = GenericValidator.isBlankOrNull(endDate) ? null
                    : DateUtil.convertStringDateStringTimeToTimestamp(endDate, "23:59:59");
            return getAllElectronicOrdersByTimestampAndStatus(startTimestamp, endTimestamp, form.getStatusId(),
                    SortOrder.STATUS_ID);
        default:
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicOrder> searchForStudyElectronicOrders(ElectronicOrderViewForm form) {
        switch (form.getSearchType()) {
        case IDENTIFIER:
            IGenericClient fhirClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());
            Bundle searchBundle = fhirClient.search().forResource(ServiceRequest.class)
                    .where(ServiceRequest.IDENTIFIER.exactly().code(form.getSearchValue())).returnBundle(Bundle.class)
                    .execute();

            List<String> identifierValues = new ArrayList<>(searchBundle.getEntry().size() + 1);
            identifierValues.add(form.getSearchValue());
            for (BundleEntryComponent bundleEntry : searchBundle.getEntry()) {
                if (bundleEntry.hasResource()
                        && ResourceType.ServiceRequest.equals(bundleEntry.getResource().getResourceType())) {
                    identifierValues.add(bundleEntry.getResource().getIdElement().getIdPart());
                }
            }
            String nameValue = form.getSearchValue();

            List<ElectronicOrder> eOrders = baseObjectDAO.getAllElectronicOrdersMatchingAnyValue(identifierValues,
                    nameValue, SortOrder.LAST_UPDATED_ASC);

            return eOrders;
        case DATE_STATUS:
            String startDate = form.getStartDate();
            String endDate = form.getEndDate();
            if (GenericValidator.isBlankOrNull(startDate) && !GenericValidator.isBlankOrNull(endDate)) {
                startDate = endDate;
            }
            if (GenericValidator.isBlankOrNull(endDate) && !GenericValidator.isBlankOrNull(startDate)) {
                endDate = startDate;
            }
            java.sql.Timestamp startTimestamp = GenericValidator.isBlankOrNull(startDate) ? null
                    : DateUtil.convertStringDateStringTimeToTimestamp(startDate, "00:00:00.0");
            java.sql.Timestamp endTimestamp = GenericValidator.isBlankOrNull(endDate) ? null
                    : DateUtil.convertStringDateStringTimeToTimestamp(endDate, "23:59:59");
            return getAllElectronicOrdersByTimestampAndStatus(startTimestamp, endTimestamp, form.getStatusId(),
                    SortOrder.STATUS_ID);
        default:
            return null;
        }
    }

    @Override
    public int getCountOfAllElectronicOrdersByDateAndStatus(Date startDate, Date endDate, String statusId) {
        return getBaseObjectDAO().getCountOfAllElectronicOrdersByDateAndStatus(startDate, endDate, statusId);
    }

    @Override
    public List<ElectronicOrder> getAllElectronicOrdersByStatusList(List<Integer> statusIds, SortOrder sortOrder) {
        return getBaseObjectDAO().getAllElectronicOrdersByStatusList(statusIds, sortOrder);
    }

    @Override
    public int getCountOfElectronicOrdersByStatusList(List<Integer> statusIds) {
        return getBaseObjectDAO().getCountOfElectronicOrdersByStatusList(statusIds);
    }
}
