package org.openelisglobal.program.controller;

import jakarta.servlet.http.HttpServletRequest;
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
import org.openelisglobal.common.domain.Domain;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.program.service.ProgramPickerRules;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.questionnaire.service.QuestionnaireStorageService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Programs REST endpoints. Authorization is per method rather than per class:
 * {@code /program/{id}} and {@code /program/{id}/questionnaire} are read by
 * order entry and so stay open to any authenticated user, while the
 * administration endpoints are restricted to ADMIN like every other Test
 * Management controller, including the {@code /rest/lab-units-management}
 * endpoint the Programs screen already calls.
 */
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
        Program program = requireProgram(id);
        EditProgramForm form = new EditProgramForm();
        form.setAdditionalOrderEntryQuestions(
                questionnaireStorageService.getQuestionnaire(program.getQuestionnaireUUID()).orElse(null));
        project(form, program);
        return form;
    }

    /**
     * Creates or updates a program. Programs V2 fields ({@code domain},
     * {@code active}, {@code labUnitIds}) are additive: a client that does not send
     * one keeps the persisted value, so the legacy editor, the lifecycle flip and a
     * partial payload can never silently reset a program to Clinical, reactivate
     * it, drop its lab units or orphan its questionnaire.
     */
    @PostMapping(value = "/program", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public EditProgramForm createProgram(@RequestBody EditProgramForm form) {
        Program program = form.getProgram();
        if (program == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "program is required");
        }
        Program existing = GenericValidator.isBlankOrNull(program.getId()) ? null : requireProgram(program.getId());
        if (existing != null) {
            program.setLastupdated(existing.getLastupdated());
            if (program.getQuestionnaireUUID() == null) {
                program.setQuestionnaireUUID(existing.getQuestionnaireUUID());
            }
        }
        Questionnaire questionnaire = form.getAdditionalOrderEntryQuestions();
        if (questionnaire == null && existing == null) {
            questionnaire = new Questionnaire();
        }
        // Only mint a questionnaire id when one is actually being stored, so a
        // lifecycle flip on a program that never had a questionnaire leaves it
        // exactly as it was instead of pointing it at an empty resource.
        if (program.getQuestionnaireUUID() == null && questionnaire != null) {
            program.setQuestionnaireUUID(UUID.randomUUID());
        }

        program.setDomain(resolveDomain(form.getDomain(), existing));
        program.setIsActive(resolveActive(form.getActive(), existing));
        Set<TestSection> labUnits = resolveLabUnits(form, existing);
        program.setLabUnits(labUnits);
        program.setTestSection(ProgramPickerRules.firstLabUnit(labUnits));
        program.setManuallyChanged(true);

        program = programService.save(program);
        if (questionnaire != null) {
            questionnaire.setId(program.getQuestionnaireUUID().toString());
            questionnaireStorageService.saveQuestionnaire(questionnaire);
        } else if (program.getQuestionnaireUUID() != null) {
            questionnaire = questionnaireStorageService.getQuestionnaire(program.getQuestionnaireUUID()).orElse(null);
        }
        DisplayListService.getInstance().refreshList(ListType.PROGRAM);

        form.setAdditionalOrderEntryQuestions(questionnaire);
        project(form, program);
        return form;
    }

    @GetMapping(value = "/program/{id}/questionnaire", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Questionnaire getAdditionalEntryQuestions(HttpServletRequest request, @PathVariable String id) {
        Program program = findProgram(id);
        if (program == null) {
            return null;
        }
        return questionnaireStorageService.getQuestionnaire(program.getQuestionnaireUUID()).orElse(null);
    }

    /**
     * Admin list rows with domain, status and lab units.
     * {@code /rest/displayList/PROGRAM} only carries id and name. Not paginated:
     * the list is small and the admin screen filters client-side.
     */
    @GetMapping(value = "/program-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public List<Map<String, Object>> listPrograms() {
        return programService.getAll().stream()
                .sorted(Comparator.comparing(Program::getProgramName,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::toListRow).collect(Collectors.toList());
    }

    /**
     * FR-18.2: read-time count of orders filed under a program, for the deactivate
     * confirmation.
     */
    @GetMapping(value = "/program/{id}/orderCount", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public Map<String, Object> getOrderCount(@PathVariable String id) {
        if (!StringUtil.isInteger(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "program id must be numeric");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("programId", id);
        body.put("count", programSampleService.countByProgramId(id));
        return body;
    }

    private Program requireProgram(String id) {
        Program program = findProgram(id);
        if (program == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No program with id " + id);
        }
        return program;
    }

    private Program findProgram(String id) {
        if (!StringUtil.isInteger(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "program id must be numeric");
        }
        return programService.getMatch("id", id).orElse(null);
    }

    private String resolveDomain(String requested, Program existing) {
        if (StringUtils.isNotBlank(requested)) {
            Domain domain = Domain.fromRaw(requested);
            if (domain == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown program domain: " + requested);
            }
            return domain.name();
        }
        if (existing != null) {
            return Domain.normalize(existing.getDomain());
        }
        return Domain.DEFAULT.name();
    }

    private String resolveActive(Boolean requested, Program existing) {
        if (requested != null) {
            return requested ? "Y" : "N";
        }
        return existing == null || ProgramPickerRules.isActive(existing) ? "Y" : "N";
    }

    /**
     * {@code labUnitIds} present (even empty) is the caller's full statement of the
     * lab units; the legacy single {@code testSectionId} is honoured when the
     * many-to-many list is absent; and a payload that names neither keeps what is
     * persisted.
     */
    private Set<TestSection> resolveLabUnits(EditProgramForm form, Program existing) {
        Set<TestSection> resolved = new HashSet<>();
        if (form.getLabUnitIds() != null) {
            for (String labUnitId : form.getLabUnitIds()) {
                TestSection section = findTestSection(labUnitId);
                if (section != null) {
                    resolved.add(section);
                }
            }
            return resolved;
        }
        if (StringUtils.isNotBlank(form.getTestSectionId())) {
            TestSection section = findTestSection(form.getTestSectionId());
            if (section != null) {
                resolved.add(section);
            }
            return resolved;
        }
        if (existing != null) {
            if (existing.getLabUnits() != null) {
                resolved.addAll(existing.getLabUnits());
            }
            if (resolved.isEmpty() && existing.getTestSection() != null) {
                resolved.add(existing.getTestSection());
            }
        }
        return resolved;
    }

    private TestSection findTestSection(String id) {
        if (StringUtils.isBlank(id) || !StringUtil.isInteger(id.trim())) {
            return null;
        }
        return testSectionService.getMatch("id", id.trim()).orElse(null);
    }

    private void project(EditProgramForm form, Program program) {
        form.setProgram(program);
        form.setTestSectionId(program.getTestSection() == null ? null : program.getTestSection().getId());
        form.setDomain(Domain.normalize(program.getDomain()));
        form.setActive(ProgramPickerRules.isActive(program));
        form.setLabUnitIds(ProgramPickerRules.labUnitIds(program));
    }

    private Map<String, Object> toListRow(Program program) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", program.getId());
        row.put("name", program.getProgramName());
        row.put("code", program.getCode());
        row.put("domain", Domain.normalize(program.getDomain()));
        row.put("active", ProgramPickerRules.isActive(program));
        row.put("labUnitIds", ProgramPickerRules.labUnitIds(program));
        return row;
    }
}
