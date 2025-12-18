package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.Hibernate;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.notebook.bean.NoteBookDisplayBean;
import org.openelisglobal.notebook.bean.NoteBookFullDisplayBean;
import org.openelisglobal.notebook.bean.SampleDisplayBean;
import org.openelisglobal.notebook.bean.SampleDisplayBean.ResultDisplayBean;
import org.openelisglobal.notebook.dao.NoteBookDAO;
import org.openelisglobal.notebook.dao.NoteBookPageDAO;
import org.openelisglobal.notebook.form.NoteBookForm;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.openelisglobal.notebook.valueholder.NoteBookComment;
import org.openelisglobal.notebook.valueholder.NoteBookFile;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteBookServiceImpl extends AuditableBaseObjectServiceImpl<NoteBook, Integer> implements NoteBookService {

    @Autowired
    private NoteBookDAO baseObjectDAO;

    @Autowired
    private NoteBookPageDAO noteBookPageDAO;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    ResultService resultService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private org.openelisglobal.inventory.service.InventoryItemService inventoryItemService;

    public NoteBookServiceImpl() {
        super(NoteBook.class);
        this.auditTrailLog = true;
    }

    @Override
    protected BaseDAO<NoteBook, Integer> getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public List<NoteBook> filterNoteBookEntries(List<NoteBookStatus> statuses, List<String> types, List<String> tags,
            Date fromDate, Date toDate, Integer noteBookId) {
        List<Integer> entryIds = new ArrayList<>();
        if (noteBookId != null) {
            entryIds = getNoteBookEntries(noteBookId).stream().map(e -> e.getId()).collect(Collectors.toList());
        }
        if (noteBookId != null && entryIds.isEmpty()) {
            return new ArrayList<>();
        }

        return baseObjectDAO.filterNoteBookEntries(statuses, types, tags, fromDate, toDate, entryIds);
    }

    @Override
    @Transactional
    public List<NoteBook> filterNoteBooks(List<NoteBookStatus> statuses, List<String> types, List<String> tags,
            Date fromDate, Date toDate) {
        return baseObjectDAO.filterNoteBooks(statuses, types, tags, fromDate, toDate);
    }

    @Override
    @Transactional
    public void updateWithStatus(Integer notebookId, NoteBookStatus status, String sysUserId) {
        Optional<NoteBook> optionalNoteBook = baseObjectDAO.get(notebookId);
        if (optionalNoteBook.isPresent()) {
            NoteBook noteBook = optionalNoteBook.get();
            noteBook.setStatus(status);
            if (sysUserId != null) {
                noteBook.setSysUserId(sysUserId);
            }
            update(noteBook);
        }
    }

    @Override
    @Transactional
    public NoteBook createWithFormValues(NoteBookForm form) {
        NoteBook noteBook = new NoteBook();
        noteBook = createNoteBookFromForm(noteBook, form);
        noteBook = save(noteBook);

        // If creating a new TEMPLATE, automatically assign all active departments
        // This ensures the template is accessible to all users by default
        if (noteBook.getIsTemplate()) {
            assignAllActiveDepartments(noteBook);
            assignAllActiveOrganizations(noteBook);
            update(noteBook);
        } else {
            // Creating an entry from a template
            NoteBook templateNoteBook = get(form.getTemplateId());
            if (templateNoteBook != null && templateNoteBook.getIsTemplate()) {
                templateNoteBook.getEntries().add(noteBook);
                // Set sysUserId for audit trail tracking when updating template
                if (form.getSystemUserId() != null) {
                    templateNoteBook.setSysUserId(form.getSystemUserId().toString());
                }
                initializeLazyCollections(templateNoteBook);
                update(templateNoteBook);
            }
        }

        return noteBook;
    }

    /**
     * Assign all active departments (test sections) to a notebook template. This
     * makes the template accessible to all departments by default.
     */
    private void assignAllActiveDepartments(NoteBook noteBook) {
        List<TestSection> allDepartments = testSectionService.getAllActiveTestSections();
        if (noteBook.getDepartments() == null) {
            noteBook.setDepartments(new HashSet<>());
        }
        noteBook.getDepartments().addAll(allDepartments);
    }

    /**
     * Assign all active organizations to a notebook template. This makes the
     * template accessible to all organizations by default.
     */
    private void assignAllActiveOrganizations(NoteBook noteBook) {
        List<Organization> allOrganizations = organizationService.getActiveOrganizations();
        if (noteBook.getOrganizations() == null) {
            noteBook.setOrganizations(new HashSet<>());
        }
        noteBook.getOrganizations().addAll(allOrganizations);
    }

    @Override
    @Transactional
    public void updateWithFormValues(Integer noteBookId, NoteBookForm form) {
        Optional<NoteBook> optionalNoteBook = baseObjectDAO.get(noteBookId);
        if (optionalNoteBook.isPresent()) {
            NoteBook noteBook = optionalNoteBook.get();
            noteBook = createNoteBookFromForm(noteBook, form);
            initializeLazyCollections(noteBook);
            update(noteBook);
        }
    }

    @Override
    @Transactional
    protected NoteBook update(NoteBook noteBook, String auditTrailType) {
        // Note: Do NOT evict the modified noteBook - it would cause
        // "collection no longer referenced" errors with orphanRemoval collections.
        // The parent class handles getting the old object for audit trail comparison.
        // We just need to ensure lazy collections are initialized on the entity being
        // saved.
        initializeLazyCollections(noteBook);
        return super.update(noteBook, auditTrailType);
    }

    /**
     * Initialize all lazy collections on a NoteBook entity to prevent
     * LazyInitializationException when the entity is accessed outside of a
     * transaction (e.g., in audit trail comparison).
     */
    private void initializeLazyCollections(NoteBook noteBook) {
        Hibernate.initialize(noteBook.getTags());
        Hibernate.initialize(noteBook.getSamples());
        Hibernate.initialize(noteBook.getAnalysers());
        Hibernate.initialize(noteBook.getInventoryInstrumentIds());
        Hibernate.initialize(noteBook.getPages());
        // Initialize panels and tests for each page (panels is LAZY to avoid
        // MultipleBagFetchException)
        if (noteBook.getPages() != null) {
            for (NoteBookPage page : noteBook.getPages()) {
                Hibernate.initialize(page.getPanels());
                Hibernate.initialize(page.getTests());
            }
        }
        Hibernate.initialize(noteBook.getFiles());
        Hibernate.initialize(noteBook.getComments());
        Hibernate.initialize(noteBook.getEntries());
        Hibernate.initialize(noteBook.getOrganizations());
        Hibernate.initialize(noteBook.getDepartments());
        Hibernate.initialize(noteBook.getAllowedRoles());
        if (noteBook.getTechnician() != null) {
            Hibernate.initialize(noteBook.getTechnician());
        }
    }

    @Override
    @Transactional
    public NoteBookDisplayBean convertToDisplayBean(Integer noteBookId) {
        NoteBookDisplayBean displayBean = new NoteBookDisplayBean();
        Optional<NoteBook> optionalNoteBook = baseObjectDAO.get(noteBookId);
        if (optionalNoteBook.isPresent()) {
            NoteBook noteBook = optionalNoteBook.get();
            Hibernate.initialize(noteBook.getTags());
            Hibernate.initialize(noteBook.getEntries());
            displayBean.setId(noteBook.getId());
            displayBean.setTitle(noteBook.getTitle());
            displayBean.setTags(noteBook.getTags());
            if (noteBook.getTechnician() != null) {
                displayBean.setTechnicianId(Integer.valueOf(noteBook.getTechnician().getId()));
            }
            // Handle type - it's now a Dictionary entity
            if (noteBook.getType() != null) {
                displayBean.setType(Integer.valueOf(noteBook.getType().getId()));
                displayBean.setTypeName(noteBook.getType().getDictEntry());
            }

            displayBean.setDateCreated(DateUtil.formatDateAsText(noteBook.getDateCreated()));
            displayBean.setStatus(noteBook.getStatus());
            displayBean.setIsTemplate(noteBook.getIsTemplate());
            displayBean.setEntriesCount(noteBook.getEntries().size());
            displayBean.setQuestionnaireFhirUuid(noteBook.getQuestionnaireFhirUuid());

            // For non-template entries, find and set entry number and parent notebook name
            // Also inherit allowedRoles from parent template
            if (noteBook.getIsTemplate() != null && !noteBook.getIsTemplate()) {
                NoteBook parentTemplate = baseObjectDAO.findParentTemplate(noteBook.getId());
                if (parentTemplate != null) {
                    displayBean.setNotebookName(parentTemplate.getTitle());
                    // Calculate entry number (1-based index in parent's entries list)
                    Hibernate.initialize(parentTemplate.getEntries());
                    List<NoteBook> entries = parentTemplate.getEntries();
                    if (entries != null) {
                        // Sort entries by dateCreated to get consistent numbering
                        List<NoteBook> sortedEntries = entries.stream().sorted((e1, e2) -> {
                            if (e1.getDateCreated() == null && e2.getDateCreated() == null) {
                                return 0;
                            }
                            if (e1.getDateCreated() == null) {
                                return 1;
                            }
                            if (e2.getDateCreated() == null) {
                                return -1;
                            }
                            return e1.getDateCreated().compareTo(e2.getDateCreated());
                        }).collect(Collectors.toList());
                        for (int i = 0; i < sortedEntries.size(); i++) {
                            if (sortedEntries.get(i).getId().equals(noteBook.getId())) {
                                displayBean.setEntryNumber(i + 1);
                                break;
                            }
                        }
                    }
                    // Entries inherit allowedRoles from their parent template
                    Hibernate.initialize(parentTemplate.getAllowedRoles());
                    displayBean.setAllowedRoles(new HashSet<>(parentTemplate.getAllowedRoles()));
                }
            } else {
                // For templates, use their own allowedRoles
                Hibernate.initialize(noteBook.getAllowedRoles());
                displayBean.setAllowedRoles(new HashSet<>(noteBook.getAllowedRoles()));
            }
        }
        return displayBean;
    }

    @Override
    @Transactional
    public NoteBookFullDisplayBean convertToFullDisplayBean(Integer noteBookId) {
        NoteBookFullDisplayBean fullDisplayBean = new NoteBookFullDisplayBean();
        NoteBook noteBook = get(noteBookId);
        if (noteBook != null) {
            Hibernate.initialize(noteBook.getAnalysers());
            Hibernate.initialize(noteBook.getInventoryInstrumentIds());
            Hibernate.initialize(noteBook.getSamples());
            Hibernate.initialize(noteBook.getPages());

            // Initialize panels, tests, and allowedRoles for each page (LAZY to avoid
            // MultipleBagFetchException)
            // Also explicitly copy allowedRoles to ensure proper JSON serialization
            if (noteBook.getPages() != null) {
                for (NoteBookPage page : noteBook.getPages()) {
                    Hibernate.initialize(page.getPanels());
                    Hibernate.initialize(page.getTests());
                    Hibernate.initialize(page.getAllowedRoles());
                    // Ensure allowedRoles is a regular HashSet (not Hibernate proxy) for JSON
                    // serialization
                    page.setAllowedRoles(new HashSet<>(page.getAllowedRoles()));
                }
            }
            Hibernate.initialize(noteBook.getFiles());
            Hibernate.initialize(noteBook.getComments());
            Hibernate.initialize(noteBook.getTags());
            Hibernate.initialize(noteBook.getEntries());
            fullDisplayBean.setId(noteBook.getId());
            fullDisplayBean.setTitle(noteBook.getTitle());
            if (noteBook.getType() != null) {
                fullDisplayBean.setType(Integer.valueOf(noteBook.getType().getId()));
                fullDisplayBean.setTypeName(noteBook.getType().getDictEntry());
            }
            fullDisplayBean.setTags(noteBook.getTags());
            fullDisplayBean.setDateCreated(DateUtil.formatDateAsText(noteBook.getDateCreated()));
            fullDisplayBean.setStatus(noteBook.getStatus());
            fullDisplayBean.setContent(noteBook.getContent());
            fullDisplayBean.setObjective(noteBook.getObjective());
            fullDisplayBean.setProtocol(noteBook.getProtocol());
            // Prefer inventory instruments over legacy analyzers
            List<IdValuePair> instrumentList = new ArrayList<>();
            List<Long> instrumentIds = noteBook.getInventoryInstrumentIds();

            // If this is an entry (not a template) and has no instruments, get from parent
            // template
            if ((instrumentIds == null || instrumentIds.isEmpty()) && noteBook.getIsTemplate() != null
                    && !noteBook.getIsTemplate()) {
                NoteBook parentTemplate = baseObjectDAO.findParentTemplate(noteBook.getId());
                if (parentTemplate != null) {
                    Hibernate.initialize(parentTemplate.getInventoryInstrumentIds());
                    instrumentIds = parentTemplate.getInventoryInstrumentIds();
                }
            }

            if (instrumentIds != null && !instrumentIds.isEmpty()) {
                for (Long instrumentId : instrumentIds) {
                    try {
                        var inventoryItem = inventoryItemService.get(instrumentId);
                        if (inventoryItem != null) {
                            instrumentList
                                    .add(new IdValuePair(inventoryItem.getId().toString(), inventoryItem.getName()));
                        }
                    } catch (Exception e) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "convertToFullDisplayBean",
                                "Could not find inventory item with id: " + instrumentId);
                    }
                }
            } else {
                // Fallback to legacy analyzers if no inventory instruments
                instrumentList = noteBook.getAnalysers().stream()
                        .map(analyzer -> new IdValuePair(analyzer.getId(), analyzer.getName())).toList();
            }
            fullDisplayBean.setAnalyzers(instrumentList);
            fullDisplayBean.setPages(noteBook.getPages());
            fullDisplayBean.setFiles(noteBook.getFiles());
            // Initialize author for each comment
            for (NoteBookComment comment : noteBook.getComments()) {
                Hibernate.initialize(comment.getAuthor());
            }
            fullDisplayBean.setComments(noteBook.getComments());
            if (noteBook.getTechnician() != null) {
                fullDisplayBean.setTechnicianName(noteBook.getTechnician().getDisplayName());
                fullDisplayBean.setTechnicianId(Integer.valueOf(noteBook.getTechnician().getId()));
            }
            if (noteBook.getCreator() != null) {
                fullDisplayBean.setCreatorName(noteBook.getCreator().getDisplayName());
            }
            fullDisplayBean.setIsTemplate(noteBook.getIsTemplate());
            fullDisplayBean.setEntriesCount(noteBook.getEntries().size());
            fullDisplayBean.setQuestionnaireFhirUuid(noteBook.getQuestionnaireFhirUuid());

            // If this is an instance (isTemplate=false), find and set the parent template
            // ID
            if (noteBook.getIsTemplate() != null && !noteBook.getIsTemplate()) {
                NoteBook parentTemplate = baseObjectDAO.findParentTemplate(noteBook.getId());
                if (parentTemplate != null) {
                    fullDisplayBean.setTemplateId(parentTemplate.getId());
                }
            }

            List<SampleDisplayBean> sampleDisplayBeans = new ArrayList<>();

            for (SampleItem sampleItem : noteBook.getSamples()) {
                SampleDisplayBean displayBean = convertSampleToDisplayBean(sampleItem);
                sampleDisplayBeans.add(displayBean);
            }
            fullDisplayBean.setSamples(sampleDisplayBeans);

            fullDisplayBean.setSamples(sampleDisplayBeans);

            Hibernate.initialize(noteBook.getOrganizations());
            fullDisplayBean.setOrganizations(noteBook.getOrganizations());

            Hibernate.initialize(noteBook.getDepartments());
            fullDisplayBean.setDepartments(noteBook.getDepartments());

            Hibernate.initialize(noteBook.getAllowedRoles());
            fullDisplayBean.setAllowedRoles(noteBook.getAllowedRoles());

        }
        return fullDisplayBean;
    }

    @Override
    public SampleDisplayBean convertSampleToDisplayBean(SampleItem sampleItem) {
        SampleDisplayBean sampleDisplayBean = new SampleDisplayBean();
        sampleDisplayBean.setId(Integer.valueOf(sampleItem.getId()));
        sampleDisplayBean.setSampleItemId(sampleItem.getId()); // Store SampleItem ID
        sampleDisplayBean
                .setSampleType(typeOfSampleService.getNameForTypeOfSampleId(sampleItem.getTypeOfSample().getId()));
        sampleDisplayBean.setCollectionDate(DateUtil.convertTimestampToStringDate(sampleItem.getCollectionDate()));
        sampleDisplayBean.setVoided(sampleItem.isVoided());
        sampleDisplayBean.setVoidReason(sampleItem.getVoidReason());
        sampleDisplayBean.setExternalId(sampleItem.getExternalId());

        // Get accession number from parent Sample
        if (sampleItem.getSample() != null) {
            Sample sample = (Sample) sampleItem.getSample();
            sampleDisplayBean.setAccessionNumber(sample.getAccessionNumber());
            sampleDisplayBean.setSampleStatus(sample.getStatus());
        }

        List<Analysis> analyses = analysisService.getAnalysesBySampleItem(sampleItem);
        List<ResultDisplayBean> resultsDisplayBeans = new ArrayList<>();
        for (Analysis analysis : analyses) {
            List<Result> results = resultService.getResultsByAnalysis(analysis);
            for (Result result : results) {
                ResultDisplayBean resultDisplayBean = new ResultDisplayBean();
                resultDisplayBean.setResult(resultService.getResultValue(result, true));
                resultDisplayBean.setTest(analysis.getTest().getLocalizedName());
                resultDisplayBean.setDateCreated(DateUtil.convertTimestampToStringDate(result.getLastupdated()));
                resultsDisplayBeans.add(resultDisplayBean);
            }
        }
        sampleDisplayBean.setResults(resultsDisplayBeans);
        return sampleDisplayBean;
    }

    private NoteBook createNoteBookFromForm(NoteBook noteBook, NoteBookForm form) {

        if (!GenericValidator.isBlankOrNull(form.getTitle())) {
            noteBook.setTitle(form.getTitle());
        }
        if (form.getType() != null) {
            noteBook.setType(dictionaryService.get(form.getType().toString()));
        }
        if (form.getTags() != null && !form.getTags().isEmpty()) {
            noteBook.setTags(new ArrayList<>(form.getTags()));
        }
        if (!GenericValidator.isBlankOrNull(form.getContent())) {
            noteBook.setContent(form.getContent());
        }
        if (!GenericValidator.isBlankOrNull(form.getObjective())) {
            noteBook.setObjective(form.getObjective());
        }
        if (!GenericValidator.isBlankOrNull(form.getProtocol())) {
            noteBook.setProtocol(form.getProtocol());
        }
        noteBook.setIsTemplate(form.getIsTemplate());
        if (form.getStatus() != null) {
            noteBook.setStatus(form.getStatus());
        }

        if (form.getQuestionnaireFhirUuid() != null) {
            noteBook.setQuestionnaireFhirUuid(form.getQuestionnaireFhirUuid());
        }
        // Set sysUserId for audit trail tracking
        if (form.getSystemUserId() != null) {
            noteBook.setSysUserId(form.getSystemUserId().toString());
            noteBook.setCreator(systemUserService.get(form.getSystemUserId().toString()));
        }
        if (noteBook.getId() == null) {
            noteBook.setDateCreated(new Date());
            // Only set technician from systemUserId if technicianId is not provided in form
            if (form.getTechnicianId() != null) {
                noteBook.setTechnician(systemUserService.get(form.getTechnicianId().toString()));
            } else if (form.getSystemUserId() != null) {
                noteBook.setTechnician(systemUserService.get(form.getSystemUserId().toString()));
            }
        } else {
            noteBook.setDateCreated(noteBook.getDateCreated());
            // Only update technician if provided in form, otherwise keep existing
            if (form.getTechnicianId() != null) {
                noteBook.setTechnician(systemUserService.get(form.getTechnicianId().toString()));
            }
        }

        noteBook.getAnalysers().clear();
        if (form.getAnalyzerIds() != null) {
            for (Integer analyserId : form.getAnalyzerIds()) {
                noteBook.getAnalysers().add(analyzerService.get(analyserId.toString()));
            }
        }

        // Handle inventory instruments
        noteBook.getInventoryInstrumentIds().clear();
        if (form.getInventoryInstrumentIds() != null) {
            noteBook.getInventoryInstrumentIds().addAll(form.getInventoryInstrumentIds());
        }

        noteBook.getSamples().clear();
        if (form.getSampleIds() != null) {
            for (Integer sampleId : form.getSampleIds()) {
                noteBook.getSamples().add(sampleItemService.get(sampleId.toString()));
            }
        }

        noteBook.getFiles().clear();
        if (form.getFiles() != null) {
            for (NoteBookForm.NoteBookFileForm fileForm : form.getFiles()) {
                NoteBookFile file = new NoteBookFile();
                file.setId(null);
                file.setFileName(fileForm.getFileName());
                file.setFileType(fileForm.getFileType());
                file.setFileData(fileForm.getFileData());
                file.setNotebook(noteBook);
                noteBook.getFiles().add(file);
            }
        }

        noteBook.getPages().clear();
        if (form.getPages() != null) {
            for (NoteBookPage page : form.getPages()) {
                page.setId(null);
                page.setNotebook(noteBook);
                noteBook.getPages().add(page);
            }
        }

        // Handle comments - only add new comments (those without id)
        if (form.getComments() != null) {
            for (NoteBookForm.NoteBookCommentForm commentForm : form.getComments()) {
                // Only process new comments (id is null)
                if (commentForm.getId() == null && !GenericValidator.isBlankOrNull(commentForm.getText())) {
                    NoteBookComment comment = new NoteBookComment();
                    comment.setText(commentForm.getText());
                    comment.setDateCreated(new Date());
                    comment.setNotebook(noteBook);
                    // Set author from systemUserId (current user)
                    if (form.getSystemUserId() != null) {
                        comment.setAuthor(systemUserService.get(form.getSystemUserId().toString()));
                    }
                    noteBook.getComments().add(comment);
                }
            }
        }

        // Handle organizations (for templates - defines which locations can use this
        // template)
        // Check if the form included this field (it might be empty list, which implies
        // clearing)
        if (form.getOrganizationIds() != null) {
            // Use clear/add on existing collection to ensure persistence
            noteBook.getOrganizations().clear();
            for (String orgId : form.getOrganizationIds()) {
                Organization org = organizationService.get(orgId);
                if (org != null) {
                    noteBook.getOrganizations().add(org);
                }
            }
        }

        // Handle departments (for templates - defines which departments can use this
        // template)
        // Only update departments if explicitly provided in the form (not null)
        if (form.getDepartmentIds() != null) {
            noteBook.getDepartments().clear();
            for (String deptId : form.getDepartmentIds()) {
                TestSection ts = testSectionService.get(deptId);
                if (ts != null) {
                    noteBook.getDepartments().add(ts);
                }
            }
        }

        // Handle allowed roles (for templates - defines which roles can create entries)
        if (form.getAllowedRoles() != null) {
            noteBook.getAllowedRoles().clear();
            noteBook.getAllowedRoles().addAll(form.getAllowedRoles());
        }

        return noteBook;
    }

    @Override
    @Transactional
    public Long getCountWithStatus(List<NoteBookStatus> statuses) {
        return baseObjectDAO.getCountWithStatus(statuses);
    }

    @Override
    @Transactional
    public Long getCountWithStatusBetweenDates(List<NoteBookStatus> statuses, Timestamp from, Timestamp to) {
        return baseObjectDAO.getCountWithStatusBetweenDates(statuses, from, to);
    }

    @Override
    @Transactional
    public Long getTotalCount() {
        return baseObjectDAO.getTotalCount();
    }

    @Override
    @Transactional
    public List<SampleDisplayBean> searchSampleItems(String accession) {

        List<Sample> samples = StringUtils.isNotBlank(accession) ? Optional
                .ofNullable(sampleService.getSampleByAccessionNumber(accession)).map(List::of).orElseGet(List::of)
                : new ArrayList<>();

        return samples.stream().flatMap(sample -> sampleItemService.getSampleItemsBySampleId(sample.getId()).stream())
                .map(this::convertSampleToDisplayBean).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<NoteBook> getAllTemplateNoteBooks() {
        List<NoteBook> noteBooks = baseObjectDAO.getAllMatching("isTemplate", true);
        noteBooks.forEach(nb -> {
            Hibernate.initialize(nb.getDepartments());
            Hibernate.initialize(nb.getOrganizations());
            Hibernate.initialize(nb.getAllowedRoles());
        });
        return noteBooks;
    }

    @Override
    @Transactional
    public List<NoteBook> getNoteBookEntries(Integer templateId) {
        NoteBook template = get(templateId);
        if (template != null && template.getIsTemplate()) {
            Hibernate.initialize(template.getEntries());
            return template.getEntries();
        }
        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteBook> getAllActiveNotebooks() {
        // Get all notebooks that are not chilled
        List<NoteBookStatus> activeStatuses = List.of(NoteBookStatus.DRAFT, NoteBookStatus.SUBMITTED,
                NoteBookStatus.FINALIZED, NoteBookStatus.LOCKED);
        List<NoteBook> noteBooks = filterNoteBooks(activeStatuses, null, null, null, null);
        noteBooks.forEach(nb -> {
            Hibernate.initialize(nb.getDepartments());
            Hibernate.initialize(nb.getOrganizations());
            Hibernate.initialize(nb.getAllowedRoles());
        });
        return noteBooks;
    }

    @Override
    @Transactional
    public NoteBook createInstanceFromTemplate(Integer templateId, String title, String sysUserId) {
        NoteBook template = get(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }
        if (!template.getIsTemplate()) {
            throw new IllegalArgumentException("Notebook " + templateId + " is not a template");
        }

        // Initialize lazy collections
        Hibernate.initialize(template.getPages());
        Hibernate.initialize(template.getTags());
        Hibernate.initialize(template.getAnalysers());
        if (template.getPages() != null) {
            for (NoteBookPage page : template.getPages()) {
                Hibernate.initialize(page.getPanels());
                Hibernate.initialize(page.getTests());
            }
        }

        // Create new instance
        NoteBook instance = new NoteBook();
        instance.setTitle(title);
        instance.setIsTemplate(false);
        instance.setStatus(NoteBookStatus.DRAFT);
        instance.setDateCreated(new Date());
        instance.setType(template.getType());
        instance.setContent(template.getContent());
        instance.setObjective(template.getObjective());
        instance.setProtocol(template.getProtocol());
        instance.setQuestionnaireFhirUuid(template.getQuestionnaireFhirUuid());

        // Copy tags
        if (template.getTags() != null) {
            instance.setTags(new ArrayList<>(template.getTags()));
        }

        // Copy analyzers (legacy)
        if (template.getAnalysers() != null) {
            instance.getAnalysers().addAll(template.getAnalysers());
        }

        // Copy inventory instruments
        if (template.getInventoryInstrumentIds() != null) {
            instance.getInventoryInstrumentIds().addAll(template.getInventoryInstrumentIds());
        }

        // Set creator and technician
        if (sysUserId != null) {
            instance.setSysUserId(sysUserId);
            instance.setCreator(systemUserService.get(sysUserId));
            instance.setTechnician(systemUserService.get(sysUserId));
        }

        // Copy pages
        if (template.getPages() != null) {
            for (NoteBookPage templatePage : template.getPages()) {
                NoteBookPage instancePage = new NoteBookPage();
                instancePage.setTitle(templatePage.getTitle());
                instancePage.setOrder(templatePage.getOrder());
                instancePage.setContent(templatePage.getContent());
                instancePage.setNotebook(instance);

                // Copy panels and tests references
                if (templatePage.getPanels() != null) {
                    instancePage.getPanels().addAll(templatePage.getPanels());
                }
                if (templatePage.getTests() != null) {
                    instancePage.getTests().addAll(templatePage.getTests());
                }

                instance.getPages().add(instancePage);
            }
        }

        // Save the instance
        instance = save(instance);

        // Link to template's entries list
        template.getEntries().add(instance);
        template.setSysUserId(sysUserId);
        update(template);

        return instance;
    }

    @Override
    @Transactional(readOnly = true)
    public NoteBookPage getPage(Integer pageId) {
        if (pageId == null) {
            return null;
        }
        return noteBookPageDAO.get(pageId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public NoteBookPage getNextPage(Integer pageId) {
        if (pageId == null) {
            LogEvent.logDebug(this.getClass().getName(), "getNextPage", "pageId is null");
            return null;
        }

        NoteBookPage currentPage = noteBookPageDAO.get(pageId).orElse(null);
        if (currentPage == null) {
            LogEvent.logDebug(this.getClass().getName(), "getNextPage", "currentPage not found for pageId=" + pageId);
            return null;
        }

        // Get the notebook and its pages
        NoteBook notebook = currentPage.getNotebook();
        if (notebook == null) {
            LogEvent.logDebug(this.getClass().getName(), "getNextPage",
                    "notebook is null for pageId=" + pageId + " title='" + currentPage.getTitle() + "'");
            return null;
        }

        Hibernate.initialize(notebook.getPages());
        List<NoteBookPage> pages = notebook.getPages();
        if (pages == null || pages.isEmpty()) {
            LogEvent.logDebug(this.getClass().getName(), "getNextPage",
                    "pages is empty for notebook id=" + notebook.getId());
            return null;
        }

        // Sort pages by order and find the next one
        Integer currentOrder = currentPage.getOrder();
        if (currentOrder == null) {
            LogEvent.logDebug(this.getClass().getName(), "getNextPage",
                    "currentOrder is null for pageId=" + pageId + " title='" + currentPage.getTitle() + "'");
            return null;
        }

        LogEvent.logInfo(this.getClass().getName(), "getNextPage",
                "Finding next page: currentPageId=" + pageId + " currentOrder=" + currentOrder + " title='"
                        + currentPage.getTitle() + "' notebookId=" + notebook.getId() + " totalPages=" + pages.size());

        // Log all pages for debugging
        for (NoteBookPage p : pages) {
            LogEvent.logDebug(this.getClass().getName(), "getNextPage",
                    "  Page: id=" + p.getId() + " order=" + p.getOrder() + " title='" + p.getTitle() + "'");
        }

        NoteBookPage nextPage = pages.stream().filter(p -> p.getOrder() != null && p.getOrder() > currentOrder)
                .min((p1, p2) -> p1.getOrder().compareTo(p2.getOrder())).orElse(null);

        if (nextPage != null) {
            LogEvent.logInfo(this.getClass().getName(), "getNextPage", "Found next page: id=" + nextPage.getId()
                    + " order=" + nextPage.getOrder() + " title='" + nextPage.getTitle() + "'");
        } else {
            LogEvent.logInfo(this.getClass().getName(), "getNextPage",
                    "No next page found for currentOrder=" + currentOrder + " (this might be the last page)");
        }

        return nextPage;
    }

    @Override
    @Transactional(readOnly = true)
    public NoteBookPage getPreviousPage(Integer pageId) {
        if (pageId == null) {
            return null;
        }

        NoteBookPage currentPage = noteBookPageDAO.get(pageId).orElse(null);
        if (currentPage == null) {
            return null;
        }

        // Get the notebook and its pages
        NoteBook notebook = currentPage.getNotebook();
        if (notebook == null) {
            return null;
        }

        Hibernate.initialize(notebook.getPages());
        List<NoteBookPage> pages = notebook.getPages();
        if (pages == null || pages.isEmpty()) {
            return null;
        }

        // Sort pages by order and find the previous one
        Integer currentOrder = currentPage.getOrder();
        if (currentOrder == null) {
            return null;
        }

        return pages.stream().filter(p -> p.getOrder() != null && p.getOrder() < currentOrder)
                .max((p1, p2) -> p1.getOrder().compareTo(p2.getOrder())).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public NoteBookPage getLastPage(Integer notebookId) {
        if (notebookId == null) {
            return null;
        }

        NoteBook notebook = get(notebookId);
        if (notebook == null) {
            return null;
        }

        Hibernate.initialize(notebook.getPages());
        List<NoteBookPage> pages = notebook.getPages();
        if (pages == null || pages.isEmpty()) {
            return null;
        }

        // Return the page with the highest order (last page / archiving page)
        return pages.stream().filter(p -> p.getOrder() != null).max((p1, p2) -> p1.getOrder().compareTo(p2.getOrder()))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRoutingPage(Integer pageId) {
        if (pageId == null) {
            return false;
        }

        NoteBookPage page = noteBookPageDAO.get(pageId).orElse(null);
        if (page == null) {
            return false;
        }

        // Check if the page title indicates it's a routing page
        // Note: Routing happens on the "Child Samples" page (order 4) which includes
        // both child sample creation and destination routing per User Story 4
        String title = page.getTitle() != null ? page.getTitle().toLowerCase() : "";
        if (title.contains("routing") || title.contains("route")) {
            return true;
        }

        // Check for "child sample" in title since routing is combined with child
        // sample creation (Immunology workflow)
        if (title.contains("child sample")) {
            return true;
        }

        // Check by page order - order 4 is the Child Samples page where routing
        // happens, but ONLY for Immunology workflow (not MNTD)
        // MNTD has "Sample Processing Preparation" at order 4 which is NOT a routing
        // page
        if (page.getOrder() != null && page.getOrder() == 4) {
            // Check if this is an Immunology notebook (not MNTD)
            NoteBook notebook = page.getNotebook();
            if (notebook != null) {
                Hibernate.initialize(notebook);
                String notebookTitle = notebook.getTitle() != null ? notebook.getTitle().toLowerCase() : "";
                // Only treat order 4 as routing for Immunology workflows
                if (notebookTitle.contains("immunology")) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    @Transactional
    public Integer attachFile(Integer notebookId, byte[] fileData, String fileName, String fileType, String sysUserId) {
        if (notebookId == null) {
            throw new IllegalArgumentException("Notebook ID is required");
        }
        if (fileData == null || fileData.length == 0) {
            throw new IllegalArgumentException("File data is required");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }

        NoteBook notebook = get(notebookId);
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId);
        }

        // Create NoteBookFile record
        NoteBookFile file = new NoteBookFile();
        file.setNotebook(notebook);
        file.setFileData(fileData);
        file.setFileName(fileName);
        file.setFileType(fileType != null ? fileType : "application/octet-stream");

        // Initialize files list if needed
        if (notebook.getFiles() == null) {
            notebook.setFiles(new ArrayList<>());
        }
        notebook.getFiles().add(file);

        // Set sysUserId for audit trail
        if (sysUserId != null) {
            notebook.setSysUserId(sysUserId);
        }

        // Update notebook which will cascade to NoteBookFile
        update(notebook);

        return file.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteBookFile> getFiles(Integer notebookId) {
        if (notebookId == null) {
            return new ArrayList<>();
        }

        NoteBook notebook = get(notebookId);
        if (notebook == null) {
            return new ArrayList<>();
        }

        Hibernate.initialize(notebook.getFiles());
        return notebook.getFiles() != null ? notebook.getFiles() : new ArrayList<>();
    }

    @Override
    @Transactional
    public int syncPagesFromTemplate(Integer instanceId, String sysUserId) {
        if (instanceId == null) {
            throw new IllegalArgumentException("Instance ID is required");
        }

        NoteBook instance = get(instanceId);
        if (instance == null) {
            throw new IllegalArgumentException("Notebook instance not found: " + instanceId);
        }

        if (instance.getIsTemplate() != null && instance.getIsTemplate()) {
            throw new IllegalArgumentException("Cannot sync pages for a template notebook");
        }

        // Find the parent template
        NoteBook template = baseObjectDAO.findParentTemplate(instanceId);
        if (template == null) {
            LogEvent.logWarn(this.getClass().getName(), "syncPagesFromTemplate",
                    "No parent template found for instance " + instanceId);
            return 0;
        }

        // Initialize pages collections
        Hibernate.initialize(template.getPages());
        Hibernate.initialize(instance.getPages());
        if (template.getPages() != null) {
            for (NoteBookPage p : template.getPages()) {
                Hibernate.initialize(p.getPanels());
                Hibernate.initialize(p.getTests());
            }
        }

        List<NoteBookPage> templatePages = template.getPages();
        List<NoteBookPage> instancePages = instance.getPages();

        if (templatePages == null || templatePages.isEmpty()) {
            return 0;
        }

        // Create a set of existing page orders in the instance
        java.util.Set<Integer> existingOrders = new java.util.HashSet<>();
        if (instancePages != null) {
            for (NoteBookPage p : instancePages) {
                if (p.getOrder() != null) {
                    existingOrders.add(p.getOrder());
                }
            }
        }

        // Add any missing pages from template to instance
        int addedCount = 0;
        for (NoteBookPage templatePage : templatePages) {
            Integer templateOrder = templatePage.getOrder();
            if (templateOrder == null) {
                continue; // Skip pages without order
            }

            if (!existingOrders.contains(templateOrder)) {
                // This page is in template but not in instance - add it
                NoteBookPage newPage = new NoteBookPage();
                newPage.setTitle(templatePage.getTitle());
                newPage.setOrder(templatePage.getOrder());
                newPage.setContent(templatePage.getContent());
                newPage.setNotebook(instance);
                newPage.setCompleted(false);

                // Copy panels and tests references
                if (templatePage.getPanels() != null) {
                    newPage.getPanels().addAll(templatePage.getPanels());
                }
                if (templatePage.getTests() != null) {
                    newPage.getTests().addAll(templatePage.getTests());
                }

                instance.getPages().add(newPage);
                addedCount++;

                LogEvent.logInfo(this.getClass().getName(), "syncPagesFromTemplate", "Added page '"
                        + templatePage.getTitle() + "' (order=" + templateOrder + ") to instance " + instanceId);
            }
        }

        if (addedCount > 0) {
            // Set sysUserId for audit trail and update
            instance.setSysUserId(sysUserId);
            update(instance);
            LogEvent.logInfo(this.getClass().getName(), "syncPagesFromTemplate",
                    "Synced " + addedCount + " pages from template " + template.getId() + " to instance " + instanceId);
        }

        return addedCount;
    }

    @Override
    @Transactional
    public void updateTemplateOrganizations(Integer notebookId, List<String> organizationIds, String sysUserId) {
        if (notebookId == null) {
            return;
        }

        NoteBook notebook = get(notebookId);
        if (notebook == null) {
            return;
        }

        // Initialize lazy collections
        initializeLazyCollections(notebook);

        // Clear existing organizations and add new ones to the EXISTING collection
        notebook.getOrganizations().clear();
        if (organizationIds != null) {
            for (String orgId : organizationIds) {
                Organization org = organizationService.get(orgId);
                if (org != null) {
                    notebook.getOrganizations().add(org);
                }
            }
        }

        if (sysUserId != null) {
            notebook.setSysUserId(sysUserId);
        }

        update(notebook);
    }

    @Override
    @Transactional
    public void updateTemplateAllowedRoles(Integer notebookId, List<String> allowedRoles, String sysUserId) {
        if (notebookId == null) {
            return;
        }

        NoteBook notebook = get(notebookId);
        if (notebook == null) {
            return;
        }

        // Initialize lazy collections
        initializeLazyCollections(notebook);

        // Clear existing roles and add new ones to the EXISTING collection
        notebook.getAllowedRoles().clear();
        if (allowedRoles != null) {
            notebook.getAllowedRoles().addAll(allowedRoles);
        }

        if (sysUserId != null) {
            notebook.setSysUserId(sysUserId);
        }

        update(notebook);
    }

    @Override
    @Transactional
    public void updateTemplateDepartments(Integer notebookId, List<String> departmentIds, String sysUserId) {
        if (notebookId == null) {
            return;
        }

        NoteBook notebook = get(notebookId);
        if (notebook == null) {
            return;
        }

        // Initialize lazy collections
        initializeLazyCollections(notebook);

        // Clear existing departments and add new ones to the EXISTING collection
        notebook.getDepartments().clear();
        if (departmentIds != null) {
            for (String deptId : departmentIds) {
                TestSection ts = testSectionService.get(deptId);
                if (ts != null) {
                    notebook.getDepartments().add(ts);
                }
            }
        }

        if (sysUserId != null) {
            notebook.setSysUserId(sysUserId);
        }

        update(notebook);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<TestSection> getNoteBookDepartments(Integer notebookId) {
        if (notebookId == null) {
            return new HashSet<>();
        }
        NoteBook notebook = get(notebookId);
        if (notebook == null) {
            return new HashSet<>();
        }
        Hibernate.initialize(notebook.getDepartments());
        return notebook.getDepartments();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Organization> getNoteBookOrganizations(Integer notebookId) {
        if (notebookId == null) {
            return new HashSet<>();
        }
        NoteBook notebook = get(notebookId);
        if (notebook == null) {
            return new HashSet<>();
        }
        Hibernate.initialize(notebook.getOrganizations());
        return notebook.getOrganizations();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getNoteBookAllowedRoles(Integer notebookId) {
        if (notebookId == null) {
            return new HashSet<>();
        }
        NoteBook notebook = get(notebookId);
        if (notebook == null) {
            return new HashSet<>();
        }
        Hibernate.initialize(notebook.getAllowedRoles());
        return notebook.getAllowedRoles();
    }

    @Override
    @Transactional(readOnly = true)
    public NoteBook getParentTemplate(Integer entryId) {
        if (entryId == null) {
            return null;
        }
        return baseObjectDAO.findParentTemplate(entryId);
    }
}
