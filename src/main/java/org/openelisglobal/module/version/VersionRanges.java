package org.openelisglobal.module.version;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.semver4j.RangesList;
import org.semver4j.RangesListFactory;
import org.semver4j.Semver;

/**
 * Semantic versions and npm-style ranges as the module system uses them:
 * {@code requires.openelis}, {@code requires.coreApi}, {@code requires.spi},
 * {@code requires.frontendContract} and module-to-module ranges.
 *
 * <p>
 * Pre-release and build labels are dropped before a version is compared with a
 * range, so that the running {@code 3.4.0-SNAPSHOT} satisfies {@code >=3.4.0}:
 * a snapshot of a version is treated as that version for compatibility
 * purposes, which is what a module built and tested against the development
 * line expects.
 */
public final class VersionRanges {

    private static final Pattern VERSION_CORE = Pattern.compile("^v?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    private VersionRanges() {
    }

    /**
     * The version with its pre-release and build labels removed, or empty when it
     * is not a version at all.
     */
    public static Optional<Semver> coreVersion(String version) {
        if (version == null) {
            return Optional.empty();
        }
        Matcher matcher = VERSION_CORE.matcher(version.trim());
        if (!matcher.find()) {
            return Optional.empty();
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
        return Optional.of(new Semver(major + "." + minor + "." + patch));
    }

    private static final Pattern COMPARATOR = Pattern.compile("^(?:>=|<=|>|<|=|\\^|~)?v?(?:\\d+|[xX*])"
            + "(?:\\.(?:\\d+|[xX*]))?(?:\\.(?:\\d+|[xX*]))?(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?$");

    private static final Pattern PLAIN_VERSION = Pattern
            .compile("^v?\\d+(?:\\.\\d+)?(?:\\.\\d+)?" + "(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?$");

    /**
     * Whether the range is valid npm range syntax: alternatives separated by
     * {@code ||}, each a space-separated list of comparators ({@code >=1.2.0 <2},
     * {@code ^1.0}, {@code ~1.2}, {@code 1.x}, {@code *}) or a hyphen range
     * ({@code 1.2.3 - 2.0.0}). semver4j accepts almost any text, so the grammar is
     * checked here first.
     */
    public static boolean isValidRange(String range) {
        if (range == null || range.isBlank()) {
            return false;
        }
        for (String alternative : range.trim().split("\\|\\|", -1)) {
            String[] tokens = alternative.trim().split("\\s+");
            if (tokens.length == 1 && tokens[0].isEmpty()) {
                return false;
            }
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if (token.equals("-")) {
                    boolean hyphenRange = i == 1 && tokens.length == 3 && PLAIN_VERSION.matcher(tokens[0]).matches()
                            && PLAIN_VERSION.matcher(tokens[2]).matches();
                    if (!hyphenRange) {
                        return false;
                    }
                    i++;
                    continue;
                }
                if (!COMPARATOR.matcher(token).matches()) {
                    return false;
                }
            }
        }
        try {
            RangesListFactory.create(range.trim());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Whether a range carries a lower bound: a comparator other than {@code <},
     * {@code <=} or a bare {@code *}. {@code requires.openelis} must have one,
     * because the minimum tested version is what the module promises.
     */
    public static boolean hasLowerBound(String range) {
        if (!isValidRange(range)) {
            return false;
        }
        String trimmed = range.trim();
        if (trimmed.equals("*") || trimmed.equalsIgnoreCase("x")) {
            return false;
        }
        for (String alternative : trimmed.split("\\|\\|")) {
            boolean bounded = false;
            for (String comparator : alternative.trim().split("\\s+")) {
                if (comparator.isEmpty()) {
                    continue;
                }
                if (!(comparator.startsWith("<") || comparator.matches("[xX*.]+"))) {
                    bounded = true;
                }
            }
            if (!bounded) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the version (labels dropped) satisfies the range.
     *
     * @throws IllegalArgumentException when the version or the range cannot be
     *                                  parsed
     */
    public static boolean satisfies(String version, String range) {
        Semver core = coreVersion(version)
                .orElseThrow(() -> new IllegalArgumentException("not a version: '" + version + "'"));
        if (!isValidRange(range)) {
            throw new IllegalArgumentException("not a version range: '" + range + "'");
        }
        RangesList ranges = RangesListFactory.create(range.trim());
        return core.satisfies(ranges);
    }

    /**
     * Whether {@code candidate} is strictly lower than {@code reference}, both with
     * labels dropped.
     */
    public static boolean isLowerThan(String candidate, String reference) {
        Semver a = coreVersion(candidate)
                .orElseThrow(() -> new IllegalArgumentException("not a version: '" + candidate + "'"));
        Semver b = coreVersion(reference)
                .orElseThrow(() -> new IllegalArgumentException("not a version: '" + reference + "'"));
        return a.isLowerThan(b);
    }
}
