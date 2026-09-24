package org.openelisglobal.dataexchange.service.order;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * An electronic order names its tests in the ServiceRequest it arrived with,
 * which lives in the local FHIR store rather than on the order row. The
 * requests behind a page of orders are read in batches, each order's codes are
 * resolved to OpenELIS tests the same way an order is resolved when it is
 * imported, and the tests' lab units are matched against the user's Results lab
 * units. An order whose codes resolve to no test, or whose request cannot be
 * read, is kept: without a test there is no lab unit to judge it by.
 */
@Service
@Transactional(readOnly = true)
public class ElectronicOrderLabUnitScopeImpl implements ElectronicOrderLabUnitScope {

    private static final int REQUESTS_PER_READ = 50;
    private static final String LOINC_SYSTEM = "http://loinc.org";

    @Autowired
    private UserService userService;
    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private PanelService panelService;
    @Autowired
    private PanelItemService panelItemService;

    @Override
    public List<ElectronicOrder> restrictToUserLabUnits(List<ElectronicOrder> orders, String systemUserId) {
        if (orders == null || orders.isEmpty() || userService.hasAllLabUnits(systemUserId, Constants.ROLE_RESULTS)) {
            return orders;
        }
        Set<String> allowedTestIds = userService.getTestIdsInUserLabUnits(systemUserId, Constants.ROLE_RESULTS);
        Map<String, ServiceRequest> requests = readServiceRequests(orders);
        List<ElectronicOrder> visible = new ArrayList<>();
        for (ElectronicOrder order : orders) {
            Set<String> orderedTestIds = orderedTestIds(requests.get(order.getExternalId()));
            if (orderedTestIds.isEmpty() || orderedTestIds.stream().anyMatch(allowedTestIds::contains)) {
                visible.add(order);
            }
        }
        return visible;
    }

    private Map<String, ServiceRequest> readServiceRequests(List<ElectronicOrder> orders) {
        List<String> ids = orders.stream().map(ElectronicOrder::getExternalId)
                .filter(id -> !GenericValidator.isBlankOrNull(id)).distinct().collect(Collectors.toList());
        Map<String, ServiceRequest> byId = new HashMap<>();
        if (ids.isEmpty()) {
            return byId;
        }
        IGenericClient client = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());
        for (int from = 0; from < ids.size(); from += REQUESTS_PER_READ) {
            List<String> batch = ids.subList(from, Math.min(ids.size(), from + REQUESTS_PER_READ));
            try {
                for (BundleEntryComponent entry : searchServiceRequests(client, batch).getEntry()) {
                    if (entry.getResource() instanceof ServiceRequest request) {
                        byId.put(request.getIdElement().getIdPart(), request);
                    }
                }
            } catch (RuntimeException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "readServiceRequests",
                        "orders whose request could not be read stay visible: " + e.getMessage());
            }
        }
        return byId;
    }

    /** One read of the store for a batch of request ids. */
    Bundle searchServiceRequests(IGenericClient client, List<String> ids) {
        return client.search().forResource(ServiceRequest.class).where(ServiceRequest.RES_ID.exactly().codes(ids))
                .count(ids.size()).returnBundle(Bundle.class).execute();
    }

    private Set<String> orderedTestIds(ServiceRequest request) {
        if (request == null || !request.hasCode()) {
            return Collections.emptySet();
        }
        Set<String> testIds = new HashSet<>();
        for (Test test : fhirTransformService.resolveTestsFromCodeableConcept(request.getCode())) {
            testIds.add(test.getId());
        }
        for (Coding coding : request.getCode().getCoding()) {
            if (LOINC_SYSTEM.equalsIgnoreCase(coding.getSystem()) && coding.hasCode()) {
                Panel panel = panelService.getPanelByLoincCode(coding.getCode());
                if (panel != null) {
                    for (PanelItem item : panelItemService.getPanelItemsForPanel(panel.getId())) {
                        if (item.getTest() != null) {
                            testIds.add(item.getTest().getId());
                        }
                    }
                }
            }
        }
        return testIds;
    }
}
