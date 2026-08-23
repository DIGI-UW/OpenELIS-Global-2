package org.openelisglobal.analyzer.contract;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class AnalyzerConnectionProbeCutoverContractTest {

    private static final Path CONTROLLER_ROOT = Path.of("src", "main", "java", "org", "openelisglobal", "analyzer",
            "controller");

    @Test
    public void callerSuppliedTransportProbePathIsAbsent() throws Exception {
        Path analyzerController = CONTROLLER_ROOT.resolve("AnalyzerRestController.java");
        Path probeController = CONTROLLER_ROOT.resolve("AnalyzerConnectionProbeRestController.java");

        assertTrue("missing analyzer controller", Files.isRegularFile(analyzerController));
        assertTrue("missing analyzer-scoped probe controller", Files.isRegularFile(probeController));

        String analyzerSource = Files.readString(analyzerController);
        String probeSource = Files.readString(probeController);
        for (String removed : new String[] { "/api/test-connectivity", "testConnectivityViaBridge",
                "callBridgeTestConnectivity", "testFileViaBridge", "testSerialViaBridge",
                "testTcpAnalyzerConnection" }) {
            assertFalse("superseded raw probe path remains: " + removed, analyzerSource.contains(removed));
            assertFalse("superseded raw probe path remains: " + removed, probeSource.contains(removed));
        }
    }
}
