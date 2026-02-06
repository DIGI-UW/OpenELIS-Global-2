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

    @Autowired
    private NotebookAuditService notebookAuditService;

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

    // Audit integration overrides

    @Override
    @Transactional
    public NoteBookSample save(NoteBookSample entity) {
        NoteBookSample saved = super.save(entity);
        try {
            notebookAuditService.saveAuditLog(saved, "notebook_sample", "I", saved.getSysUserId());
        } catch (Exception e) {
            // Use simple println since LogEvent import might not be available
            System.err.println("Failed to create audit log for NoteBookSample: " + e.getMessage());
        }
        return saved;
    }

    @Override
    @Transactional
    public NoteBookSample update(NoteBookSample entity) {
        NoteBookSample original = get(entity.getId());
        NoteBookSample updated = super.update(entity);
        try {
            notebookAuditService.saveAuditLog(updated, original, "notebook_sample", "U", updated.getSysUserId());
        } catch (Exception e) {
            System.err.println("Failed to create audit log for NoteBookSample: " + e.getMessage());
        }
        return updated;
    }

    @Override
    @Transactional
    public void delete(NoteBookSample entity) {
        try {
            notebookAuditService.saveAuditLog(entity, "notebook_sample", "D", entity.getSysUserId());
        } catch (Exception e) {
            System.err.println("Failed to create audit log for NoteBookSample: " + e.getMessage());
        }
        super.delete(entity);
    }
}
