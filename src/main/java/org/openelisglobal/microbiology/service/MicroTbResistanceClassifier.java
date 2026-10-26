package org.openelisglobal.microbiology.service;

import java.util.*;

/**
 * Classification uses configured drug roles and validated evidence, never
 * catalog labels.
 */
public final class MicroTbResistanceClassifier {
    public enum Role {
        NONE, RIFAMPICIN, ISONIAZID, OTHER_FIRST_LINE, FLUOROQUINOLONE, XDR_GROUP_A
    }

    public enum Protocol {
        GENERAL, TB, NTM
    }

    public enum Category {
        NOT_APPLICABLE, NTM, INCOMPLETE, DISCORDANT, PAN_SUSCEPTIBLE, MONO_RESISTANT, POLY_RESISTANT, RR, MDR, PRE_XDR,
        XDR
    }

    public record Drug(String id, Role role, String interpretation) {
    }

    private MicroTbResistanceClassifier() {
    }

    public static Category classify(List<Drug> drugs) {
        if (drugs.stream().anyMatch(d -> "DISCORDANT".equals(d.interpretation())))
            return Category.DISCORDANT;
        boolean rif = resistant(drugs, Role.RIFAMPICIN), inh = resistant(drugs, Role.ISONIAZID);
        // WHO definitions: pre-XDR includes RR-TB even when isoniazid resistance is
        // absent.
        if (rif && resistant(drugs, Role.FLUOROQUINOLONE))
            return resistant(drugs, Role.XDR_GROUP_A) ? Category.XDR : Category.PRE_XDR;
        if (rif && inh)
            return Category.MDR;
        if (rif)
            return Category.RR;
        var firstLine = drugs.stream()
                .filter(d -> Set.of(Role.RIFAMPICIN, Role.ISONIAZID, Role.OTHER_FIRST_LINE).contains(d.role()))
                .toList();
        if (firstLine.stream().noneMatch(d -> d.role() == Role.RIFAMPICIN)
                || firstLine.stream().noneMatch(d -> d.role() == Role.ISONIAZID)
                || firstLine.stream().anyMatch(d -> !Set.of("SUSCEPTIBLE", "RESISTANT").contains(d.interpretation())))
            return Category.INCOMPLETE;
        long resistant = firstLine.stream().filter(d -> "RESISTANT".equals(d.interpretation())).count();
        return resistant == 0 ? Category.PAN_SUSCEPTIBLE
                : resistant == 1 ? Category.MONO_RESISTANT : Category.POLY_RESISTANT;
    }

    private static boolean resistant(List<Drug> drugs, Role role) {
        return drugs.stream().anyMatch(d -> d.role() == role && "RESISTANT".equals(d.interpretation()));
    }
}
