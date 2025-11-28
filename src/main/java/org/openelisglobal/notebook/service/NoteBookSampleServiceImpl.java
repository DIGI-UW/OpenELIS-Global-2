package org.openelisglobal.notebook.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.dao.NoteBookSampleDAO;
import org.openelisglobal.notebook.valueholder.NoteBookSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteBookSampleServiceImpl extends AuditableBaseObjectServiceImpl<NoteBookSample, Integer>
        implements NoteBookSampleService {

    @Autowired
    private NoteBookSampleDAO baseObjectDAO;

    @PersistenceContext
    private EntityManager entityManager;

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
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM NoteBookSample nbs WHERE nbs.sampleItem.id = :sampleItemId";
        return session.createQuery(hql, NoteBookSample.class).setParameter("sampleItemId", sampleItemId)
                .getResultList();
    }
}
