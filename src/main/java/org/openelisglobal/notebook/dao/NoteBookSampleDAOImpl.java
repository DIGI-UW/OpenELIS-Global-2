package org.openelisglobal.notebook.dao;

import java.util.List;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.NoteBookSample;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NoteBookSampleDAOImpl extends BaseDAOImpl<NoteBookSample, Integer> implements NoteBookSampleDAO {

    public NoteBookSampleDAOImpl() {
        super(NoteBookSample.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteBookSample> getNotebookSamplesBySampleItemId(Integer sampleItemId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM NoteBookSample nbs WHERE nbs.sampleItem.id = :sampleItemId";
        return session.createQuery(hql, NoteBookSample.class).setParameter("sampleItemId", sampleItemId)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteBookSample> getNotebookSamplesByNoteBookId(Integer noteBookId) {
        Session session = entityManager.unwrap(Session.class);
        String hql = "FROM NoteBookSample nbs WHERE nbs.notebook.id = :noteBookId";
        return session.createQuery(hql, NoteBookSample.class).setParameter("noteBookId", noteBookId).getResultList();
    }
}
