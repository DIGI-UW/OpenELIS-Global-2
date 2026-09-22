package org.openelisglobal.program.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
    private QuestionnaireStorageService questionnaireStorageService;
    @Autowired
    private ProgramService programService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private ProgramSampleService programSampleService;

    @GetMapping(value = "/program/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public EditProgramForm createProgram(@PathVariable String id) {
        EditProgramForm form = new EditProgramForm();
        Program program = programService.get(id);
        form.setProgram(program);
        form.setAdditionalOrderEntryQuestions(
                questionnaireStorageService.getQuestionnaire(program.getQuestionnaireUUID()).orElse(null));
        if (program.getTestSection() != null) {
            form.setTestSectionId(program.getTestSection().getId());
        }
        form.setDomain(program.getDomain());
        form.setActive(!"N".equalsIgnoreCase(program.getIsActive()));
        if (program.getLabUnits() != null && !program.getLabUnits().isEmpty()) {
            form.setLabUnitIds(
                    program.getLabUnits().stream().map(TestSection::getId).sorted().collect(Collectors.toList()));
        } else if (program.getTestSection() != null) {
            // Legacy single-FK fallback so the admin editor still shows a lab
            // unit before the row has been re-saved through the many-to-many.
            List<String> single = new ArrayList<>();
            single.add(program.getTestSection().getId());
            form.setLabUnitIds(single);
        }
        return form;
    }

    @PostMapping(value = "/program", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public EditProgramForm createProgram(@RequestBody EditProgramForm form) {
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

        // Programs V2 - domain and active flow onto the entity so Liquibase's
        // NOT NULL constraint is satisfied and the picker filter works. The
        // JSON field name is `active` (boolean); storage is 'Y'/'N' to match
        // panel / test_section.
        if (StringUtils.isNotBlank(form.getDomain())) {
            program.setDomain(form.getDomain());
        }
        if (form.getActive() != null) {
            program.setIsActive(form.getActive() ? "Y" : "N");
        }

        // Many-to-many lab units. The legacy single testSection FK is kept
        // populated (first entry wins) so older readers still resolve one lab
        // unit before they migrate to labUnitIds.
        Set<TestSection> resolvedLabUnits = new HashSet<>();
        List<String> labUnitIds = form.getLabUnitIds();
        if (labUnitIds != null && !labUnitIds.isEmpty()) {
            for (String labUnitId : labUnitIds) {
                if (StringUtils.isBlank(labUnitId)) {
                    continue;
                }
                TestSection section = testSectionService.get(labUnitId);
                if (section != null) {
                    resolvedLabUnits.add(section);
                }
            }
        }
        program.setLabUnits(resolvedLabUnits);

        program.setTestSection(null);
        if (!resolvedLabUnits.isEmpty()) {
            program.setTestSection(resolvedLabUnits.iterator().next());
        } else if (StringUtils.isNotBlank(form.getTestSectionId())) {
            TestSection testSection = testSectionService.get(form.getTestSectionId());
            if (testSection != null) {
                program.setTestSection(testSection);
                program.getLabUnits().add(testSection);
            }
        }

        program.setManuallyChanged(true);
        program = programService.save(program);
        questionnaire.setId(program.getQuestionnaireUUID().toString());
        questionnaireStorageService.saveQuestionnaire(questionnaire);
        DisplayListService.getInstance().refreshList(ListType.PROGRAM);

        // Re-project the persisted state so the caller sees canonicalized
        // domain/active/labUnitIds without a follow-up GET.
        form.setProgram(program);
        form.setDomain(program.getDomain());
        form.setActive(!"N".equalsIgnoreCase(program.getIsActive()));
        form.setLabUnitIds(
                program.getLabUnits().stream().map(TestSection::getId).sorted().collect(Collectors.toList()));
        return form;
    }

    @GetMapping(value = "/program/{id}/questionnaire", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Questionnaire getAdditionalEntryQuestions(HttpServletRequest request, @PathVariable String id) {
        Program program = programService.get(id);
        if (program == null) {
            return null;
        }
        return questionnaireStorageService.getQuestionnaire(program.getQuestionnaireUUID()).orElse(null);
    }

    // Programs V2 - the admin list needs domain / active / labUnitIds per row.
    // /rest/displayList/PROGRAM only carries id + name, so the admin UI reads
    // this instead. Not paginated because the list is small (typically < 50
    // rows) and the admin screen filters client-side.
    @GetMapping(value = "/program-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> listPrograms() {
        List<Program> programs = programService.getAll();
        return programs.stream()
                .sorted(Comparator.comparing(Program::getProgramName,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toListRow).collect(Collectors.toList());
    }

    private Map<String, Object> toListRow(Program program) {
        List<String> labUnitIds = new ArrayList<>();
        if (program.getLabUnits() != null && !program.getLabUnits().isEmpty()) {
            labUnitIds = program.getLabUnits().stream().map(TestSection::getId).sorted().collect(Collectors.toList());
        } else if (program.getTestSection() != null) {
            labUnitIds.add(program.getTestSection().getId());
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", program.getId());
        row.put("name", program.getProgramName());
        row.put("code", program.getCode());
        row.put("domain", program.getDomain());
        row.put("active", !"N".equalsIgnoreCase(program.getIsActive()));
        row.put("labUnitIds", labUnitIds);
        return row;
    }

    // Programs V2 - read-time historical order count for the deactivate modal
    // (FR-18.2). Returns 0 for programs with no orders. The frontend degrades
    // gracefully on 404 in deployments that have not applied this slice.
    @GetMapping(value = "/program/{id}/orderCount", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getOrderCount(@PathVariable String id) {
        long count = programSampleService.countByProgramId(id);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("programId", id);
        body.put("count", count);
        return body;
    }
}
