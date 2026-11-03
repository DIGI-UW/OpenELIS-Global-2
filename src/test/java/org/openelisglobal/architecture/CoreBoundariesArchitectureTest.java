package org.openelisglobal.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.runner.RunWith;

/**
 * Boundaries between the core's feature packages (the direct sub-packages of
 * {@code org.openelisglobal}), so that the module system's extension points
 * stay narrow and features stay extractable (design section 9.11).
 *
 * <p>
 * Every rule is frozen: the violations present when the rule was introduced are
 * recorded in {@code src/test/resources/archunit_store} and tolerated; a pull
 * request that adds a violation fails, one that removes violations may update
 * the store ({@code freeze.store.default.allowStoreUpdate}). The stored counts
 * are the coupling baseline the Phase 0 decision checks ask for.
 *
 * <p>
 * The slice-cycle rule of the design is measured
 * ({@link #countFeatureCycles()}) but not frozen: ArchUnit caps cycle detection
 * at 100 cycles per run and the feature packages form one large cycle cluster
 * today, so the set of reported cycles shifts with unrelated changes and a
 * frozen version would fail pull requests at random. It becomes a gate once the
 * cluster has been broken up feature by feature.
 */
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "org.openelisglobal", importOptions = { ImportOption.DoNotIncludeTests.class,
        ImportOption.DoNotIncludeJars.class })
public class CoreBoundariesArchitectureTest {

    private static final String CORE_ROOT = "org.openelisglobal.";

    /**
     * Packages every feature may use: shared infrastructure and the module runtime,
     * which is the only consumer of seams.
     */
    private static final String[] SHARED_FEATURES = { "common", "spring", "hibernate", "module", "config", "security",
            "internationalization", "audittrail", "login", "systemuser" };

    /**
     * The cycle rule as the design states it, evaluated by
     * {@code countFeatureCycles} for the baseline report only.
     */
    static ArchRule featureSlicesAreFreeOfCycles() {
        return slices().matching("org.openelisglobal.(*)..").should().beFreeOfCycles();
    }

    /**
     * Reports the number of feature cycles ArchUnit finds (decision check D5);
     * never fails. The count is capped by {@code cycles.maxNumberOfCyclesToDetect}
     * (100 by default).
     */
    @ArchTest
    static void countFeatureCycles(JavaClasses classes) {
        EvaluationResult result = featureSlicesAreFreeOfCycles().evaluate(classes);
        long cycles = result.getFailureReport().getDetails().stream()
                .filter(detail -> detail.startsWith("Cycle detected")).count();
        System.out.println("[architecture] feature cycles detected (capped): " + cycles);
    }

    @ArchTest
    static final ArchRule daosAreUsedOnlyByTheirOwnFeature = FreezingArchRule
            .freeze(classes().that().resideInAnyPackage("org.openelisglobal.*.dao..", "org.openelisglobal.*.daoimpl..")
                    .should(onlyBeDependedOnByTheirOwnFeature()))
            .as("a feature's DAOs are used only inside that feature; other features use its services");

    @ArchTest
    static final ArchRule controllersDoNotCallDaos = FreezingArchRule
            .freeze(noClasses().that().resideInAnyPackage("org.openelisglobal.*.controller..").should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("org.openelisglobal.*.dao..", "org.openelisglobal.*.daoimpl.."))
            .as("controllers talk to services, never to DAOs");

    static String featureOf(JavaClass javaClass) {
        String name = javaClass.getPackageName();
        if (!name.startsWith(CORE_ROOT)) {
            return name;
        }
        String rest = name.substring(CORE_ROOT.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }

    static boolean isSharedFeature(String feature) {
        for (String shared : SHARED_FEATURES) {
            if (shared.equals(feature)) {
                return true;
            }
        }
        return false;
    }

    static ArchCondition<JavaClass> onlyBeDependedOnByTheirOwnFeature() {
        return new ArchCondition<JavaClass>("only be depended on by classes of their own feature") {
            @Override
            public void check(JavaClass dao, ConditionEvents events) {
                String feature = featureOf(dao);
                if (isSharedFeature(feature)) {
                    return;
                }
                for (Dependency dependency : dao.getDirectDependenciesToSelf()) {
                    JavaClass origin = dependency.getOriginClass();
                    String originFeature = featureOf(origin);
                    if (originFeature.equals(feature) || isSharedFeature(originFeature)) {
                        continue;
                    }
                    events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                }
            }
        };
    }
}
