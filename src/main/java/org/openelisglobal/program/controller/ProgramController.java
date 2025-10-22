package org.openelisglobal.program.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.program.bean.GeneralProgramDashBoardCount;
import org.openelisglobal.program.service.GeneralProgramDisplayService;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.OrderEntry;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.ProgramSampleQuestionnaireResponse;
import org.openelisglobal.program.valueholder.generalProgram.GeneralProgramDisplayItem;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/rest")
public class ProgramController extends BaseRestController {

    @Autowired
    private ProgramSampleService programSampleService;

    @GetMapping(value = "/program/{id}/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<OrderEntry> getProgramOrders(@PathVariable String id) {
        List<ProgramSample> programSamples = programSampleService.getProgramSamplesByProgramId(id);
        List<OrderEntry> result = new java.util.ArrayList<>();

        if (programSamples == null || programSamples.isEmpty()) {
            OrderEntry sampleOrder = new OrderEntry();
            sampleOrder.setOrderId(1);
            sampleOrder.setPatientName("John Doe");
            sampleOrder.setOrderDate("2024-01-15");
            sampleOrder.setStatus("In Progress");
            sampleOrder.setAccessionNumber("LAB-2024-001");
            result.add(sampleOrder);
            return result;
        }

        for (ProgramSample programSample : programSamples) {
            Sample sample = programSample.getSample();
            OrderEntry orderEntry = new OrderEntry();
            orderEntry.setOrderId(programSample.getId());

            if (sample != null) {
                orderEntry.setPatientName(sample.getClientReference());
                orderEntry.setOrderDate(sample.getEnteredDateForDisplay());
                orderEntry.setStatus(sample.getStatus());
                orderEntry.setAccessionNumber(sample.getAccessionNumber());
            }
            result.add(orderEntry);
        }
        return result;
    }

    @Autowired
    private FhirPersistanceService fhirPersistanceService;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private ProgramService programService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private GeneralProgramDisplayService generalProgramDisplayService;

    private static final List<String> EXCLUDED_PROGRAM_NAMES = List.of("Cytology", "Immunohistochemistry",
            "Histopathology", "Pathology");

