package org.openelisglobal.microbiology.service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.microbiology.dao.MicroPatientOriginDAO;
import org.openelisglobal.microbiology.valueholder.MicroPatientOrigin;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MicroPatientOriginConfigurationHandler implements DomainConfigurationHandler {
    private final MicroPatientOriginDAO origins;

    public MicroPatientOriginConfigurationHandler(MicroPatientOriginDAO origins) {
        this.origins = origins;
    }

    public String getDomainName() {
        return "microbiology-patient-origins";
    }

    public String getFileExtension() {
        return "csv";
    }

    public int getLoadOrder() {
        return 300;
    }

    @Transactional
    public void processConfiguration(InputStream input, String fileName) throws Exception {
        try (var rows = CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim()
                .parse(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            for (var row : rows) {
                String code = row.get("code");
                var existing = origins.getByCodes(List.of(code));
                if (!existing.isEmpty() && row.isMapped("createOnly") && Boolean.parseBoolean(row.get("createOnly")))
                    continue;
                String name = row.get("displayName"), whonet = row.get("whonetCode"), active = row.get("isActive");
                int order = Integer.parseInt(row.get("sortOrder"));
                if (code.isBlank() || code.length() > 40 || name.isBlank() || name.length() > 100 || whonet.isBlank()
                        || whonet.length() > 10 || !List.of("Y", "N").contains(active))
                    throw new IllegalArgumentException("Invalid patient origin configuration: " + code);
                var origin = existing.isEmpty() ? new MicroPatientOrigin() : existing.get(0);
                origin.setCode(code);
                origin.setDisplayName(name);
                origin.setWhonetCode(whonet);
                origin.setSortOrder(order);
                origin.setIsActive(active);
                origin.setSysUserId("1");
                if (existing.isEmpty())
                    origins.insert(origin);
                else
                    origins.update(origin);
            }
        }
    }
}
