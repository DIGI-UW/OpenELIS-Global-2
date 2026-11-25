package org.openelisglobal.provider.service;

import org.openelisglobal.person.valueholder.Person;

/**
 * How a provider's name reads when a screen, a report or a dropdown has room
 * for only one string: the title's abbreviation, then the given name, then the
 * family name, with each part dropped cleanly when it is absent so nothing ever
 * renders a leading space or a stray comma (OGC-1223, FR-11).
 * <p>
 * Every caller goes through here rather than concatenating its own, which is
 * what let "Dr" end up inside a first name at one site and printed twice at
 * another.
 */
public final class ProviderDisplayName {

    private ProviderDisplayName() {
    }

    /** {@code Dr John Kila}, the form a person reads. */
    public static String titled(String titleAbbreviation, String firstName, String lastName) {
        return join(titleAbbreviation, firstName, lastName);
    }

    /**
     * The same name sorted by family name, for a list an administrator scans:
     * {@code Kila, Dr John}.
     */
    public static String titledFamilyFirst(String titleAbbreviation, String firstName, String lastName) {
        String given = join(titleAbbreviation, firstName);
        String family = trimToEmpty(lastName);
        if (family.isEmpty()) {
            return given;
        }
        return given.isEmpty() ? family : family + ", " + given;
    }

    /**
     * The titled name of a person, using whatever the caller resolved the code to.
     */
    public static String titled(Person person, String titleAbbreviation) {
        if (person == null) {
            return "";
        }
        return titled(titleAbbreviation, person.getFirstName(), person.getLastName());
    }

    private static String join(String... parts) {
        StringBuilder joined = new StringBuilder();
        for (String part : parts) {
            String value = trimToEmpty(part);
            if (value.isEmpty()) {
                continue;
            }
            if (joined.length() > 0) {
                joined.append(' ');
            }
            joined.append(value);
        }
        return joined.toString();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
