package org.openelisglobal.addon.service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.openelisglobal.addon.dao.AddonAccessLogDAO.Entry;

/**
 * Records every authenticated request to the add-on API, and serves the record
 * to administrators. The controller names what a request was about with
 * {@link #subject}; the agent and the purpose are what the add-on reports in
 * {@code X-Addon-Actor} and {@code X-Addon-Purpose}.
 */
public interface AddonAccessLogService {

    String SUBJECT_TYPE = "addon.accessLog.subjectType";
    String SUBJECT = "addon.accessLog.subject";
    String ROWS = "addon.accessLog.rows";

    /** Types of {@code subject}. */
    String ORDER = "order";
    String PATIENT = "patient";
    String QUERY = "query";
    String NONE = "none";

    static void subject(HttpServletRequest request, String type, String value) {
        request.setAttribute(SUBJECT_TYPE, type);
        request.setAttribute(SUBJECT, value);
    }

    static void rows(HttpServletRequest request, int rows) {
        request.setAttribute(ROWS, rows);
    }

    void record(Entry entry);

    List<Entry> find(String addonId, Instant from, Instant to, String actor, String subject, int limit);

    /**
     * What a request was about (a lab number, a patient id, a search with a
     * patient's name in any script): control characters dropped, at most
     * {@code max} code points; null when nothing is left.
     */
    static String cleanText(String value, int max) {
        if (value == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        value.codePoints().filter(c -> !Character.isISOControl(c)).limit(max).forEach(out::appendCodePoint);
        String s = out.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * For what an add-on reports in a header: keeps only printable ASCII, at most
     * {@code max} characters; null when nothing is left.
     */
    static String clean(String value, int max) {
        if (value == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c >= 0x20 && c < 0x7f) {
                out.append(c);
            }
            if (out.length() == max) {
                break;
            }
        }
        String s = out.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
