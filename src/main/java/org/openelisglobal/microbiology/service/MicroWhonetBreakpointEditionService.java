package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointRuleDAO;
import org.openelisglobal.microbiology.dao.MicroBreakpointStandardDAO;
import org.openelisglobal.microbiology.dao.MicroWhonetBreakpointDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointRule;
import org.openelisglobal.microbiology.valueholder.MicroBreakpointStandard;
import org.openelisglobal.microbiology.valueholder.MicroWhonetBreakpoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Offers the recent CLSI and EUCAST years of WHONET's breakpoint table as
 * breakpoint editions a laboratory can activate.
 */
@Service
@Transactional
public class MicroWhonetBreakpointEditionService {
    private static final List<String> GUIDELINES = List.of("CLSI", "EUCAST");
    private static final int YEARS = 3;
    private static final String ACTOR = "1";
    private static final Pattern PACKAGE_FILE = Pattern.compile("(?:^|/)pkg--.+?--(.+?)--[^/]*$");
    private static final Pattern NUMBER = Pattern.compile("\\d+(\\.\\d+)?");
    private static final Pattern RANGE = Pattern.compile("(\\d+(?:\\.\\d+)?)-(\\d+(?:\\.\\d+)?)");

    private final MicroWhonetBreakpointDAO whonetBreakpoints;
    private final MicroBreakpointStandardDAO standards;
    private final MicroBreakpointRuleDAO rules;
    private final MicroAntibioticDAO antibiotics;

    public MicroWhonetBreakpointEditionService(MicroWhonetBreakpointDAO whonetBreakpoints,
            MicroBreakpointStandardDAO standards, MicroBreakpointRuleDAO rules, MicroAntibioticDAO antibiotics) {
        this.whonetBreakpoints = whonetBreakpoints;
        this.standards = standards;
        this.rules = rules;
        this.antibiotics = antibiotics;
    }

    /**
     * A year whose rows an edition already holds gets no new edition.
     *
     * @return antibiotic codes OpenELIS lacks; their rules are left out
     */
    public List<String> publish(String fileName) {
        String latest = whonetBreakpoints.latestYear(GUIDELINES);
        if (latest == null) {
            return List.of();
        }
        String fromYear = String.valueOf(Integer.parseInt(latest) - YEARS + 1);
        Map<String, List<MicroWhonetBreakpoint>> editions = new TreeMap<>();
        for (MicroWhonetBreakpoint row : whonetBreakpoints.findHumanFromYear(GUIDELINES, fromYear)) {
            // Left in the session, these rows would be dirty-checked on every rule insert.
            whonetBreakpoints.evict(row);
            editions.computeIfAbsent(row.getGuidelines() + " " + row.getYear(), key -> new ArrayList<>()).add(row);
        }
        Matcher packageFile = PACKAGE_FILE.matcher(fileName == null ? "" : fileName);
        String packageVersion = packageFile.find() ? packageFile.group(1) : "local";
        Map<String, Optional<MicroAntibiotic>> byCode = new HashMap<>();
        TreeSet<String> unknownAntibiotics = new TreeSet<>();
        for (List<MicroWhonetBreakpoint> rows : editions.values()) {
            String authority = rows.get(0).getGuidelines();
            String version = rows.get(0).getYear() + "-" + packageVersion;
            String hash = hash(rows);
            if (standards.findByAuthorityAndConfigurationHash(authority, hash).isPresent()) {
                continue;
            }
            if (standards.findByAuthorityAndVersion(authority, version).isPresent()) {
                throw new IllegalArgumentException("Breakpoint edition " + authority + " " + version
                        + " is already loaded with different rules; a correction needs a new package version");
            }
            MicroBreakpointStandard standard = new MicroBreakpointStandard();
            standard.setAuthority(authority);
            standard.setVersion(version);
            standard.setLifecycleStatus("LOADED");
            standard.setIsActive("N");
            standard.setConfigurationHash(hash);
            standard.setLastUpdatedBy(ACTOR);
            standard.setSysUserId(ACTOR);
            String standardId = standards.insert(standard);
            for (MicroWhonetBreakpoint row : rows) {
                if (row.getWhonetAbxCode() == null || row.getOrganismCodeType() == null) {
                    continue;
                }
                Optional<MicroAntibiotic> antibiotic = byCode.computeIfAbsent(row.getWhonetAbxCode(),
                        antibiotics::findByWhonetCodeIgnoreCase);
                if (antibiotic.isEmpty()) {
                    unknownAntibiotics.add(row.getWhonetAbxCode());
                    continue;
                }
                MicroBreakpointRule rule = rule(row, standardId, antibiotic.get().getId());
                rules.insert(rule);
                rules.evict(rule);
            }
        }
        return List.copyOf(unknownAntibiotics);
    }

    private static MicroBreakpointRule rule(MicroWhonetBreakpoint row, String standardId, String antibioticId) {
        boolean disk = "DISK".equals(row.getTestMethod());
        MicroBreakpointRule rule = new MicroBreakpointRule();
        rule.setStandardId(standardId);
        rule.setAntibioticId(antibioticId);
        rule.setBreakpointType(disk ? "ZONE" : "MIC");
        rule.setMethod(rule.getBreakpointType());
        rule.setUnits(disk ? "mm" : "mg/L");
        rule.setDiskContent(disk && row.getPotency() != null ? row.getPotency().replace("µg", " mcg") : null);
        rule.setOrganismCodeType(row.getOrganismCodeType());
        rule.setOrganismCode(row.getOrganismCode());
        rule.setSiteOfInfection(row.getSiteOfInfection());
        rule.setSusceptibleValue(number(row.getS()));
        rule.setResistantValue(number(row.getR()));
        // WHONET writes R as the first resistant value: MIC at or above it, zone at or
        // below it.
        rule.setResistantInclusive(true);
        Matcher range = RANGE.matcher(row.getI() == null ? "" : row.getI());
        if (range.matches()) {
            rule.setIntermediateLowerValue(new BigDecimal(range.group(1)));
            rule.setIntermediateUpperValue(new BigDecimal(range.group(2)));
        } else if (number(row.getI()) != null) {
            rule.setIntermediateLowerValue(number(row.getI()));
            rule.setIntermediateUpperValue(number(row.getI()));
        }
        rule.setNotes(row.getComments());
        rule.setSeeded(true);
        rule.setLocallyCustomized(false);
        rule.setLastUpdatedBy(ACTOR);
        rule.setSysUserId(ACTOR);
        rule.setIsActive("Y");
        return rule;
    }

    private static BigDecimal number(String value) {
        return value != null && NUMBER.matcher(value).matches() ? new BigDecimal(value) : null;
    }

    /** The same rows give the same hash whatever their order in the file. */
    private static String hash(List<MicroWhonetBreakpoint> rows) {
        List<String> lines = new ArrayList<>();
        for (MicroWhonetBreakpoint row : rows) {
            lines.add(String.join("\t", text(row.getGuidelines()), text(row.getYear()), text(row.getTestMethod()),
                    text(row.getPotency()), text(row.getOrganismCode()), text(row.getOrganismCodeType()),
                    text(row.getSiteOfInfection()), text(row.getWhonetAbxCode()), text(row.getR()), text(row.getI()),
                    text(row.getSdd()), text(row.getS()), text(row.getComments())));
        }
        lines.sort(null);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(String.join("\n", lines).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
