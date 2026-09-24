package org.openelisglobal.dataexchange.service.order;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.systemuser.service.UserService;

/**
 * Incoming orders are scoped by lab unit the same way Results and Validation
 * are, but against the Reception role, which is the one that works the incoming
 * orders: an order is shown when one of its tests is in the user's Reception
 * lab units, and always shown when it cannot be tied to a test at all.
 */
@RunWith(MockitoJUnitRunner.class)
public class ElectronicOrderLabUnitScopeImplTest {

    private static final String USER = "42";
    private static final String HEMATOLOGY_TEST = "945";
    private static final String BIOCHEMISTRY_TEST = "4";
    private static final String PANEL_TEST = "966";

    @Mock
    private UserService userService;
    @Mock
    private FhirTransformService fhirTransformService;
    @Mock
    private FhirUtil fhirUtil;
    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private PanelService panelService;
    @Mock
    private PanelItemService panelItemService;
    @Mock
    private IGenericClient fhirClient;

    @Spy
    @InjectMocks
    private ElectronicOrderLabUnitScopeImpl scope;

    private final ElectronicOrder hematologyOrder = order("sr-hema");
    private final ElectronicOrder biochemistryOrder = order("sr-bio");
    private final ElectronicOrder unknownCodeOrder = order("sr-unknown");
    private final ElectronicOrder missingRequestOrder = order("sr-missing");
    private final ElectronicOrder panelOrder = order("sr-panel");

    @Before
    public void setUp() {
        lenient().when(fhirConfig.getLocalFhirStorePath()).thenReturn("https://fhir/");
        lenient().when(fhirUtil.getFhirClient(anyString())).thenReturn(fhirClient);

        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(request("sr-hema", "718-7"));
        bundle.addEntry().setResource(request("sr-bio", "2160-0"));
        bundle.addEntry().setResource(request("sr-unknown", "99999-9"));
        bundle.addEntry().setResource(request("sr-panel", "58410-2"));
        lenient().doReturn(bundle).when(scope).searchServiceRequests(any(), anyList());

        lenient().when(fhirTransformService.resolveTestsFromCodeableConcept(any(CodeableConcept.class)))
                .thenAnswer(invocation -> {
                    String code = ((CodeableConcept) invocation.getArgument(0)).getCodingFirstRep().getCode();
                    switch (code) {
                    case "718-7":
                        return List.of(test(HEMATOLOGY_TEST));
                    case "2160-0":
                        return List.of(test(BIOCHEMISTRY_TEST));
                    default:
                        return Collections.emptyList();
                    }
                });

        Panel panel = new Panel();
        panel.setId("12");
        lenient().when(panelService.getPanelByLoincCode("58410-2")).thenReturn(panel);
        PanelItem item = new PanelItem();
        item.setTest(test(PANEL_TEST));
        lenient().when(panelItemService.getPanelItemsForPanel("12")).thenReturn(List.of(item));
    }

    @Test
    public void aUserHoldingEveryLabUnitSeesTheListUnchangedWithoutReadingTheFhirStore() {
        when(userService.hasAllLabUnits(USER, Constants.ROLE_RECEPTION)).thenReturn(true);
        List<ElectronicOrder> orders = Arrays.asList(hematologyOrder, biochemistryOrder);

        assertSame(orders, scope.restrictToUserLabUnits(orders, USER));

        verify(fhirUtil, never()).getFhirClient(anyString());
    }

    @Test
    public void aRestrictedUserSeesOrdersForTestsInTheirLabUnitsOnly() {
        restrictTo(HEMATOLOGY_TEST);

        List<ElectronicOrder> visible = scope.restrictToUserLabUnits(Arrays.asList(hematologyOrder, biochemistryOrder),
                USER);

        assertEquals(ids(hematologyOrder), ids(visible));
    }

    @Test
    public void aUserWithSeveralLabUnitsSeesOrdersFromAnyOfThem() {
        restrictTo(HEMATOLOGY_TEST, BIOCHEMISTRY_TEST);

        List<ElectronicOrder> visible = scope.restrictToUserLabUnits(Arrays.asList(hematologyOrder, biochemistryOrder),
                USER);

        assertEquals(ids(hematologyOrder, biochemistryOrder), ids(visible));
    }

    @Test
    public void anOrderNoTestMatchesIsShownToEveryone() {
        restrictTo(BIOCHEMISTRY_TEST);

        List<ElectronicOrder> visible = scope
                .restrictToUserLabUnits(Arrays.asList(hematologyOrder, unknownCodeOrder, missingRequestOrder), USER);

        assertEquals(ids(unknownCodeOrder, missingRequestOrder), ids(visible));
    }

    @Test
    public void aPanelCodedOrderFollowsThePanelsTests() {
        restrictTo(PANEL_TEST);
        assertEquals(ids(panelOrder),
                ids(scope.restrictToUserLabUnits(Arrays.asList(panelOrder, hematologyOrder), USER)));

        restrictTo(HEMATOLOGY_TEST);
        assertEquals(ids(hematologyOrder),
                ids(scope.restrictToUserLabUnits(Arrays.asList(panelOrder, hematologyOrder), USER)));
    }

    @Test
    public void anUnreadableFhirStoreLeavesEveryOrderVisible() {
        restrictTo(BIOCHEMISTRY_TEST);
        doThrow(new IllegalStateException("store down")).when(scope).searchServiceRequests(any(), anyList());

        List<ElectronicOrder> visible = scope.restrictToUserLabUnits(Arrays.asList(hematologyOrder, biochemistryOrder),
                USER);

        assertEquals(ids(hematologyOrder, biochemistryOrder), ids(visible));
    }

    private void restrictTo(String... testIds) {
        when(userService.hasAllLabUnits(USER, Constants.ROLE_RECEPTION)).thenReturn(false);
        Set<String> allowed = new LinkedHashSet<>(Arrays.asList(testIds));
        when(userService.getTestIdsInUserLabUnits(eq(USER), eq(Constants.ROLE_RECEPTION))).thenReturn(allowed);
    }

    private static ElectronicOrder order(String externalId) {
        ElectronicOrder order = new ElectronicOrder();
        order.setExternalId(externalId);
        return order;
    }

    private static ServiceRequest request(String id, String loinc) {
        ServiceRequest request = new ServiceRequest();
        request.setId(id);
        request.getCode().addCoding().setSystem("http://loinc.org").setCode(loinc);
        return request;
    }

    private static org.openelisglobal.test.valueholder.Test test(String id) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(id);
        return test;
    }

    private static List<String> ids(ElectronicOrder... orders) {
        return ids(Arrays.asList(orders));
    }

    private static List<String> ids(List<ElectronicOrder> orders) {
        return orders.stream().map(ElectronicOrder::getExternalId).collect(Collectors.toList());
    }
}
