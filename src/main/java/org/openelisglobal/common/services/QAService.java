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
package org.openelisglobal.common.services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.qaevent.service.QaEventService;
import org.openelisglobal.qaevent.service.QaObservationService;
import org.openelisglobal.qaevent.service.QaObservationTypeService;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.qaevent.valueholder.QaEvent;
import org.openelisglobal.qaevent.valueholder.QaObservation;
import org.openelisglobal.qaevent.valueholder.QaObservation.ObservedType;
import org.openelisglobal.qaevent.valueholder.QaObservationType;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
@DependsOn({ "springContext" })
public class QAService {
    public static final String TABLE_REFERENCE_ID;
    public static final String NOTE_TYPE = "I";
    public static final String NOTE_SUBJECT = "QaEvent Note";

    private QaObservationService observationService = SpringContext.getBean(QaObservationService.class);
    private DictionaryService dictService = SpringContext.getBean(DictionaryService.class);
    private QaEventService qaEventService = SpringContext.getBean(QaEventService.class);
    private static SampleQaEventService sampleQaEventService = SpringContext.getBean(SampleQaEventService.class);
    private static QaObservationTypeService qaObservationTypeService = SpringContext
            .getBean(QaObservationTypeService.class);
    private static ReferenceTablesService referenceTablesService = SpringContext.getBean(ReferenceTablesService.class);
    private static NceSpecimenService nceSpecimenService = SpringContext.getBean(NceSpecimenService.class);

    private SampleQaEvent sampleQaEvent;
    private final List<QaObservation> observationList = new ArrayList<>();
    private String currentUserId = null;

    public enum QAObservationType {
        AUTHORIZER("authorizer"), SECTION("section"), DOC_NUMBER("documentNumber");

        private final String dbName;
        private QaObservationType type;

        QAObservationType(String dbName) {
            this.dbName = dbName;
        }

        QaObservationType getType() {
            if (type == null) {
                type = qaObservationTypeService.getQaObservationTypeByName(getDBName());
            }

            return type;
        }

        String getDBName() {
            return dbName;
        }
    }

    public enum QAObservationValueType {
        LITERAL("L"), DICTIONARY("D"), KEY("K");

        private final String dbSymbol;

        QAObservationValueType(String symbol) {
            dbSymbol = symbol;
        }

        String getDBSymbol() {
            return dbSymbol;
        }
    }

    static {
        ReferenceTables referenceTable = new ReferenceTables();

        referenceTable.setTableName("SAMPLE_QAEVENT");
        referenceTable = referenceTablesService.getReferenceTableByName(referenceTable);
        TABLE_REFERENCE_ID = referenceTable.getId();
    }

    public QAService(SampleQaEvent event) {
        sampleQaEvent = event;
    }

    public QAService() {
    }

    public void setSampleQaEvent(SampleQaEvent event) {
        sampleQaEvent = event;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
        sampleQaEvent.setSysUserId(currentUserId);
        for (QaObservation observation : observationList) {
            observation.setSysUserId(currentUserId);
        }
    }

    public String getEventId() {
        return sampleQaEvent.getId();
    }

    public SampleItem getSampleItem() {
        return sampleQaEvent.getSampleItem();
    }

    public String getObservationValue(QAObservationType section) {
        QaObservation observation = observationService.getQaObservationByTypeAndObserved(section.getDBName(), "SAMPLE",
                sampleQaEvent.getId());
        return observation == null ? null : observation.getValue();
    }

    public String getObservationForDisplay(QAObservationType section) {
        QaObservation observation = observationService.getQaObservationByTypeAndObserved(section.getDBName(), "SAMPLE",
                sampleQaEvent.getId());

        if (observation != null) {
            if ("K".equals(observation.getValueType())) {
                return MessageUtil.getContextualMessage(observation.getValue());
            } else if ("L".equals(observation.getValueType())) {
                return observation.getValue();
            } else if ("D".equals(observation.getValueType())) {
                return dictService.getDictionaryById(observation.getValue()).getDictEntry();
            }
        }

        return null;
    }

    public Timestamp getLastupdated() {
        return sampleQaEvent.getLastupdated();
    }

    public QaEvent getQAEvent() {
        return sampleQaEvent.getQaEvent();
    }

    public void setQaEventById(String qaEventId) {
        QaEvent event = new QaEvent();
        event.setId(qaEventId);
        qaEventService.getData(event);
        sampleQaEvent.setQaEvent(event);
    }

