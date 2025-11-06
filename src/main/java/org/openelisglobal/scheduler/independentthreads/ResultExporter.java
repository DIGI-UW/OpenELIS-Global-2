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
import jakarta.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalExport;
import org.openelisglobal.dataexchange.common.IRowTransmissionResponseHandler;
import org.openelisglobal.dataexchange.common.ReportTransmission;
import org.openelisglobal.dataexchange.common.ReportTransmission.HTTP_TYPE;
import org.openelisglobal.dataexchange.orderresult.valueholder.HL7MessageOut;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportExternalExportService;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportQueueTypeService;
import org.openelisglobal.dataexchange.service.orderresult.HL7MessageOutService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.reports.service.DocumentTrackService;
import org.openelisglobal.reports.service.DocumentTypeService;
import org.openelisglobal.reports.valueholder.DocumentTrack;
import org.openelisglobal.reports.valueholder.DocumentType;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ResultExporter {

    @Autowired
    private ReportQueueTypeService reportQueueTypeService;
    @Autowired
    private ReferenceTablesService referenceTablesService;
    @Autowired
    private DocumentTypeService documentTypeService;
    @Autowired
    private DocumentTrackService trackService;
    @Autowired
    private HL7MessageOutService hl7MessageService;
    @Autowired
    private ReportExternalExportService reportExternalExportService;

    private String resultReportTypeId;

    @PostConstruct
    public void setupGlobalVariables() {
        resultReportTypeId = reportQueueTypeService.getReportQueueTypeByName("Results").getId();
    }

    @Scheduled(fixedRateString = "#{resultsResendTime}")
    private void exportResults() {
        if (shouldReportResults()) {
            List<ReportExternalExport> reportList = reportExternalExportService
                    .getUnsentReportExports(resultReportTypeId);

            ReportTransmission transmitter = new ReportTransmission();
            String url = ConfigurationProperties.getInstance().getPropertyValue(Property.resultReportingURL);
            boolean sendAsychronously = false;

            for (ReportExternalExport report : reportList) {
                IRowTransmissionResponseHandler responseHandler = (IRowTransmissionResponseHandler) SpringContext
                        .getBean("successReportHandler");
                responseHandler.setRowId(report.getId());
                transmitter.sendRawReport(report.getData(), url, sendAsychronously, responseHandler, HTTP_TYPE.POST);
            }
        }
    }

    private boolean shouldReportResults() {
        String reportResults = ConfigurationProperties.getInstance().getPropertyValueLowerCase(Property.reportResults);
        return ("true".equals(reportResults) || "enable".equals(reportResults));
    }

    @Service("successReportHandler")
    @Scope("prototype")
    class SuccessReportHandler implements IRowTransmissionResponseHandler {
        String externalExportRowId;

        public SuccessReportHandler(String rowId) {
            externalExportRowId = rowId;
        }

        public SuccessReportHandler() {
        }

        @Override
        public void setRowId(String rowId) {
            externalExportRowId = rowId;
        }

        @Override
        @Transactional
        public void handleResponse(int httpReturnStatus, List<String> errors, String msg) {

            if (httpReturnStatus == HttpServletResponse.SC_OK) {
                ReportExternalExport report = reportExternalExportService.readReportExternalExport(externalExportRowId);
                List<DocumentTrack> documents = getSentDocuments(report.getBookkeepingData());

                try {
                    HL7MessageOut hl7Message = hl7MessageService.getByData(msg);
                    if (hl7Message != null) {
                        hl7Message.setStatus(HL7MessageOut.SUCCESS);
                        hl7MessageService.update(hl7Message);
                    }
                    for (DocumentTrack document : documents) {
                        trackService.insert(document);
                    }
                    reportExternalExportService.delete(report);

                } catch (LIMSRuntimeException e) {
                    LogEvent.logError(e);
                    throw e;
                }
            }
        }

        private List<DocumentTrack> getSentDocuments(String bookkeepingData) {
            List<DocumentTrack> documentList = new ArrayList<>();
            String resultTableId = getResultTableId();
            DocumentType type = getResultType();
            Timestamp now = DateUtil.getNowAsTimestamp();

            if (!GenericValidator.isBlankOrNull(bookkeepingData) && !"null".equals(bookkeepingData)) {
                String[] resultIdList = bookkeepingData.split(",");

                for (int i = 0; i < resultIdList.length; i++) {
                    DocumentTrack document = new DocumentTrack();
                    document.setDocumentTypeId(type.getId());
                    document.setRecordId(resultIdList[i]);
                    document.setReportTime(now);
                    document.setTableId(resultTableId);
                    document.setSysUserId("1");
                    documentList.add(document);
                }
            }
            return documentList;
        }

        private DocumentType getResultType() {
            return documentTypeService.getDocumentTypeByName("resultExport");
        }

        private String getResultTableId() {
            return referenceTablesService.getReferenceTableByName("RESULT").getId();
        }
    }
}
