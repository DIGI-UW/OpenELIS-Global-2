package org.openelisglobal.module.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.openelisglobal.common.util.Versioning;
import org.openelisglobal.module.api.OpenElisModule;
import org.openelisglobal.spi.ExtensionPoint;

/**
 * The versions a module's {@code requires} section is checked against: the
 * running OpenELIS version (from the build properties) and the
 * {@code openelis-module-api} and {@code openelis-spi} versions compiled into
 * the WAR (from the Maven descriptors inside those JARs).
 */
public final class ContractVersions {

    private final String openelis;
    private final String coreApi;
    private final String spi;

    public ContractVersions(String openelis, String coreApi, String spi) {
        this.openelis = openelis;
        this.coreApi = coreApi;
        this.spi = spi;
    }

    /** The versions of the running application. */
    public static ContractVersions running() {
        return new ContractVersions(new Versioning().getReleaseNumber(),
                mavenVersion(OpenElisModule.class, "org.openelisglobal", "openelis-module-api"),
                mavenVersion(ExtensionPoint.class, "org.openelisglobal", "openelis-spi"));
    }

    static String mavenVersion(Class<?> anchor, String groupId, String artifactId) {
        String resource = "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        try (InputStream in = anchor.getResourceAsStream(resource)) {
            if (in == null) {
                return "0.0.0";
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("version", "0.0.0");
        } catch (IOException e) {
            return "0.0.0";
        }
    }

    public String openelis() {
        return openelis;
    }

    public String coreApi() {
        return coreApi;
    }

    public String spi() {
        return spi;
    }

    @Override
    public String toString() {
        return "openelis " + openelis + ", coreApi " + coreApi + ", spi " + spi;
    }
}
