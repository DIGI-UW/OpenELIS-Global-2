package org.openelisglobal.program.util;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;

/**
 * The rules about the histopathology bench sequence that the dashboard, the
 * case view and the stage transitions all need to agree on (OGC-264).
 *
 * <p>
 * {@link PathologyStatus}'s declaration order is the single source of the bench
 * sequence (FR-2.6), so nothing here re-lists it. This class is kept free of
 * Spring and I/O so every rule is unit-testable in isolation; which stages a
 * deployment has switched off is passed in as a {@link Predicate} rather than
 * read from configuration here, so this class does not care where that setting
 * lives or how it is stored.
 */
public final class PathologyStages {

    /**
     * The spine no deployment may disable (FR-2.3): every case passes through
     * these.
     */
    private static final Set<PathologyStatus> MANDATORY = EnumSet.of(PathologyStatus.ACCESSIONED,
            PathologyStatus.GROSSING, PathologyStatus.READY_PATHOLOGIST, PathologyStatus.COMPLETED);

    /** The two stages at which nobody is working the tissue. */
    private static final Set<PathologyStatus> OFF_THE_BENCH = EnumSet.of(PathologyStatus.READY_PATHOLOGIST,
            PathologyStatus.COMPLETED);

    /**
     * {@link #ordered()} minus {@link #OFF_THE_BENCH}, computed once since neither
     * one changes at runtime.
     */
    private static final List<PathologyStatus> IN_PROGRESS = ordered().stream()
            .filter(status -> !OFF_THE_BENCH.contains(status)).collect(Collectors.toUnmodifiableList());

    private static final String DISPLAY_KEY_PREFIX = "pathology.stage.";

    private PathologyStages() {
    }

    /**
     * Every stage in bench order, exactly as {@link PathologyStatus} declares them.
     */
    public static List<PathologyStatus> ordered() {
        return List.of(PathologyStatus.values());
    }

    /**
     * The stages at which a case counts as work in progress (OGC-264, AC-4).
     *
     * <p>
     * The Pathology Dashboard's "in progress" tile and the dashboard's own stage
     * filter each spelled out their own list of statuses and the two lists were not
     * the same, so a case could sit in the filter's results while no tile counted
     * it. Both now read this one rule.
     *
     * <p>
     * {@code READY_PATHOLOGIST} is excluded because a case there is queued for a
     * pathologist and is counted on the "awaiting review" tile instead, and
     * {@code COMPLETED} because the case is finished. {@code UNDER_REVIEW} is
     * included: a pathologist has taken the case out of that queue and is working
     * it.
     */
    public static List<PathologyStatus> inProgress() {
        return IN_PROGRESS;
    }

    /** True for the four stages a deployment can never switch off (FR-2.3). */
    public static boolean isMandatory(PathologyStatus status) {
        Objects.requireNonNull(status, "status");
        return MANDATORY.contains(status);
    }

    /**
     * Whether a case actually visits this stage. A mandatory stage is enabled
     * whatever the deployment says: {@code ACCESSIONED}, {@code GROSSING},
     * {@code READY_PATHOLOGIST} and {@code COMPLETED} are the spine every case must
     * pass through, so nothing may take them out of the sequence (AC-7). Every
     * other stage follows the deployment's own setting.
     */
    public static boolean isEnabled(PathologyStatus status, Predicate<PathologyStatus> deploymentEnabled) {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(deploymentEnabled, "deploymentEnabled");
        return isMandatory(status) || deploymentEnabled.test(status);
    }

    /**
     * {@link #ordered()} filtered down to the stages this deployment actually uses.
     */
    public static List<PathologyStatus> enabled(Predicate<PathologyStatus> deploymentEnabled) {
        Objects.requireNonNull(deploymentEnabled, "deploymentEnabled");
        return ordered().stream().filter(status -> isEnabled(status, deploymentEnabled))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * The React Intl message id for a stage's label. Derived from the constant name
     * rather than stored on the enum, because the enum has no room for a per-locale
     * label: the English message bundle holds the label the screen renders, and the
     * enum's own display string stays the fallback the server keeps serving.
     */
    public static String displayKey(PathologyStatus status) {
        Objects.requireNonNull(status, "status");
        return DISPLAY_KEY_PREFIX + toLowerCamelCase(status.name());
    }

    private static String toLowerCamelCase(String constantName) {
        String[] words = constantName.split("_");
        StringBuilder camelCase = new StringBuilder(words[0].toLowerCase());
        for (int i = 1; i < words.length; i++) {
            String word = words[i].toLowerCase();
            camelCase.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return camelCase.toString();
    }
}
