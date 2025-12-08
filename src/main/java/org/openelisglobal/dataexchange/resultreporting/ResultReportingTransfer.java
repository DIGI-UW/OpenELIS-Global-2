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
package org.openelisglobal.dataexchange.resultreporting;

import jakarta.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalExport;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportQueueType;
import org.openelisglobal.dataexchange.common.ITransmissionResponseHandler;
import org.openelisglobal.dataexchange.common.ReportTransmission;
import org.openelisglobal.dataexchange.orderresult.OrderResponseWorker.Event;
import org.openelisglobal.dataexchange.orderresult.valueholder.HL7MessageOut;
import org.openelisglobal.dataexchange.resultreporting.beans.ResultReportXmit;
import org.openelisglobal.dataexchange.resultreporting.beans.TestResultsXmit;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportExternalExportService;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportQueueTypeService;
import org.openelisglobal.dataexchange.service.orderresult.HL7MessageOutService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.reports.service.DocumentTrackService;
import org.openelisglobal.reports.service.DocumentTypeService;
import org.openelisglobal.reports.valueholder.DocumentTrack;
import org.openelisglobal.reports.valueholder.DocumentType;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.spring.util.SpringContext;

public class ResultReportingTransfer {

    protected PatientService patientService = SpringContext.getBean(PatientService.class);

    private static DocumentType DOCUMENT_TYPE;
    private static String QUEUE_TYPE_ID;
    private static String RESULT_REFERRANCE_TABLE_ID;

    static {
        DOCUMENT_TYPE = SpringContext.getBean(DocumentTypeService.class).getDocumentTypeByName("resultExport");
        ReferenceTables referenceTable = SpringContext.getBean(ReferenceTablesService.class)
                .getReferenceTableByName("RESULT");
        if (referenceTable != null) {
            RESULT_REFERRANCE_TABLE_ID = referenceTable.getId();
        }
        ReportQueueType queueType = SpringContext.getBean(ReportQueueTypeService.class)
                .getReportQueueTypeByName("Results");
        if (queueType != null) {
            QUEUE_TYPE_ID = queueType.getId();
        }
    }

    public void sendResults(ResultReportXmit resultReport, List<Result> reportingResult, String url) {
        for (TestResultsXmit result : resultReport.getTestResults()) {

            // TODO delete if successfully moved to other area
            // if (result.getReferringOrderNumber() == null) { // walk-in create FHIR
            // String patientGuid = result.getPatientGUID();
            // String accessionNumber = result.getAccessionNumber();
            // accessionNumber = accessionNumber.substring(0,accessionNumber.indexOf('-'));
            // // disregard test number within set
            // org.openelisglobal.patient.valueholder.Patient patient =
            // patientService.getPatientForGuid(patientGuid);
            // fhirTransformService.createObservationAndDiagnosticReportFromResult(result,
            // patient);
            //// String fhirJson = fhirTransformService.CreateFhirFromOESample(result,
            // patient);
            //// LogEvent.logDebug(this.getClass().getSimpleName(), "sendResults", "" +
            // fhirJson);
            // continue;
            // }
            // if (!result.getReferringOrderNumber().isEmpty()) { // eOrder create FHIR
            //
            // String orderNumber = result.getReferringOrderNumber();
            // List<ElectronicOrder> eOrders =
            // electronicOrderService.getElectronicOrdersByExternalId(orderNumber);
            // ElectronicOrder eOrder = eOrders.get(eOrders.size() - 1);
            // ExternalOrderStatus eOrderStatus =
            // SpringContext.getBean(IStatusService.class)
            // .getExternalOrderStatusForID(eOrder.getStatusId());
            //
            // fhirTransformService.CreateFhirFromOESample(eOrder, result);
            // continue;
            // }
            if (!GenericValidator.isBlankOrNull(result.getReferringOrderNumber())) { // HL7
                ITransmissionResponseHandler responseHandler = new ResultFailHandler(reportingResult);
                new ReportTransmission().sendHL7Report(resultReport, url, responseHandler);
                continue;
            }
        }
    }

    class ResultFailHandler implements ITransmissionResponseHandler {

        private List<Result> reportingResults;

        public ResultFailHandler(List<Result> reportingResults) {
            this.reportingResults = reportingResults;
        }

        @Override
        public void handleResponse(int httpReturnStatus, List<String> errors, String msg) {
            if (httpReturnStatus == HttpServletResponse.SC_OK) {
                markFinalResultsAsSent();
                persistMessage(msg, true);
            } else {
                bufferResults(msg);
                persistMessage(msg, false);
            }
        }

        private void persistMessage(String msg, boolean success) {
            HL7MessageOutService messageOutService = SpringContext.getBean(HL7MessageOutService.class);
            HL7MessageOut messageOut = new HL7MessageOut();
            messageOut.setData(msg);
            if (success) {
                messageOut.setStatus(HL7MessageOut.SUCCESS);
            } else {
                messageOut.setStatus(HL7MessageOut.FAIL);
            }
            messageOutService.insert(messageOut);
        }

        private void bufferResults(String msg) {
            ReportExternalExport report = new ReportExternalExport();
            report.setData(msg);
            report.setSysUserId("1");
            report.setEventDate(DateUtil.getNowAsTimestamp());
            report.setCollectionDate(DateUtil.getNowAsTimestamp());
            report.setTypeId(QUEUE_TYPE_ID);
            report.setBookkeepingData(getResultIdListString() == null ? "" : getResultIdListString());
            report.setSend(true);

            try {
                SpringContext.getBean(ReportExternalExportService.class).insert(report);
            } catch (LIMSRuntimeException e) {
                LogEvent.logError(e);
            }
        }

        private String getResultIdListString() {
            String comma = "";

            StringBuilder builder = new StringBuilder();

            for (Result result : reportingResults) {
                builder.append(comma); // empty first time through
                builder.append(result.getId());

                comma = ",";
            }

            return builder.toString();
        }

        private void markFinalResultsAsSent() {
            Timestamp now = DateUtil.getNowAsTimestamp();

            List<DocumentTrack> documents = new ArrayList<>();

            for (Result result : reportingResults) {
                if (result.getResultEvent() == Event.FINAL_RESULT || result.getResultEvent() == Event.CORRECTION) {
                    DocumentTrack document = new DocumentTrack();
                    document.setDocumentTypeId(DOCUMENT_TYPE.getId());
                    document.setRecordId(result.getId());
                    document.setReportTime(now);
                    document.setTableId(RESULT_REFERRANCE_TABLE_ID);
                    document.setSysUserId("1");
                    documents.add(document);
                }
            }

            DocumentTrackService trackService = SpringContext.getBean(DocumentTrackService.class);

            try {
                trackService.insertAll(documents);
                // for (DocumentTrack document : documents) {
                // trackService.insert(document);
                // }
            } catch (LIMSRuntimeException e) {
                LogEvent.logError(e);
            }
        }
    }
}
