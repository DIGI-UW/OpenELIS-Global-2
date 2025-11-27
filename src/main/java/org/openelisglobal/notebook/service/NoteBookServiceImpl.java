package org.openelisglobal.notebook.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.Hibernate;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.notebook.bean.NoteBookDisplayBean;
import org.openelisglobal.notebook.bean.NoteBookFullDisplayBean;
import org.openelisglobal.notebook.bean.SampleDisplayBean;
import org.openelisglobal.notebook.bean.SampleDisplayBean.ResultDisplayBean;
import org.openelisglobal.notebook.dao.NoteBookDAO;
import org.openelisglobal.notebook.form.NoteBookForm;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.openelisglobal.notebook.valueholder.NoteBookComment;
import org.openelisglobal.notebook.valueholder.NoteBookFile;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteBookServiceImpl extends AuditableBaseObjectServiceImpl<NoteBook, Integer> implements NoteBookService {

    @Autowired
    private NoteBookDAO baseObjectDAO;

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
        if (!noteBook.getIsTemplate()) {
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
        // CRITICAL: Evict the modified object from the session cache BEFORE loading the
        // old object.
        // This ensures that when we load the old object, we get a fresh copy from the
        // database
        // with the original values, not the modified instance from the session cache.
        if (auditTrailLog && noteBook.getId() != null) {
            // Evict the modified object so that get() will return a fresh copy from DB
            baseObjectDAO.evict(noteBook);

            // Now load the old object - this will get a fresh copy from the database
            Optional<NoteBook> oldNoteBook = baseObjectDAO.get(noteBook.getId());
            if (oldNoteBook.isPresent()) {
                NoteBook oldObject = oldNoteBook.get();
                // Initialize all lazy collections before the parent evicts the object
                initializeLazyCollections(oldObject);
            }
        }
        // Let the parent handle the audit trail logic normally
        // The parent will re-attach the modified object and persist it
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
        Hibernate.initialize(noteBook.getPages());
        Hibernate.initialize(noteBook.getFiles());
        Hibernate.initialize(noteBook.getComments());
        Hibernate.initialize(noteBook.getEntries());
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
            Hibernate.initialize(noteBook.getSamples());
            Hibernate.initialize(noteBook.getPages());
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
            fullDisplayBean.setProject(noteBook.getProject());
            List<IdValuePair> analyzers = noteBook.getAnalysers().stream()
                    .map(analyzer -> new IdValuePair(analyzer.getId(), analyzer.getName())).toList();
            fullDisplayBean.setAnalyzers(analyzers);
            fullDisplayBean.setPages(noteBook.getPages());
            fullDisplayBean.setFiles(noteBook.getFiles());
            // Initialize author for each comment
            for (NoteBookComment comment : noteBook.getComments()) {
                Hibernate.initialize(comment.getAuthor());
            }
            fullDisplayBean.setComments(noteBook.getComments());
            fullDisplayBean.setTechnicianName(noteBook.getTechnician().getDisplayName());
            fullDisplayBean.setTechnicianId(Integer.valueOf(noteBook.getTechnician().getId()));
            fullDisplayBean.setIsTemplate(noteBook.getIsTemplate());
            fullDisplayBean.setEntriesCount(noteBook.getEntries().size());

            List<SampleDisplayBean> sampleDisplayBeans = new ArrayList<>();

            for (SampleItem sampleItem : noteBook.getSamples()) {
                SampleDisplayBean displayBean = convertSampleToDisplayBean(sampleItem);
                sampleDisplayBeans.add(displayBean);
            }
            fullDisplayBean.setSamples(sampleDisplayBeans);

        }
        return fullDisplayBean;
    }

    private SampleDisplayBean convertSampleToDisplayBean(SampleItem sampleItem) {
        SampleDisplayBean sampleDisplayBean = new SampleDisplayBean();
        sampleDisplayBean.setId(Integer.valueOf(sampleItem.getId()));
        sampleDisplayBean
                .setSampleType(typeOfSampleService.getNameForTypeOfSampleId(sampleItem.getTypeOfSample().getId()));
        sampleDisplayBean.setCollectionDate(DateUtil.convertTimestampToStringDate(sampleItem.getLastupdated()));
        sampleDisplayBean.setVoided(sampleItem.isVoided());
        sampleDisplayBean.setVoidReason(sampleItem.getVoidReason());
        sampleDisplayBean.setExternalId(sampleItem.getExternalId());
        List<Analysis> analyses = analysisService.getAnalysesBySampleItem(sampleItem);
        List<ResultDisplayBean> resultsDisplayBeans = new ArrayList<>();
        for (Analysis analysis : analyses) {
            List<Result> results = resultService.getResultsByAnalysis(analysis);
            for (Result result : results) {
                ResultDisplayBean resultDisplayBean = new ResultDisplayBean();
                resultDisplayBean.setResult(resultService.getResultValue(result, true));
                resultDisplayBean.setTest(result.getTestResult().getTest().getLocalizedName());
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
        if (!GenericValidator.isBlankOrNull(form.getProject())) {
            noteBook.setProject(form.getProject());
        }
        noteBook.setIsTemplate(form.getIsTemplate());
        if (form.getStatus() != null) {
            noteBook.setStatus(form.getStatus());
        }
        // Set sysUserId for audit trail tracking
        if (form.getSystemUserId() != null) {
            noteBook.setSysUserId(form.getSystemUserId().toString());
        }
        if (noteBook.getId() == null) {
            noteBook.setDateCreated(new Date());
            noteBook.setTechnician(systemUserService.get(form.getSystemUserId().toString()));
        } else {
            noteBook.setDateCreated(noteBook.getDateCreated());
            noteBook.setTechnician(systemUserService.get(form.getTechnicianId().toString()));
        }

        noteBook.getAnalysers().clear();
        if (form.getAnalyzerIds() != null) {
            for (Integer analyserId : form.getAnalyzerIds()) {
                noteBook.getAnalysers().add(analyzerService.get(analyserId.toString()));
            }
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
        return baseObjectDAO.getAllMatching("isTemplate", true);
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
        // Get all notebooks that are not archived
        List<NoteBookStatus> activeStatuses = List.of(NoteBookStatus.DRAFT, NoteBookStatus.SUBMITTED,
                NoteBookStatus.FINALIZED, NoteBookStatus.LOCKED);
        return filterNoteBooks(activeStatuses, null, null, null, null);
    }

}
