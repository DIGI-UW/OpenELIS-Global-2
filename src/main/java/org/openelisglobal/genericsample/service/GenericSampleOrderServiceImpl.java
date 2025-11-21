package org.openelisglobal.genericsample.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TimeType;
import org.hl7.fhir.r4.model.Type;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.genericsample.form.GenericSampleImportResult;
import org.openelisglobal.genericsample.form.GenericSampleImportResult.ImportRow;
import org.openelisglobal.genericsample.form.GenericSampleOrderForm;
import org.openelisglobal.notebook.service.NoteBookSampleService;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookSample;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.sample.dao.SampleDAO;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenericSampleOrderServiceImpl implements GenericSampleOrderService {

    // Lock object for synchronizing accession number generation
    private static final Object ACCESSION_NUMBER_LOCK = new Object();

    @Autowired
    private GenericSampleOrderService self;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleDAO sampleDAO;

    private IAccessionNumberGenerator accessionNumberGenerator;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private StatusOfSampleService statusOfSampleService;

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramSampleService programSampleService;

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private NoteBookSampleService noteBookSampleService;

    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private FhirConfig fhirConfig;

    @Override
    public Map<String, Object> saveGenericSampleOrder(GenericSampleOrderForm form, String sysUserId)
            throws FhirLocalPersistingException {
        return saveGenericSampleOrderInternal(form, sysUserId);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Map<String, Object> saveGenericSampleOrderInternal(GenericSampleOrderForm form, String sysUserId)
            throws FhirLocalPersistingException {
        LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrderInternal",
                "Starting new transaction for sample import");
        Map<String, Object> result = new HashMap<>();

        GenericSampleOrderForm.DefaultFields defaultFields = form.getDefaultFields();

        // Create and save Sample
        Sample sample = createSample(defaultFields, sysUserId);

        // Generate accession number if not provided, otherwise use provided one
        // Synchronize to prevent race conditions when generating accession numbers in
        // concurrent transactions
        String sampleId;
        if (GenericValidator.isBlankOrNull(sample.getAccessionNumber())) {
            // Synchronize accession number generation AND insertion to prevent duplicates
            // Use the generator directly to ensure atomic increment from
            // accession_number_info table
            // The generator's getNextAccessionNumber() uses atomic UPDATE, and we
            // synchronize
            // to ensure sequential generation even with REQUIRES_NEW transactions
            synchronized (ACCESSION_NUMBER_LOCK) {
                // Generate accession number using the generator (atomic increment)
                // Keep generating until we find one that doesn't exist in the database
                String generatedAccessionNumber = null;
                int maxAttempts = 100; // Prevent infinite loop
                int attempts = 0;

                LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                        "Generating accession number for new sample");

                while (generatedAccessionNumber == null && attempts < maxAttempts) {
                    String candidateNumber = getNextAccessionNumber();

                    // Check if this accession number already exists in the database
                    Sample existingSample = sampleService.getSampleByAccessionNumber(candidateNumber);
                    if (existingSample == null) {
                        // This number is available
                        generatedAccessionNumber = candidateNumber;
                        LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                                "Generated unique accession number: " + generatedAccessionNumber + " (attempt "
                                        + (attempts + 1) + ")");
                    } else {
                        // This number already exists, try again
                        attempts++;
                        String existingSampleId = existingSample.getId();
                        LogEvent.logWarn(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                                "Generated accession number " + candidateNumber
                                        + " already exists in database (Sample ID: " + existingSampleId
                                        + "). This indicates the accession_number_info counter is out of sync. Retrying... (attempt "
                                        + attempts + ")");
                        // The generator will increment on the next call
                    }
                }

                if (generatedAccessionNumber == null) {
                    String errorMsg = "Failed to generate unique accession number after " + maxAttempts + " attempts";
                    LogEvent.logError(errorMsg, null);
                    throw new RuntimeException(errorMsg);
                }

                sample.setAccessionNumber(generatedAccessionNumber);

                // Insert with the generated accession number (inside synchronized block to
                // ensure atomicity)
                sampleService.insertDataWithAccessionNumber(sample);
                sampleId = sample.getId();
                LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                        "Sample inserted with generated accession number: " + sample.getAccessionNumber() + ", ID: "
                                + sampleId);
            }
        } else {
            // Use provided accession number
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                    "Using provided accession number: " + sample.getAccessionNumber());
            sampleService.insertDataWithAccessionNumber(sample);
            sampleId = sample.getId();
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                    "Sample inserted with provided accession number: " + sample.getAccessionNumber() + ", ID: "
                            + sampleId);
        }

        sample = sampleService.get(sampleId);
        if (sample == null) {
            String errorMsg = "Failed to retrieve sample after insert. SampleId: " + sampleId;
            LogEvent.logError(errorMsg, null);
            throw new RuntimeException(errorMsg);
        }
        result.put("sampleId", sampleId);
        result.put("accessionNumber", sample.getAccessionNumber());
        LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                "Sample retrieved successfully. Accession: " + sample.getAccessionNumber());

        // Create and save SampleItem
        String sampleItemId = null;
        if (!GenericValidator.isBlankOrNull(defaultFields.getSampleTypeId())) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                    "Creating sample item for sample: " + sample.getAccessionNumber());
            SampleItem sampleItem = createSampleItem(sample, defaultFields, sysUserId);
            sampleItemId = sampleItemService.insert(sampleItem);
            result.put("sampleItemId", sampleItemId);
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                    "Sample item created successfully. ID: " + sampleItemId);
        }

        // Save additional fields
        LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                "Saving additional fields for sample: " + sample.getAccessionNumber());
        saveAdditionalFields(sample, defaultFields, sysUserId);
        LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                "Additional fields saved successfully");

        // Save notebook sample and questionnaire response if notebook is selected
        if (form.getNotebookId() != null && form.getFhirQuestionnaire() != null && form.getFhirResponses() != null
                && !form.getFhirResponses().isEmpty() && sampleItemId != null) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                    "Saving notebook sample and questionnaire response for sample: " + sample.getAccessionNumber());
            saveNotebookSample(sample, form, sysUserId, sampleItemId);
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                    "Notebook sample and questionnaire response saved successfully");
        } else if (form.getFhirQuestionnaire() != null && form.getFhirResponses() != null
                && !form.getFhirResponses().isEmpty()) {
            // Fallback to program sample for backward compatibility
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                    "Saving program sample and questionnaire response for sample: " + sample.getAccessionNumber());
            saveProgramSample(sample, form, sysUserId);
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveGenericSampleOrder",
                    "Program sample and questionnaire response saved successfully");
        }

        result.put("success", true);
        result.put("message", "Generic sample order saved successfully");

        return result;
    }

    private Sample createSample(GenericSampleOrderForm.DefaultFields defaultFields, String sysUserId) {
        Sample sample = new Sample();
        sample.setSysUserId(sysUserId);
        // Only set accession number if provided, otherwise let
        // insertDataWithAccessionNumber auto-generate it
        if (!GenericValidator.isBlankOrNull(defaultFields.getLabNo())) {
            sample.setAccessionNumber(defaultFields.getLabNo());
        }
        sample.setEnteredDate(DateUtil.getNowAsSqlDate());
        sample.setDomain(ConfigurationProperties.getInstance().getPropertyValue("domain.human"));
        sample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Entered));
        sample.setFhirUuid(UUID.randomUUID());

        // Set collection date and time if provided
        if (!GenericValidator.isBlankOrNull(defaultFields.getCollectionDate())) {
            String collectionDateTime = defaultFields.getCollectionDate();
            if (!GenericValidator.isBlankOrNull(defaultFields.getCollectionTime())) {
                collectionDateTime += " " + defaultFields.getCollectionTime();
            } else {
                collectionDateTime += " 00:00";
            }
            // Use convertStringDateToTimestamp to preserve time (not truncate it)
            // setCollectionDate() will automatically set collectionDateForDisplay and
            // collectionTimeForDisplay
            Timestamp collectionTimestamp = DateUtil.convertStringDateToTimestamp(collectionDateTime);
            sample.setCollectionDate(collectionTimestamp);
        }

        // Set received date to current date if not provided
        String receivedDate = DateUtil.getCurrentDateAsText();
        sample.setReceivedDateForDisplay(receivedDate);
        sample.setReceivedDate(DateUtil.convertStringDateToSqlDate(receivedDate));
        sample.setReceivedTimestamp(DateUtil.getNowAsTimestamp());

        return sample;
    }

    private SampleItem createSampleItem(Sample sample, GenericSampleOrderForm.DefaultFields defaultFields,
            String sysUserId) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setSysUserId(sysUserId);

        if (!GenericValidator.isBlankOrNull(defaultFields.getSampleTypeId())) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "createSampleItem",
                    "Retrieving TypeOfSample with ID: " + defaultFields.getSampleTypeId());
            TypeOfSample typeOfSample = typeOfSampleService.get(defaultFields.getSampleTypeId());
            if (typeOfSample == null) {
                String errorMsg = "TypeOfSample not found with ID: " + defaultFields.getSampleTypeId();
                LogEvent.logError(errorMsg, null);
                throw new RuntimeException(errorMsg);
            }
            sampleItem.setTypeOfSample(typeOfSample);
            LogEvent.logInfo(this.getClass().getSimpleName(), "createSampleItem",
                    "TypeOfSample retrieved: " + typeOfSample.getLocalizedName());
        }

        // Set quantity if provided
        if (!GenericValidator.isBlankOrNull(defaultFields.getQuantity())) {
            try {
                sampleItem.setQuantity(Double.parseDouble(defaultFields.getQuantity()));
            } catch (NumberFormatException e) {
                LogEvent.logError("Invalid quantity format: " + defaultFields.getQuantity() + " for sample: "
                        + sample.getAccessionNumber(), e);
                // Ignore if quantity is not a valid number, but log it
            }
        }

        // Set unit of measure if provided
        if (!GenericValidator.isBlankOrNull(defaultFields.getSampleUnitOfMeasure())) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "createSampleItem",
                    "Retrieving UnitOfMeasure with ID: " + defaultFields.getSampleUnitOfMeasure());
            UnitOfMeasure uom = unitOfMeasureService.get(defaultFields.getSampleUnitOfMeasure());
            if (uom == null) {
                LogEvent.logError("UnitOfMeasure not found with ID: " + defaultFields.getSampleUnitOfMeasure()
                        + " for sample: " + sample.getAccessionNumber(), null);
                // Continue without unit of measure
            } else {
                sampleItem.setUnitOfMeasure(uom);
                LogEvent.logInfo(this.getClass().getSimpleName(), "createSampleItem",
                        "UnitOfMeasure retrieved: " + uom.getUnitOfMeasureName());
            }
        }

        // Set collector if provided
        if (!GenericValidator.isBlankOrNull(defaultFields.getCollector())) {
            sampleItem.setCollector(defaultFields.getCollector());
        }

        // Set external ID: accessionNumber + "-1" (every sample has one sample item)
        String externalId = sample.getAccessionNumber() + "-1";
        sampleItem.setExternalId(externalId);
        LogEvent.logInfo(this.getClass().getSimpleName(), "createSampleItem",
                "Setting external ID: " + externalId + " for sample: " + sample.getAccessionNumber());

        // Set collection date from sample if available, otherwise from defaultFields
        if (sample.getCollectionDate() != null) {
            sampleItem.setCollectionDate(sample.getCollectionDate());
        } else if (!GenericValidator.isBlankOrNull(defaultFields.getCollectionDate())) {
            String collectionDateTime = defaultFields.getCollectionDate();
            if (!GenericValidator.isBlankOrNull(defaultFields.getCollectionTime())) {
                collectionDateTime += " " + defaultFields.getCollectionTime();
            } else {
                collectionDateTime += " 00:00";
            }
            sampleItem.setCollectionDate(DateUtil.convertStringDateToTimestamp(collectionDateTime));
        }

        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(SpringContext.getBean(IStatusService.class)
                .getStatusID(org.openelisglobal.common.services.StatusService.SampleStatus.Entered));

        return sampleItem;
    }

    private void saveAdditionalFields(Sample sample, GenericSampleOrderForm.DefaultFields defaultFields,
            String sysUserId) {
        // Save "from" field as referringId (if this makes sense for your use case)
        // You can also store it as a note or in a different field if needed
        if (!GenericValidator.isBlankOrNull(defaultFields.getFrom())) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveAdditionalFields", "Updating sample referringId to: "
                    + defaultFields.getFrom() + " for sample: " + sample.getAccessionNumber());
            sample.setReferringId(defaultFields.getFrom());
            sampleService.update(sample);
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveAdditionalFields",
                    "Successfully updated referringId for sample: " + sample.getAccessionNumber());
        }

        // Note: "collector" is stored in SampleItem
    }

    private void saveProgramSample(Sample sample, GenericSampleOrderForm form, String sysUserId)
            throws FhirLocalPersistingException {
        // Try to determine program from questionnaire or use default (program ID 7 as
        // seen in frontend)
        LogEvent.logInfo(this.getClass().getSimpleName(), "saveProgramSample",
                "Retrieving program ID 7 for sample: " + sample.getAccessionNumber());
        Program program = programService.get("7");
        if (program == null) {
            String errorMsg = "Program with ID 7 not found for sample: " + sample.getAccessionNumber();
            LogEvent.logError(errorMsg, null);
            throw new RuntimeException(errorMsg);
        }
        LogEvent.logInfo(this.getClass().getSimpleName(), "saveProgramSample",
                "Program retrieved successfully. Program ID: " + program.getId() + ", Name: "
                        + program.getProgramName());

        ProgramSample programSample = new ProgramSample();
        programSample.setProgram(program);
        programSample.setSample(sample);
        programSample.setSysUserId(sysUserId);

        // Generate and save questionnaire response if responses exist
        if (form.getFhirQuestionnaire() != null && form.getFhirResponses() != null
                && !form.getFhirResponses().isEmpty()) {
            UUID questionnaireResponseUuid = UUID.randomUUID();
            programSample.setQuestionnaireResponseUuid(questionnaireResponseUuid);

            // Create and save the FHIR QuestionnaireResponse
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveProgramSample",
                    "Creating QuestionnaireResponse with UUID: " + questionnaireResponseUuid + " for sample: "
                            + sample.getAccessionNumber());
            QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse(form.getFhirQuestionnaire(),
                    form.getFhirResponses(), questionnaireResponseUuid);
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveProgramSample",
                    "Saving QuestionnaireResponse to FHIR store for sample: " + sample.getAccessionNumber());
            fhirPersistanceService.updateFhirResourceInFhirStore(questionnaireResponse);
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveProgramSample",
                    "QuestionnaireResponse saved successfully to FHIR store");
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "saveProgramSample",
                "Saving ProgramSample for sample: " + sample.getAccessionNumber() + ", program: " + program.getId());
        programSampleService.save(programSample);
        LogEvent.logInfo(this.getClass().getSimpleName(), "saveProgramSample",
                "ProgramSample saved successfully. Sample: " + sample.getAccessionNumber());
    }

    /**
     * Saves a notebook sample link with questionnaire response to FHIR store. Uses
     * the notebook_samples table instead of program_sample.
     */
    private void saveNotebookSample(Sample sample, GenericSampleOrderForm form, String sysUserId, String sampleItemId)
            throws FhirLocalPersistingException {
        LogEvent.logInfo(this.getClass().getSimpleName(), "saveNotebookSample",
                "Starting saveNotebookSample for sample: " + sample.getAccessionNumber());

        // Get the notebook
        NoteBook notebook = noteBookService.get(form.getNotebookId());
        if (notebook == null) {
            String errorMsg = "Notebook with ID " + form.getNotebookId() + " not found for sample: "
                    + sample.getAccessionNumber();
            LogEvent.logError(errorMsg, null);
            throw new RuntimeException(errorMsg);
        }
        LogEvent.logInfo(this.getClass().getSimpleName(), "saveNotebookSample",
                "Notebook retrieved successfully. Notebook ID: " + notebook.getId() + ", Title: "
                        + notebook.getTitle());

        // Get the sample item
        SampleItem sampleItem = sampleItemService.get(sampleItemId);
        if (sampleItem == null) {
            String errorMsg = "SampleItem with ID " + sampleItemId + " not found for sample: "
                    + sample.getAccessionNumber();
            LogEvent.logError(errorMsg, null);
            throw new RuntimeException(errorMsg);
        }

        // Create NoteBookSample entry
        NoteBookSample noteBookSample = new NoteBookSample();
        noteBookSample.setNotebook(notebook);
        noteBookSample.setSampleItem(sampleItem);
        noteBookSample.setSysUserId(sysUserId);

        // Generate and save questionnaire response if responses exist
        if (form.getFhirQuestionnaire() != null && form.getFhirResponses() != null
                && !form.getFhirResponses().isEmpty()) {
            UUID questionnaireResponseUuid = UUID.randomUUID();
            noteBookSample.setQuestionnaireResponseUuid(questionnaireResponseUuid);

            // Create and save the FHIR QuestionnaireResponse
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveNotebookSample",
                    "Creating QuestionnaireResponse with UUID: " + questionnaireResponseUuid + " for sample: "
                            + sample.getAccessionNumber());
            QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse(form.getFhirQuestionnaire(),
                    form.getFhirResponses(), questionnaireResponseUuid);
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveNotebookSample",
                    "Saving QuestionnaireResponse to FHIR store for sample: " + sample.getAccessionNumber());
            fhirPersistanceService.updateFhirResourceInFhirStore(questionnaireResponse);
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveNotebookSample",
                    "QuestionnaireResponse saved successfully to FHIR store");
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "saveNotebookSample",
                "Saving NoteBookSample for sample: " + sample.getAccessionNumber() + ", notebook: " + notebook.getId());
        noteBookSampleService.save(noteBookSample);
        LogEvent.logInfo(this.getClass().getSimpleName(), "saveNotebookSample",
                "NoteBookSample saved successfully. Sample: " + sample.getAccessionNumber());
    }

    /**
     * Creates a FHIR QuestionnaireResponse from the questionnaire and responses
     * map.
     * 
     * @param questionnaire The FHIR Questionnaire
     * @param responses     Map of linkId to answer value
     * @param uuid          UUID to assign to the QuestionnaireResponse
     * @return The created QuestionnaireResponse
     */
    private QuestionnaireResponse createQuestionnaireResponse(Questionnaire questionnaire,
            Map<String, Object> responses, UUID uuid) {
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
        questionnaireResponse.setId(uuid.toString());
        questionnaireResponse.setStatus(QuestionnaireResponseStatus.COMPLETED);

        // Set reference to the questionnaire
        if (questionnaire.getId() != null && !questionnaire.getId().isEmpty()) {
            questionnaireResponse.setQuestionnaire("Questionnaire/" + questionnaire.getId());
        }

        // Build items from questionnaire items
        if (questionnaire.getItem() != null && !questionnaire.getItem().isEmpty()) {
            for (QuestionnaireItemComponent questionnaireItem : questionnaire.getItem()) {
                QuestionnaireResponseItemComponent responseItem = createResponseItem(questionnaireItem, responses);
                if (responseItem != null) {
                    questionnaireResponse.addItem(responseItem);
                }
            }
        }

        return questionnaireResponse;
    }

    /**
     * Creates a QuestionnaireResponseItemComponent from a
     * QuestionnaireItemComponent and the responses map.
     *
     * @param questionnaireItem The questionnaire item
     * @param responses         Map of linkId to answer value (can be String or
     *                          List)
     * @return The response item with answers, or null if no answer provided
     */
    private QuestionnaireResponseItemComponent createResponseItem(QuestionnaireItemComponent questionnaireItem,
            Map<String, Object> responses) {
        String linkId = questionnaireItem.getLinkId();
        Object answerValue = responses.get(linkId);

        // Skip if no answer provided
        if (answerValue == null) {
            return null;
        }

        // Skip if empty string
        if (answerValue instanceof String && ((String) answerValue).trim().isEmpty()) {
            return null;
        }

        // Skip if empty list
        if (answerValue instanceof java.util.List && ((java.util.List<?>) answerValue).isEmpty()) {
            return null;
        }

        QuestionnaireResponseItemComponent responseItem = new QuestionnaireResponseItemComponent();
        responseItem.setLinkId(linkId);
        responseItem.setText(questionnaireItem.getText());

        // Handle multiple answers (for choice questions with repeats=true)
        if (answerValue instanceof java.util.List) {
            java.util.List<?> answerList = (java.util.List<?>) answerValue;
            for (Object value : answerList) {
                if (value != null) {
                    QuestionnaireResponseItemAnswerComponent answer = new QuestionnaireResponseItemAnswerComponent();
                    Type answerType = createAnswerType(questionnaireItem.getType(), value.toString());
                    if (answerType != null) {
                        answer.setValue(answerType);
                        responseItem.addAnswer(answer);
                    }
                }
            }
        } else {
            // Single answer
            QuestionnaireResponseItemAnswerComponent answer = new QuestionnaireResponseItemAnswerComponent();
            Type answerType = createAnswerType(questionnaireItem.getType(), answerValue.toString());
            if (answerType != null) {
                answer.setValue(answerType);
                responseItem.addAnswer(answer);
            }
        }

        // Handle nested items (for groups)
        if (questionnaireItem.getItem() != null && !questionnaireItem.getItem().isEmpty()) {
            for (QuestionnaireItemComponent nestedItem : questionnaireItem.getItem()) {
                QuestionnaireResponseItemComponent nestedResponseItem = createResponseItem(nestedItem, responses);
                if (nestedResponseItem != null) {
                    responseItem.addItem(nestedResponseItem);
                }
            }
        }

        return responseItem;
    }

    /**
     * Creates the appropriate FHIR type for an answer based on the question type.
     * 
     * @param questionType The question type (string, boolean, decimal, etc.)
     * @param answerValue  The answer value as a string
     * @return The appropriate FHIR Type, or null if not supported
     */
    private Type createAnswerType(org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType questionType,
            String answerValue) {
        if (answerValue == null || answerValue.trim().isEmpty()) {
            return null;
        }

        try {
            switch (questionType) {
            case STRING:
            case TEXT:
                return new StringType(answerValue);
            case BOOLEAN:
                return new BooleanType(Boolean.parseBoolean(answerValue));
            case DECIMAL:
                return new DecimalType(Double.parseDouble(answerValue));
            case INTEGER:
                return new IntegerType(Integer.parseInt(answerValue));
            case DATE:
                return new DateType(answerValue);
            case TIME:
                return new TimeType(answerValue);
            case CHOICE:
            case OPENCHOICE:
                // For choice questions, try to parse as coding if format is "code|display"
                // Otherwise, treat as string
                if (answerValue.contains("|")) {
                    String[] parts = answerValue.split("\\|", 2);
                    Coding coding = new Coding();
                    coding.setCode(parts[0]);
                    if (parts.length > 1) {
                        coding.setDisplay(parts[1]);
                    }
                    return coding;
                } else {
                    return new StringType(answerValue);
                }
            case QUANTITY:
                // For quantity, expect format like "value|unit" or just "value"
                if (answerValue.contains("|")) {
                    String[] parts = answerValue.split("\\|", 2);
                    Quantity quantity = new Quantity();
                    quantity.setValue(Double.parseDouble(parts[0]));
                    if (parts.length > 1) {
                        quantity.setUnit(parts[1]);
                    }
                    return quantity;
                } else {
                    Quantity quantity = new Quantity();
                    quantity.setValue(Double.parseDouble(answerValue));
                    return quantity;
                }
            default:
                // Default to string for unsupported types
                return new StringType(answerValue);
            }
        } catch (Exception e) {
            // LogEvent.logWarn("Failed to parse answer value: " + answerValue + " for type:
            // " + questionType, e);
            LogEvent.logWarn("GenericSampleOrderServiceImpl", "parseAnswer",
                    "Failed to parse answer value: " + answerValue + " for type: " + questionType);
            // Fallback to string
            return new StringType(answerValue);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public GenericSampleOrderForm getGenericSampleOrderByAccessionNumber(String accessionNumber) {
        GenericSampleOrderForm form = new GenericSampleOrderForm();

        if (GenericValidator.isBlankOrNull(accessionNumber)) {
            return form;
        }

        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        if (sample == null || GenericValidator.isBlankOrNull(sample.getId())) {
            return form;
        }

        // Populate default fields from sample
        GenericSampleOrderForm.DefaultFields defaultFields = new GenericSampleOrderForm.DefaultFields();
        defaultFields.setLabNo(sample.getAccessionNumber());
        defaultFields.setFrom(sample.getReferringId());

        // Get collection date and time from sample
        if (sample.getCollectionDate() != null) {
            // Get collection date - use display field if available, otherwise convert from
            // timestamp
            String collectionDateStr = sample.getCollectionDateForDisplay();
            if (collectionDateStr == null || collectionDateStr.isEmpty()) {
                collectionDateStr = DateUtil.convertTimestampToStringDate(sample.getCollectionDate());
            }
            // collectionDateForDisplay might contain date+time, so extract just the date
            // part
            // Date format is typically MM/dd/yyyy (10 chars) or similar
            if (collectionDateStr != null && !collectionDateStr.isEmpty()) {
                // Check if it contains a space (date + time format)
                int spaceIndex = collectionDateStr.indexOf(' ');
                if (spaceIndex > 0) {
                    defaultFields.setCollectionDate(collectionDateStr.substring(0, spaceIndex));
                } else if (collectionDateStr.length() >= 10) {
                    // Assume first 10 characters are the date (MM/dd/yyyy format)
                    defaultFields.setCollectionDate(collectionDateStr.substring(0, 10));
                } else {
                    defaultFields.setCollectionDate(collectionDateStr);
                }
            }

            // Get collection time from sample
            String collectionTimeStr = sample.getCollectionTimeForDisplay();
            if (collectionTimeStr == null || collectionTimeStr.isEmpty()) {
                collectionTimeStr = DateUtil.convertTimestampToStringTime(sample.getCollectionDate());
            }
            if (collectionTimeStr != null && !collectionTimeStr.isEmpty()) {
                defaultFields.setCollectionTime(collectionTimeStr);
            }
        }

        // Get sample items
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        if (!sampleItems.isEmpty()) {
            SampleItem sampleItem = sampleItems.get(0);
            if (sampleItem.getTypeOfSample() != null) {
                defaultFields.setSampleTypeId(sampleItem.getTypeOfSample().getId());
            }
            if (sampleItem.getQuantity() != null) {
                defaultFields.setQuantity(sampleItem.getQuantity().toString());
            }
            if (sampleItem.getUnitOfMeasure() != null) {
                defaultFields.setSampleUnitOfMeasure(sampleItem.getUnitOfMeasure().getId());
            }
            if (!GenericValidator.isBlankOrNull(sampleItem.getCollector())) {
                defaultFields.setCollector(sampleItem.getCollector());
            }

            // Use sample item collection date if sample doesn't have one
            if (defaultFields.getCollectionDate() == null || defaultFields.getCollectionDate().isEmpty()) {
                if (sampleItem.getCollectionDate() != null) {
                    String collectionDateStr = DateUtil.convertTimestampToStringDate(sampleItem.getCollectionDate());
                    if (collectionDateStr != null && collectionDateStr.length() >= 10) {
                        defaultFields.setCollectionDate(collectionDateStr.substring(0, 10));
                    }
                }
            }
        }

        form.setDefaultFields(defaultFields);

        // First check for notebook sample
        boolean foundNotebookSample = false;
        if (!sampleItems.isEmpty()) {
            SampleItem sampleItem = sampleItems.get(0);
            try {
                List<NoteBookSample> notebookSamples = noteBookSampleService
                        .getNotebookSamplesBySampleItemId(Integer.parseInt(sampleItem.getId()));

                if (!notebookSamples.isEmpty()) {
                    NoteBookSample notebookSample = notebookSamples.get(0);
                    if (notebookSample.getNotebook() != null) {
                        form.setNotebookId(notebookSample.getNotebook().getId());
                        foundNotebookSample = true;

                        // Get questionnaire response if available
                        if (notebookSample.getQuestionnaireResponseUuid() != null) {
                            QuestionnaireResponse questionnaireResponse = getQuestionnaireResponseFromFhir(
                                    notebookSample.getQuestionnaireResponseUuid().toString());

                            if (questionnaireResponse != null) {
                                // Get questionnaire reference
                                String questionnaireRef = questionnaireResponse.getQuestionnaire();
                                if (!GenericValidator.isBlankOrNull(questionnaireRef)) {
                                    String questionnaireId = questionnaireRef.replace("Questionnaire/", "");
                                    Questionnaire questionnaire = getQuestionnaireFromFhir(questionnaireId);
                                    if (questionnaire != null) {
                                        form.setFhirQuestionnaire(questionnaire);
                                    }
                                }

                                // Extract responses from QuestionnaireResponse
                                Map<String, Object> responses = extractResponsesFromQuestionnaireResponse(
                                        questionnaireResponse);
                                form.setFhirResponses(responses);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LogEvent.logError("Failed to retrieve notebook sample for accession: " + accessionNumber, e);
            }
        }

        // If no notebook sample found, check for program sample (legacy approach)
        if (!foundNotebookSample) {
            try {
                // Try to find program sample by sample ID (using null program name to get base
                // ProgramSample)
                ProgramSample programSample = programSampleService
                        .getProgrammeSampleBySample(Integer.parseInt(sample.getId()), null);

                // If not found, try with empty string
                if (programSample == null) {
                    programSample = programSampleService.getProgrammeSampleBySample(Integer.parseInt(sample.getId()),
                            "");
                }

                if (programSample != null && programSample.getQuestionnaireResponseUuid() != null) {
                    // Retrieve questionnaire response from FHIR store
                    QuestionnaireResponse questionnaireResponse = getQuestionnaireResponseFromFhir(
                            programSample.getQuestionnaireResponseUuid().toString());

                    if (questionnaireResponse != null) {
                        // Get questionnaire reference
                        String questionnaireRef = questionnaireResponse.getQuestionnaire();
                        if (!GenericValidator.isBlankOrNull(questionnaireRef)) {
                            String questionnaireId = questionnaireRef.replace("Questionnaire/", "");
                            Questionnaire questionnaire = getQuestionnaireFromFhir(questionnaireId);
                            if (questionnaire != null) {
                                form.setFhirQuestionnaire(questionnaire);
                            }
                        }

                        // Extract responses from QuestionnaireResponse
                        Map<String, Object> responses = extractResponsesFromQuestionnaireResponse(
                                questionnaireResponse);
                        form.setFhirResponses(responses);
                    }
                }
            } catch (Exception e) {
                LogEvent.logError("Failed to retrieve questionnaire response for sample: " + accessionNumber, e);
            }
        }

        return form;
    }

    @Transactional
    @Override
    public Map<String, Object> updateGenericSampleOrder(String accessionNumber, GenericSampleOrderForm form,
            String sysUserId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
            if (sample == null || GenericValidator.isBlankOrNull(sample.getId())) {
                result.put("success", false);
                result.put("error", "Sample not found with accession number: " + accessionNumber);
                return result;
            }

            GenericSampleOrderForm.DefaultFields defaultFields = form.getDefaultFields();

            // Update sample fields
            if (!GenericValidator.isBlankOrNull(defaultFields.getFrom())) {
                sample.setReferringId(defaultFields.getFrom());
            }
            sample.setSysUserId(sysUserId);

            // Update collection date
            if (!GenericValidator.isBlankOrNull(defaultFields.getCollectionDate())) {
                String collectionDateTime = defaultFields.getCollectionDate();
                if (!GenericValidator.isBlankOrNull(defaultFields.getCollectionTime())) {
                    collectionDateTime += " " + defaultFields.getCollectionTime();
                } else {
                    collectionDateTime += " 00:00";
                }
                // Use convertStringDateToTimestamp to preserve time (not truncate it)
                // setCollectionDate() will automatically set collectionDateForDisplay and
                // collectionTimeForDisplay
                Timestamp collectionTimestamp = DateUtil.convertStringDateToTimestamp(collectionDateTime);
                sample.setCollectionDate(collectionTimestamp);
            }

            sampleService.update(sample);

            // Update sample item
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
            if (!sampleItems.isEmpty()) {
                SampleItem sampleItem = sampleItems.get(0);
                sampleItem.setSysUserId(sysUserId);

                if (!GenericValidator.isBlankOrNull(defaultFields.getSampleTypeId())) {
                    TypeOfSample typeOfSample = typeOfSampleService.get(defaultFields.getSampleTypeId());
                    sampleItem.setTypeOfSample(typeOfSample);
                }

                if (!GenericValidator.isBlankOrNull(defaultFields.getQuantity())) {
                    try {
                        sampleItem.setQuantity(Double.parseDouble(defaultFields.getQuantity()));
                    } catch (NumberFormatException e) {
                        // Ignore if quantity is not a valid number
                    }
                }

                if (!GenericValidator.isBlankOrNull(defaultFields.getSampleUnitOfMeasure())) {
                    try {
                        UnitOfMeasure uom = unitOfMeasureService.get(defaultFields.getSampleUnitOfMeasure());
                        sampleItem.setUnitOfMeasure(uom);
                    } catch (Exception e) {
                        // Ignore if unit of measure not found
                    }
                }

                if (!GenericValidator.isBlankOrNull(defaultFields.getCollector())) {
                    sampleItem.setCollector(defaultFields.getCollector());
                }

                if (sample.getCollectionDate() != null) {
                    sampleItem.setCollectionDate(sample.getCollectionDate());
                }

                sampleItemService.update(sampleItem);
            }

            // Update notebook sample or program sample and questionnaire response
            if (form.getFhirQuestionnaire() != null && form.getFhirResponses() != null
                    && !form.getFhirResponses().isEmpty()) {
                // Check if this sample has a notebook sample
                if (form.getNotebookId() != null && !sampleItems.isEmpty()) {
                    updateNotebookSample(sampleItems.get(0), form, sysUserId);
                } else {
                    // Fall back to program sample (legacy)
                    updateProgramSample(sample, form, sysUserId);
                }
            }

            result.put("success", true);
            result.put("message", "Generic sample order updated successfully");
            result.put("accessionNumber", sample.getAccessionNumber());

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            throw e;
        }

        return result;
    }

    private QuestionnaireResponse getQuestionnaireResponseFromFhir(String uuid) {
        try {
            if (StringUtils.isBlank(fhirConfig.getLocalFhirStorePath())) {
                return null;
            }
            IGenericClient fhirClient = fhirUtil.getLocalFhirClient();
            return fhirClient.read().resource(QuestionnaireResponse.class).withId(uuid).execute();
        } catch (Exception e) {
            LogEvent.logError("Failed to retrieve QuestionnaireResponse from FHIR store: " + uuid, e);
            return null;
        }
    }

    private Questionnaire getQuestionnaireFromFhir(String questionnaireId) {
        try {
            if (StringUtils.isBlank(fhirConfig.getLocalFhirStorePath())) {
                return null;
            }
            IGenericClient fhirClient = fhirUtil.getLocalFhirClient();
            return fhirClient.read().resource(Questionnaire.class).withId(questionnaireId).execute();
        } catch (Exception e) {
            LogEvent.logError("Failed to retrieve Questionnaire from FHIR store: " + questionnaireId, e);
            return null;
        }
    }

    private Map<String, Object> extractResponsesFromQuestionnaireResponse(QuestionnaireResponse questionnaireResponse) {
        Map<String, Object> responses = new HashMap<>();

        if (questionnaireResponse.getItem() != null) {
            for (QuestionnaireResponseItemComponent item : questionnaireResponse.getItem()) {
                extractResponsesFromItem(item, responses);
            }
        }

        return responses;
    }

    private void extractResponsesFromItem(QuestionnaireResponseItemComponent item, Map<String, Object> responses) {
        if (item.getAnswer() != null && !item.getAnswer().isEmpty()) {
            // If multiple answers, store as a list
            if (item.getAnswer().size() > 1) {
                java.util.List<String> values = new java.util.ArrayList<>();
                for (QuestionnaireResponseItemAnswerComponent answer : item.getAnswer()) {
                    String value = extractAnswerValue(answer);
                    if (value != null) {
                        values.add(value);
                    }
                }
                if (!values.isEmpty()) {
                    responses.put(item.getLinkId(), values);
                }
            } else {
                // Single answer, store as string
                QuestionnaireResponseItemAnswerComponent answer = item.getAnswer().get(0);
                String value = extractAnswerValue(answer);
                if (value != null) {
                    responses.put(item.getLinkId(), value);
                }
            }
        }

        // Handle nested items
        if (item.getItem() != null) {
            for (QuestionnaireResponseItemComponent nestedItem : item.getItem()) {
                extractResponsesFromItem(nestedItem, responses);
            }
        }
    }

    private String extractAnswerValue(QuestionnaireResponseItemAnswerComponent answer) {
        if (answer.getValue() == null) {
            return null;
        }

        Type value = answer.getValue();
        if (value instanceof StringType) {
            return ((StringType) value).getValue();
        } else if (value instanceof BooleanType) {
            return Boolean.toString(((BooleanType) value).getValue());
        } else if (value instanceof DecimalType) {
            return ((DecimalType) value).getValue().toString();
        } else if (value instanceof IntegerType) {
            return ((IntegerType) value).getValue().toString();
        } else if (value instanceof DateType) {
            return ((DateType) value).getValueAsString();
        } else if (value instanceof TimeType) {
            return ((TimeType) value).getValue();
        } else if (value instanceof Coding) {
            Coding coding = (Coding) value;
            if (!GenericValidator.isBlankOrNull(coding.getDisplay())) {
                return coding.getCode() + "|" + coding.getDisplay();
            }
            return coding.getCode();
        } else if (value instanceof Quantity) {
            Quantity quantity = (Quantity) value;
            if (!GenericValidator.isBlankOrNull(quantity.getUnit())) {
                return quantity.getValue().toString() + "|" + quantity.getUnit();
            }
            return quantity.getValue().toString();
        }

        return value.toString();
    }

    private void updateNotebookSample(SampleItem sampleItem, GenericSampleOrderForm form, String sysUserId) {
        try {
            // Find existing notebook sample for this sample item
            List<NoteBookSample> notebookSamples = noteBookSampleService
                    .getNotebookSamplesBySampleItemId(Integer.parseInt(sampleItem.getId()));

            NoteBookSample notebookSample;
            if (!notebookSamples.isEmpty()) {
                // Update existing notebook sample
                notebookSample = notebookSamples.get(0);
            } else {
                // Create new notebook sample if it doesn't exist
                NoteBook notebook = noteBookService.get(form.getNotebookId());
                if (notebook == null) {
                    LogEvent.logError(this.getClass().getSimpleName(), "updateNotebookSample",
                            "Notebook not found with ID: " + form.getNotebookId());
                    return;
                }
                notebookSample = new NoteBookSample();
                notebookSample.setNotebook(notebook);
                notebookSample.setSampleItem(sampleItem);
            }

            // Update questionnaire response
            if (form.getFhirResponses() != null && !form.getFhirResponses().isEmpty()) {
                UUID questionnaireResponseUuid;
                if (notebookSample.getQuestionnaireResponseUuid() == null) {
                    questionnaireResponseUuid = UUID.randomUUID();
                    notebookSample.setQuestionnaireResponseUuid(questionnaireResponseUuid);
                } else {
                    // Reuse existing UUID to update the response
                    questionnaireResponseUuid = notebookSample.getQuestionnaireResponseUuid();
                }

                try {
                    QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse(
                            form.getFhirQuestionnaire(), form.getFhirResponses(), questionnaireResponseUuid);
                    fhirPersistanceService.updateFhirResourceInFhirStore(questionnaireResponse);
                } catch (FhirLocalPersistingException e) {
                    LogEvent.logError("Failed to update QuestionnaireResponse to FHIR store", e);
                }
            }

            noteBookSampleService.save(notebookSample);
            LogEvent.logInfo(this.getClass().getSimpleName(), "updateNotebookSample",
                    "Successfully updated notebook sample for sample item: " + sampleItem.getId());
        } catch (Exception e) {
            LogEvent.logError("Failed to update notebook sample", e);
        }
    }

    private void updateProgramSample(Sample sample, GenericSampleOrderForm form, String sysUserId) {
        try {
            // Try to find program sample by sample ID
            ProgramSample programSample = programSampleService
                    .getProgrammeSampleBySample(Integer.parseInt(sample.getId()), null);

            // If not found, try with empty string
            if (programSample == null) {
                programSample = programSampleService.getProgrammeSampleBySample(Integer.parseInt(sample.getId()), "");
            }

            if (programSample == null) {
                // Create new program sample if it doesn't exist
                Program program = programService.get("7");
                if (program == null) {
                    return;
                }
                programSample = new ProgramSample();
                programSample.setProgram(program);
                programSample.setSample(sample);
                programSample.setSysUserId(sysUserId);
            } else {
                programSample.setSysUserId(sysUserId);
            }

            // Update questionnaire response
            if (form.getFhirResponses() != null && !form.getFhirResponses().isEmpty()) {
                UUID questionnaireResponseUuid;
                if (programSample.getQuestionnaireResponseUuid() == null) {
                    questionnaireResponseUuid = UUID.randomUUID();
                    programSample.setQuestionnaireResponseUuid(questionnaireResponseUuid);
                } else {
                    questionnaireResponseUuid = programSample.getQuestionnaireResponseUuid();
                }

                try {
                    QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse(
                            form.getFhirQuestionnaire(), form.getFhirResponses(), questionnaireResponseUuid);
                    fhirPersistanceService.updateFhirResourceInFhirStore(questionnaireResponse);
                } catch (FhirLocalPersistingException e) {
                    LogEvent.logError("Failed to update QuestionnaireResponse to FHIR store", e);
                }
            }

            programSampleService.save(programSample);
        } catch (Exception e) {
            LogEvent.logError("Failed to update program sample", e);
        }
    }

    @Override
    public GenericSampleImportResult validateImportFile(InputStream inputStream, String fileName, String contentType) {
        GenericSampleImportResult result = new GenericSampleImportResult();

        try {
            ParseResult parseResult = parseFile(inputStream, fileName, contentType);
            List<String> headers = parseResult.getHeaders();
            List<Map<String, String>> dataRows = parseResult.getDataRows();

            result.setTotalRows(dataRows.size());

            // Validate headers
            if (headers.isEmpty()) {
                result.addError(0, "file", "File is empty or has no header row");
                result.setValid(false);
                return result;
            }

            Map<String, String> headerMap = new HashMap<>();
            for (String header : headers) {
                headerMap.put(header, header);
            }
            List<String> validationErrors = validateHeaders(headerMap);
            if (!validationErrors.isEmpty()) {
                for (String error : validationErrors) {
                    result.addError(0, "header", error);
                }
            }

            // Validate each data row
            int validCount = 0;
            int invalidCount = 0;
            int totalSamples = 0;

            for (int i = 0; i < dataRows.size(); i++) {
                Map<String, String> row = dataRows.get(i);
                int rowNumber = i + 2; // 1-based for user display (row 1 is header, row 2 is first data)

                GenericSampleOrderForm.DefaultFields defaultFields = new GenericSampleOrderForm.DefaultFields();
                List<String> rowErrors = validateAndPopulateRow(row, defaultFields, rowNumber);

                // Validate notebook if specified
                String notebookValue = findValue(row, "notebook", "notebookid", "notebook_id");
                if (!GenericValidator.isBlankOrNull(notebookValue)) {
                    NoteBook notebook = findNotebookByTitleOrId(notebookValue);
                    if (notebook == null) {
                        rowErrors.add("notebook: Notebook not found: '" + notebookValue
                                + "'. Please use a valid notebook title or ID.");
                    } else if (notebook.getQuestionnaireFhirUuid() == null) {
                        rowErrors.add("notebook: Notebook '" + notebookValue + "' has no associated questionnaire.");
                    }
                }

                if (rowErrors.isEmpty()) {
                    validCount++;
                    int sampleQuantity = parseSampleQuantity(
                            findValue(row, "samplequantity", "numberofsamples", "qty"));
                    totalSamples += sampleQuantity;

                    ImportRow importRow = new ImportRow(rowNumber, defaultFields, sampleQuantity);
                    result.addPreviewRow(importRow);
                } else {
                    invalidCount++;
                    for (String error : rowErrors) {
                        String[] parts = error.split(":", 2);
                        String field = parts.length > 0 ? parts[0] : "unknown";
                        String message = parts.length > 1 ? parts[1] : error;
                        result.addError(rowNumber, field, message);
                    }
                }
            }

            result.setValidRows(validCount);
            result.setInvalidRows(invalidCount);
            result.setTotalSamplesToCreate(totalSamples);
            result.setValid(result.getErrors().isEmpty());

        } catch (Exception e) {
            LogEvent.logError("Error validating import file", e);
            result.addError(0, "file", "Error reading file: " + e.getMessage());
            result.setValid(false);
        }

        return result;
    }

    @Override
    public synchronized Map<String, Object> importSamplesFromFile(InputStream inputStream, String fileName,
            String contentType, String sysUserId) {
        Map<String, Object> result = new HashMap<>();
        List<String> createdAccessionNumbers = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int totalCreated = 0;
        int totalFailed = 0;

        try {
            ParseResult parseResult = parseFile(inputStream, fileName, contentType);
            List<Map<String, String>> dataRows = parseResult.getDataRows();

            if (dataRows.isEmpty()) {
                result.put("success", false);
                result.put("error", "File has no data rows");
                return result;
            }

            // Note: Questionnaire will be loaded per row based on notebook column if
            // present
            // If no notebook is specified, no questionnaire will be loaded

            // Process each data row
            for (int i = 0; i < dataRows.size(); i++) {
                Map<String, String> row = dataRows.get(i);
                int rowNumber = i + 2; // 1-based for user display (row 1 is header)

                GenericSampleOrderForm.DefaultFields defaultFields = new GenericSampleOrderForm.DefaultFields();
                List<String> validationErrors = validateAndPopulateRow(row, defaultFields, rowNumber);

                if (!validationErrors.isEmpty()) {
                    totalFailed++;
                    errors.add("Row " + rowNumber + ": " + String.join(", ", validationErrors));
                    continue;
                }

                // Get sample quantity (look for column with sample quantity)
                int sampleQuantity = parseSampleQuantity(findValue(row, "samplequantity", "numberofsamples", "qty"));

                // Create samples based on quantity
                // Each sample will auto-generate its own accession number in its own
                // transaction
                for (int j = 0; j < sampleQuantity; j++) {
                    try {
                        GenericSampleOrderForm form = new GenericSampleOrderForm();
                        GenericSampleOrderForm.DefaultFields fieldsForSample = new GenericSampleOrderForm.DefaultFields();

                        // Copy all fields - but leave labNo empty so it auto-generates
                        // (CSV doesn't have labNo column, so defaultFields.getLabNo() will be empty)
                        fieldsForSample.setLabNo(""); // Force empty to trigger auto-generation
                        fieldsForSample.setSampleTypeId(defaultFields.getSampleTypeId());
                        fieldsForSample.setQuantity(defaultFields.getQuantity());
                        fieldsForSample.setSampleUnitOfMeasure(defaultFields.getSampleUnitOfMeasure());
                        fieldsForSample.setFrom(defaultFields.getFrom());
                        fieldsForSample.setCollector(defaultFields.getCollector());
                        fieldsForSample.setCollectionDate(defaultFields.getCollectionDate());
                        fieldsForSample.setCollectionTime(defaultFields.getCollectionTime());

                        form.setDefaultFields(fieldsForSample);

                        // Check if CSV has notebook column and load notebook-specific questionnaire
                        String notebookValue = findValue(row, "notebook", "notebookid", "notebook_id");
                        Questionnaire questionnaireToUse = null;

                        if (!GenericValidator.isBlankOrNull(notebookValue)) {
                            // Try to find notebook by title or ID
                            NoteBook notebook = findNotebookByTitleOrId(notebookValue);
                            if (notebook != null) {
                                form.setNotebookId(notebook.getId());

                                // Load questionnaire from notebook
                                if (notebook.getQuestionnaireFhirUuid() != null) {
                                    try {
                                        IGenericClient fhirClient = fhirUtil.getLocalFhirClient();
                                        questionnaireToUse = fhirClient.read().resource(Questionnaire.class)
                                                .withId(notebook.getQuestionnaireFhirUuid().toString()).execute();
                                    } catch (Exception e) {
                                        LogEvent.logError("Could not load questionnaire for notebook: " + notebookValue,
                                                e);
                                        // Add error and skip this row
                                        totalFailed++;
                                        errors.add("Row " + rowNumber + ", Sample " + (j + 1)
                                                + ": Invalid notebook - could not load questionnaire for notebook '"
                                                + notebookValue + "'");
                                        continue;
                                    }
                                }
                            } else {
                                // Notebook not found - add error and skip this row
                                totalFailed++;
                                errors.add("Row " + rowNumber + ", Sample " + (j + 1) + ": Notebook not found: '"
                                        + notebookValue + "'. Please use a valid notebook title or ID.");
                                LogEvent.logWarn(this.getClass().getSimpleName(), "importSamplesFromFile",
                                        "Notebook not found: " + notebookValue + " for row " + rowNumber);
                                continue;
                            }
                        }

                        // Dynamically map CSV columns to questionnaire responses
                        if (questionnaireToUse != null) {
                            form.setFhirQuestionnaire(questionnaireToUse);

                            // Build questionnaire responses by matching CSV columns to questionnaire fields
                            Map<String, Object> fhirResponses = mapCsvToQuestionnaireResponses(row, questionnaireToUse);

                            // Only set responses if we have at least one
                            if (!fhirResponses.isEmpty()) {
                                form.setFhirResponses(fhirResponses);
                                LogEvent.logInfo(this.getClass().getSimpleName(), "importSamplesFromFile", "Mapped "
                                        + fhirResponses.size() + " questionnaire responses for row " + rowNumber);
                            }
                        }

                        Map<String, Object> saveResult = self.saveGenericSampleOrderInternal(form, sysUserId);
                        if (saveResult.get("success") != null && Boolean.TRUE.equals(saveResult.get("success"))) {
                            totalCreated++;
                            String accessionNumber = (String) saveResult.get("accessionNumber");
                            if (accessionNumber != null) {
                                createdAccessionNumbers.add(accessionNumber);
                            }
                        } else {
                            // This shouldn't happen if saveGenericSampleOrder throws on error
                            String errorMsg = "Row " + rowNumber + ", Sample " + (j + 1) + ": "
                                    + saveResult.get("error");
                            errors.add(errorMsg);
                            LogEvent.logError(errorMsg, null);
                            throw new RuntimeException(errorMsg);
                        }
                    } catch (Exception e) {
                        // Log the error with full context and root cause
                        String errorMsg = "Row " + rowNumber + ", Sample " + (j + 1) + ": " + e.getMessage();
                        errors.add(errorMsg);

                        // Log with detailed information
                        String logMsg = "Error creating sample from import row " + rowNumber + ", sample " + (j + 1);
                        if (e.getCause() != null) {
                            logMsg += ". Root cause: " + e.getCause().getClass().getSimpleName() + " - "
                                    + e.getCause().getMessage();
                        }
                        LogEvent.logError(logMsg, e);
                        // Also log the exception directly to ensure full stack trace
                        LogEvent.logError(e);

                        // Log root cause separately if it exists
                        if (e.getCause() != null) {
                            LogEvent.logError(
                                    "Root cause exception for import row " + rowNumber + ", sample " + (j + 1),
                                    e.getCause());
                            LogEvent.logError(e.getCause());
                        }

                        // Rethrow to rollback entire transaction
                        throw new RuntimeException(
                                "Import failed at row " + rowNumber + ", sample " + (j + 1) + ": " + e.getMessage(), e);
                    }
                }
            }

            result.put("success", true);
            result.put("totalCreated", totalCreated);
            result.put("totalFailed", totalFailed);
            result.put("createdAccessionNumbers", createdAccessionNumbers);
            result.put("errors", errors);
            result.put("message", "Imported " + totalCreated + " samples successfully");

            LogEvent.logInfo(this.getClass().getSimpleName(), "importSamplesFromFile",
                    "Import completed successfully. Total created: " + totalCreated + ". About to commit transaction.");

        } catch (Exception e) {
            // Log the error with full details - transaction will rollback automatically
            String logMsg = "Error importing samples from file - transaction rolled back";
            if (e.getCause() != null) {
                logMsg += ". Root cause: " + e.getCause().getClass().getSimpleName() + " - "
                        + e.getCause().getMessage();
            } else {
                logMsg += ". Error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
            }
            LogEvent.logError(logMsg, e);
            // Also log the exception directly to ensure full stack trace
            LogEvent.logError(e);

            // Log root cause separately if it exists
            if (e.getCause() != null) {
                LogEvent.logError("Root cause exception for import file", e.getCause());
                LogEvent.logError(e.getCause());
            }

            // Build comprehensive error message
            StringBuilder errorMsg = new StringBuilder("Import failed and transaction rolled back. ");
            if (!errors.isEmpty()) {
                errorMsg.append("Errors encountered: ");
                errorMsg.append(String.join("; ", errors));
                errorMsg.append(". ");
            }
            errorMsg.append("Root cause: ").append(e.getMessage());
            if (e.getCause() != null) {
                errorMsg.append(" (Caused by: ").append(e.getCause().getClass().getSimpleName()).append(" - ")
                        .append(e.getCause().getMessage()).append(")");
            }

            result.put("success", false);
            result.put("error", errorMsg.toString());
            result.put("errors", errors);
            result.put("totalCreated", 0);
            result.put("totalFailed", totalFailed);

            // Rethrow to ensure transaction rollback
            throw new RuntimeException(errorMsg.toString(), e);
        }

        return result;
    }

    private static class ParseResult {
        private List<String> headers;
        private List<Map<String, String>> dataRows;

        public ParseResult(List<String> headers, List<Map<String, String>> dataRows) {
            this.headers = headers;
            this.dataRows = dataRows;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public List<Map<String, String>> getDataRows() {
            return dataRows;
        }
    }

    private ParseResult parseFile(InputStream inputStream, String fileName, String contentType)
            throws IOException, CsvException {
        if (fileName.toLowerCase().endsWith(".csv") || (contentType != null && contentType.contains("text/csv"))) {
            return parseCSV(inputStream);
        } else if (fileName.toLowerCase().endsWith(".xls")
                || (contentType != null && (contentType.contains("spreadsheet") || contentType.contains("excel")))) {
            return parseExcel(inputStream, fileName);
        } else {
            throw new IOException("Unsupported file type. Please use CSV or Excel (.xls) files.");
        }
    }

    private ParseResult parseCSV(InputStream inputStream) throws IOException, CsvException {
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> dataRows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            CSVReader csvReader = new CSVReader(reader);
            List<String[]> allRows = csvReader.readAll();

            if (allRows.isEmpty()) {
                return new ParseResult(headers, dataRows);
            }

            // First row is header
            String[] headerArray = allRows.get(0);
            normalizeHeaders(headerArray);
            for (String header : headerArray) {
                headers.add(header);
            }

            // Process data rows
            for (int i = 1; i < allRows.size(); i++) {
                String[] values = allRows.get(i);
                Map<String, String> row = new HashMap<>();

                for (int j = 0; j < headers.size() && j < values.length; j++) {
                    row.put(headers.get(j), values[j] != null ? values[j].trim() : "");
                }

                dataRows.add(row);
            }
        }

        return new ParseResult(headers, dataRows);
    }

    private ParseResult parseExcel(InputStream inputStream, String fileName) throws IOException {
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> dataRows = new ArrayList<>();

        Workbook workbook = new HSSFWorkbook(inputStream);

        Sheet sheet = workbook.getSheetAt(0);

        // Get header row
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            workbook.close();
            return new ParseResult(headers, dataRows);
        }

        for (Cell cell : headerRow) {
            String headerValue = getCellValueAsString(cell).trim();
            headers.add(headerValue);
        }
        String[] headerArray = headers.toArray(new String[0]);
        normalizeHeaders(headerArray);
        headers.clear();
        for (String header : headerArray) {
            headers.add(header);
        }

        // Process data rows
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            Map<String, String> rowData = new HashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                Cell cell = row.getCell(j);
                String value = getCellValueAsString(cell);
                rowData.put(headers.get(j), value != null ? value.trim() : "");
            }

            dataRows.add(rowData);
        }

        workbook.close();
        return new ParseResult(headers, dataRows);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
        case STRING:
            return cell.getStringCellValue();
        case NUMERIC:
            if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toString();
            } else {
                // Format as integer if it's a whole number, otherwise as decimal
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue)) {
                    return String.valueOf((long) numValue);
                } else {
                    return String.valueOf(numValue);
                }
            }
        case BOOLEAN:
            return String.valueOf(cell.getBooleanCellValue());
        case FORMULA:
            return cell.getCellFormula();
        default:
            return "";
        }
    }

    private void normalizeHeaders(String[] headers) {
        for (int i = 0; i < headers.length; i++) {
            String headerValue = headers[i];
            if (headerValue == null) {
                headers[i] = "column" + (i + 1);
                continue;
            }

            String trimmed = headerValue.trim();
            if (GenericValidator.isBlankOrNull(trimmed)) {
                headers[i] = "column" + (i + 1);
                continue;
            }

            String lower = trimmed.toLowerCase();
            if ("#".equals(trimmed) || "#".equals(lower) || "no.".equals(lower) || "no".equals(lower)) {
                headers[i] = "number";
                continue;
            }

            String normalized = lower.replaceAll("\\s+", "").replaceAll("[^a-z0-9]", "");
            if (GenericValidator.isBlankOrNull(normalized)) {
                normalized = "column" + (i + 1);
            }
            headers[i] = normalized;
        }
    }

    private List<String> validateHeaders(Map<String, String> headerRow) {
        List<String> errors = new ArrayList<>();

        Set<String> headerKeys = headerRow.keySet().stream()
                .map(k -> k.toLowerCase().replaceAll("\\s+", "").replaceAll("[^a-z0-9]", ""))
                .collect(Collectors.toSet());

        // Check if at least one sample type field is present (recommended but not
        // required)
        boolean hasSampleType = headerKeys.stream()
                .anyMatch(k -> k.contains("sampletype") || k.contains("type") || k.equals("type"));

        // Lab number is optional - system will auto-generate if not provided
        // No validation error for missing identifier columns since they're
        // auto-generated

        return errors;
    }

    private List<String> validateAndPopulateRow(Map<String, String> row,
            GenericSampleOrderForm.DefaultFields defaultFields, int rowNumber) {
        List<String> errors = new ArrayList<>();

        // Find lab number (try multiple column names)
        String labNo = findValue(row, "labno", "labnumber", "accessionnumber", "accession");
        if (GenericValidator.isBlankOrNull(labNo)) {
            // Lab number is optional - will be auto-generated
            defaultFields.setLabNo("");
        } else {
            defaultFields.setLabNo(labNo.trim());
        }

        // Find sample type (try multiple column names)
        String sampleTypeId = findValue(row, "sampletypeid", "sampletype", "type");
        if (!GenericValidator.isBlankOrNull(sampleTypeId)) {
            // Try to find by ID or description
            TypeOfSample typeOfSample = findTypeOfSample(sampleTypeId);
            if (typeOfSample != null) {
                defaultFields.setSampleTypeId(typeOfSample.getId());
                LogEvent.logInfo(this.getClass().getSimpleName(), "validateAndPopulateRow",
                        "Found sample type: " + typeOfSample.getDescription() + " (ID: " + typeOfSample.getId()
                                + ") for value: " + sampleTypeId);
            } else {
                // Sample type is optional - just warn instead of error
                LogEvent.logWarn(this.getClass().getSimpleName(), "validateAndPopulateRow",
                        "Could not find sample type for value: " + sampleTypeId + ". Sample item will not be created.");
                // Leave it blank and let the system handle it
            }
        } else {
            LogEvent.logWarn(this.getClass().getSimpleName(), "validateAndPopulateRow",
                    "No sample type specified in CSV row " + rowNumber + ". Sample item will not be created.");
        }

        // Quantity (for sample item)
        String quantity = findValue(row, "quantity", "samplequantity");
        if (!GenericValidator.isBlankOrNull(quantity)) {
            try {
                Double.parseDouble(quantity);
                defaultFields.setQuantity(quantity);
            } catch (NumberFormatException e) {
                errors.add("quantity: Invalid number format: " + quantity);
            }
        }

        // Unit of measure
        String uom = findValue(row, "sampleunitofmeasure", "uom", "unit");
        if (!GenericValidator.isBlankOrNull(uom)) {
            UnitOfMeasure unitOfMeasure = findUnitOfMeasure(uom);
            if (unitOfMeasure != null) {
                defaultFields.setSampleUnitOfMeasure(unitOfMeasure.getId());
            } else {
                // Unit of measure is optional - just warn instead of error
                // errors.add("sampleUnitOfMeasure: Invalid unit of measure: " + uom);
                // Leave it blank and let the system handle it
            }
        }

        // From
        String from = findValue(row, "from", "referringid", "referring");
        if (!GenericValidator.isBlankOrNull(from)) {
            defaultFields.setFrom(from.trim());
        }

        // Collector
        String collector = findValue(row, "collector", "collectedby");
        if (!GenericValidator.isBlankOrNull(collector)) {
            defaultFields.setCollector(collector.trim());
        }

        // Collection date
        String collectionDate = findValue(row, "collectiondate", "date", "coldate");
        if (!GenericValidator.isBlankOrNull(collectionDate)) {
            // Try to parse and normalize date format
            String normalizedDate = normalizeDate(collectionDate.trim());
            if (normalizedDate != null) {
                // Validate using system's DateUtil
                try {
                    DateUtil.convertStringDateToSqlDate(normalizedDate);
                    defaultFields.setCollectionDate(normalizedDate);
                } catch (Exception e) {
                    errors.add("collectionDate: Invalid date format: " + collectionDate
                            + " (could not parse after normalization)");
                }
            } else {
                errors.add("collectionDate: Invalid date format: " + collectionDate
                        + " (supported formats: MM/dd/yyyy, dd/MM/yyyy, yyyy-MM-dd)");
            }
        }

        // Collection time
        String collectionTime = findValue(row, "collectiontime", "time", "coltime");
        if (!GenericValidator.isBlankOrNull(collectionTime)) {
            if (isValidTime(collectionTime)) {
                defaultFields.setCollectionTime(collectionTime.trim());
            } else {
                errors.add("collectionTime: Invalid time format: " + collectionTime);
            }
        }

        return errors;
    }

    private String findValue(Map<String, String> row, String... possibleKeys) {
        for (String key : possibleKeys) {
            // Try exact match first
            if (row.containsKey(key)) {
                String value = row.get(key);
                if (!GenericValidator.isBlankOrNull(value)) {
                    return value;
                }
            }
            // Try case-insensitive match
            for (String rowKey : row.keySet()) {
                String normalizedRowKey = rowKey.toLowerCase().replaceAll("\\s+", "").replaceAll("[^a-z0-9]", "");
                String normalizedKey = key.toLowerCase().replaceAll("\\s+", "").replaceAll("[^a-z0-9]", "");
                if (normalizedRowKey.equals(normalizedKey)) {
                    String value = row.get(rowKey);
                    if (!GenericValidator.isBlankOrNull(value)) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    private int parseSampleQuantity(String quantityStr) {
        if (GenericValidator.isBlankOrNull(quantityStr)) {
            return 1; // Default to 1 sample
        }

        try {
            int qty = Integer.parseInt(quantityStr.trim());
            return Math.max(1, qty); // Ensure at least 1
        } catch (NumberFormatException e) {
            return 1; // Default to 1 if invalid
        }
    }

    private NoteBook findNotebookByTitleOrId(String identifier) {
        if (GenericValidator.isBlankOrNull(identifier)) {
            return null;
        }

        String searchKey = identifier.trim();

        // Try to find by ID first
        try {
            Integer id = Integer.parseInt(searchKey);
            NoteBook notebook = noteBookService.get(id);
            if (notebook != null) {
                return notebook;
            }
        } catch (NumberFormatException e) {
            // Not a number, try finding by title
        }

        // Try to find by title
        List<NoteBook> allNotebooks = noteBookService.getAll();
        for (NoteBook notebook : allNotebooks) {
            if (notebook.getTitle() != null && notebook.getTitle().equalsIgnoreCase(searchKey)) {
                return notebook;
            }
        }

        return null;
    }

    private TypeOfSample findTypeOfSample(String identifier) {
        if (GenericValidator.isBlankOrNull(identifier)) {
            return null;
        }

        String searchKey = identifier.trim();

        // Try to find by ID first
        try {
            TypeOfSample type = typeOfSampleService.get(searchKey);
            if (type != null) {
                return type;
            }
        } catch (Exception e) {
            // Not found by ID, try other methods
        }

        // Try to find by description, localized name, or local abbreviation
        // (case-insensitive)
        List<TypeOfSample> allTypes = typeOfSampleService.getAllTypeOfSamples();
        for (TypeOfSample type : allTypes) {
            // Check description
            if (type.getDescription() != null && type.getDescription().trim().equalsIgnoreCase(searchKey)) {
                return type;
            }

            // Check local abbreviation
            if (type.getLocalAbbreviation() != null && type.getLocalAbbreviation().trim().equalsIgnoreCase(searchKey)) {
                return type;
            }

            // Check localized name
            try {
                if (type.getLocalization() != null) {
                    String localizedName = type.getLocalization().getLocalizedValue();
                    if (localizedName != null && localizedName.trim().equalsIgnoreCase(searchKey)) {
                        return type;
                    }
                }
            } catch (Exception e) {
                // Ignore localization errors
            }
        }

        return null;
    }

    private UnitOfMeasure findUnitOfMeasure(String identifier) {
        if (GenericValidator.isBlankOrNull(identifier)) {
            return null;
        }

        // Try to find by ID first
        try {
            UnitOfMeasure uom = unitOfMeasureService.get(identifier);
            if (uom != null) {
                return uom;
            }
        } catch (Exception e) {
            // Not found by ID
        }

        // Try to find by name/description - search through all
        List<UnitOfMeasure> allUoms = unitOfMeasureService.getAll();
        for (UnitOfMeasure uom : allUoms) {
            if ((uom.getUnitOfMeasureName() != null && uom.getUnitOfMeasureName().equalsIgnoreCase(identifier))
                    || (uom.getDescription() != null && uom.getDescription().equalsIgnoreCase(identifier))) {
                return uom;
            }
        }

        return null;
    }

    private String normalizeDate(String dateStr) {
        if (GenericValidator.isBlankOrNull(dateStr)) {
            return null;
        }

        // Clean the date string - remove HTML entities and trim
        String cleanedDate = dateStr.trim().replace("&#34;", "\"").replace("&quot;", "\"").replace("\"", "").trim();

        // Get the system's expected date format
        String systemDateFormat = DateUtil.getDateFormat();

        // Try multiple date formats in order of likelihood
        String[] formats = { "MM/dd/yyyy", // 12/15/2024 (US format)
                "dd/MM/yyyy", // 15/12/2024 (European format)
                "yyyy-MM-dd", // 2024-12-15 (ISO format)
                "MM-dd-yyyy", // 12-15-2024
                "yyyy/MM/dd", // 2024/12/15
                "dd-MM-yyyy", // 15-12-2024
                "M/d/yyyy", // 12/5/2024 (single digit month/day)
                "d/M/yyyy", // 5/12/2024 (single digit day/month)
                "MM/dd/yy", // 12/15/24 (2-digit year)
                "dd/MM/yy", // 15/12/24 (2-digit year)
        };

        for (String format : formats) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format);
                sdf.setLenient(false);
                java.util.Date date = sdf.parse(cleanedDate);

                // Validate the parsed date makes sense (not too far in past/future)
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                int year = cal.get(Calendar.YEAR);
                if (year < 1900 || year > 2100) {
                    continue; // Try next format
                }

                // Convert to system's expected format
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat(systemDateFormat);
                outputFormat.setLenient(false);
                String normalized = outputFormat.format(date);

                // Verify the normalized date can be parsed back using system format
                try {
                    outputFormat.parse(normalized);
                    return normalized;
                } catch (Exception e) {
                    continue; // Try next format
                }
            } catch (Exception e) {
                // Try next format
                continue;
            }
        }

        return null;
    }

    private boolean isValidDate(String dateStr) {
        if (GenericValidator.isBlankOrNull(dateStr)) {
            return false;
        }

        // First try to normalize the date
        String normalizedDate = normalizeDate(dateStr);
        if (normalizedDate == null) {
            return false;
        }

        // Then validate using system's DateUtil
        try {
            DateUtil.convertStringDateToSqlDate(normalizedDate);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidTime(String timeStr) {
        if (GenericValidator.isBlankOrNull(timeStr)) {
            return false;
        }

        // Basic time format validation (HH:mm or HH:mm:ss)
        return timeStr.matches("^\\d{1,2}:\\d{2}(:\\d{2})?$");
    }

    private String getNextAccessionNumber() {
        // Note: This method is called from within a synchronized block, so no need to
        // synchronize here
        // The generator's getNextAccessionNumber() uses atomic UPDATE on
        // accession_number_info table
        if (accessionNumberGenerator == null) {
            accessionNumberGenerator = AccessionNumberUtil.getMainAccessionNumberGenerator();
        }
        if (accessionNumberGenerator != null) {
            // Use getNextAccessionNumber with null programCode and true for reserve
            return accessionNumberGenerator.getNextAccessionNumber(null, true);
        }
        // Fallback to old method if generator not available
        return sampleDAO.getNextAccessionNumber();
    }

    private String incrementAccessionNumber(String baseAccessionNumber, int increment) {
        try {
            // Get the generator to understand the format
            if (accessionNumberGenerator == null) {
                accessionNumberGenerator = AccessionNumberUtil.getMainAccessionNumberGenerator();
            }

            if (accessionNumberGenerator instanceof org.openelisglobal.common.provider.validation.BaseSiteYearAccessionValidator) {
                // Use SiteYear format: PREFIX + 2-digit year + 13-digit sequence
                org.openelisglobal.common.provider.validation.BaseSiteYearAccessionValidator validator = (org.openelisglobal.common.provider.validation.BaseSiteYearAccessionValidator) accessionNumberGenerator;

                // getPrefix() is accessible through IAccessionNumberValidator interface
                String prefix = accessionNumberGenerator.getPrefix();
                int prefixLength = prefix.length();
                int yearLength = 2;
                int sequenceLength = 13;

                if (baseAccessionNumber == null
                        || baseAccessionNumber.length() < prefixLength + yearLength + sequenceLength) {
                    // Invalid format, fall back to generating a new number
                    return getNextAccessionNumber();
                }

                // Extract components
                String extractedPrefix = baseAccessionNumber.substring(0, prefixLength);
                String year = baseAccessionNumber.substring(prefixLength, prefixLength + yearLength);
                String sequenceStr = baseAccessionNumber.substring(prefixLength + yearLength);

                // Validate prefix matches
                if (!extractedPrefix.equals(prefix)) {
                    // Prefix doesn't match, generate new number
                    return getNextAccessionNumber();
                }

                // Parse and increment sequence
                long sequence;
                try {
                    sequence = Long.parseLong(sequenceStr);
                } catch (NumberFormatException e) {
                    // Invalid sequence format, fall back to generating a new number
                    return getNextAccessionNumber();
                }

                sequence += increment;

                // Check if sequence exceeds maximum (13 digits = 9999999999999)
                if (sequence > 9999999999999L) {
                    LogEvent.logError("Error incrementing accession number: sequence exceeds maximum. Base: "
                            + baseAccessionNumber + ", Increment: " + increment + ", Result sequence: " + sequence,
                            null);
                    throw new RuntimeException("Accession number sequence exceeds maximum");
                }

                // Format sequence back to 13 digits with leading zeros
                String newSequence = String.format("%013d", sequence);

                return prefix + year + newSequence;
            } else {
                // For other formats, fall back to generating a new number
                return getNextAccessionNumber();
            }

        } catch (Exception e) {
            LogEvent.logError(
                    "Error incrementing accession number: " + baseAccessionNumber + ", increment: " + increment, e);
            // Fallback: generate a new number using the generator
            return getNextAccessionNumber();
        }
    }

    private String generateLabNumber() {
        try {
            // Call the same endpoint that the frontend uses
            // This is a simplified version - you might want to use the actual service
            Sample tempSample = new Sample();
            sampleService.insertDataWithAccessionNumber(tempSample);
            return tempSample.getAccessionNumber();
        } catch (Exception e) {
            LogEvent.logError("Error generating lab number", e);
            // Fallback: generate a timestamp-based number
            return "IMP-" + System.currentTimeMillis();
        }
    }

    private Questionnaire loadQuestionnaire(String questionnaireId) {
        if (GenericValidator.isBlankOrNull(questionnaireId)) {
            return null;
        }

        try {
            IGenericClient fhirClient = fhirUtil.getLocalFhirClient();
            Questionnaire questionnaire = fhirClient.read().resource(Questionnaire.class).withId(questionnaireId)
                    .execute();
            LogEvent.logInfo(this.getClass().getSimpleName(), "loadQuestionnaire",
                    "Loaded questionnaire " + questionnaireId);
            return questionnaire;
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "loadQuestionnaire",
                    "Unable to load questionnaire " + questionnaireId + ". " + e.getMessage());
            return null;
        }
    }

    /**
     * Dynamically maps CSV row columns to FHIR Questionnaire responses. Matches CSV
     * column names to questionnaire item text/linkId.
     *
     * @param row           CSV row data
     * @param questionnaire FHIR Questionnaire
     * @return Map of linkId to answer value
     */
    private Map<String, Object> mapCsvToQuestionnaireResponses(Map<String, String> row, Questionnaire questionnaire) {
        Map<String, Object> responses = new HashMap<>();

        if (questionnaire == null || questionnaire.getItem() == null || questionnaire.getItem().isEmpty()) {
            return responses;
        }

        // Iterate through questionnaire items
        for (org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent item : questionnaire.getItem()) {
            String linkId = item.getLinkId();
            String questionText = item.getText();
            boolean isMultiSelect = item.getRepeats() && "choice".equals(item.getType().toCode());

            if (GenericValidator.isBlankOrNull(questionText)) {
                continue;
            }

            // Try to find matching CSV column in this priority order:
            // 1. Direct linkId match (e.g., column "1", "2", "3")
            // 2. Normalized question text (e.g., "Sample Type" -> "sampletype")
            // 3. CamelCase version (e.g., "Sample Type" -> "sampleType")
            String value = null;

            // Try 1: Direct linkId match
            value = findValue(row, linkId);

            // Try 2: Normalized question text
            if (GenericValidator.isBlankOrNull(value)) {
                String normalizedText = normalizeFieldName(questionText);
                value = findValue(row, normalizedText);
            }

            // Try 3: CamelCase version (e.g., "sampleType", "controlType",
            // "expectedCtValue")
            if (GenericValidator.isBlankOrNull(value)) {
                String camelCase = toCamelCase(questionText);
                value = findValue(row, camelCase);
            }

            if (!GenericValidator.isBlankOrNull(value)) {
                // Handle multi-select values (pipe-separated: "n-gene|e-gene|orf1ab")
                if (isMultiSelect && value.contains("|")) {
                    String[] values = value.split("\\|");
                    List<String> valueList = new ArrayList<>();
                    for (String v : values) {
                        String trimmed = v.trim();
                        if (!GenericValidator.isBlankOrNull(trimmed)) {
                            valueList.add(trimmed);
                        }
                    }
                    if (!valueList.isEmpty()) {
                        responses.put(linkId, valueList);
                        LogEvent.logInfo(this.getClass().getSimpleName(), "mapCsvToQuestionnaireResponses",
                                "Mapped multi-select CSV column: '" + questionText + "' (linkId: " + linkId + ") = "
                                        + valueList);
                    }
                } else {
                    responses.put(linkId, value);
                    LogEvent.logInfo(this.getClass().getSimpleName(), "mapCsvToQuestionnaireResponses",
                            "Mapped CSV column to questionnaire item: '" + questionText + "' (linkId: " + linkId
                                    + ") = '" + value + "'");
                }
            }
        }

        return responses;
    }

    /**
     * Converts a question text to camelCase. "Sample Type" -> "sampleType"
     * "Expected Ct Value" -> "expectedCtValue"
     */
    private String toCamelCase(String text) {
        String[] words = text.split("\\s+");
        if (words.length == 0) {
            return "";
        }

        StringBuilder camelCase = new StringBuilder(words[0].toLowerCase());
        for (int i = 1; i < words.length; i++) {
            String word = words[i];
            if (!word.isEmpty()) {
                camelCase.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    camelCase.append(word.substring(1).toLowerCase());
                }
            }
        }
        return camelCase.toString();
    }

    /**
     * Normalizes a field name for matching. Removes spaces, special chars, converts
     * to lowercase.
     */
    private String normalizeFieldName(String text) {
        return text.toLowerCase().replaceAll("[^a-z0-9]", "") // Remove non-alphanumeric
                .trim();
    }

    /**
     * Finds a value in the CSV row by trying multiple variations of the question
     * text. Tries: exact match, normalized match, individual words, etc.
     */
    private String findValueByQuestionText(Map<String, String> row, String questionText, String normalized) {
        // Try 1: Exact normalized match (e.g., "samplesource")
        String value = findValue(row, normalized);
        if (!GenericValidator.isBlankOrNull(value)) {
            return value;
        }
        return null;
    }
}
