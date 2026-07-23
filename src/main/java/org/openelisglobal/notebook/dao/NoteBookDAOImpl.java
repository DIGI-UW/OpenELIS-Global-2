package org.openelisglobal.notebook.dao;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBook.NoteBookStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class NoteBookDAOImpl extends BaseDAOImpl<NoteBook, Integer> implements NoteBookDAO {
    public NoteBookDAOImpl() {
        super(NoteBook.class);
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<NoteBook> filterNoteBooks(List<NoteBookStatus> statuses, List<String> types, List<String> tags,
            Date fromDate, Date toDate) {

        StringBuilder hql = new StringBuilder("select distinct nb from NoteBook nb ");
        hql.append("left join nb.tags t where nb.isTemplate = true ");

        if (statuses != null && !statuses.isEmpty()) {
            hql.append("and nb.status in (:statuses) ");
        }

        if (types != null && !types.isEmpty()) {
            hql.append("and nb.type.id in (:types) ");
        }

        if (tags != null && !tags.isEmpty()) {
            hql.append("and t in (:tags) ");
        }

        if (fromDate != null) {
            hql.append("and nb.dateCreated >= :fromDate ");
        }

        if (toDate != null) {
            hql.append("and nb.dateCreated <= :toDate ");
        }

        Query<NoteBook> query = entityManager.unwrap(Session.class).createQuery(hql.toString(), NoteBook.class);

        if (statuses != null && !statuses.isEmpty()) {
            query.setParameterList("statuses", statuses);
        }

        if (types != null && !types.isEmpty()) {
            query.setParameterList("types", types);
        }

        if (tags != null && !tags.isEmpty()) {
            query.setParameterList("tags", tags);
        }

        if (fromDate != null) {
            query.setParameter("fromDate", fromDate);
        }

        if (toDate != null) {
            query.setParameter("toDate", toDate);
        }

        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteBook> filterNoteBookEntries(List<NoteBookStatus> statuses, List<String> types, List<String> tags,
            Date fromDate, Date toDate, List<Integer> entryIds) {

        StringBuilder hql = new StringBuilder("select distinct nb from NoteBook nb ");
        hql.append("left join nb.tags t where nb.isTemplate = false ");

        if (statuses != null && !statuses.isEmpty()) {
            hql.append("and nb.status in (:statuses) ");
        }

        if (types != null && !types.isEmpty()) {
            hql.append("and nb.type.id in (:types) ");
        }

        if (tags != null && !tags.isEmpty()) {
            hql.append("and t in (:tags) ");
        }

        if (fromDate != null) {
            hql.append("and nb.dateCreated >= :fromDate ");
        }

        if (toDate != null) {
            hql.append("and nb.dateCreated <= :toDate ");
        }

        if (entryIds != null && !entryIds.isEmpty()) {
            hql.append("and nb.id in (:ids) ");
        }
        Query<NoteBook> query = entityManager.unwrap(Session.class).createQuery(hql.toString(), NoteBook.class);

        if (statuses != null && !statuses.isEmpty()) {
            query.setParameterList("statuses", statuses);
        }

        if (types != null && !types.isEmpty()) {
            query.setParameterList("types", types);
        }

        if (tags != null && !tags.isEmpty()) {
            query.setParameterList("tags", tags);
        }

        if (fromDate != null) {
            query.setParameter("fromDate", fromDate);
        }

        if (toDate != null) {
            query.setParameter("toDate", toDate);
        }
        if (entryIds != null && !entryIds.isEmpty()) {
            query.setParameterList("ids", entryIds);
        }
        return query.list();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getCountWithStatus(List<NoteBookStatus> statuses) {
        String sql = "select count(*) from NoteBook nb where status in (:statuses) and nb.isTemplate = false";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameterList("statuses", statuses);
        Long count = query.uniqueResult();
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public Long getCountWithStatusBetweenDates(List<NoteBookStatus> statuses, Timestamp from, Timestamp to) {
        String sql = "select count(*) from NoteBook nb where nb.status in (:statuses) and nb.lastupdated"
                + " between :datefrom and :dateto and nb.isTemplate = false";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameterList("statuses", statuses);
        query.setParameter("datefrom", from);
        query.setParameter("dateto", to);
        Long count = query.uniqueResult();
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalCount() {
        String sql = "select count(*) from NoteBook nb where nb.isTemplate = false";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        Long count = query.uniqueResult();
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public long countByAnalyzerId(String analyzerId) {
        String hql = "SELECT COUNT(n) FROM NoteBook n JOIN n.analysers a WHERE a.id = :analyzerId";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
        query.setParameter("analyzerId", analyzerId);
        Long count = query.uniqueResult();
        return count != null ? count : 0L;
    }

    @Override
    public String getTableName() {
        return "notebook";
    }

    @Override
    @Transactional(readOnly = true)
    public NoteBook findParentTemplate(Integer entryId) {
        String hql = "select nb from NoteBook nb join nb.entries e where e.id = :entryId and nb.isTemplate = true";
        Query<NoteBook> query = entityManager.unwrap(Session.class).createQuery(hql, NoteBook.class);
        query.setParameter("entryId", entryId);
        List<NoteBook> results = query.list();
        return results.isEmpty() ? null : results.get(0);
    }
}
