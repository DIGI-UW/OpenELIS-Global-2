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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.medlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.medlab.dao.OrderSampleLinkDAO;
import org.openelisglobal.medlab.valueholder.OrderSampleLink;

/**
 * Unit tests for OrderSampleLinkServiceImpl.
 *
 * <p>
 * Tests the critical order-driven validation logic (FR-021, FR-025): samples
 * without corresponding orders MUST be rejected at QC.
 */
@RunWith(MockitoJUnitRunner.class)
public class OrderSampleLinkServiceImplTest {

    @Mock
    private OrderSampleLinkDAO orderSampleLinkDAO;

    @InjectMocks
    private OrderSampleLinkServiceImpl orderSampleLinkService;

    private OrderSampleLink testLink;

    @Before
    public void setUp() {
        testLink = new OrderSampleLink();
        testLink.setId(1);
        testLink.setElectronicOrderId(100);
        testLink.setSampleId(200);
        testLink.setTestId(300);
        testLink.setValidated(false);
        testLink.setCreatedAt(new Timestamp(System.currentTimeMillis()));
    }

    // ============================================================
    // Tests for hasOrderForSample - CRITICAL QC validation (FR-021, FR-025)
    // ============================================================

    @Test
    public void testHasOrderForSample_WhenOrderExists_ReturnsTrue() {
        // Arrange
        Integer sampleId = 200;
        when(orderSampleLinkDAO.hasOrderForSample(sampleId)).thenReturn(true);

        // Act
        boolean result = orderSampleLinkService.hasOrderForSample(sampleId);

        // Assert
        assertTrue("Sample with order link should return true", result);
        verify(orderSampleLinkDAO).hasOrderForSample(sampleId);
    }

    @Test
    public void testHasOrderForSample_WhenNoOrderExists_ReturnsFalse() {
        // Arrange - This is the CRITICAL case: samples without orders MUST be rejected
        Integer sampleId = 999;
        when(orderSampleLinkDAO.hasOrderForSample(sampleId)).thenReturn(false);

        // Act
        boolean result = orderSampleLinkService.hasOrderForSample(sampleId);

        // Assert - QC validation should reject this sample
        assertFalse("Sample without order link should return false (reject at QC)", result);
        verify(orderSampleLinkDAO).hasOrderForSample(sampleId);
    }

    // ============================================================
    // Tests for linkSampleToOrder
    // ============================================================

    @Test
    public void testLinkSampleToOrder_WhenLinkDoesNotExist_CreatesNewLink() {
        // Arrange
        Integer orderId = 100;
        Integer sampleId = 200;
        Integer testId = 300;
        Integer createdBy = 1;

        when(orderSampleLinkDAO.getLinkByOrderSampleTest(orderId, sampleId, testId)).thenReturn(null);
        when(orderSampleLinkDAO.insert(any(OrderSampleLink.class))).thenReturn(1);
        when(orderSampleLinkDAO.get(1)).thenReturn(Optional.of(testLink));

        // Act
        OrderSampleLink result = orderSampleLinkService.linkSampleToOrder(orderId, sampleId, null, testId, createdBy);

        // Assert
        assertNotNull("Should return created link", result);
        assertEquals("Order ID should match", Integer.valueOf(100), result.getElectronicOrderId());
        assertEquals("Sample ID should match", Integer.valueOf(200), result.getSampleId());
    }

    @Test
    public void testLinkSampleToOrder_WhenLinkExists_ReturnsExistingLink() {
        // Arrange
        Integer orderId = 100;
        Integer sampleId = 200;
        Integer testId = 300;
        Integer createdBy = 1;

        when(orderSampleLinkDAO.getLinkByOrderSampleTest(orderId, sampleId, testId)).thenReturn(testLink);

        // Act
        OrderSampleLink result = orderSampleLinkService.linkSampleToOrder(orderId, sampleId, null, testId, createdBy);

        // Assert
        assertNotNull("Should return existing link", result);
        assertEquals("Should return the existing link ID", Integer.valueOf(1), result.getId());
        verify(orderSampleLinkDAO, never()).insert(any(OrderSampleLink.class));
    }

