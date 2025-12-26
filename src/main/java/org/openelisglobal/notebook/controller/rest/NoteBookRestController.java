package org.openelisglobal.notebook.controller.rest;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.form.AuditTrailViewForm;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.historyservices.NoteBookHistoryService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.bean.NoteBookDashboardMetrics;
import org.openelisglobal.notebook.bean.NoteBookDisplayBean;
import org.openelisglobal.notebook.bean.SampleDisplayBean;
import org.openelisglobal.notebook.form.NoteBookForm;
import org.openelisglobal.notebook.service.NoteBookSampleService;
import org.openelisglobal.notebook.service.NoteBookService;
import org.openelisglobal.notebook.service.NotebookSecurityService;
import org.openelisglobal.notebook.service.WorkflowPageTemplateService;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.openelisglobal.notebook.valueholder.WorkflowPageTemplate;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.openelisglobal.notebook.service.NoteBookPageService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(value = "/rest/notebook")
public class NoteBookRestController extends BaseRestController {

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private NoteBookSampleService noteBookSampleService;

    @Autowired
    private WorkflowPageTemplateService workflowPageTemplateService;

    @Autowired
    private FhirConfig fhirConfig;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private NotebookSecurityService notebookSecurityService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private RoleService roleService;

    @GetMapping(value = "/dashboard/entries", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<NoteBookDisplayBean>> getFilteredNoteBooks(
            @RequestParam(required = false) List<NoteBookStatus> statuses,
            @RequestParam(required = false) List<String> types, @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String fromDate, @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Integer noteBookId, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);

        // If specific notebook template is requested, check access to it first
        if (noteBookId != null) {
            if (!notebookSecurityService.canViewTemplate(noteBookId, sysUserId, loginLabUnit)) {
                return ResponseEntity.ok(new ArrayList<>());
            }
        }

        List<NoteBookDisplayBean> results = noteBookService.filterNoteBookEntries(statuses, types, tags,
                getFormatedDate(fromDate), getFormatedDate(toDate), noteBookId).stream().filter(entry -> {
                    // Security Check: User must be able to view the parent template of the entry
                    NoteBook parent = noteBookService.getParentTemplate(entry.getId());
                    if (parent != null) {
                        return notebookSecurityService.canViewTemplate(parent.getId(), sysUserId, loginLabUnit);
                    }
                    // If no parent (standalone?), check the entry itself as if it were a
                    // template/independent
                    // But entries usually don't have depts/orgs set, so this might be open.
                    // Assuming if no parent, we check the entry using the same rules (it might have
                    // its own restrictions)
                    return notebookSecurityService.canViewTemplate(entry.getId(), sysUserId, loginLabUnit);
                }).map(e -> noteBookService.convertToDisplayBean(e.getId())).collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @GetMapping(value = "/dashboard/notebooks", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<NoteBookDisplayBean>> getAllNoteBooks(HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);

        // Use ID-based canViewTemplate to avoid lazy initialization issues
        List<NoteBookDisplayBean> results = noteBookService.getAllTemplateNoteBooks().stream()
                .filter(nb -> notebookSecurityService.canViewTemplate(nb.getId(), sysUserId, loginLabUnit))
                .map(e -> noteBookService.convertToDisplayBean(e.getId())).collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @GetMapping(value = "/dashboard/entries/{noteBookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<NoteBookDisplayBean>> getNoteBookEntries(@PathVariable("noteBookId") Integer noteBookId,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);

        // First check if user can view the template
        // Using ID-based check which handles safe fetching of departments/organizations
        if (!notebookSecurityService.canViewTemplate(noteBookId, sysUserId, loginLabUnit)) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<NoteBookDisplayBean> results = noteBookService.getNoteBookEntries(noteBookId).stream()
                .map(e -> noteBookService.convertToDisplayBean(e.getId())).collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    private Date getFormatedDate(String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        }

        try {
            String locale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_DATE_LOCALE);

            String pattern;
            if ("fr-FR".equalsIgnoreCase(locale)) {
                pattern = "dd/MM/yyyy";
            } else {
                pattern = "MM/dd/yyyy";
            }

            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // normalize
            return sdf.parse(date);

        } catch (ParseException e) {
            // consider logging or rethrowing
            return null;
        }
    }

    @GetMapping(value = "/dashboard/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<NoteBookDashboardMetrics> getNoteBookDashboardMetrics() {
        NoteBookDashboardMetrics metrics = new NoteBookDashboardMetrics();
        metrics.setTotal(noteBookService.getTotalCount());
        metrics.setDrafts(noteBookService.getCountWithStatus(Arrays.asList(NoteBookStatus.DRAFT)));
        metrics.setPending(noteBookService.getCountWithStatus(Arrays.asList(NoteBookStatus.SUBMITTED)));

        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        Instant weekAgoInstant = Instant.now().minus(7, ChronoUnit.DAYS);
        Timestamp weekAgoTimestamp = Timestamp.from(weekAgoInstant);
        metrics.setFinalized(noteBookService.getCountWithStatusBetweenDates(
                Arrays.asList(NoteBookStatus.FINALIZED, NoteBookStatus.ARCHIVED, NoteBookStatus.LOCKED),
                weekAgoTimestamp, currentTimestamp));
        return ResponseEntity.ok(metrics);
    }

    @GetMapping(value = "/view/{noteBookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getNoteBookEntry(@PathVariable("noteBookId") Integer noteBookId,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);

        NoteBook notebook = noteBookService.get(noteBookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isAllowed = false;

        // If it's a template, check template permissions directly
        if (Boolean.TRUE.equals(notebook.getIsTemplate())) {
            isAllowed = notebookSecurityService.canViewTemplate(noteBookId, sysUserId, loginLabUnit);
        } else {
            // If it's an entry, permission is inherited from the parent template
            NoteBook parent = noteBookService.getParentTemplate(noteBookId);
            if (parent != null) {
                isAllowed = notebookSecurityService.canViewTemplate(parent.getId(), sysUserId, loginLabUnit);
            } else {
                // Standalone/Orphaned entry - strict check, mostly for admins or fallback
                // If user is admin, allow. Otherwise deny as we can't verify org access.
                // Could potentially check if user is the creator/technician here.
                if (notebookSecurityService.hasGlobalAdminRole(sysUserId)) {
                    isAllowed = true;
                } else if (notebook.getTechnician() != null
                        && String.valueOf(notebook.getTechnician().getId()).equals(sysUserId)) {
                    // Allow technician assigned to the notebook to view it
                    isAllowed = true;
                } else if (notebook.getCreator() != null
                        && String.valueOf(notebook.getCreator().getId()).equals(sysUserId)) {
                    // Allow creator to view it
                    isAllowed = true;
                }
            }
        }

        if (!isAllowed) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied to this notebook"));
        }

        return ResponseEntity.ok(noteBookService.convertToFullDisplayBean(noteBookId));
    }

    /**
     * Get all available workflow page templates for adding to notebook templates.
     */
    @GetMapping(value = "/workflow-page-templates", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<WorkflowPageTemplate>> getWorkflowPageTemplates(
            @RequestParam(required = false) String category) {
        List<WorkflowPageTemplate> templates;
        if (category != null && !category.isEmpty()) {
            templates = workflowPageTemplateService.getByCategory(category);
        } else {
            templates = workflowPageTemplateService.getAllActive();
        }
        return ResponseEntity.ok(templates);
    }

    @PostMapping(value = "/update/{noteBookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> updateNoteBookEntry(@PathVariable("noteBookId") Integer noteBookId,
            @RequestBody NoteBookForm form, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);

        // Only admin can edit notebook templates
        if (!notebookSecurityService.canEditTemplate(sysUserId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required to edit templates"));
        }

        form.setSystemUserId(Integer.valueOf(sysUserId));
        noteBookService.updateWithFormValues(noteBookId, form);

        return ResponseEntity.ok(Map.of("id", noteBookId));
    }

    @PostMapping(value = "/updatestatus/{noteBookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> updateNoteBookStatus(@PathVariable("noteBookId") Integer noteBookId,
            @RequestParam(required = false) NoteBookStatus status, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);

        // Only admin can update notebook template status
        if (!notebookSecurityService.canEditTemplate(sysUserId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required to update template status"));
        }

        noteBookService.updateWithStatus(noteBookId, status, sysUserId);
        return ResponseEntity.ok(Map.of("id", noteBookId, "status", status.name()));
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> createNoteBookEntry(@RequestBody NoteBookForm form, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);

        org.openelisglobal.common.log.LogEvent.logInfo(this.getClass().getSimpleName(), "createNoteBookEntry",
                "Creating notebook entry: isTemplate=" + form.getIsTemplate() + ", templateId=" + form.getTemplateId()
                        + ", sysUserId=" + sysUserId + ", loginLabUnit=" + loginLabUnit);

        // Check permissions:
        // 1. If creating a template, must be Admin
        // 2. If creating an entry from a template, must have canCreateEntry permission
        // on that template
        if (Boolean.TRUE.equals(form.getIsTemplate())) {
            if (!notebookSecurityService.canEditTemplate(sysUserId)) {
                org.openelisglobal.common.log.LogEvent.logInfo(this.getClass().getSimpleName(), "createNoteBookEntry",
                        "403: Admin access required to create templates");
                return ResponseEntity.status(403).body(Map.of("error", "Admin access required to create templates"));
            }
        } else if (form.getTemplateId() != null) {
            boolean canCreate = notebookSecurityService.canCreateEntry(form.getTemplateId(), sysUserId, loginLabUnit);
            org.openelisglobal.common.log.LogEvent.logInfo(this.getClass().getSimpleName(), "createNoteBookEntry",
                    "canCreateEntry result=" + canCreate + " for templateId=" + form.getTemplateId());
            if (!canCreate) {
                // Get more details for the error message
                boolean canView = notebookSecurityService.canViewTemplate(form.getTemplateId(), sysUserId,
                        loginLabUnit);
                String errorMsg;
                if (!canView) {
                    errorMsg = "Access denied. This template is not assigned to your department: " + loginLabUnit;
                } else {
                    // User can view but lacks required role
                    java.util.Set<String> allowedRoles = noteBookService.getNoteBookAllowedRoles(form.getTemplateId());
                    if (allowedRoles != null && !allowedRoles.isEmpty()) {
                        errorMsg = "Access denied. You need one of these roles to create entries: "
                                + String.join(", ", allowedRoles);
                    } else {
                        errorMsg = "Access denied to create entry from this template";
                    }
                }
                org.openelisglobal.common.log.LogEvent.logInfo(this.getClass().getSimpleName(), "createNoteBookEntry",
                        "403: " + errorMsg);
                return ResponseEntity.status(403).body(Map.of("error", errorMsg));
            }
        } else {
            // Creating a standalone notebook (not a template, not from a template)
            // Default to requiring admin or specific permission? existing logic was admin
            // only.
            // Keeping admin only for un-templated notebooks to be safe.
            if (!notebookSecurityService.canEditTemplate(sysUserId)) {
                org.openelisglobal.common.log.LogEvent.logInfo(this.getClass().getSimpleName(), "createNoteBookEntry",
                        "403: Access denied to create standalone notebook");
                return ResponseEntity.status(403).body(Map.of("error", "Access denied to create standalone notebook"));
            }
        }

        form.setSystemUserId(Integer.valueOf(sysUserId));
        NoteBook noteBook = noteBookService.createWithFormValues(form);
        org.openelisglobal.common.log.LogEvent.logInfo(this.getClass().getSimpleName(), "createNoteBookEntry",
                "Successfully created notebook entry with id=" + noteBook.getId());
        return ResponseEntity.ok(Map.of("id", noteBook.getId()));
    }

    @GetMapping(value = "/samples", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<SampleDisplayBean>> searchSamples(@RequestParam(required = true) String accession) {
        List<SampleDisplayBean> results = noteBookService.searchSampleItems(accession);
        return ResponseEntity.ok(results);
    }

    @GetMapping(value = "/notebooksamples", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<SampleDisplayBean>> getNoteBookSamples(
            @RequestParam(required = true) Integer noteBookId) {
        List<SampleDisplayBean> results = noteBookSampleService.getNotebookSamplesByNoteBookId(noteBookId);
        return ResponseEntity.ok(results);
    }

    @GetMapping(value = "/auditTrail", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AuditTrailViewForm> getNoteBookAuditTrail(@RequestParam Integer notebookId) {
        AuditTrailViewForm response = new AuditTrailViewForm();

        if (notebookId == null) {
            return ResponseEntity.ok(response);
        }

        NoteBook noteBook = noteBookService.get(notebookId);
        if (noteBook == null) {
            return ResponseEntity.ok(response);
        }

        NoteBookHistoryService historyService = new NoteBookHistoryService(noteBook);
        List<AuditTrailItem> items = historyService.getAuditTrailItems();

        if (items.size() == 0) {
            return ResponseEntity.ok(response);
        }

        // Populate the response object with status-only audit trail
        response.setLog(items);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<NoteBookDisplayBean>> getAvailableNotebooks(HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);

        List<NoteBookDisplayBean> results = noteBookService.getAllActiveNotebooks().stream()
                .filter(nb -> notebookSecurityService.canViewTemplate(nb, sysUserId, loginLabUnit))
                .map(notebook -> noteBookService.convertToDisplayBean(notebook.getId())).collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @GetMapping(value = "/questionnaires", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<IdValuePair>> getQuestionnaires() {
        List<IdValuePair> questionnaires = new ArrayList<>();

        if (StringUtils.isBlank(fhirConfig.getLocalFhirStorePath())) {
            return ResponseEntity.ok(questionnaires);
        }

        try {
            IGenericClient fhirClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());
            String identifierSystem = fhirConfig.getOeFhirSystem() + "/notebook_questionare";

            Bundle searchBundle = fhirClient.search().forResource(Questionnaire.class)
                    .where(Questionnaire.IDENTIFIER.hasSystemWithAnyCode(identifierSystem))
                    .where(Questionnaire.STATUS.exactly().code("active")).returnBundle(Bundle.class).execute();

            for (BundleEntryComponent entry : searchBundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Questionnaire) {
                    Questionnaire questionnaire = (Questionnaire) entry.getResource();
                    String uuid = questionnaire.getIdElement().getIdPart();
                    String value = questionnaire.getTitle();
                    if (StringUtils.isBlank(value)) {
                        value = questionnaire.getName();
                    }
                    if (StringUtils.isBlank(value)) {
                        value = uuid;
                    }
                    questionnaires.add(new IdValuePair(uuid, value));
                }
            }

            // Handle pagination
            while (searchBundle.getLink(org.hl7.fhir.instance.model.api.IBaseBundle.LINK_NEXT) != null) {
                searchBundle = fhirClient.loadPage().next(searchBundle).execute();
                for (BundleEntryComponent entry : searchBundle.getEntry()) {
                    if (entry.hasResource() && entry.getResource() instanceof Questionnaire) {
                        Questionnaire questionnaire = (Questionnaire) entry.getResource();
                        String uuid = questionnaire.getIdElement().getIdPart();
                        String value = questionnaire.getTitle();
                        if (StringUtils.isBlank(value)) {
                            value = questionnaire.getName();
                        }
                        if (StringUtils.isBlank(value)) {
                            value = uuid;
                        }
                        questionnaires.add(new IdValuePair(uuid, value));
                    }
                }
            }
        } catch (Exception e) {
            // Log error and return empty list
            org.openelisglobal.common.log.LogEvent.logError(this.getClass().getSimpleName(), "getQuestionnaires",
                    "Error fetching questionnaires from FHIR server: " + e.getMessage());
        }

        return ResponseEntity.ok(questionnaires);
    }

    /**
     * Get all organizations for dropdown selection in notebook template
     * configuration. Returns active organizations with id, name, and short name for
     * display.
     *
     * @return list of organizations as IdValuePair (id=organizationId,
     *         value=organizationName)
     */
    @GetMapping(value = "/organizations", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, String>>> getAllOrganizations() {
        List<Organization> organizations = organizationService.getActiveOrganizations();

        List<Map<String, String>> result = organizations.stream()
                .map(org -> Map.of("id", org.getId(), "name",
                        org.getOrganizationName() != null ? org.getOrganizationName() : "", "shortName",
                        org.getShortName() != null ? org.getShortName() : ""))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get all departments (test sections/lab units) for dropdown selection in
     * notebook template configuration. These are the departments that can be
     * assigned to users for access control.
     *
     * @return list of departments with id and name
     */
    @GetMapping(value = "/departments", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, String>>> getAllDepartments() {
        List<TestSection> testSections = testSectionService.getAllActiveTestSections();

        List<Map<String, String>> result = testSections.stream()
                .map(ts -> Map.of("id", ts.getId(), "name",
                        ts.getLocalizedName() != null ? ts.getLocalizedName() : ts.getTestSectionName(), "shortName",
                        ts.getTestSectionName() != null ? ts.getTestSectionName() : ""))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get organizations assigned to a specific notebook template.
     *
     * @param noteBookId the template notebook ID
     * @return list of organizations assigned to this template
     */
    @GetMapping(value = "/{noteBookId}/organizations", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getTemplateOrganizations(@PathVariable("noteBookId") Integer noteBookId,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);

        // Check if user can view this template
        if (!notebookSecurityService.canViewTemplate(noteBookId, sysUserId, loginLabUnit)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied to this notebook template"));
        }

        List<Map<String, String>> result = noteBookService.getNoteBookOrganizations(noteBookId).stream()
                .map(org -> Map.of("id", org.getId(), "name",
                        org.getOrganizationName() != null ? org.getOrganizationName() : "", "shortName",
                        org.getShortName() != null ? org.getShortName() : ""))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Update organizations assigned to a notebook template. Only admins can update
     * template organizations.
     *
     * @param noteBookId      the template notebook ID
     * @param organizationIds list of organization IDs to assign
     * @param request         HTTP request for user session
     * @return success response
     */
    @PostMapping(value = "/{noteBookId}/organizations", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> updateTemplateOrganizations(@PathVariable("noteBookId") Integer noteBookId,
            @RequestBody Map<String, List<String>> body, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);

        // Only admin can edit notebook templates
        if (!notebookSecurityService.canEditTemplate(sysUserId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required to edit templates"));
        }

        NoteBook notebook = noteBookService.get(noteBookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        List<String> organizationIds = body.get("organizationIds");
        noteBookService.updateTemplateOrganizations(noteBookId, organizationIds, sysUserId);

        return ResponseEntity.ok(Map.of("id", noteBookId, "success", true));
    }

    /**
     * Get allowed roles for a notebook template.
     *
     * @param noteBookId the template notebook ID
     * @return list of allowed role names
     */
    @GetMapping(value = "/{noteBookId}/allowed-roles", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getTemplateAllowedRoles(@PathVariable("noteBookId") Integer noteBookId,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);

        // Check if user can view this template
        if (!notebookSecurityService.canViewTemplate(noteBookId, sysUserId, loginLabUnit)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied to this notebook template"));
        }

        return ResponseEntity.ok(noteBookService.getNoteBookAllowedRoles(noteBookId));
    }

    /**
     * Update allowed roles for a notebook template. Only admins can update template
     * allowed roles.
     *
     * @param noteBookId   the template notebook ID
     * @param allowedRoles list of role names to allow
     * @param request      HTTP request for user session
     * @return success response
     */
    @PostMapping(value = "/{noteBookId}/allowed-roles", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> updateTemplateAllowedRoles(@PathVariable("noteBookId") Integer noteBookId,
            @RequestBody Map<String, List<String>> body, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);

        // Only admin can edit notebook templates
        if (!notebookSecurityService.canEditTemplate(sysUserId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required to edit templates"));
        }

        NoteBook notebook = noteBookService.get(noteBookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        List<String> allowedRoles = body.get("allowedRoles");
        noteBookService.updateTemplateAllowedRoles(noteBookId, allowedRoles, sysUserId);

        return ResponseEntity.ok(Map.of("id", noteBookId, "success", true));
    }

    /**
     * Get departments (test sections) assigned to a specific notebook template.
     *
     * @param noteBookId the template notebook ID
     * @return list of departments assigned to this template
     */
    @GetMapping(value = "/{noteBookId}/departments", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getTemplateDepartments(@PathVariable("noteBookId") Integer noteBookId,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);

        // Check if user can view this template
        if (!notebookSecurityService.canViewTemplate(noteBookId, sysUserId, loginLabUnit)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied to this notebook template"));
        }

        List<Map<String, String>> result = noteBookService.getNoteBookDepartments(noteBookId).stream()
                .map(ts -> Map.of("id", ts.getId(), "name",
                        ts.getLocalizedName() != null ? ts.getLocalizedName() : ts.getTestSectionName(), "shortName",
                        ts.getTestSectionName() != null ? ts.getTestSectionName() : ""))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Update departments (test sections) assigned to a notebook template. Only
     * admins can update template departments.
     *
     * @param noteBookId    the template notebook ID
     * @param departmentIds list of test section IDs to assign
     * @param request       HTTP request for user session
     * @return success response
     */
    @PostMapping(value = "/{noteBookId}/departments", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> updateTemplateDepartments(@PathVariable("noteBookId") Integer noteBookId,
            @RequestBody Map<String, List<String>> body, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);

        // Only admin can edit notebook templates
        if (!notebookSecurityService.canEditTemplate(sysUserId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required to edit templates"));
        }

        NoteBook notebook = noteBookService.get(noteBookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        List<String> departmentIds = body.get("departmentIds");
        noteBookService.updateTemplateDepartments(noteBookId, departmentIds, sysUserId);

        return ResponseEntity.ok(Map.of("id", noteBookId, "success", true));
    }

    /**
     * Get all available roles for assignment to notebook templates and pages.
     * Returns all active roles from the system that can be used for access control.
     *
     * @return list of roles with id (name), name, and displayKey
     */
    @GetMapping(value = "/available-roles", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, String>>> getAvailableRoles() {
        List<Role> roles = roleService.getAllActiveRoles();

        List<Map<String, String>> result = roles.stream().filter(role -> !Boolean.TRUE.equals(role.getGroupingRole())) // Exclude
                                                                                                                       // grouping
                                                                                                                       // roles
                .map(role -> Map.of("id", role.getName() != null ? role.getName().trim() : "", "name",
                        role.getName() != null ? role.getName().trim() : "", "description",
                        role.getDescription() != null ? role.getDescription() : "", "displayKey",
                        role.getDisplayKey() != null ? role.getDisplayKey() : ""))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get the login lab unit name for the current user from session. Returns the
     * TestSection localized name which maps to lab unit names used in access
     * control.
     *
     * @param request the HTTP request
     * @return the login lab unit name, or null if not set
     */
    private String getLoginLabUnit(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        if (usd == null) {
            return null;
        }
        int loginLabUnitId = usd.getLoginLabUnit();
        if (loginLabUnitId == 0) {
            return null;
        }
        TestSection testSection = testSectionService.getTestSectionById(String.valueOf(loginLabUnitId));
        if (testSection != null) {
            return testSection.getLocalizedName();
        }
        return null;
    }

    /**
     * Get summary statistics for a notebook (aggregated data across all pages).
     * Used by the Reporting & Data Export page to show notebook-level summaries.
     *
     * @param notebookId the notebook ID
     * @param request    HTTP request for user session
     * @return summary statistics including total samples, QC pass rate, etc.
     */
    @GetMapping(value = "/{notebookId}/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getNotebookSummary(@PathVariable("notebookId") Integer notebookId,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);

        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            return ResponseEntity.notFound().build();
        }

        // Check permissions
        boolean isAllowed = false;
        if (Boolean.TRUE.equals(notebook.getIsTemplate())) {
            isAllowed = notebookSecurityService.canViewTemplate(notebookId, sysUserId, loginLabUnit);
        } else {
            NoteBook parent = noteBookService.getParentTemplate(notebookId);
            if (parent != null) {
                isAllowed = notebookSecurityService.canViewTemplate(parent.getId(), sysUserId, loginLabUnit);
            }
        }

        if (!isAllowed) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied to this notebook"));
        }

        // Calculate summary statistics across all pages
        List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebookId);

        int totalSamples = 0;
        int completedSamples = 0;
        int totalCultures = 0;
        int positiveIsolates = 0;
        int astCompleted = 0;
        int mdrOrganisms = 0;

        for (NoteBookPage page : pages) {
            List<NotebookPageSample> samples = notebookPageSampleService.getByPageId(page.getId());
            totalSamples += samples.size();

            for (NotebookPageSample sample : samples) {
                if (sample.getStatus() == NotebookPageSample.Status.COMPLETED) {
                    completedSamples++;
                }

                // Check for bacteriology-specific data in the data JSON map
                Map<String, Object> data = sample.getData();
                if (data != null) {
                    // Count cultures (samples with culture data)
                    if (data.containsKey("cultureResult") || data.containsKey("inoculated")) {
                        totalCultures++;
                    }
                    // Count positive isolates
                    if ("POSITIVE".equals(data.get("cultureResult"))
                            || "POSITIVE".equals(data.get("isolateStatus"))) {
                        positiveIsolates++;
                    }
                    // Count AST completed
                    if (data.containsKey("astCompleted") && Boolean.TRUE.equals(data.get("astCompleted"))) {
                        astCompleted++;
                    } else if (data.containsKey("susceptibilityResults")) {
                        astCompleted++;
                    }
                    // Count MDR organisms
                    if (Boolean.TRUE.equals(data.get("mdrOrganism"))
                            || "MDR".equals(data.get("resistancePattern"))) {
                        mdrOrganisms++;
                    }
                }
            }
        }

        // Calculate QC pass rate
        String qcPassRate = totalSamples > 0
                ? String.format("%.0f%%", (completedSamples * 100.0) / totalSamples)
                : "N/A";

        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("totalSamples", totalSamples);
        summary.put("completedSamples", completedSamples);
        summary.put("totalCultures", totalCultures > 0 ? totalCultures : totalSamples);
        summary.put("positiveIsolates", positiveIsolates);
        summary.put("astCompleted", astCompleted);
        summary.put("mdrOrganisms", mdrOrganisms);
        summary.put("qcPassRate", qcPassRate);

        return ResponseEntity.ok(summary);
    }

    /**
     * Generate and download a CSV report for all data points in a notebook.
     * Used by the Reporting & Data Export page.
     *
     * @param notebookId   the notebook ID
     * @param request      HTTP request for user session
     * @param response     HTTP response for file download
     */
    @PostMapping(value = "/{notebookId}/generate-report")
    public void generateNotebookReport(@PathVariable("notebookId") Integer notebookId,
            @RequestBody(required = false) Map<String, Object> requestBody,
            HttpServletRequest request, HttpServletResponse response) {

        String sysUserId = getSysUserId(request);
        String loginLabUnit = getLoginLabUnit(request);

        try {
            NoteBook notebook = noteBookService.get(notebookId);
            if (notebook == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"Notebook not found\"}");
                return;
            }

            // Check permissions
            boolean isAllowed = false;
            if (Boolean.TRUE.equals(notebook.getIsTemplate())) {
                isAllowed = notebookSecurityService.canViewTemplate(notebookId, sysUserId, loginLabUnit);
            } else {
                NoteBook parent = noteBookService.getParentTemplate(notebookId);
                if (parent != null) {
                    isAllowed = notebookSecurityService.canViewTemplate(parent.getId(), sysUserId, loginLabUnit);
                }
            }

            if (!isAllowed) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"Access denied to this notebook\"}");
                return;
            }

            // Generate CSV report
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

            // Write CSV header
            writer.println(
                    "Sample ID,External ID,Accession Number,Sample Type,Collection Date,Page,Status,Culture Result,Organism,AST Result,MDR,QC Status,Notes");

            // Get all pages and their samples
            List<NoteBookPage> pages = noteBookPageService.getByNotebookId(notebookId);

            for (NoteBookPage page : pages) {
                List<NotebookPageSample> samples = notebookPageSampleService.getByPageId(page.getId());

                for (NotebookPageSample sample : samples) {
                    Map<String, Object> data = sample.getData();

                    // Build CSV row
                    StringBuilder row = new StringBuilder();
                    row.append(escapeCSV(sample.getSampleItemId())).append(",");
                    // Get externalId, accessionNumber, sampleType, collectionDate from data map
                    row.append(escapeCSV(data != null ? getStringValue(data, "externalId") : "")).append(",");
                    row.append(escapeCSV(data != null ? getStringValue(data, "accessionNumber") : "")).append(",");
                    row.append(escapeCSV(data != null ? getStringValue(data, "sampleType") : "")).append(",");
                    row.append(escapeCSV(data != null ? getStringValue(data, "collectionDate") : "")).append(",");
                    row.append(escapeCSV(page.getTitle())).append(",");
                    row.append(escapeCSV(sample.getStatus() != null ? sample.getStatus().name() : "")).append(",");

                    // Data fields
                    if (data != null) {
                        row.append(escapeCSV(getStringValue(data, "cultureResult"))).append(",");
                        row.append(escapeCSV(getStringValue(data, "organismIdentified"))).append(",");
                        row.append(escapeCSV(getStringValue(data, "susceptibilityResults"))).append(",");
                        row.append(escapeCSV(data.get("mdrOrganism") != null
                                ? String.valueOf(data.get("mdrOrganism"))
                                : "")).append(",");
                        row.append(escapeCSV(getStringValue(data, "qcResult"))).append(",");
                        row.append(escapeCSV(getStringValue(data, "notes")));
                    } else {
                        row.append(",,,,,");
                    }

                    writer.println(row.toString());
                }
            }

            writer.flush();
            writer.close();

            byte[] csvBytes = baos.toByteArray();

            // Set response headers for file download
            String filename = "Bacteriology_Notebook_Report_"
                    + java.time.LocalDate.now().toString() + ".csv";
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setContentLength(csvBytes.length);
            response.getOutputStream().write(csvBytes);
            response.getOutputStream().flush();

        } catch (Exception e) {
            org.openelisglobal.common.log.LogEvent.logError(this.getClass().getSimpleName(),
                    "generateNotebookReport", "Error generating report: " + e.getMessage());
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"Failed to generate report: " + e.getMessage() + "\"}");
            } catch (Exception ignored) {
                // Response may already be committed
            }
        }
    }

    /**
     * Escape a value for CSV output.
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Get string value from a map, handling nulls.
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

}
