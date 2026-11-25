package org.openelisglobal.terminology;

/**
 * A code in a code system. The system is a canonical URI or any identifier a
 * catalog owner chose; a terminology source and its canonical URI name the same
 * system.
 */
public record Coding(String system, String code, String display) {

    public Coding(String system, String code) {
        this(system, code, null);
    }

    public static Coding loinc(String code) {
        return new Coding(TerminologySystems.LOINC, code);
    }

    /**
     * Whether both name the same concept: the same system, and the same code
     * without case or surrounding space.
     */
    public boolean sameAs(Coding other) {
        String mine = TerminologySystems.systemOf(system);
        return other != null && mine != null && mine.equals(TerminologySystems.systemOf(other.system)) && code != null
                && other.code != null && !code.isBlank() && code.trim().equalsIgnoreCase(other.code.trim());
    }
}
