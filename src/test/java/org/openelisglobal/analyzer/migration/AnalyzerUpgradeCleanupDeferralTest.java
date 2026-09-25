package org.openelisglobal.analyzer.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.filter.IgnoreChangeSetFilter;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;

public class AnalyzerUpgradeCleanupDeferralTest {

    // Published Liquibase 4.8 checksums, captured before cleanup was deferred.
    private static final Map<String, String> PUBLISHED_CLEANUP = Map.of(
            "OGC-1054-remove-direct-analyzer-profile-reference", "8:239480c6544baef5e2d4526cbc21cb1d",
            "OGC-1054-remove-analyzer-qc-rule", "8:079053e59b1f5ba05548b7e5a51b2284",
            "OGC-1054-remove-analyzer-activation-candidate", "8:796fd27c36a78ad8d4bca0a72b22d5a6",
            "OGC-1054-remove-analyzer-site-transport", "8:1eff9d0ff793e99722c29f1728a8e113",
            "OGC-1054-remove-openelis-analyzer-connection-runtime", "8:594c0ebac5bddef29c87e4fb78dd18b1",
            "097-remove-openelis-analyzer-error-queue", "8:cef90c7836ed7269f336d4c7dcfde06f",
            "098-remove-superseded-analyzer-schema", "8:174283a444a158e0c81e074710050e03");

    @Test
    public void startupDoesNotScheduleDestructiveCleanupBeforeDataTransfer() throws Exception {
        Map<String, ChangeSet> changes = startupChanges();
        IgnoreChangeSetFilter filter = new IgnoreChangeSetFilter();
        for (String id : PUBLISHED_CLEANUP.keySet()) {
            ChangeSet change = changes.get(id);
            assertNotNull("Keep published changeset identity: " + id, change);
            assertFalse("Preserve existing analyzer data until migration completes: " + id,
                    filter.accepts(change).isAccepted());
        }
    }

    @Test
    public void deferralPreservesPublishedChecksumsAndStillCreatesTargetSchema() throws Exception {
        Map<String, ChangeSet> changes = startupChanges();
        for (Map.Entry<String, String> published : PUBLISHED_CLEANUP.entrySet()) {
            assertEquals(published.getKey(), published.getValue(),
                    changes.get(published.getKey()).generateCheckSum().toString());
        }
        IgnoreChangeSetFilter filter = new IgnoreChangeSetFilter();
        for (String id : Set.of("OGC-1054-analyzer-profile-binding", "OGC-1054-analyzer-site-binding-tables",
                "OGC-1054-analyzer-site-binding-reference", "OGC-1054-analyzer-activation-record")) {
            assertNotNull(id, changes.get(id));
            assertTrue("Additive migration must remain scheduled: " + id,
                    filter.accepts(changes.get(id)).isAccepted());
        }
    }

    private Map<String, ChangeSet> startupChanges() throws Exception {
        var changelog = new XMLChangeLogSAXParser().parse("liquibase/3.5.x.x/base.xml",
                new ChangeLogParameters(), new ClassLoaderResourceAccessor());
        return changelog.getChangeSets().stream()
                .filter(change -> PUBLISHED_CLEANUP.containsKey(change.getId())
                        || change.getId().startsWith("OGC-1054-"))
                .collect(Collectors.toMap(ChangeSet::getId, Function.identity()));
    }
}
