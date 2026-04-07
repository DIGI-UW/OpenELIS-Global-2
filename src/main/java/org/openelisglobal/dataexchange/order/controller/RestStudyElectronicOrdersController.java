package org.openelisglobal.dataexchange.order.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.ExternalOrderStatus;
import org.openelisglobal.dataexchange.order.ElectronicOrderSortOrderCategoryConvertor;
import org.openelisglobal.dataexchange.order.form.ElectronicOrderPaging;
import org.openelisglobal.dataexchange.order.form.ElectronicOrderViewForm;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderDisplayItem;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.organization.util.OrganizationTypeList;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestStudyElectronicOrdersController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "searchType", "searchValue", "startDate", "endDate",
            "testIds", "statusId", "useAllInfo", "organizationId", "organizationList", "qaEventId", "qaAuthorizer",
            "qaNote", "electronicOrderId", };

    @Autowired
    private ElectronicOrderService electronicOrderService;

    @InitBinder
    public void initBinder(final WebDataBinder webdataBinder) {
        webdataBinder.registerCustomEditor(ElectronicOrder.SortOrder.class,
                new ElectronicOrderSortOrderCategoryConvertor());
        webdataBinder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/rest/StudyElectronicOrders", method = RequestMethod.GET)
    public ElectronicOrderViewForm showStudyElectronicOrders(HttpServletRequest request,
            @ModelAttribute("form") @Valid ElectronicOrderViewForm form, BindingResult result)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        LogEvent.logDebug(this.getClass().getSimpleName(), "showStudyElectronicOrders",
                "Received request with parameters: " + "searchType=" + request.getParameter("searchType")
                        + ", searchValue=" + request.getParameter("searchValue"));

        try {
            form.setReferralFacilitySelectionList(
                    DisplayListService.getInstance().getList(ListType.REFERRAL_ORGANIZATIONS));
            form.setTestSelectionList(DisplayListService.getInstance().getList(ListType.ORDERABLE_TESTS));
            form.setStatusSelectionList(DisplayListService.getInstance().getList(ListType.ELECTRONIC_ORDER_STATUSES));
            form.setOrganizationList(OrganizationTypeList.ARV_ORGS.getList());
            form.setQaEvents(DisplayListService.getInstance().getList(ListType.QA_EVENTS));

            // Explicitly bind request parameters to form
            String searchType = request.getParameter("searchType");
            if (searchType != null) {
                try {
                    form.setSearchType(ElectronicOrderViewForm.SearchType.valueOf(searchType));
                    LogEvent.logDebug(this.getClass().getSimpleName(), "showStudyElectronicOrders",
                            "Successfully set searchType to: " + searchType);
                } catch (IllegalArgumentException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "showStudyElectronicOrders",
                            "Invalid searchType parameter: " + searchType);
                }
            }
            String searchValue = request.getParameter("searchValue");
            if (searchValue != null) {
                form.setSearchValue(searchValue);
            }
            String startDate = request.getParameter("startDate");
            if (startDate != null) {
                form.setStartDate(startDate);
            }
            String endDate = request.getParameter("endDate");
            if (endDate != null) {
                form.setEndDate(endDate);
            }
            String statusId = request.getParameter("statusId");
            if (statusId != null) {
                form.setStatusId(statusId);
            }

            List<ElectronicOrder> electronicOrders;
            List<ElectronicOrderDisplayItem> eOrderDisplayItems = new ArrayList<>();
            ElectronicOrderPaging paging = new ElectronicOrderPaging();
            String requestedPage = request.getParameter("page");
            if (GenericValidator.isBlankOrNull(requestedPage)) {
                if (form.getSearchType() != null) {
                    LogEvent.logDebug(this.getClass().getSimpleName(), "showStudyElectronicOrders",
                            "Searching for electronic orders with searchType: " + form.getSearchType());
                    electronicOrders = electronicOrderService.searchForStudyElectronicOrders(form);
                    LogEvent.logDebug(this.getClass().getSimpleName(), "showStudyElectronicOrders",
                            "Found " + electronicOrders.size() + " electronic orders");
                    eOrderDisplayItems = electronicOrders.stream()
                            .map(electronicOrderService::buildStudyElectronicOrderDisplayItem)
                            .collect(Collectors.toList());
                    paging.setDatabaseResults(request, form, eOrderDisplayItems);
                }
            } else {
                int requestedPageNumber = Integer.parseInt(requestedPage);
                // Sets the requested page in the response.
                paging.page(request, form, requestedPageNumber);
            }
            form.setSearchFinished(true);

            LogEvent.logDebug(this.getClass().getSimpleName(), "showStudyElectronicOrders",
                    "Successfully returning form with " + (form.geteOrders() != null ? form.geteOrders().size() : 0)
                            + " orders");

            return form;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "showStudyElectronicOrders",
                    "Error processing request: " + e.getMessage());
            LogEvent.logError(e);
            throw e;
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        LogEvent.logError(this.getClass().getSimpleName(), "handleException", "Unhandled exception: " + e.getMessage());
        LogEvent.logError(e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "studyElectronicOrderViewDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "eorder.study.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "eorder.study.title";
    }

    @RequestMapping(value = "/rest/rejectStudyElectronicOrder", method = RequestMethod.POST)
    public ResponseEntity<?> rejectStudyElectronicOrder(HttpServletRequest request,
            @ModelAttribute("form") @Valid ElectronicOrderViewForm form, BindingResult result) {
        LogEvent.logDebug(this.getClass().getSimpleName(), "rejectStudyElectronicOrder", "Rejecting electronic order: "
                + form.getQaEventId() + " for order ID: " + request.getParameter("electronicOrderId"));

        try {
            String electronicOrderId = request.getParameter("electronicOrderId");
            String qaEventId = request.getParameter("qaEventId");
            String qaNote = request.getParameter("qaNote");
            String qaAuthorizer = request.getParameter("qaAuthorizer");

            if (GenericValidator.isBlankOrNull(electronicOrderId)) {
                return ResponseEntity.badRequest().body("Electronic order ID is required");
            }

            if (GenericValidator.isBlankOrNull(qaEventId)) {
                return ResponseEntity.badRequest().body("Rejection reason is required");
            }

            ElectronicOrder electronicOrder = electronicOrderService.get(electronicOrderId);
            if (electronicOrder == null) {
                return ResponseEntity.badRequest().body("Electronic order not found");
            }

            // Set rejection details
            electronicOrder.setRejectReasonId(qaEventId);
            electronicOrder.setRejectComment(qaNote);
            electronicOrder.setQaAuthorizer(qaAuthorizer);

            // Set status to Cancelled (status_id = 22 for EXTERNAL_ORDER Cancelled)
            electronicOrder.setStatusId(
                    SpringContext.getBean(IStatusService.class).getStatusID(ExternalOrderStatus.Cancelled));

            // Update the electronic order
            electronicOrderService.update(electronicOrder);

            LogEvent.logDebug(this.getClass().getSimpleName(), "rejectStudyElectronicOrder",
                    "Successfully rejected electronic order: " + electronicOrderId);

            return ResponseEntity.ok().body(java.util.Collections.singletonMap("success", true));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "rejectStudyElectronicOrder",
                    "Error rejecting electronic order: " + e.getMessage());
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }
}
