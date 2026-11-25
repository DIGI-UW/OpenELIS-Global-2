package org.openelisglobal.orderentry.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.openelisglobal.orderentry.valueholder.PossibleMatchCandidate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class PossibleMatchCandidateDAOImpl implements PossibleMatchCandidateDAO {

    private static final int MAX_FUZZY_LENGTH = 255;

    private static final String NAME_SCORES = "similarity(lower(coalesce(pe.last_name, '')), :last) AS s_ll, "
            + "similarity(lower(coalesce(pe.first_name, '')), :first) AS s_ff, "
            + "similarity(lower(coalesce(pe.last_name, '')), :first) AS s_lf, "
            + "similarity(lower(coalesce(pe.first_name, '')), :last) AS s_fl, "
            + "(:last <> '' AND soundex(coalesce(pe.last_name, '')) = soundex(:last)) AS ph_l, "
            + "(:first <> '' AND soundex(coalesce(pe.first_name, '')) = soundex(:first)) AS ph_f";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<PossibleMatchCandidate> patientCandidates(String firstName, String lastName, LocalDate birthDate,
            String identifier, int limit) {
        String first = normalized(firstName);
        String last = normalized(lastName);
        String id = normalized(identifier);
        List<String> conditions = new ArrayList<>();
        nameConditions(conditions, first, last);
        if (birthDate != null) {
            conditions.add("(p.birth_date BETWEEN :dobFrom AND :dobTo AND ("
                    + "(:last <> '' AND (soundex(coalesce(pe.last_name, '')) = soundex(:last) "
                    + "OR soundex(coalesce(pe.first_name, '')) = soundex(:last))) "
                    + "OR (:first <> '' AND (soundex(coalesce(pe.first_name, '')) = soundex(:first) "
                    + "OR soundex(coalesce(pe.last_name, '')) = soundex(:first)))))");
        }
        if (!id.isEmpty()) {
            conditions.add("(p.national_id IS NOT NULL AND length(p.national_id) <= " + MAX_FUZZY_LENGTH
                    + " AND levenshtein(lower(p.national_id), :id) <= 1)");
        }
        if (conditions.isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "SELECT * FROM (SELECT CAST(p.id AS varchar) AS id, pe.first_name, pe.last_name, "
                + "p.birth_date, p.national_id, " + NAME_SCORES
                + " FROM clinlims.patient p JOIN clinlims.person pe ON pe.id = p.person_id WHERE "
                + String.join(" OR ", conditions)
                + ") c ORDER BY greatest(c.s_ll + c.s_ff, c.s_lf + c.s_fl) DESC LIMIT :limit";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("first", first);
        query.setParameter("last", last);
        if (birthDate != null) {
            query.setParameter("dobFrom", Timestamp.valueOf(birthDate.minusYears(1).atStartOfDay()));
            query.setParameter("dobTo", Timestamp.valueOf(birthDate.plusYears(1).plusDays(1).atStartOfDay()));
        }
        if (!id.isEmpty()) {
            query.setParameter("id", id);
        }
        query.setParameter("limit", limit);
        List<PossibleMatchCandidate> candidates = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] cells = (Object[]) row;
            PossibleMatchCandidate candidate = new PossibleMatchCandidate();
            candidate.setId((String) cells[0]);
            candidate.setFirstName((String) cells[1]);
            candidate.setLastName((String) cells[2]);
            candidate.setBirthDate(toLocalDate(cells[3]));
            candidate.setIdentifier((String) cells[4]);
            readNameScores(candidate, cells, 5);
            candidates.add(candidate);
        }
        return candidates;
    }

    @Override
    public List<PossibleMatchCandidate> facilityCandidates() {
        Query query = entityManager.createNativeQuery("SELECT CAST(o.id AS varchar), o.name, o.code, o.short_name, "
                + "o.city, o.is_active FROM clinlims.organization o WHERE o.name IS NOT NULL");
        List<PossibleMatchCandidate> candidates = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] cells = (Object[]) row;
            PossibleMatchCandidate candidate = new PossibleMatchCandidate();
            candidate.setId((String) cells[0]);
            candidate.setName((String) cells[1]);
            String code = (String) cells[2];
            candidate.setCode(code == null || code.isBlank() ? (String) cells[3] : code);
            candidate.setCity((String) cells[4]);
            candidate.setActive(!"N".equalsIgnoreCase(String.valueOf(cells[5])));
            candidates.add(candidate);
        }
        return candidates;
    }

    @Override
    public List<PossibleMatchCandidate> providerCandidates(String firstName, String lastName, int limit) {
        String first = normalized(firstName);
        String last = normalized(lastName);
        List<String> conditions = new ArrayList<>();
        nameConditions(conditions, first, last);
        if (conditions.isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "SELECT * FROM (SELECT CAST(pr.id AS varchar) AS id, pe.first_name, pe.last_name, "
                + "pe.title_code, " + NAME_SCORES
                + " FROM clinlims.provider pr JOIN clinlims.person pe ON pe.id = pr.person_id "
                + "WHERE pr.active IS DISTINCT FROM false AND (" + String.join(" OR ", conditions)
                + ")) c ORDER BY greatest(c.s_ll + c.s_ff, c.s_lf + c.s_fl) DESC LIMIT :limit";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("first", first);
        query.setParameter("last", last);
        query.setParameter("limit", limit);
        List<PossibleMatchCandidate> candidates = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] cells = (Object[]) row;
            PossibleMatchCandidate candidate = new PossibleMatchCandidate();
            candidate.setId((String) cells[0]);
            candidate.setFirstName((String) cells[1]);
            candidate.setLastName((String) cells[2]);
            candidate.setCode((String) cells[3]);
            readNameScores(candidate, cells, 4);
            candidates.add(candidate);
        }
        return candidates;
    }

    private static void nameConditions(List<String> conditions, String first, String last) {
        if (!last.isEmpty()) {
            conditions.add("lower(pe.last_name) % :last");
            conditions.add("lower(pe.first_name) % :last");
        }
        if (!first.isEmpty()) {
            conditions.add("lower(pe.first_name) % :first");
            conditions.add("lower(pe.last_name) % :first");
        }
    }

    private static void readNameScores(PossibleMatchCandidate candidate, Object[] cells, int from) {
        candidate.setLastToLast(number(cells[from]));
        candidate.setFirstToFirst(number(cells[from + 1]));
        candidate.setLastToFirst(number(cells[from + 2]));
        candidate.setFirstToLast(number(cells[from + 3]));
        candidate.setLastSoundsAlike(Boolean.TRUE.equals(cells[from + 4]));
        candidate.setFirstSoundsAlike(Boolean.TRUE.equals(cells[from + 5]));
    }

    private static double number(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0d;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().toLocalDate();
        }
        if (value instanceof Date) {
            return ((Date) value).toLocalDate();
        }
        if (value instanceof java.time.LocalDateTime) {
            return ((java.time.LocalDateTime) value).toLocalDate();
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        return null;
    }

    private static String normalized(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.length() > MAX_FUZZY_LENGTH ? trimmed.substring(0, MAX_FUZZY_LENGTH) : trimmed;
    }
}
