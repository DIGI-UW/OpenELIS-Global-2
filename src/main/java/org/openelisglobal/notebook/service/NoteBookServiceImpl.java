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
import org.openelisglobal.notebook.valueholder.NoteBookFile;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
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
    private PatientService patientService;

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

    private Patient patient;

    public NoteBookServiceImpl() {
        super(NoteBook.class);
    }

    @Override
    protected BaseDAO<NoteBook, Integer> getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public List<NoteBook> filterNoteBooks(List<NoteBookStatus> statuses, List<String> types, List<String> tags,
            Date fromDate, Date toDate) {
        return baseObjectDAO.filterNoteBooks(statuses, types, tags, fromDate, toDate);
    }

    @Override
    @Transactional
    public void updateWithStatus(Integer notebookId, NoteBookStatus status) {
        Optional<NoteBook> optionalNoteBook = baseObjectDAO.get(notebookId);
        if (optionalNoteBook.isPresent()) {
            NoteBook noteBook = optionalNoteBook.get();
            noteBook.setStatus(status);
            update(noteBook);
        }
    }

    @Override
    @Transactional
    public void createWithFormValues(NoteBookForm form) {
        NoteBook noteBook = new NoteBook();
        noteBook = createNoteBookFromForm(noteBook, form);
        save(noteBook);
    }

    @Override
    @Transactional
    public void updateWithFormValues(Integer noteBookId, NoteBookForm form) {
        Optional<NoteBook> optionalNoteBook = baseObjectDAO.get(noteBookId);
        if (optionalNoteBook.isPresent()) {
            NoteBook noteBook = optionalNoteBook.get();
            noteBook = createNoteBookFromForm(noteBook, form);
            update(noteBook);
        }
    }

    @Override
    @Transactional
    public NoteBookDisplayBean convertToDisplayBean(Integer noteBookId) {
        NoteBookDisplayBean displayBean = new NoteBookDisplayBean();
        Optional<NoteBook> optionalNoteBook = baseObjectDAO.get(noteBookId);
        if (optionalNoteBook.isPresent()) {
            NoteBook noteBook = optionalNoteBook.get();
            Patient patient = patientService.getData(noteBook.getPatient().getId());
            Hibernate.initialize(noteBook.getTags());
            displayBean.setId(noteBook.getId());
            displayBean.setTitle(noteBook.getTitle());
            displayBean.setType(Integer.valueOf(noteBook.getType()));
            displayBean.setLastName(patientService.getLastName(patient));
            displayBean.setFirstName(patientService.getFirstName(patient));
            displayBean.setGender(patientService.getGender(patient));
            displayBean.setTags(noteBook.getTags());
            displayBean.setDateCreated(DateUtil.formatDateAsText(noteBook.getDateCreated()));
            displayBean.setStatus(noteBook.getStatus());
            displayBean.setTypeName(dictionaryService.get(noteBook.getType().toString()).getDictEntry());
        }
        return displayBean;
    }

    @Override
    @Transactional
    public NoteBookFullDisplayBean convertToFullDisplayBean(Integer noteBookId) {
        NoteBookFullDisplayBean fullDisplayBean = new NoteBookFullDisplayBean();
        Optional<NoteBook> optionalNoteBook = baseObjectDAO.get(noteBookId);
        if (optionalNoteBook.isPresent()) {
            NoteBook noteBook = optionalNoteBook.get();
            Hibernate.initialize(noteBook.getAnalysers());
            Hibernate.initialize(noteBook.getSamples());
            Hibernate.initialize(noteBook.getPages());
            Hibernate.initialize(noteBook.getFiles());
            Hibernate.initialize(noteBook.getTags());
            patient = patientService.getData(noteBook.getPatient().getId());
            fullDisplayBean.setId(noteBook.getId());
            fullDisplayBean.setTitle(noteBook.getTitle());
            fullDisplayBean.setType((Integer.valueOf(noteBook.getType())));
            if (patient != null) {
                fullDisplayBean.setLastName(patientService.getLastName(patient));
                fullDisplayBean.setFirstName(patientService.getFirstName(patient));
                fullDisplayBean.setGender(patientService.getGender(patient));
                fullDisplayBean.setPatientId(Integer.valueOf(patient.getId()));
            }
            fullDisplayBean.setTags(noteBook.getTags());
            fullDisplayBean.setDateCreated(DateUtil.formatDateAsText(noteBook.getDateCreated()));
            fullDisplayBean.setStatus(noteBook.getStatus());
            fullDisplayBean.setTypeName(dictionaryService.get(noteBook.getType().toString()).getDictEntry());
            fullDisplayBean.setContent(noteBook.getContent());
            fullDisplayBean.setObjective(noteBook.getObjective());
            fullDisplayBean.setProtocol(noteBook.getProtocol());
            fullDisplayBean.setProject(noteBook.getProject());
            List<IdValuePair> analyzers = noteBook.getAnalysers().stream()
                    .map(analyzer -> new IdValuePair(analyzer.getId(), analyzer.getName())).toList();
            fullDisplayBean.setAnalyzers(analyzers);
            fullDisplayBean.setPages(noteBook.getPages());
            fullDisplayBean.setFiles(noteBook.getFiles());
            fullDisplayBean.setTechnicianName(noteBook.getTechnician().getDisplayName());
            fullDisplayBean.setTechnicianId(Integer.valueOf(noteBook.getTechnician().getId()));

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
        if (patient != null) {
            sampleDisplayBean.setPatientId(Integer.valueOf(patient.getId()));
        }
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
            noteBook.setType(form.getType().toString());
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

        if (noteBook.getId() == null) {
            noteBook.setDateCreated(new Date());
            noteBook.setStatus(NoteBookStatus.DRAFT);
            noteBook.setTechnician(systemUserService.get(form.getSystemUserId().toString()));
        } else {
            noteBook.setDateCreated(noteBook.getDateCreated());
            noteBook.setStatus(form.getStatus());
            noteBook.setTechnician(systemUserService.get(form.getTechnicianId().toString()));
        }

        noteBook.setPatient(patientService.getData(form.getPatientId().toString()));

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
            for (NoteBookFile file : form.getFiles()) {
                file.setId(null);
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
    public List<SampleDisplayBean> searchSampleItems(String patientId, String accession) {

        if (StringUtils.isNotBlank(patientId)) {
            patient = patientService.get(patientId);
        } else if (StringUtils.isNotBlank(accession)) {
            Sample sample = sampleService.getSampleByAccessionNumber(accession);
            if (sample != null) {
                patient = sampleService.getPatient(sample);
            }
        }
        List<Sample> samples = StringUtils.isNotBlank(accession)
                ? Optional.ofNullable(sampleService.getSampleByAccessionNumber(accession)).map(List::of)
                        .orElseGet(List::of)
                : StringUtils.isNotBlank(patientId) ? sampleService.getSamplesForPatient(patientId) : List.of();

        return samples.stream().flatMap(sample -> sampleItemService.getSampleItemsBySampleId(sample.getId()).stream())
                .map(this::convertSampleToDisplayBean).collect(Collectors.toList());
    }

}
