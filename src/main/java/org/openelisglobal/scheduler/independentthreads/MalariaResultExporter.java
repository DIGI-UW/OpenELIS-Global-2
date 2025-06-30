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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.scheduler.independentthreads;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalExport;
import org.openelisglobal.dataexchange.common.IRowTransmissionResponseHandler;
import org.openelisglobal.dataexchange.common.ReportTransmission;
import org.openelisglobal.dataexchange.common.ReportTransmission.HTTP_TYPE;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportExternalExportService;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportQueueTypeService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MalariaResultExporter {

    @Autowired
    private ReportQueueTypeService reportQueueTypeService;
    @Autowired
    private ReportExternalExportService reportExternalExportService;

    private String resultReportTypeId;

    @PostConstruct
    private void initializeGlobalVariables() {
        resultReportTypeId = reportQueueTypeService.getReportQueueTypeByName("malariaCase").getId();
    }

    @Scheduled(fixedRateString = "#{resultsResendTime}")
    private void exportResults() {
        if (shouldReportResults()) {
            List<ReportExternalExport> reportList = reportExternalExportService
                    .getUnsentReportExports(resultReportTypeId);

            ReportTransmission transmitter = new ReportTransmission();
            String url = ConfigurationProperties.getInstance().getPropertyValue(Property.malariaCaseReportURL);
            boolean sendAsychronously = false;

            for (ReportExternalExport report : reportList) {
                IRowTransmissionResponseHandler responseHandler = (IRowTransmissionResponseHandler) SpringContext
                        .getBean("malariaSuccessReportHandler");
                responseHandler.setRowId(report.getId());
                transmitter.sendRawReport(report.getData(), url, sendAsychronously, responseHandler, HTTP_TYPE.POST);
            }
        }
    }

    private boolean shouldReportResults() {
        String reportResults = ConfigurationProperties.getInstance()
                .getPropertyValueLowerCase(Property.malariaCaseReport);
        return ("true".equals(reportResults) || "enable".equals(reportResults));
    }
}