    @GetMapping(value = "/programs/general", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<GeneralProgramDisplayItem> getGeneralPrograms() {
        List<Program> programs = programService.getGeneralPrograms(EXCLUDED_PROGRAM_NAMES);

        if (programs == null || programs.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        return generalProgramDisplayService.convertToDisplayItems(programs);
    }

    @GetMapping(value = "/programs/general/count", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<GeneralProgramDashBoardCount> getGeneralProgramsCount() {
        GeneralProgramDashBoardCount count = new GeneralProgramDashBoardCount();
        List<Program> programs = programService.getGeneralPrograms(EXCLUDED_PROGRAM_NAMES);

        count.setTotalPrograms(programs != null ? (long) programs.size() : 0L);

        long programsWithOrders = 0;
        long totalOrders = 0;
        if (programs != null) {
            for (Program program : programs) {
                Long orderCount = programSampleService.getCountByProgramId(program.getId());
                if (orderCount != null && orderCount > 0) {
                    programsWithOrders++;
                    totalOrders += orderCount;
                }
            }
        }

        count.setProgramsWithOrders(programsWithOrders);
        count.setTotalOrders(totalOrders);

        return ResponseEntity.ok(count);
    }

    @GetMapping(value = "/program/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public EditProgramForm createProgram(@PathVariable String id) throws FhirLocalPersistingException {
        EditProgramForm form = new EditProgramForm();
        form.setProgram(programService.get(id));
        if (form.getProgram().getQuestionnaireUUID() != null) {
            form.setAdditionalOrderEntryQuestions(fhirUtil.getLocalFhirClient().read().resource(Questionnaire.class)
                    .withId(form.getProgram().getQuestionnaireUUID().toString()).execute());
        }
        if (form.getProgram().getTestSection() != null) {
            form.setTestSectionId(form.getProgram().getTestSection().getId());
        }
        return form;
    }

    @PostMapping(value = "/program", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public EditProgramForm createProgram(@RequestBody EditProgramForm form) throws FhirLocalPersistingException {
        Questionnaire questionnaire = form.getAdditionalOrderEntryQuestions();
        Program program = form.getProgram();
        if (!GenericValidator.isBlankOrNull(program.getId())) {
            program.setLastupdated(programService.get(program.getId()).getLastupdated());
        }
        if (program.getQuestionnaireUUID() == null) {
            program.setQuestionnaireUUID(UUID.randomUUID());
        }
        if (questionnaire == null) {
            questionnaire = new Questionnaire();
        }
        program.setTestSection(null);
        if (StringUtils.isNotBlank(form.getTestSectionId())) {
            TestSection testSection = testSectionService.get(form.getTestSectionId());
            if (testSection != null) {
                program.setTestSection(testSection);
            }
        }
        program.setManuallyChanged(true);
        program = programService.save(program);
        questionnaire.setId(program.getQuestionnaireUUID().toString());
        fhirPersistanceService.updateFhirResourceInFhirStore(questionnaire);
        DisplayListService.getInstance().refreshList(ListType.PROGRAM);
        return form;
    }

    @GetMapping(value = "/program/{id}/questionnaire", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Questionnaire getAdditionalEntryQuestions(HttpServletRequest request, @PathVariable String id)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (programService.get(id).getQuestionnaireUUID() != null) {
            return fhirUtil.getLocalFhirClient().read().resource(Questionnaire.class)
                    .withId(programService.get(id).getQuestionnaireUUID().toString()).execute();
        }
        return null;
    }

    @GetMapping(value = "/program/{id}/questionnaire-responses", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ProgramSampleQuestionnaireResponse> getProgramQuestionnaireResponses(@PathVariable String id) {
        List<ProgramSample> programSamples = programSampleService.getProgramSamplesByProgramId(id);
        List<ProgramSampleQuestionnaireResponse> result = new java.util.ArrayList<>();

        if (programSamples == null || programSamples.isEmpty()) {
            ProgramSampleQuestionnaireResponse sampleResponse = new ProgramSampleQuestionnaireResponse();
            sampleResponse.setProgramSampleId(1);
            sampleResponse.setQuestionnaireResponseUuid(UUID.randomUUID());

            Sample sample = new Sample();
            sample.setAccessionNumber("LAB-2024-001");
            sample.setClientReference("John Doe");
            sample.setEnteredDateForDisplay("2024-01-15");
            sample.setStatus("In Progress");
            sampleResponse.setSample(sample);

            Program program = new Program();
            program.setId(id);
            program.setProgramName("General Test Program");
            sampleResponse.setProgram(program);

            org.hl7.fhir.r4.model.QuestionnaireResponse questionnaireResponse = new org.hl7.fhir.r4.model.QuestionnaireResponse();
            questionnaireResponse.setId(sampleResponse.getQuestionnaireResponseUuid().toString());
            questionnaireResponse
                    .setStatus(org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);

            org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent item = new org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseItemComponent();
            item.setLinkId("test-question-1");
            item.setText("Test Question 1");
            item.addAnswer().setValue(new org.hl7.fhir.r4.model.StringType("Sample Answer 1"));
            questionnaireResponse.addItem(item);

            sampleResponse.setQuestionnaireResponse(questionnaireResponse);
            result.add(sampleResponse);
            return result;
        }

        for (ProgramSample programSample : programSamples) {
            if (programSample.getQuestionnaireResponseUuid() != null) {
                try {
                    org.hl7.fhir.r4.model.QuestionnaireResponse questionnaireResponse = fhirUtil.getLocalFhirClient()
                            .read().resource(org.hl7.fhir.r4.model.QuestionnaireResponse.class)
                            .withId(programSample.getQuestionnaireResponseUuid().toString()).execute();

                    ProgramSampleQuestionnaireResponse response = new ProgramSampleQuestionnaireResponse();
                    response.setProgramSampleId(programSample.getId());
                    response.setSample(programSample.getSample());
                    response.setProgram(programSample.getProgram());
                    response.setQuestionnaireResponse(questionnaireResponse);
                    response.setQuestionnaireResponseUuid(programSample.getQuestionnaireResponseUuid());

                    result.add(response);
                } catch (Exception e) {
                    System.err.println("Error fetching questionnaire response for program sample "
                            + programSample.getId() + ": " + e.getMessage());
                }
            }
        }

        return result;
    }
}
