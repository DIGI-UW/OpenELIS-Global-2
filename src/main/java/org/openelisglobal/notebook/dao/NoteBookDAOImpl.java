package org.openelisglobal.notebook.dao;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Override
    public NoteBook findParentTemplate(Integer entryId) {
        // First try: Find a parent template directly (legacy behavior for entries
        // linked to templates)
        String hql = "select nb from NoteBook nb join nb.entries e where e.id = :entryId and nb.isTemplate = true";
        Query<NoteBook> query = entityManager.unwrap(Session.class).createQuery(hql, NoteBook.class);
        query.setParameter("entryId", entryId);
        List<NoteBook> results = query.list();
        if (!results.isEmpty()) {
            return results.get(0);
        }

        // Second try: Find if entry is linked to a child instance, then get that
        // child's parent template
        // This handles entries created under child instances (e.g., "Demo - Lab 4"
        // entries)
        String hql2 = "select child.parentNotebook from NoteBook child join child.entries e "
                + "where e.id = :entryId and child.isTemplate = false and child.parentNotebook is not null";
        Query<NoteBook> query2 = entityManager.unwrap(Session.class).createQuery(hql2, NoteBook.class);
        query2.setParameter("entryId", entryId);
        List<NoteBook> results2 = query2.list();
        return results2.isEmpty() ? null : results2.get(0);
    }

    @Override
    public List<NoteBook> findChildrenByParentId(Integer parentId) {
        String hql = "FROM NoteBook nb WHERE nb.parentNotebook.id = :parentId ORDER BY nb.title ASC";
        Query<NoteBook> query = entityManager.unwrap(Session.class).createQuery(hql, NoteBook.class);
        query.setParameter("parentId", parentId);
        return query.list();
    }

    @Override
    public List<NoteBook> findAllParentTemplates() {
        String hql = "FROM NoteBook nb WHERE nb.isTemplate = true AND nb.parentNotebook IS NULL ORDER BY nb.title ASC";
        Query<NoteBook> query = entityManager.unwrap(Session.class).createQuery(hql, NoteBook.class);
        return query.list();
    }

    @Override
    public Map<Integer, Long> countEntriesForChildren(List<Integer> childIds) {
        if (childIds == null || childIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String hql = "SELECT nb.id, COUNT(e) FROM NoteBook nb JOIN nb.entries e WHERE nb.id IN :childIds GROUP BY nb.id";
        Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(hql, Object[].class);
        query.setParameterList("childIds", childIds);

        Map<Integer, Long> result = new HashMap<>();
        for (Object[] row : query.list()) {
            result.put((Integer) row[0], (Long) row[1]);
        }
        return result;
    }

    @Override
    public Long countEntriesForParent(Integer parentId) {
        String hql = "SELECT COUNT(e) FROM NoteBook nb JOIN nb.entries e " + "WHERE nb.parentNotebook.id = :parentId";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql, Long.class);
        query.setParameter("parentId", parentId);
        Long count = query.uniqueResult();
        return count != null ? count : 0L;
    }
}
