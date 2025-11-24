package org.openelisglobal.notebook.dao;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
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
    @SuppressWarnings("unchecked")
    public List<NoteBook> filterNoteBooks(List<NoteBookStatus> statuses, List<String> types, List<String> tags,
            Date fromDate, Date toDate) {

        StringBuilder hql = new StringBuilder("select distinct nb from NoteBook nb ");
        hql.append("left join nb.tags t where 1=1 ");

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
            query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
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
            query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
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
    public Long getCountWithStatus(List<NoteBookStatus> statuses) {
        String sql = "select count(*) from NoteBook nb where status in (:statuses) and nb.isTemplate = false";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        Long count = query.uniqueResult();
        return count;
    }

    @Override
    public Long getCountWithStatusBetweenDates(List<NoteBookStatus> statuses, Timestamp from, Timestamp to) {
        String sql = "select count(*) from NoteBook nb where nb.status in (:statuses) and nb.lastupdated"
                + " between :datefrom and :dateto and nb.isTemplate = false";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        query.setParameter("datefrom", from);
        query.setParameter("dateto", to);
        Long count = query.uniqueResult();
        return count;
    }

    @Override
    public Long getTotalCount() {
        String sql = "select count(*) from NoteBook nb where nb.isTemplate = false";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        Long count = query.uniqueResult();
        return count;
    }

    @Override
    public String getTableName() {
        return "notebook";
    }
}
