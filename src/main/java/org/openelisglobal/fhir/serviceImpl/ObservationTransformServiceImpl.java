package org.openelisglobal.fhir.serviceImpl;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.FHIRTransformUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.fhir.service.ObservationTransformService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ObservationTransformServiceImpl implements ObservationTransformService {
    @Autowired
    private FHIRTransformUtil fhirTransformUtil;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private TestService testService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private PatientService patientService;

    @Override
    public Observation transformResultToObservation(Result result) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "transformResultToObservation",
                "transformResultToObservation called");

        Analysis analysis = result.getAnalysis();
        Test test = analysis.getTest();
        SampleItem sampleItem = analysis.getSampleItem();
        Patient patient = sampleHumanService.getPatientForSample(sampleItem.getSample());
        Observation observation = new Observation();

        observation.setId(result.getFhirUuidAsString());
        observation.addIdentifier(fhirTransformUtil.createIdentifier(fhirConfig.getOeFhirSystem() + "/result_uuid",
                result.getFhirUuidAsString()));
        Identifier facilityId = fhirTransformUtil.createFacilityIdentifier();
        if (facilityId != null) {
            observation.addIdentifier(facilityId);
        }

        // TODO make sure these align with each other.
        // we may need to add detection for when result is changed and add those status
        // to list
        if (result.getAnalysis().getStatusId().equals(statusService.getStatusID(AnalysisStatus.Finalized))) {
            observation.setStatus(ObservationStatus.FINAL);
        } else if (result.getAnalysis().getStatusId().equals(statusService.getStatusID(AnalysisStatus.NotStarted))) {
            LogEvent.logError(this.getClass().getSimpleName(), "transformResultToObservation",
                    "recording result for analysis that is not started.");
            observation.setStatus(ObservationStatus.UNKNOWN);
        } else {
            observation.setStatus(ObservationStatus.PRELIMINARY);
        }

        if (!GenericValidator.isBlankOrNull(result.getValue())) {
            // in case of Viral load test
            if (result.getAnalysis().getTest().getName().equalsIgnoreCase("Viral Load")) {
                Quantity quantity = new Quantity();
                long finalResult = result.getVLValueAsNumber();
                quantity.setValue(finalResult);
                quantity.setUnit(resultService.getUOM(result));
                observation.setValue(quantity);
            } else if (TypeOfTestResultServiceImpl.ResultType.isMultiSelectVariant(result.getResultType())
                    && !"0".equals(result.getValue())) {
                Dictionary dictionary = dictionaryService.getDataForId(result.getValue());
                CodeableConcept codeableConcept = new CodeableConcept();
                if (dictionary.getLoincCode() != null && !dictionary.getLoincCode().isEmpty()) {
                    codeableConcept.addCoding(new Coding("http://loinc.org", dictionary.getLoincCode(),
                            dictionary.getLocalizedDictionaryName() == null ? dictionary.getDictEntry()
                                    : dictionary.getLocalizedDictionaryName().getEnglish()));
                }
                codeableConcept.addCoding(
                        new Coding(fhirConfig.getOeFhirSystem() + "/dictionary_entry", dictionary.getDictEntry(),
                                dictionary.getLocalizedDictionaryName() == null ? dictionary.getDictEntry()
                                        : dictionary.getLocalizedDictionaryName().getEnglish()));
                observation.setValue(codeableConcept);
            } else if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.getResultType())
                    && !"0".equals(result.getValue())) {
                Dictionary dictionary = dictionaryService.getDataForId(result.getValue());
                CodeableConcept codeableConcept = new CodeableConcept();
                if (dictionary.getLoincCode() != null && !dictionary.getLoincCode().isEmpty()) {
                    codeableConcept.addCoding(new Coding("http://loinc.org", dictionary.getLoincCode(),
                            dictionary.getLocalizedDictionaryName() == null ? dictionary.getDictEntry()
                                    : dictionary.getLocalizedDictionaryName().getEnglish()));
                }
                codeableConcept.addCoding(
                        new Coding(fhirConfig.getOeFhirSystem() + "/dictionary_entry", dictionary.getDictEntry(),
                                dictionary.getLocalizedDictionaryName() == null ? dictionary.getDictEntry()
                                        : dictionary.getLocalizedDictionaryName().getEnglish()));
                observation.setValue(codeableConcept);
            } else if (TypeOfTestResultServiceImpl.ResultType.isNumeric(result.getResultType())) {
                Quantity quantity = new Quantity();
                quantity.setValue(new BigDecimal(result.getValue(true)));
                quantity.setUnit(resultService.getUOM(result));
                observation.setValue(quantity);
            } else if (TypeOfTestResultServiceImpl.ResultType.isTextOnlyVariant(result.getResultType())) {
                observation.setValue(new StringType(result.getValue()));
            }
        }
        observation.setCode(fhirTransformUtil.transformTestToCodeableConcept(test.getId()));
        observation.addBasedOn(
                fhirTransformUtil.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
        observation.setSpecimen(
                fhirTransformUtil.createReferenceFor(ResourceType.Specimen, sampleItem.getFhirUuidAsString()));
        // OGC-356: Environmental samples don't have a patient
        if (patient != null) {
            observation.setSubject(
                    fhirTransformUtil.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));
        }
        // observation.setIssued(result.getOriginalLastupdated());
        observation.setIssued(analysis.getReleasedDate()); // update to get Released Date instead of commpleted date
        // observation.setEffective(new
        // DateTimeType(result.getLastupdated()));
        if (analysis.getReleasedDate() != null) {
            observation.setEffective(new DateTimeType(analysis.getReleasedDate()));
        } else {
            observation.setEffective(new DateTimeType(analysis.getStartedDate()));
        }
        // observation.setIssued(new Date());

        return observation;
    }

    @Override
    public TestResultItem createResultFromObservation(org.hl7.fhir.r4.model.Observation observation) {

        TestResultItem bean = new TestResultItem();
        Result result = new Result();
        bean.setResult(result);

        if (observation.hasSpecimen()) {
            String sampleItemUUID = observation.getSpecimen().getReferenceElement().getIdPart();
            SampleItem sampleItem = fhirTransformUtil.getItemByFhirId(sampleItemUUID, sampleItemService);

            if (sampleItem == null) {
                throw new UnprocessableEntityException("SampleItem not found: " + sampleItemUUID);
            }

            Sample sample = sampleItem.getSample();
            bean.setSampleItemId(sampleItem.getId());
            bean.setAccessionNumber(sample.getAccessionNumber());
        }

        if (observation.hasBasedOn()) {

            String analysisUUID = observation.getBasedOnFirstRep().getReferenceElement().getIdPart();

            Analysis analysis = analysisService.getAllMatching("fhirUuid", UUID.fromString(analysisUUID)).get(0);

            Test test = analysis.getTest();

            bean.setAnalysisId(analysis.getId());
            bean.setTestId(test.getId());
        }
        if (observation.hasSubject()) {
            String patientUUID = observation.getSubject().getReferenceElement().getIdPart();
            Patient patient = fhirTransformUtil.getItemByFhirId(patientUUID, patientService);

            if (patient == null) {
                throw new UnprocessableEntityException("Patient not found: " + patientUUID);
            }

            bean.setPatientId(patient.getId());
        }

        bean.setIsModified(true);
        bean.setResultId(null);
        bean.setReportable(true);
        String locale = ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.DEFAULT_DATE_LOCALE);

        String pattern = "en-US".equals(locale) ? "MM/dd/yyyy" : "dd/MM/yyyy";

        String formattedDate = new SimpleDateFormat(pattern).format(new Date());
        bean.setTestDate(formattedDate);

        if (bean.getTechnician() == null) {
            bean.setTechnician("");
        }

        if (observation.hasStatus()) {
            String status = observation.getStatusElement().getValue().toString();
            if (status.equals(org.hl7.fhir.r4.model.Observation.ObservationStatus.FINAL.toString())) {
                bean.setAnalysisStatusId(statusService.getStatusID(AnalysisStatus.Finalized));
            } else if (status.equals(org.hl7.fhir.r4.model.Observation.ObservationStatus.CANCELLED.toString())) {
                bean.setAnalysisStatusId(statusService.getStatusID(AnalysisStatus.Canceled));
            } else if (status.equals(org.hl7.fhir.r4.model.Observation.ObservationStatus.REGISTERED.toString())) {
                bean.setAnalysisStatusId(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance));
            }
        }

        if (observation.hasCode()) {
            boolean matchedLoinc = false;

            for (Coding code : observation.getCode().getCoding()) {
                if ("http://loinc.org".equals(code.getSystem())) {
                    matchedLoinc = true;

                    List<Test> tests = testService.getTestsByLoincCode(code.getCode());
                    if (tests.isEmpty()) {
                        throw new InternalErrorException("No test with loinc code " + code.getCode());
                    }

                    if (tests.getFirst().getLoinc().equals(code.getCode())) {
                        bean.setTestId(tests.getFirst().getId());
                    } else {
                        throw new InternalErrorException("Observation code " + code.getCode()
                                + " does not match test loinc code " + tests.getFirst().getLoinc());
                    }

                    break;
                }
            }

            if (!matchedLoinc) {
                throw new InternalErrorException("Observation has code but no LOINC code was found");
            }
        }

        if (observation.hasValueStringType()) {

            String value = observation.getValueStringType().getValueAsString();

            bean.setResultValue(value);
            bean.setShadowResultValue(value);
            bean.setResultType("T");
        }

        else if (observation.hasValueCodeableConcept()) {

            for (Coding code : observation.getValueCodeableConcept().getCoding()) {

                if (code.getSystem().equals(fhirConfig.getOeFhirSystem() + "/dictionary_entry")) {

                    List<Dictionary> dictionaries = dictionaryService.getAllMatching("dictEntry", code.getCode());

                    if (!dictionaries.isEmpty()) {

                        Dictionary dictionary = dictionaries.get(0);

                        bean.setResultValue(dictionary.getId());
                        bean.setShadowResultValue(dictionary.getId());

                        List<TestResult> testResults = testResultService.getAllMatching("value", dictionary.getId());
                        TestResult testResult = testResults.get(0);
                        if (testResult != null) {

                            bean.setResultType(testResult.getTestResultType());

                            result.setTestResult(testResult);

                        }

                    }
                }
            }
        }

        else if (observation.hasValueQuantity()) {

            String value = observation.getValueQuantity().getValueElement().getValueAsString();

            bean.setResultValue(value);
            bean.setShadowResultValue(value);
            bean.setResultType("N");
            bean.setUnitsOfMeasure(observation.getValueQuantity().getUnit());

        }

        if (bean.getResultType() == null) {
            bean.setResultType("T");
        }

        bean.setHasQualifiedResult(false);

        if (bean.getAnalysisId() == null || bean.getTestId() == null || bean.getSampleItemId() == null) {
            throw new UnprocessableEntityException("Missing required fields for result creation");
        }
        return bean;
    }

}
