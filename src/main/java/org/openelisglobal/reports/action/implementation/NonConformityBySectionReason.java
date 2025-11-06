/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.reports.action.implementation;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.services.QAService;
import org.openelisglobal.common.services.QAService.QAObservationType;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;

public abstract class NonConformityBySectionReason extends NonConformityBy {
    List<SampleQaEvent> sampleQaEvents = null;

    protected SampleQaEventService sampleQaEventService = SpringContext.getBean(SampleQaEventService.class);

    @Override
    protected String reportFileName() {
        return "NonConformityByGroupCategory";
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        reportParameters.put("reportTitle", MessageUtil.getMessage("reports.nonConformity.bySectionReason.title"));
        reportParameters.put("reportPeriod", dateRange.toString());
        reportParameters.put("supervisorSignature", ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.SIGNATURES_ON_NONCONFORMITY_REPORTS, "true"));
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.configurationName, "CI LNSP")) {
            reportParameters.put("headerName", "CILNSPHeader.jasper");
        } else {
            reportParameters.put("headerName", getHeaderName());
        }
    }

    protected abstract String getHeaderName();

    @Override
    void createReportItems() {
        sampleQaEvents = sampleQaEventService.getSampleQaEventsByUpdatedDate(dateRange.getLowDate(),
                DateUtil.addDaysToSQLDate(dateRange.getHighDate(), 1));
        reportItems = new ArrayList<>();
        // put them all in a list as reportable counts
        for (SampleQaEvent event : sampleQaEvents) {
            CountReportItem item = new CountReportItem();
            QAService qa = new QAService(event);
            item.setGroup(qa.getObservationForDisplay(QAObservationType.SECTION));
            item.setCategory(qa.getQAEvent().getLocalizedName());
            item.setCategoryCount(0);
            reportItems.add(item);
        }
        makeReportItemsSortable();
        sortReportItems(); // by group and category
        cleanupReportItems();
        totalReportItems();
    }

    /** */
    private void makeReportItemsSortable() {
        for (CountReportItem item : reportItems) {
            if (item.getGroup() == null) {
                item.setGroup("0");
            }
        }
    }

    private void cleanupReportItems() {
        for (CountReportItem item : reportItems) {
            if (item.getGroup() == "0") {
                item.setGroup(MessageUtil.getMessage("report.section.not.specified"));
            }
        }
    }

    private void totalReportItems() {
        if (reportItems.size() == 0) {
            return;
        }
        CountReportItem groupItem = reportItems.get(0);
        CountReportItem categoryItem = groupItem;
        for (CountReportItem item : reportItems) {
            if (!categoryItem.getCategory().equals(item.getCategory())) {
                categoryItem = item;
            }
            categoryItem.addCategoryCount(1);

            if (StringUtil.compareWithNulls(groupItem.getGroup(), item.getGroup()) != 0) {
                groupItem = item;
            }
            groupItem.addGroupCount(1);
        }
    }
}
