package org.openelisglobal.program.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.program.bean.DashboardSummary;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.ProgramUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenericProgramServiceImpl implements GenericProgramService {

    @Autowired
    private ProgramSampleService programSampleService;

    @Autowired
    private FhirUtil fhirUtil;

    @Override
    @Transactional(readOnly = true)
    public List<ProgramUtil> getAllProgramEntries() {
        List<ProgramSample> programSamples = programSampleService.getAll();

        return programSamples.stream().map(this::convertToProgramUtil).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramUtil getProgramEntry(Integer programSampleId) {
        ProgramSample programSample = programSampleService.get(programSampleId);
        return convertToProgramUtil(programSample);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary() {
        List<ProgramSample> allSamples = programSampleService.getAll();

        long totalEntries = allSamples.size();
        long completedQuestionnaires = allSamples.stream().filter(ps -> ps.getQuestionnaireResponseUuid() != null)
                .count();
        long pendingQuestionnaires = totalEntries - completedQuestionnaires;
        Map<String, Long> entriesByProgram = allSamples.stream()
                .collect(Collectors.groupingBy(ps -> ps.getProgram().getProgramName(), Collectors.counting()));

        return new DashboardSummary(totalEntries, completedQuestionnaires, pendingQuestionnaires, entriesByProgram);
    }

    private ProgramUtil convertToProgramUtil(ProgramSample programSample) {
        if (programSample == null) {
            return null;
        }

        ProgramUtil programUtil = new ProgramUtil();
        programUtil.setProgramSample(programSample);

        // Load questionnaire if UUID exists
        if (programSample.getProgram() != null && programSample.getProgram().getQuestionnaireUUID() != null) {
            try {
                Questionnaire questionnaire = fhirUtil.getLocalFhirClient().read().resource(Questionnaire.class)
                        .withId(programSample.getProgram().getQuestionnaireUUID().toString()).execute();
                programUtil.setProgramQuestionnaire(questionnaire);
            } catch (Exception e) {
                // Log error but don't fail completely
                System.err.println("Error loading questionnaire: " + e.getMessage());
            }
        }

        // Load questionnaire response if UUID exists
        if (programSample.getQuestionnaireResponseUuid() != null) {
            try {
                QuestionnaireResponse questionnaireResponse = fhirUtil.getLocalFhirClient().read()
                        .resource(QuestionnaireResponse.class)
                        .withId(programSample.getQuestionnaireResponseUuid().toString()).execute();
                programUtil.setProgramQuestionnaireResponse(questionnaireResponse);
            } catch (Exception e) {
                // Log error but don't fail completely
                System.err.println("Error loading questionnaire response: " + e.getMessage());
            }
        }

        return programUtil;
    }
}