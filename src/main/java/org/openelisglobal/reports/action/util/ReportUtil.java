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
package org.openelisglobal.reports.action.util;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.reports.service.DocumentTrackService;
import org.openelisglobal.reports.service.DocumentTypeService;
import org.openelisglobal.reports.valueholder.DocumentTrack;
import org.openelisglobal.reports.valueholder.DocumentType;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleproject.service.SampleProjectService;
import org.openelisglobal.sampleproject.valueholder.SampleProject;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;

public class ReportUtil {

    private static String SAMPLE_QAEVENT_TABLE_ID;
    private static String DOCTOR_OBSERVATION_ID;
    private static DocumentType NON_CONFORMITY_NOTIFICATION_TYPE;

    private static SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
    private static SampleProjectService sampleProjectService = SpringContext.getBean(SampleProjectService.class);
    private static TypeOfSampleService typeOfSampleService = SpringContext.getBean(TypeOfSampleService.class);
    private static ObservationHistoryService observationService = SpringContext
            .getBean(ObservationHistoryService.class);
    private static DocumentTrackService documentTrackService = SpringContext.getBean(DocumentTrackService.class);

    static {
        ObservationHistoryType oht = SpringContext.getBean(ObservationHistoryTypeService.class)
                .getByName("nameOfDoctor");
        if (oht != null) {
            DOCTOR_OBSERVATION_ID = oht.getId();
        }

        NON_CONFORMITY_NOTIFICATION_TYPE = SpringContext.getBean(DocumentTypeService.class)
                .getDocumentTypeByName("nonConformityNotification");
        SAMPLE_QAEVENT_TABLE_ID = SpringContext.getBean(ReferenceTablesService.class)
                .getReferenceTableByName("SAMPLE_QAEVENT").getId();
    }

    public enum DocumentTypes {
        NON_CONFORMITY_NOTIFCATION
    }

    public static Patient findPatient(Sample sample) {
        return sampleHumanService.getPatientForSample(sample);
    }

    public static Project findProject(Sample sample) {
        SampleProject sampleProject = sampleProjectService.getSampleProjectBySampleId(sample.getId());
        if (sampleProject == null) {
            return null;
        }
        return sampleProject.getProject();
    }

    public static String getSampleType(SampleQaEvent event) {
        SampleItem sampleItem = event.getSampleItem();
        return (sampleItem == null) ? MessageUtil.getContextualMessage("nonConformant.allSampleTypesText")
                : findTypeOfSample(sampleItem.getTypeOfSampleId());
    }

    private static String findTypeOfSample(String typeOfSampleId) {
        return typeOfSampleService.getTypeOfSampleById(typeOfSampleId).getLocalizedName();
    }

    public static String findDoctorForSample(Sample sample) {
        ObservationHistory oh = observationService.getObservationHistoriesBySampleIdAndType(sample.getId(),
                DOCTOR_OBSERVATION_ID);
        return oh == null ? "" : oh.getValue();
    }

    public static void markDocumentsAsPrinted(DocumentTypes docType, List<String> recordIds, String currentUserId,
            Set<String> checkPriorPrintList) {

        DocumentType documentType = null;
        String tableId = null;
        Timestamp reportTime = DateUtil.getNowAsTimestamp();

        switch (docType) {
        case NON_CONFORMITY_NOTIFCATION:
            documentType = NON_CONFORMITY_NOTIFICATION_TYPE;
            tableId = SAMPLE_QAEVENT_TABLE_ID;
            break;
        default:
            throw new IllegalStateException("docType must be a supported type");
        }

        List<DocumentTrack> documents = new ArrayList<>();

        for (String recordId : recordIds) {
            DocumentTrack document = new DocumentTrack();
            document.setDocumentTypeId(documentType.getId());
            document.setRecordId(recordId);
            document.setTableId(tableId);
            document.setReportTime(reportTime);
            document.setSysUserId(currentUserId);
            document.setLastupdated(reportTime);

            if (checkPriorPrintList.contains(recordId)) {
                List<DocumentTrack> priorDocuments = documentTrackService.getByTypeRecordAndTable(documentType.getId(),
                        tableId, recordId);
                if (!priorDocuments.isEmpty()) {
                    document.setParent(priorDocuments.get(priorDocuments.size() - 1));
                }
            }

            documents.add(document);
        }

        try {
            documentTrackService.insertAll(documents);
            // for (DocumentTrack document : documents) {
            // documentTrackService.insert(document);
            // }

        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
        }
    }

    public static boolean documentHasBeenPrinted(DocumentTypes docType, String recordId) {
        DocumentType documentType = null;
        String tableId = null;

        switch (docType) {
        case NON_CONFORMITY_NOTIFCATION:
            documentType = NON_CONFORMITY_NOTIFICATION_TYPE;
            tableId = SAMPLE_QAEVENT_TABLE_ID;
            break;
        default:
            throw new IllegalStateException("docType must be a supported type");
        }

        return !documentTrackService.getByTypeRecordAndTable(documentType.getId(), tableId, recordId).isEmpty();
    }
}
