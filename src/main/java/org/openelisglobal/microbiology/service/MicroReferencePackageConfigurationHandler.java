package org.openelisglobal.microbiology.service;

import java.io.InputStream;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.springframework.stereotype.Component;

@Component
public class MicroReferencePackageConfigurationHandler implements DomainConfigurationHandler {
    private final MicroReferencePackageService packages;

    public MicroReferencePackageConfigurationHandler(MicroReferencePackageService packages) {
        this.packages = packages;
    }

    public String getDomainName() {
        return "microbiology-reference-packages";
    }

    public String getFileExtension() {
        return "zip";
    }

    public int getLoadOrder() {
        return 350;
    }

    public void processConfiguration(InputStream input, String fileName) throws Exception {
        byte[] artifact = input.readNBytes(MicroReferencePackageServiceImpl.MAX_BYTES + 1);
        if (artifact.length > MicroReferencePackageServiceImpl.MAX_BYTES) {
            throw new IllegalArgumentException("Reference package exceeds the size limit");
        }
        packages.load(artifact);
    }
}
