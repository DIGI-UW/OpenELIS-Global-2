package org.openelisglobal.patient.saving;

/**
 * In the context of saving & editing by project, a request type is not the type
 * of the HttpServletRequest, but the reason the user is using the form.
 */
public enum RequestType {
    UNKNOWN, INITIAL, VERIFY, READWRITE, READONLY;

    /**
     * Convert a possible improperly cased string to one of the RequestTypes
     *
     * @param anyCase
     * @return never null; something UNKNOWN
     */
    public static RequestType valueOfAsUpperCase(String anyCase) {
        RequestType rt = null;
        if (anyCase != null) {
            rt = RequestType.valueOf(anyCase.toUpperCase());
        }
        return (rt == null) ? UNKNOWN : rt;
    }
}