    public void setObservation(QAObservationType observationType, String value, QAObservationValueType type,
            boolean rejectEmptyValues) {
        if (rejectEmptyValues && ((type == QAObservationValueType.DICTIONARY && "O".equals(value))
                || GenericValidator.isBlankOrNull(value))) {
            return;
        }

        QaObservation observation = null;

        if (sampleQaEvent.getId() != null) {
            observation = observationService.getQaObservationByTypeAndObserved(observationType.getDBName(),
                    ObservedType.SAMPLE.getDBName(), sampleQaEvent.getId());
        }

        if (observation == null) {
            observation = new QaObservation();
            observation.setObservationType(observationType.getType());
            // id may be null at this point
            observation.setObservedId(sampleQaEvent.getId());
            observation.setObservedType(ObservedType.SAMPLE.getDBName());
            observation.setValueType(type.getDBSymbol());
        }

        observation.setValue(value);
        observation.setSysUserId(currentUserId);
        observationList.add(observation);
    }

    public void setSampleItem(SampleItem sampleItem) {
        sampleQaEvent.setSampleItem(sampleItem);
    }

    public SampleQaEvent getSampleQaEvent() {
        return sampleQaEvent;
    }

    public List<QaObservation> getUpdatedObservations() {
        return observationList;
    }

    public void setReportTime(String enteredDate) {
        sampleQaEvent.setEnteredDate(DateUtil.convertStringDateToTimestamp(enteredDate));
    }

    public static boolean isAnalysisParentNonConforming(Analysis analysis) {
        SampleItem sampleItem = analysis.getSampleItem();

        if (sampleItem != null) {
            boolean nonconforming = nonconformingByDepricatedStatus(sampleItem.getSample(), analysis);
            nonconforming = nonconforming || hasNonConformingEvent(sampleItem);

            if (!nonconforming) {
                nonconforming = hasOrderOnlyQaEventOrSampleQaEvent(sampleItem);
            }
            return nonconforming;
        }

        return false;
    }

    public static boolean isOrderNonConforming(Sample sample) {
        if (sample != null) {
            boolean nonconforming = nonconformingByDepricatedStatus(sample);
            if (!nonconforming) {
                nonconforming = hasOrderSampleQaEvent(sample);
            }

            return nonconforming;
        }

        return false;
    }

    private static boolean nonconformingByDepricatedStatus(Sample sample, Analysis analysis) {

        return nonconformingByDepricatedStatus(sample) || analysis.getStatusId().equals(
                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NonConforming_depricated));
    }

    private static boolean hasNonConformingEvent(SampleItem sampleItem) {
        List<NceSpecimen> nceSpecimens = nceSpecimenService.getSpecimenBySampleItemId(sampleItem.getId());
        return !nceSpecimens.isEmpty();
    }

    private static boolean nonconformingByDepricatedStatus(Sample sample) {
        boolean nonconformingStatus = false;
        nonconformingStatus = nonconformingStatus || sample.getStatusId()
                .equals(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.NonConforming_depricated));
        return nonconformingStatus;
    }

    private static boolean hasOrderSampleQaEvent(Sample sample) {
        return !sampleQaEventService.getSampleQaEventsBySample(sample).isEmpty();
    }

    private static boolean hasOrderOnlyQaEventOrSampleQaEvent(SampleItem sampleItem) {
        List<SampleQaEvent> sampleQaEvents = sampleQaEventService.getSampleQaEventsBySample(sampleItem.getSample());

        boolean sampleItemLabeled = false;
        for (SampleQaEvent sampleEvent : sampleQaEvents) {
            if (sampleEvent.getSampleItem() != null) {
                sampleItemLabeled = true;
                if (sampleEvent.getSampleItem().getId().equals(sampleItem.getId())) {
                    return true;
                }
            }
        }

        // Return true is there was something matching in the table and
        // there was no sampleItem tagged that was not the one
        // we care about( in which case it would have returned before here)
        return !sampleQaEvents.isEmpty() && !sampleItemLabeled;
    }

    public static List<SampleItem> getNonConformingSampleItems(Sample sample) {
        List<SampleItem> nonConformingSampleItems = new ArrayList<>();
        List<SampleQaEvent> sampleQaEvents = sampleQaEventService.getSampleQaEventsBySample(sample);

        for (SampleQaEvent sampleEvent : sampleQaEvents) {
            if (sampleEvent.getSampleItem() != null) {
                nonConformingSampleItems.add(sampleEvent.getSampleItem());
            }
        }

        return nonConformingSampleItems;
    }
}
