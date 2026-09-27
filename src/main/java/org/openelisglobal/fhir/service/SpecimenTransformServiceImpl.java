package org.openelisglobal.fhir.service;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Specimen;
import org.hl7.fhir.r4.model.Specimen.SpecimenCollectionComponent;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory;
import org.openelisglobal.observationhistory.valueholder.ObservationHistory.ValueType;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sourceofsample.service.SourceOfSampleService;
import org.openelisglobal.sourceofsample.valueholder.SourceOfSample;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SpecimenTransformServiceImpl implements SpecimenTransformService {

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private ObservationHistoryService observationHistoryService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private UnitOfMeasureService unitOfMeasureService;
    @Autowired
    private SourceOfSampleService sourceOfSampleService;
    @Autowired
    private FhirCommonTransformService common;
    @Autowired
    private TerminologyTransformService terminologyTransformService;

    @Override
    public Specimen transformToFhirSpecimen(SampleTestCollection sampleTest) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToFhirSpecimen", "transformToFhirSpecimen called");

        Specimen specimen = this.transformToSpecimen(sampleTest.item.getId());
        if (sampleTest.initialSampleConditionIdList != null) {
            for (ObservationHistory initialSampleCondition : sampleTest.initialSampleConditionIdList) {
                specimen.addCondition(transformSampleConditionToCodeableConcept(initialSampleCondition));
            }
        }

        return specimen;
    }

    @Override
    public SampleItem createSampleItemFromSpecimen(Specimen specimen, String sysuserId) {

        SampleItem item;

        if (specimen.hasId()) {
            String specimenId = specimen.getIdElement().getIdPart();
            SampleItem existingItem = common.getItemByFhirId(specimenId, sampleItemService);
            item = (existingItem != null) ? existingItem : new SampleItem();
        } else {
            item = new SampleItem();
        }

        if (specimen.hasAccessionIdentifier() && specimen.getAccessionIdentifier().hasValue()) {

            String accessionNumber = specimen.getAccessionIdentifier().getValue().trim();
            Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);

            if (sample == null) {
                throw new InternalErrorException("Sample not found for accession: " + accessionNumber);
            }

            int sampleIndex;
            try {
                sampleIndex = Integer.parseInt(sample.getId());
            } catch (NumberFormatException e) {
                throw new InternalErrorException("Invalid sample ID: " + sample.getId());
            }

            item.setSample(sample);
            item.setSortOrder(String.valueOf(sampleIndex));

            sampleIndex++;
            item.setExternalId(accessionNumber + "-" + sampleIndex);
        }

        // Status
        if (specimen.hasStatus()) {
            SampleStatus mappedStatus = mapSpecimenStatus(specimen.getStatus());
            item.setStatusId(statusService.getStatusID(mappedStatus));
        }

        // Type
        if (specimen.hasType()) {
            for (Coding coding : specimen.getType().getCoding()) {
                if (coding.hasCode()) {
                    List<TypeOfSample> types = typeOfSampleService.getAllMatching("description", coding.getDisplay());

                    if (types != null && !types.isEmpty()) {
                        item.setTypeOfSample(types.get(0));
                        break;
                    }
                }
            }
        }

        // Collection
        if (specimen.hasCollection()) {

            Specimen.SpecimenCollectionComponent col = specimen.getCollection();

            if (col.hasCollectedDateTimeType()) {
                Date date = col.getCollectedDateTimeType().getValue();
                item.setCollectionDate(new Timestamp(date.getTime()));
            }

            if (col.hasCollector() && col.getCollector().hasDisplay()) {
                item.setCollector(col.getCollector().getDisplay());
            }

            if (col.hasBodySite()) {
                for (Coding coding : col.getBodySite().getCoding()) {
                    if (coding.hasCode()) {
                        List<SourceOfSample> sources = sourceOfSampleService.getAllMatching("description",
                                coding.getDisplay());

                        if (sources != null && !sources.isEmpty()) {
                            item.setSourceOfSample(sources.get(0));
                        } else {
                            item.setSourceOther(coding.getDisplay());
                        }
                        break;
                    }
                }
            }

            if (col.hasMethod()) {
                for (Coding coding : col.getMethod().getCoding()) {
                    if (coding.hasDisplay()) {
                        item.setCollectionConditions(coding.getDisplay());
                        break;
                    }
                }
            }
        }

        // Container
        if (specimen.hasContainer()) {
            for (Specimen.SpecimenContainerComponent container : specimen.getContainer()) {

                if (container.hasSpecimenQuantity()) {
                    Quantity q = container.getSpecimenQuantity();

                    if (q.hasValue()) {
                        item.setQuantity(q.getValue().doubleValue());
                    }

                    if (q.hasCode()) {
                        UnitOfMeasure unitOfMeasure = new UnitOfMeasure();
                        unitOfMeasure.setUnitOfMeasureName(q.getCode());
                        UnitOfMeasure uom = unitOfMeasureService.getUnitOfMeasureByName(unitOfMeasure);
                        if (uom != null) {
                            item.setUnitOfMeasure(uom);
                        }
                    }
                }
            }
        }

        // Received
        if (specimen.hasReceivedTime()) {
            item.setReceivedDate(new Timestamp(specimen.getReceivedTime().getTime()));
        }

        // Notes
        if (specimen.hasNote()) {
            String notes = specimen.getNote().stream().filter(Annotation::hasText).map(Annotation::getText)
                    .reduce((a, b) -> a + "; " + b).orElse(null);

            if (notes != null) {
                String existing = item.getCollectionConditions();
                item.setCollectionConditions(existing != null ? existing + "; " + notes : notes);
            }
        }

        item.setSysUserId(sysuserId);

        return item;
    }

    @Override
    public Specimen transformToSpecimen(String sampleItemId) {
        return transformToSpecimen(sampleItemService.get(sampleItemId));
    }

    @Override
    public Specimen transformToSpecimen(SampleItem sampleItem) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToSpecimen", "transformToSpecimen called");

        Specimen specimen = new Specimen();

        specimen.setId(sampleItem.getFhirUuidAsString());
        specimen.getMeta().setLastUpdated(sampleItem.getLastupdated());

        specimen.addIdentifier(common.createIdentifier(fhirConfig.getOeFhirSystem() + "/sampleItem_uuid",
                sampleItem.getFhirUuidAsString()));

        Identifier facilityId = common.createFacilityIdentifier();
        if (facilityId != null) {
            specimen.addIdentifier(facilityId);
        }

        String accessionNumber = sampleItem.getSample().getAccessionNumber();
        String sortOrder = sampleItem.getSortOrder();

        String accessionValue = accessionNumber;
        if (sortOrder != null && !sortOrder.isBlank()) {
            accessionValue = accessionNumber + "-" + sortOrder;
        }

        specimen.setAccessionIdentifier(
                common.createIdentifier(fhirConfig.getOeFhirSystem() + "/sampleItem_labNo", accessionValue));

        specimen.setStatus(mapSampleItemStatusToSpecimenStatus(sampleItem.getStatusId()));

        specimen.setType(
                terminologyTransformService.transformTypeOfSampleToCodeableConcept(sampleItem.getTypeOfSample()));

        if (sampleItem.getReceivedDate() != null) {
            specimen.setReceivedTime(new Date(sampleItem.getReceivedDate().getTime()));
        }

        specimen.setCollection(transformToCollection(sampleItem.getCollectionDate(), sampleItem.getCollector(),
                sampleItem.getSample()));

        if (sampleItem.getSourceOfSample() != null) {
            CodeableConcept bodySite = new CodeableConcept();
            bodySite.setText(sampleItem.getSourceOfSample().getDescription());
            specimen.getCollection().setBodySite(bodySite);
        } else if (sampleItem.getSourceOther() != null) {
            CodeableConcept bodySite = new CodeableConcept();
            bodySite.setText(sampleItem.getSourceOther());
            specimen.getCollection().setBodySite(bodySite);
        }

        if (sampleItem.getCollectionConditions() != null) {
            CodeableConcept method = new CodeableConcept();
            method.setText(sampleItem.getCollectionConditions());
            specimen.getCollection().setMethod(method);
        }

        Specimen.SpecimenContainerComponent container = new Specimen.SpecimenContainerComponent();

        CodeableConcept containerType = new CodeableConcept();
        containerType.addCoding().setSystem("http://snomed.info/sct").setCode("434711009")
                .setDisplay("Specimen container (physical object)");

        container.setType(containerType);

        if (sampleItem.getQuantity() != null) {
            Quantity quantity = new Quantity();
            quantity.setValue(sampleItem.getQuantity());

            if (sampleItem.getUnitOfMeasure() != null && sampleItem.getUnitOfMeasure().getName() != null) {

                quantity.setCode(sampleItem.getUnitOfMeasure().getName());
                quantity.setSystem("http://unitsofmeasure.org");
            }

            container.setSpecimenQuantity(quantity);
        }

        specimen.addContainer(container);

        if (sampleItem.getCollectionConditions() != null) {
            Annotation note = new Annotation();
            note.setText(sampleItem.getCollectionConditions());
            specimen.addNote(note);
        }

        for (Analysis analysis : analysisService.getAnalysesBySampleItem(sampleItem)) {

            specimen.addRequest(common.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
        }

        Patient patient = sampleHumanService.getPatientForSample(sampleItem.getSample());

        if (patient != null) {
            specimen.setSubject(common.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));
        }

        return specimen;
    }

    @SuppressWarnings("unused")
    private CodeableConcept transformSampleConditionToCodeableConcept(String sampleConditionId) {
        return transformSampleConditionToCodeableConcept(observationHistoryService.get(sampleConditionId));
    }

    private CodeableConcept transformSampleConditionToCodeableConcept(ObservationHistory initialSampleCondition) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformSampleConditionToCodeableConcept",
                "transformSampleConditionToCodeableConcept called");

        String observationValue;
        String observationDisplay;
        if (ValueType.DICTIONARY.getCode().equals(initialSampleCondition.getValueType())) {
            observationValue = dictionaryService.get(initialSampleCondition.getValue()).getDictEntry();
            observationDisplay = dictionaryService.get(initialSampleCondition.getValue()).getDictEntryDisplayValue();
        } else if (ValueType.KEY.getCode().equals(initialSampleCondition.getValueType())) {
            observationValue = localizationService.get(initialSampleCondition.getValue()).getEnglish();
            observationDisplay = "";
        } else {
            observationValue = initialSampleCondition.getValue();
            observationDisplay = "";
        }

        CodeableConcept condition = new CodeableConcept();
        condition.addCoding(
                new Coding(fhirConfig.getOeFhirSystem() + "/sample_condition", observationValue, observationDisplay));
        return condition;
    }

    private SpecimenCollectionComponent transformToCollection(Timestamp collectionDate, String collector,
            Sample sample) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformToCollection", "transformToCollection called");

        SpecimenCollectionComponent specimenCollectionComponent = new SpecimenCollectionComponent();
        specimenCollectionComponent.setCollected(new DateTimeType(collectionDate));

        // Add GPS coordinates extension if available
        if (sample != null && sample.hasGpsCoordinates()) {
            Extension gpsExtension = createGpsExtension(sample);
            specimenCollectionComponent.addExtension(gpsExtension);
        }

        return specimenCollectionComponent;
    }

    /**
     * Creates a FHIR extension for GPS coordinates according to FHIR R4 standards.
     * Extension URL:
     * http://openelis-global.org/fhir/StructureDefinition/collection-location-gps
     *
     * @param sample Sample with GPS coordinates
     * @return Extension containing latitude, longitude, accuracy, method, and
     *         timestamp
     */
    private Extension createGpsExtension(Sample sample) {
        Extension gpsExtension = new Extension();
        gpsExtension.setUrl("http://openelis-global.org/fhir/StructureDefinition/collection-location-gps");

        // Latitude sub-extension (required if GPS data exists)
        if (sample.getGpsLatitude() != null) {
            Extension latitudeExt = new Extension("latitude", new DecimalType(sample.getGpsLatitude()));
            gpsExtension.addExtension(latitudeExt);
        }

        // Longitude sub-extension (required if GPS data exists)
        if (sample.getGpsLongitude() != null) {
            Extension longitudeExt = new Extension("longitude", new DecimalType(sample.getGpsLongitude()));
            gpsExtension.addExtension(longitudeExt);
        }

        // Accuracy sub-extension (optional)
        if (sample.getGpsAccuracyMeters() != null) {
            Extension accuracyExt = new Extension("accuracy", new IntegerType(sample.getGpsAccuracyMeters()));
            gpsExtension.addExtension(accuracyExt);
        }

        // Capture method sub-extension (optional)
        if (sample.getGpsCaptureMethod() != null) {
            Extension methodExt = new Extension("method", new CodeType(sample.getGpsCaptureMethod()));
            gpsExtension.addExtension(methodExt);
        }

        // Capture timestamp sub-extension (optional)
        if (sample.getGpsCaptureTimestamp() != null) {
            Extension timestampExt = new Extension("captureTimestamp",
                    new DateTimeType(sample.getGpsCaptureTimestamp()));
            gpsExtension.addExtension(timestampExt);
        }

        return gpsExtension;
    }

    private SampleStatus mapSpecimenStatus(Specimen.SpecimenStatus status) {
        if (status == null) {
            return SampleStatus.Entered;
        }

        switch (status) {
        case AVAILABLE:
            return SampleStatus.Entered;

        case UNAVAILABLE:
            return SampleStatus.Disposed;

        case UNSATISFACTORY:
            return SampleStatus.SampleRejected;

        case ENTEREDINERROR:
            return SampleStatus.Canceled;

        default:
            return SampleStatus.Entered;
        }
    }

    private Specimen.SpecimenStatus mapSampleItemStatusToSpecimenStatus(String statusId) {

        SampleStatus status = statusService.getSampleStatusForID(statusId);

        if (status == null)
            return Specimen.SpecimenStatus.AVAILABLE;

        switch (status) {
        case Canceled:
            return Specimen.SpecimenStatus.UNSATISFACTORY;

        case Disposed:
            return Specimen.SpecimenStatus.UNAVAILABLE;

        case Entered:
        default:
            return Specimen.SpecimenStatus.AVAILABLE;
        }
    }
}
