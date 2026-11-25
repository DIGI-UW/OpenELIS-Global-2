package org.openelisglobal.labelpreset.service;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.apache.commons.csv.CSVFormat;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.labelpreset.form.LabelPresetForm;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LabelPresetConfigurationHandler implements DomainConfigurationHandler {
    private final LabelPresetService presets;

    public LabelPresetConfigurationHandler(LabelPresetService presets) {
        this.presets = presets;
    }

    public String getDomainName() {
        return "label-presets";
    }

    public String getFileExtension() {
        return "csv";
    }

    public int getLoadOrder() {
        return 300;
    }

    @Transactional
    public void processConfiguration(InputStream input, String fileName) throws Exception {
        try (var factory = Validation.byDefaultProvider().configure()
                .messageInterpolator(new org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator())
                .buildValidatorFactory();
                var rows = CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim()
                        .parse(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            for (var row : rows) {
                String name = row.get("name");
                var existing = presets.list(null, null).stream().filter(p -> p.getName().equalsIgnoreCase(name))
                        .findFirst();
                if (existing.isPresent() && row.isMapped("createOnly") && Boolean.parseBoolean(row.get("createOnly")))
                    continue;
                var form = new LabelPresetForm();
                form.setName(name);
                form.setHeightMm(Integer.valueOf(row.get("heightMm")));
                form.setWidthMm(Integer.valueOf(row.get("widthMm")));
                form.setBarcodeType(BarcodeType.valueOf(row.get("barcodeType")));
                form.setPrintsPerOrder(Boolean.valueOf(row.get("printsPerOrder")));
                form.setPrintsPerSample(Boolean.valueOf(row.get("printsPerSample")));
                form.setPrintsPerContainer(Boolean.valueOf(row.get("printsPerContainer")));
                form.setDefaultPerContainer(Integer.valueOf(row.get("defaultPerContainer")));
                form.setMaxPerContainer(Integer.valueOf(row.get("maxPerContainer")));
                var violations = factory.getValidator().validate(form);
                if (!violations.isEmpty())
                    throw new ConstraintViolationException(violations);
                if (existing.isPresent())
                    presets.update(existing.get().getId(), form, "1");
                else
                    presets.create(form, "1");
            }
        }
    }
}
