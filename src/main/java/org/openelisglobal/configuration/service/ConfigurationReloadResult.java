package org.openelisglobal.configuration.service;

import java.util.Collections;
import java.util.List;

public record ConfigurationReloadResult(List<ConfigurationReloadFileResult> files) {

    public ConfigurationReloadResult {
        files = files == null ? Collections.emptyList() : List.copyOf(files);
    }

    public boolean hasErrors() {
        return files.stream().anyMatch(file -> file.status() == ConfigurationReloadFileResult.Status.ERROR);
    }
}
