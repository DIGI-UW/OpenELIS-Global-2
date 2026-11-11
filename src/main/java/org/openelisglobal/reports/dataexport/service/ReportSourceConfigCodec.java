package org.openelisglobal.reports.dataexport.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.springframework.stereotype.Component;

@Component
public class ReportSourceConfigCodec {
    private final ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public ReportSourceConfig read(InputStream input, Set<String> supportedSources) {
        try {
            ReportSourceConfig config = mapper.readValue(input, ReportSourceConfig.class);
            if (config == null)
                throw new IllegalArgumentException("reporting.definition.invalid");
            config.validateSources(supportedSources);
            return config;
        } catch (IOException exception) {
            throw new IllegalArgumentException("reporting.definition.invalid", exception);
        }
    }

    public String write(ReportSourceConfig config) {
        try {
            return mapper.writeValueAsString(config);
        } catch (IOException exception) {
            throw new IllegalArgumentException("reporting.definition.invalid", exception);
        }
    }
}
