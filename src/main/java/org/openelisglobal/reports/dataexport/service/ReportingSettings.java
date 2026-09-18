package org.openelisglobal.reports.dataexport.service;

import java.nio.file.Path;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReportingSettings {
    @Value("${reporting.export.max-days:90}")
    private int maxDays;
    @Value("${reporting.export.max-active:5}")
    private int maxActive;
    @Value("${reporting.export.retention-days:7}")
    private int retentionDays;
    @Value("${reporting.export.timezone:}")
    private String timezone;
    @Value("${reporting.export.directory:${java.io.tmpdir}/openelis-reporting}")
    private String directory;

    public int maxDays() {
        return Math.max(1, maxDays);
    }

    public int maxActive() {
        return Math.max(1, maxActive);
    }

    public int retentionDays() {
        return Math.max(1, retentionDays);
    }

    public ZoneId zone() {
        return timezone.isBlank() ? ZoneId.systemDefault() : ZoneId.of(timezone);
    }

    public Path directory() {
        return Path.of(directory).toAbsolutePath().normalize();
    }
}
