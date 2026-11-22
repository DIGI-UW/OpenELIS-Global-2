package org.openelisglobal.microbiology.service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.apache.commons.csv.CSVFormat;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.program.service.ProgramService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assigns configured reporting tracks to existing Programs after catalog
 * loading.
 */
@Component
public class MicroProgramReportingTrackConfigurationHandler implements DomainConfigurationHandler {
    private final ProgramService programs;
    private final DictionaryService dictionary;

    public MicroProgramReportingTrackConfigurationHandler(ProgramService programs, DictionaryService dictionary) {
        this.programs = programs;
        this.dictionary = dictionary;
    }

    public String getDomainName() {
        return "program-reporting-tracks";
    }

    public String getFileExtension() {
        return "csv";
    }

    public int getLoadOrder() {
        return 450;
    }

    @Transactional
    public void processConfiguration(InputStream input, String fileName) throws Exception {
        try (var rows = CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim()
                .parse(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            if (!rows.getHeaderMap().keySet().containsAll(java.util.Set.of("programCode", "reportingTrack")))
                throw new IllegalArgumentException("programCode and reportingTrack are required");
            var existing = programs.getAll();
            for (var row : rows) {
                var track = dictionary.getDictionaryEntryByNameAndCategoryName(row.get("reportingTrack"),
                        "Reporting track");
                if (track == null || !"Y".equals(track.getIsActive()))
                    throw new IllegalArgumentException(
                            "Unknown or inactive reporting track: " + row.get("reportingTrack"));
                for (var program : existing) {
                    if (!row.get("programCode").equalsIgnoreCase(program.getCode()))
                        continue;
                    if (java.util.Objects.equals(track.getId(), program.getReportingTrackId()))
                        continue;
                    program.setReportingTrackId(track.getId());
                    program.setSysUserId("1");
                    programs.update(program);
                }
            }
        }
    }
}
