package org.openelisglobal.reports.dataexport.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import org.openelisglobal.common.util.UserContextHolder;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.reportdefinition.service.ReportDefinitionService;
import org.openelisglobal.reportdefinition.valueholder.ReportDefinition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads reporting-sources/*.json through the existing configuration
 * initializer.
 */
@Component
public class ReportingSourceConfigurationHandler implements DomainConfigurationHandler {
    private final ReportingCatalogService catalog;
    private final ReportDefinitionService definitions;
    private final ReportSourceConfigCodec codec;
    private final UserContextHolder userContext;

    public ReportingSourceConfigurationHandler(ReportingCatalogService catalog, ReportDefinitionService definitions,
            ReportSourceConfigCodec codec, UserContextHolder userContext) {
        this.catalog = catalog;
        this.definitions = definitions;
        this.codec = codec;
        this.userContext = userContext;
    }

    @Override
    public String getDomainName() {
        return "reporting-sources";
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream input, String fileName) {
        var configuration = catalog.readDefinition(input);
        var existing = definitions.getAllMatching("id", configuration.id());
        ReportDefinition stored = existing.isEmpty() ? new ReportDefinition() : existing.get(0);
        if (!existing.isEmpty()) {
            if (!"CSV_SOURCE".equals(stored.getReportType()))
                throw new IllegalArgumentException("reporting.definition.identityConflict");
            var previous = codec.read(
                    new ByteArrayInputStream(stored.getDefinitionJson().getBytes(StandardCharsets.UTF_8)),
                    java.util.Set.of(configuration.source()));
            if (configuration.equals(previous) && Boolean.TRUE.equals(stored.getIsActive()))
                return;
            if (configuration.version() <= previous.version())
                throw new IllegalArgumentException("reporting.definition.versionConflict");
        }
        String actor = userContext.getCurrentSysUserId();
        Timestamp now = Timestamp.from(Instant.now());
        if (existing.isEmpty()) {
            stored.setId(configuration.id());
            stored.setCreatedBy(actor);
            stored.setCreatedDate(now);
        }
        stored.setName(configuration.label());
        stored.setCategory("Data export");
        stored.setDefinitionJson(codec.write(configuration));
        stored.setReportType("CSV_SOURCE");
        stored.setIsActive(true);
        stored.setIsPublic(true);
        stored.setUpdatedBy(actor);
        stored.setSysUserId(actor);
        stored.setLastupdated(now);
        if (existing.isEmpty())
            definitions.insert(stored);
        else
            definitions.update(stored);
    }
}
