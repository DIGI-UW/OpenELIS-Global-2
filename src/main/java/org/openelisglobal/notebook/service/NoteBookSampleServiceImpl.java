package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.bean.SampleDisplayBean;
import org.openelisglobal.notebook.dao.NoteBookSampleDAO;
import org.openelisglobal.notebook.valueholder.NoteBookSample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteBookSampleServiceImpl extends AuditableBaseObjectServiceImpl<NoteBookSample, Integer>
        implements NoteBookSampleService {

    @Autowired
    private NoteBookSampleDAO baseObjectDAO;

    @Autowired
    NoteBookService noteBookService;

    public NoteBookSampleServiceImpl() {
        super(NoteBookSample.class);
    }

    @Override
    protected BaseDAO<NoteBookSample, Integer> getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteBookSample> getNotebookSamplesBySampleItemId(Integer sampleItemId) {
        return baseObjectDAO.getNotebookSamplesBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleDisplayBean> getNotebookSamplesByNoteBookId(Integer noteBookId) {
        List<NoteBookSample> noteBookSamples = baseObjectDAO.getNotebookSamplesByNoteBookId(noteBookId);

        List<SampleItem> samples = noteBookSamples.stream().map(s -> s.getSampleItem()).collect(Collectors.toList());

        return samples.stream().map(s -> noteBookService.convertSampleToDisplayBean(s)).collect(Collectors.toList());
    }
}