    // ============================================================
    // Tests for linkSampleToOrderWithRequirements
    // ============================================================

    @Test
    public void testLinkSampleToOrderWithRequirements_CreatesLinkWithSnapshot() {
        // Arrange
        Integer orderId = 100;
        Integer sampleId = 200;
        Integer testId = 300;
        String containerType = "EDTA Tube";
        BigDecimal volume = new BigDecimal("5.0");
        String handling = "Keep refrigerated";
        Integer createdBy = 1;

        OrderSampleLink linkWithRequirements = new OrderSampleLink();
        linkWithRequirements.setId(2);
        linkWithRequirements.setElectronicOrderId(orderId);
        linkWithRequirements.setSampleId(sampleId);
        linkWithRequirements.setTestId(testId);
        linkWithRequirements.setContainerTypeRequired(containerType);
        linkWithRequirements.setVolumeRequiredMl(volume);
        linkWithRequirements.setHandlingRequirements(handling);

        when(orderSampleLinkDAO.getLinkByOrderSampleTest(orderId, sampleId, testId)).thenReturn(null);
        when(orderSampleLinkDAO.insert(any(OrderSampleLink.class))).thenReturn(2);
        when(orderSampleLinkDAO.get(2)).thenReturn(Optional.of(linkWithRequirements));

        // Act
        OrderSampleLink result = orderSampleLinkService.linkSampleToOrderWithRequirements(orderId, sampleId, null,
                testId, containerType, volume, handling, createdBy);

        // Assert
        assertNotNull("Should return created link with requirements", result);
        assertEquals("Container type should be captured", "EDTA Tube", result.getContainerTypeRequired());
        assertEquals("Volume should be captured", new BigDecimal("5.0"), result.getVolumeRequiredMl());
        assertEquals("Handling requirements should be captured", "Keep refrigerated", result.getHandlingRequirements());
    }

    // ============================================================
    // Tests for markAsValidated
    // ============================================================

    @Test
    public void testMarkAsValidated_WhenLinkExists_UpdatesValidationStatus() {
        // Arrange
        Integer linkId = 1;
        Integer validatorId = 5;

        OrderSampleLink validatedLink = new OrderSampleLink();
        validatedLink.setId(linkId);
        validatedLink.setValidated(true);
        validatedLink.setValidatedBy(validatorId);
        validatedLink.setValidatedAt(new Timestamp(System.currentTimeMillis()));

        when(orderSampleLinkDAO.get(linkId)).thenReturn(Optional.of(testLink));
        when(orderSampleLinkDAO.update(any(OrderSampleLink.class))).thenReturn(validatedLink);

        // Act
        OrderSampleLink result = orderSampleLinkService.markAsValidated(linkId, validatorId);

        // Assert
        assertNotNull("Should return validated link", result);
        assertTrue("Link should be marked as validated", result.getValidated());
        assertEquals("Validator ID should be set", Integer.valueOf(5), result.getValidatedBy());
    }

    @Test
    public void testMarkAsValidated_WhenLinkDoesNotExist_ReturnsNull() {
        // Arrange
        Integer linkId = 999;
        Integer validatorId = 5;

        when(orderSampleLinkDAO.get(linkId)).thenReturn(Optional.empty());

        // Act & Assert - Service's get() throws ObjectNotFoundException when Optional
        // is empty
        try {
            orderSampleLinkService.markAsValidated(linkId, validatorId);
            // If we get here, the test should fail
            assertTrue("Should have thrown ObjectNotFoundException", false);
        } catch (org.hibernate.ObjectNotFoundException e) {
            // Expected behavior - link not found
        }
        verify(orderSampleLinkDAO, never()).update(any(OrderSampleLink.class));
    }

    // ============================================================
    // Tests for getLinksByXXX methods
    // ============================================================

