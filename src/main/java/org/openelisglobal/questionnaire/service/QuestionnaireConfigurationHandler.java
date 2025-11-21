package org.openelisglobal.questionnaire.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.InputStream;
import java.util.UUID;
import liquibase.repackaged.org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.fhir.springserialization.QuestionnaireDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QuestionnaireConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @Autowired
    private FhirConfig fhirConfig;

    @Override
    public String getDomainName() {
        return "questionnaires";
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        if (StringUtils.isBlank(fhirConfig.getLocalFhirStorePath())) {
            throw new IllegalStateException("FHIR store path is not configured. Cannot load questionnaires.");
        }

        Questionnaire questionnaire = parseQuestionnaire(inputStream);

        if (questionnaire.getId() == null || questionnaire.getId().isEmpty()) {
            // Generate a UUID based on the filename for consistency
            String uuid = generateUUIDFromFilename(fileName);
            questionnaire.setId(uuid);
        }

        // Save to FHIR store
        fhirPersistanceService.updateFhirResourceInFhirStore(questionnaire);
    }

    private Questionnaire parseQuestionnaire(InputStream inputStream) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Questionnaire.class, new QuestionnaireDeserializer());
        mapper.registerModule(module);

        return mapper.readValue(inputStream, Questionnaire.class);
    }

    private String generateUUIDFromFilename(String filename) {
        // Generate a deterministic UUID based on filename for consistency
        // This ensures the same file always gets the same UUID
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(filename.getBytes());
            // Convert to UUID format
            hashBytes[6] &= 0x0f;
            hashBytes[6] |= 0x30;
            hashBytes[8] &= 0x3f;
            hashBytes[8] |= 0x80;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                if (i == 4 || i == 6 || i == 8 || i == 10) {
                    sb.append("-");
                }
                sb.append(String.format("%02x", hashBytes[i]));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // Fallback to random UUID
            return UUID.randomUUID().toString();
        }
    }
}
