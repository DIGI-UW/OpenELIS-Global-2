package org.openelisglobal.program.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.domain.Domain;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.test.valueholder.TestSection;

/**
 * Programs V2 (OGC-781) rules shared by the admin list, the order-entry picker
 * feed and the per-user program filter, so every reader agrees on what
 * "active", "serves this lab unit" and "belongs to this domain" mean.
 */
public final class ProgramPickerRules {

    /** Numeric ids sort numerically; anything else falls back to text order. */
    public static final Comparator<String> ID_ORDER = (left, right) -> {
        try {
            return Integer.compare(Integer.parseInt(left.trim()), Integer.parseInt(right.trim()));
        } catch (NumberFormatException e) {
            return left.compareTo(right);
        }
    };

    private ProgramPickerRules() {
    }

    /**
     * Deactivated programs carry {@code is_active = 'N'}; anything else is live.
     */
    public static boolean isActive(Program program) {
        return program != null && !"N".equalsIgnoreCase(program.getIsActive());
    }

    /**
     * FR-6: a program is offerable for an order when it belongs to the order's
     * domain. A null order domain (caller does not know it) accepts every program,
     * and a program whose stored domain cannot be read is offered everywhere rather
     * than nowhere.
     */
    public static boolean offerableForDomain(Program program, Domain orderDomain) {
        if (orderDomain == null) {
            return true;
        }
        Domain programDomain = Domain.fromRaw(program.getDomain());
        return programDomain == null || programDomain == orderDomain;
    }

    /**
     * The lab units a program is run in: the {@code program_lab_unit} junction
     * rows, or the legacy single {@code test_section_id} while a row has not been
     * re-saved through the many-to-many yet.
     */
    public static List<String> labUnitIds(Program program) {
        Set<String> ids = new LinkedHashSet<>();
        if (program.getLabUnits() != null) {
            program.getLabUnits().stream().map(TestSection::getId).filter(Objects::nonNull).forEach(ids::add);
        }
        if (ids.isEmpty() && program.getTestSection() != null && program.getTestSection().getId() != null) {
            ids.add(program.getTestSection().getId());
        }
        return ids.stream().sorted(ID_ORDER).collect(Collectors.toList());
    }

    /**
     * A program without any lab unit is offered to every reception user (the
     * behaviour the single-FK picker always had); otherwise at least one of its
     * units must be one the user works in.
     */
    public static boolean servesAnyLabUnit(Program program, Collection<String> userLabUnitIds) {
        List<String> ids = labUnitIds(program);
        if (ids.isEmpty()) {
            return true;
        }
        return userLabUnitIds != null && ids.stream().anyMatch(userLabUnitIds::contains);
    }

    /**
     * The legacy single FK mirrors the lowest-id lab unit so older readers keep
     * resolving one.
     */
    public static TestSection firstLabUnit(Collection<TestSection> labUnits) {
        if (labUnits == null) {
            return null;
        }
        return labUnits.stream().filter(unit -> unit != null && unit.getId() != null)
                .min(Comparator.comparing(TestSection::getId, ID_ORDER)).orElse(null);
    }
}
