package org.openelisglobal.module.runtime;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;
import org.junit.Test;
import org.openelisglobal.spi.report.ReportOutput;
import org.openelisglobal.spi.report.ReportProvider;
import org.openelisglobal.spi.report.ReportRequest;

/** Report ids: unique across modules, and never one the core ships itself. */
public class ExtensionRegistryTest {

    private static ReportProvider provider(String... ids) {
        return new ReportProvider() {
            @Override
            public Set<String> reportIds() {
                return Set.of(ids);
            }

            @Override
            public ReportOutput run(ReportRequest request) {
                return ReportOutput.pdf(new byte[0], "report.pdf");
            }
        };
    }

    @Test
    public void aModuleCannotTakeOverACoreReportId() {
        ExtensionRegistry registry = new ExtensionRegistry();
        try {
            registry.register(ReportProvider.class, "alpha", provider("alphaSummary", "patientARV1"), 0);
            fail("expected the core report id to be refused");
        } catch (ModuleStartupException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("'patientARV1' is a core report"));
            assertTrue(e.getMessage(), e.getMessage().contains("alpha"));
        }
        assertTrue(registry.reportProvider("alphaSummary").isEmpty());
    }

    @Test
    public void twoModulesCannotShareAReportIdButOwnIdsAreAccepted() {
        ExtensionRegistry registry = new ExtensionRegistry();
        registry.register(ReportProvider.class, "alpha", provider("alphaSummary"), 0);
        try {
            registry.register(ReportProvider.class, "beta", provider("alphaSummary"), 0);
            fail("expected the duplicate id to be refused");
        } catch (ModuleStartupException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("provided by both module 'alpha' and module 'beta'"));
        }
        assertTrue(registry.reportProvider("alphaSummary").isPresent());
    }
}
