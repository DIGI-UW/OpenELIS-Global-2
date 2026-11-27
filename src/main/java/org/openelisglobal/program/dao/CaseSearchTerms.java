package org.openelisglobal.program.dao;

import java.util.Locale;
import org.hibernate.query.Query;

/**
 * The dashboards' search clause, shared by the three case DAOs: a case whose
 * lab number starts with the term, or, for a term of at least
 * {@link #MIN_NAME_PART} characters, whose patient's first or last name holds
 * it, in any letter case. The patterns are escaped with {@code !}, so a typed
 * {@code %} or {@code _} matches only itself.
 */
public final class CaseSearchTerms {

    /**
     * The shortest term matched against a part of a name; a shorter one matches lab
     * numbers from their start only.
     */
    public static final int MIN_NAME_PART = 2;

    private CaseSearchTerms() {
    }

    /**
     * The clause for the case alias of a query, to bind with {@link #bind}. The
     * name leg is an {@code exists}, so a case with no patient still matches by lab
     * number.
     */
    public static String clause(String alias, String term) {
        String clause = "(lower(" + alias + ".sample.accessionNumber) like :start escape '!'";
        if (matchesNames(term)) {
            clause += " or exists (select sh.id from SampleHuman sh, Patient pt where sh.sampleId = " + alias
                    + ".sample.id and pt.id = sh.patientId and (lower(pt.person.firstName) like :part escape '!'"
                    + " or lower(pt.person.lastName) like :part escape '!'))";
        }
        return clause + ")";
    }

    /** Binds the parameters of {@link #clause} for the same term. */
    public static void bind(Query<?> query, String term) {
        query.setParameter("start", escape(term) + "%");
        if (matchesNames(term)) {
            query.setParameter("part", "%" + escape(term) + "%");
        }
    }

    private static boolean matchesNames(String term) {
        return term.length() >= MIN_NAME_PART;
    }

    private static String escape(String term) {
        return term.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
