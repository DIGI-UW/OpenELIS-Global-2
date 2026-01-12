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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.medlab.dao.OrderSampleLinkDAO;
import org.openelisglobal.medlab.valueholder.OrderSampleLink;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for OrderSampleLink operations.
 *
 * <p>
 * Enables order-driven validation (FR-021, FR-025):
 * <ul>
 * <li>Samples without corresponding orders are rejected at QC
 * <li>Links samples to their fulfilling orders with requirements snapshot
 * </ul>
 */
@Service
public class OrderSampleLinkServiceImpl extends AuditableBaseObjectServiceImpl<OrderSampleLink, Integer>
        implements OrderSampleLinkService {

    @Autowired
    private OrderSampleLinkDAO orderSampleLinkDAO;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TestService testService;

    public OrderSampleLinkServiceImpl() {
        super(OrderSampleLink.class);
    }

    @Override
    protected OrderSampleLinkDAO getBaseObjectDAO() {
        return orderSampleLinkDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOrderForSample(Integer sampleId) {
        return orderSampleLinkDAO.hasOrderForSample(sampleId);
    }

    @Override
    @Transactional
    public OrderSampleLink linkSampleToOrder(Integer electronicOrderId, Integer sampleId, Integer sampleItemId,
            Integer testId, Integer createdBy) {
        return linkSampleToOrderWithRequirements(electronicOrderId, sampleId, sampleItemId, testId, null, null, null,
                createdBy);
    }

    @Override
    @Transactional
    public OrderSampleLink linkSampleToOrderWithRequirements(Integer electronicOrderId, Integer sampleId,
            Integer sampleItemId, Integer testId, String containerTypeRequired, BigDecimal volumeRequiredMl,
            String handlingRequirements, Integer createdBy) {

        // Check if link already exists
        OrderSampleLink existing = orderSampleLinkDAO.getLinkByOrderSampleTest(electronicOrderId, sampleId, testId);
        if (existing != null) {
            return existing;
        }

        // Create new link
        OrderSampleLink link = new OrderSampleLink();
        link.setElectronicOrderId(electronicOrderId);
        link.setSampleId(sampleId);
        link.setSampleItemId(sampleItemId);
        link.setTestId(testId);
        link.setContainerTypeRequired(containerTypeRequired);
        link.setVolumeRequiredMl(volumeRequiredMl);
        link.setHandlingRequirements(handlingRequirements);
        link.setCreatedBy(createdBy);
        link.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        link.setValidated(false);

        Integer id = insert(link);

        // Create Analysis record if testId and sampleItemId are provided
        if (testId != null && sampleItemId != null) {
            createAnalysisForLink(sampleItemId, testId, createdBy);
        }

        return get(id);
    }

    /**
     * Creates an Analysis record for a test linked to a sample item. This enables
     * the sample to show tests in the workflow.
     */
    private void createAnalysisForLink(Integer sampleItemId, Integer testId, Integer createdBy) {
        // Check if Analysis already exists for this sample item and test
        SampleItem sampleItem = sampleItemService.get(String.valueOf(sampleItemId));
        if (sampleItem == null) {
            return;
        }

        Test test = testService.get(String.valueOf(testId));
        if (test == null) {
            return;
        }

        // Check for existing analysis
        List<Analysis> existingAnalyses = analysisService.getAnalysesBySampleItem(sampleItem);
        if (existingAnalyses != null) {
            for (Analysis existing : existingAnalyses) {
                if (existing.getTest() != null && existing.getTest().getId().equals(String.valueOf(testId))) {
                    // Analysis already exists for this test
                    return;
                }
            }
        }

        // Create new Analysis
        Analysis analysis = new Analysis();
        analysis.setSampleItem(sampleItem);
        analysis.setTest(test);
        analysis.setAnalysisType("MANUAL");
        analysis.setSysUserId(String.valueOf(createdBy));

        // Set status to "Not Started" (Entered)
        IStatusService statusService = SpringContext.getBean(IStatusService.class);
        analysis.setStatusId(statusService.getStatusID(AnalysisStatus.NotStarted));

        // Set started date (Analysis.startedDate is java.sql.Date)
        analysis.setStartedDate(DateUtil.getNowAsSqlDate());

        analysisService.insert(analysis);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSampleLink> getLinksBySampleId(Integer sampleId) {
        return orderSampleLinkDAO.getLinksBySampleId(sampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSampleLink> getLinksByOrderId(Integer electronicOrderId) {
        return orderSampleLinkDAO.getLinksByOrderId(electronicOrderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSampleLink> getLinksBySampleItemId(Integer sampleItemId) {
        return orderSampleLinkDAO.getLinksBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSampleLink> getValidatedLinksByOrderId(Integer electronicOrderId) {
        return orderSampleLinkDAO.getValidatedLinksByOrderId(electronicOrderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSampleLink> getUnvalidatedLinks() {
        return orderSampleLinkDAO.getUnvalidatedLinks();
    }

    @Override
    @Transactional
    public OrderSampleLink markAsValidated(Integer linkId, Integer validatorId) {
        OrderSampleLink link = get(linkId);
        if (link != null) {
            link.markValidated(validatorId);
            return update(link);
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSampleLink getLinkByOrderSampleTest(Integer electronicOrderId, Integer sampleId, Integer testId) {
        return orderSampleLinkDAO.getLinkByOrderSampleTest(electronicOrderId, sampleId, testId);
    }
}
