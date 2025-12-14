package org.openelisglobal.notebook.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.dao.NotebookEntryDAO;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookEntry.EntryStatus;
import org.openelisglobal.notebook.valueholder.NotebookEntryComment;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for NotebookEntry operations.
 */
@Service
public class NotebookEntryServiceImpl extends AuditableBaseObjectServiceImpl<NotebookEntry, Integer>
        implements NotebookEntryService {

    @Autowired
    private NotebookEntryDAO notebookEntryDAO;

    @Autowired
    private NoteBookService noteBookService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    public NotebookEntryServiceImpl() {
        super(NotebookEntry.class);
    }

    @Override
    protected BaseDAO<NotebookEntry, Integer> getBaseObjectDAO() {
        return notebookEntryDAO;
    }

    @Override
    @Transactional
    public NotebookEntry createEntry(Integer notebookId, String title, String sysUserId) {
        NoteBook notebook = noteBookService.get(notebookId);
        if (notebook == null) {
            throw new IllegalArgumentException("Notebook not found: " + notebookId);
        }

        NotebookEntry entry = new NotebookEntry();
        entry.setNotebook(notebook);
        entry.setTitle(title);
        entry.setStatus(EntryStatus.DRAFT);
        entry.setDateCreated(new Date());

        if (sysUserId != null) {
            entry.setSysUserId(sysUserId);
            entry.setCreator(systemUserService.get(sysUserId));
            entry.setTechnician(systemUserService.get(sysUserId));
        }

        return save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookEntry> findByNotebookId(Integer notebookId) {
        List<NotebookEntry> entries = notebookEntryDAO.findByNotebookId(notebookId);
        // Initialize lazy-loaded relationships for JSON serialization
        for (NotebookEntry entry : entries) {
            initializeLazyRelationships(entry);
        }
        return entries;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookEntry> findByStatus(EntryStatus status) {
        List<NotebookEntry> entries = notebookEntryDAO.findByStatus(status);
        // Initialize lazy-loaded relationships for JSON serialization
        for (NotebookEntry entry : entries) {
            initializeLazyRelationships(entry);
        }
        return entries;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookEntry> findByTechnicianId(Integer technicianId) {
        List<NotebookEntry> entries = notebookEntryDAO.findByTechnicianId(technicianId);
        // Initialize lazy-loaded relationships for JSON serialization
        for (NotebookEntry entry : entries) {
            initializeLazyRelationships(entry);
        }
        return entries;
    }

    /**
     * Initialize lazy-loaded relationships to prevent LazyInitializationException
     * when accessing entities outside the transaction (e.g., in REST controllers).
     */
    private void initializeLazyRelationships(NotebookEntry entry) {
        if (entry != null) {
            Hibernate.initialize(entry.getNotebook());
            Hibernate.initialize(entry.getTechnician());
            Hibernate.initialize(entry.getCreator());
        }
    }

    @Override
    @Transactional
    public void updateStatus(Integer entryId, EntryStatus status, String sysUserId) {
        Optional<NotebookEntry> optEntry = notebookEntryDAO.get(entryId);
        if (optEntry.isPresent()) {
            NotebookEntry entry = optEntry.get();
            entry.setStatus(status);
            entry.setSysUserId(sysUserId);

            if (status == EntryStatus.FINALIZED || status == EntryStatus.ARCHIVED) {
                entry.setDateCompleted(new Date());
            }

            update(entry);
        }
    }

    @Override
    @Transactional
    public void addSample(Integer entryId, SampleItem sample, String sysUserId) {
        Optional<NotebookEntry> optEntry = notebookEntryDAO.get(entryId);
        if (optEntry.isPresent()) {
            NotebookEntry entry = optEntry.get();
            Hibernate.initialize(entry.getSamples());
            entry.addSample(sample);
            entry.setSysUserId(sysUserId);
            update(entry);

            // Create NotebookPageSample records for all pages in the notebook
            NoteBook notebook = entry.getNotebook();
            if (notebook != null) {
                notebookPageSampleService.createPageSamplesForNotebook(notebook.getId(),
                        Integer.valueOf(sample.getId()));
            }
        }
    }

    @Override
    @Transactional
    public void addSamples(Integer entryId, List<SampleItem> samples, String sysUserId) {
        Optional<NotebookEntry> optEntry = notebookEntryDAO.get(entryId);
        if (optEntry.isPresent()) {
            NotebookEntry entry = optEntry.get();
            Hibernate.initialize(entry.getSamples());
            for (SampleItem sample : samples) {
                entry.addSample(sample);
            }
            entry.setSysUserId(sysUserId);
            update(entry);

            // Create NotebookPageSample records for all pages in the notebook
            NoteBook notebook = entry.getNotebook();
            if (notebook != null) {
                for (SampleItem sample : samples) {
                    notebookPageSampleService.createPageSamplesForNotebook(notebook.getId(),
                            Integer.valueOf(sample.getId()));
                }
            }
        }
    }

    @Override
    @Transactional
    public void removeSample(Integer entryId, Integer sampleItemId, String sysUserId) {
        Optional<NotebookEntry> optEntry = notebookEntryDAO.get(entryId);
        if (optEntry.isPresent()) {
            NotebookEntry entry = optEntry.get();
            Hibernate.initialize(entry.getSamples());
            entry.getSamples().removeIf(s -> s.getId().equals(sampleItemId.toString()));
            entry.setSysUserId(sysUserId);
            update(entry);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleItem> getSamples(Integer entryId) {
        Optional<NotebookEntry> optEntry = notebookEntryDAO.get(entryId);
        if (optEntry.isPresent()) {
            NotebookEntry entry = optEntry.get();
            Hibernate.initialize(entry.getSamples());
            return entry.getSamples();
        }
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByNotebookId(Integer notebookId) {
        return notebookEntryDAO.countByNotebookId(notebookId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByStatus(EntryStatus status) {
        return notebookEntryDAO.countByStatus(status);
    }

    @Override
    @Transactional
    public void addComment(Integer entryId, String text, String sysUserId) {
        Optional<NotebookEntry> optEntry = notebookEntryDAO.get(entryId);
        if (optEntry.isPresent()) {
            NotebookEntry entry = optEntry.get();
            Hibernate.initialize(entry.getComments());

            NotebookEntryComment comment = new NotebookEntryComment();
            comment.setText(text);
            comment.setDateCreated(new Date());
            if (sysUserId != null) {
                comment.setAuthor(systemUserService.get(sysUserId));
            }

            entry.addComment(comment);
            entry.setSysUserId(sysUserId);
            update(entry);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotebookEntry getWithRelationships(Integer entryId) {
        Optional<NotebookEntry> optEntry = notebookEntryDAO.get(entryId);
        if (optEntry.isPresent()) {
            NotebookEntry entry = optEntry.get();
            initializeLazyRelationships(entry);
            return entry;
        }
        return null;
    }
}