    @Test
    public void testGetLinksBySampleId_ReturnsLinksForSample() {
        // Arrange
        Integer sampleId = 200;
        List<OrderSampleLink> links = new ArrayList<>();
        links.add(testLink);

        when(orderSampleLinkDAO.getLinksBySampleId(sampleId)).thenReturn(links);

        // Act
        List<OrderSampleLink> result = orderSampleLinkService.getLinksBySampleId(sampleId);

        // Assert
        assertNotNull("Should return list of links", result);
        assertEquals("Should return 1 link", 1, result.size());
        assertEquals("Link should match test link", testLink.getId(), result.get(0).getId());
    }

    @Test
    public void testGetLinksByOrderId_ReturnsLinksForOrder() {
        // Arrange
        Integer orderId = 100;
        List<OrderSampleLink> links = new ArrayList<>();
        links.add(testLink);

        OrderSampleLink anotherLink = new OrderSampleLink();
        anotherLink.setId(2);
        anotherLink.setElectronicOrderId(orderId);
        anotherLink.setSampleId(201);
        links.add(anotherLink);

        when(orderSampleLinkDAO.getLinksByOrderId(orderId)).thenReturn(links);

        // Act
        List<OrderSampleLink> result = orderSampleLinkService.getLinksByOrderId(orderId);

        // Assert
        assertNotNull("Should return list of links", result);
        assertEquals("Should return 2 links for order", 2, result.size());
    }

    @Test
    public void testGetUnvalidatedLinks_ReturnsOnlyUnvalidatedLinks() {
        // Arrange
        List<OrderSampleLink> unvalidatedLinks = new ArrayList<>();
        unvalidatedLinks.add(testLink); // testLink has validated = false

        when(orderSampleLinkDAO.getUnvalidatedLinks()).thenReturn(unvalidatedLinks);

        // Act
        List<OrderSampleLink> result = orderSampleLinkService.getUnvalidatedLinks();

        // Assert
        assertNotNull("Should return list of unvalidated links", result);
        assertEquals("Should return 1 unvalidated link", 1, result.size());
        assertFalse("Link should not be validated", result.get(0).getValidated());
    }

    @Test
    public void testGetValidatedLinksByOrderId_ReturnsOnlyValidatedLinks() {
        // Arrange
        Integer orderId = 100;
        OrderSampleLink validatedLink = new OrderSampleLink();
        validatedLink.setId(2);
        validatedLink.setElectronicOrderId(orderId);
        validatedLink.setValidated(true);

        List<OrderSampleLink> validatedLinks = new ArrayList<>();
        validatedLinks.add(validatedLink);

        when(orderSampleLinkDAO.getValidatedLinksByOrderId(orderId)).thenReturn(validatedLinks);

        // Act
        List<OrderSampleLink> result = orderSampleLinkService.getValidatedLinksByOrderId(orderId);

        // Assert
        assertNotNull("Should return list of validated links", result);
        assertEquals("Should return 1 validated link", 1, result.size());
        assertTrue("Link should be validated", result.get(0).getValidated());
    }

    @Test
    public void testGetLinkByOrderSampleTest_ReturnsMatchingLink() {
        // Arrange
        Integer orderId = 100;
        Integer sampleId = 200;
        Integer testId = 300;

        when(orderSampleLinkDAO.getLinkByOrderSampleTest(orderId, sampleId, testId)).thenReturn(testLink);

        // Act
        OrderSampleLink result = orderSampleLinkService.getLinkByOrderSampleTest(orderId, sampleId, testId);

        // Assert
        assertNotNull("Should return matching link", result);
        assertEquals("Order ID should match", Integer.valueOf(100), result.getElectronicOrderId());
        assertEquals("Sample ID should match", Integer.valueOf(200), result.getSampleId());
        assertEquals("Test ID should match", Integer.valueOf(300), result.getTestId());
    }

    @Test
    public void testGetLinkByOrderSampleTest_WhenNoMatch_ReturnsNull() {
        // Arrange
        Integer orderId = 999;
        Integer sampleId = 999;
        Integer testId = 999;

        when(orderSampleLinkDAO.getLinkByOrderSampleTest(orderId, sampleId, testId)).thenReturn(null);

        // Act
        OrderSampleLink result = orderSampleLinkService.getLinkByOrderSampleTest(orderId, sampleId, testId);

        // Assert
        assertNull("Should return null when no match found", result);
    }
}
