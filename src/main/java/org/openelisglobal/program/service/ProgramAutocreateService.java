package org.openelisglobal.program.service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import liquibase.repackaged.org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.fhir.springserialization.QuestionnaireDeserializer;
import org.openelisglobal.program.controller.EditProgramForm;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class ProgramAutocreateService {

    @Value("${org.openelisglobal.program.autocreate:true}")
    private boolean autocreateOn;

    @Value("${org.openelisglobal.program.path:/var/lib/openelis-global/programs}")
    private String programPath;

    @Value("classpath*:programs/*.json")
    private Resource[] classpathProgramResources;

    @Autowired
    private ProgramService programService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;
    @Autowired
    private FhirConfig fhirConfig;

    /**
     * Get all program resources from both classpath and filesystem
     * 
     * @return Array of all program resources
     */
    private Resource[] getAllProgramResources() {
        List<Resource> allResources = new ArrayList<>();

        // Add classpath resources (backward compatibility)
        if (classpathProgramResources != null) {
            allResources.addAll(Arrays.asList(classpathProgramResources));
        }

        // Add filesystem resources if directory exists
        File programDir = new File(programPath);
        if (programDir.exists() && programDir.isDirectory()) {
            File[] jsonFiles = programDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
            if (jsonFiles != null) {
                for (File file : jsonFiles) {
                    allResources.add(new FileSystemResource(file));
                    LogEvent.logInfo(this.getClass().getSimpleName(), "getAllProgramResources",
                            "Loading program from filesystem: " + file.getName());
                }
            }
        } else {
            LogEvent.logInfo(this.getClass().getSimpleName(), "getAllProgramResources",
                    "Program directory does not exist: " + programPath);
        }

        return allResources.toArray(new Resource[0]);
    }

    @PostConstruct
    @Transactional
    public void autocreateProgram() {
        if (autocreateOn && StringUtils.isNotBlank(fhirConfig.getLocalFhirStorePath())) {
            Resource[] programResources = getAllProgramResources();
            for (Resource programResource : programResources) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(programResource.getInputStream()))) {
                    reader.mark(100);
                    String linee = reader.readLine();
                    if (linee.startsWith("Test")) {
                        while ((linee = reader.readLine()) != null) {
                            String[] lines = linee.split("	");
                            UUID uuid = UUID.randomUUID();
                            System.out.println("\t\t<insert schemaName=\"clinlims\" tableName=\"localization\">\n" + //
                                    "\t\t\t<column name=\"id\" valueSequenceNext=\"localization_seq\" />\n" + //
                                    "\t\t\t<column name=\"lastupdated\" valueComputed=\"${now}\" />\n" + //
                                    "\t\t\t<column name=\"description\" value=\"test name\" />\n" + //
                                    "\t\t\t<column name=\"english\" value=\"" + lines[1].trim() + "\" />\n" + //
                                    "\t\t\t<column name=\"french\" value=\"" + lines[1].trim() + "\"/>\n" + //
                                    "\t\t</insert>\n" + //
                                    "\t\t<insert schemaName=\"clinlims\" tableName=\"localization\">\n" + //
                                    "\t\t\t<column name=\"id\" valueSequenceNext=\"localization_seq\" />\n" + //
                                    "\t\t\t<column name=\"lastupdated\" valueComputed=\"${now}\" />\n" + //
                                    "\t\t\t<column name=\"description\" value=\"test reporting name\" />\n" + //
                                    "\t\t\t<column name=\"english\" value=\"" + lines[2].trim() + "\" />\n" + //
                                    "\t\t\t<column name=\"french\" value=\"" + lines[2].trim() + "\"/>\n" + //
                                    "\t\t</insert>\n" + //
                                    "\t\t<insert schemaName=\"clinlims\" tableName=\"test\">\n" + //
                                    "\t\t\t<column name=\"id\" valueNumeric=\"nextval('test_seq') \"/>\n" + //
                                    "\t\t\t<column name=\"description\" value=\"" + lines[1].trim() + "\"/> \n" + //
                                    "\t\t\t<column name=\"lastupdated\" valueDate='now()' /> \n" + //
                                    "\t\t\t<column name=\"is_active\" value='Y' /> \n" + //
                                    "\t\t\t<column name=\"is_reportable\" value='N' /> \n" + //
                                    "\t\t\t<column name=\"test_section_id\" valueComputed=\"(select id from"
                                    + " test_section where name = 'Immunohistochemistry' limit 1)\" />\n" + //
                                    "\t\t\t<column name=\"local_code\" value='" + lines[0].trim() + "' />\n" + //
                                    "\t\t\t<column name=\"name\" value=\"test." + lines[0].trim() + "\"/>\n" + //
                                    "\t\t\t<column name=\"name_localization_id\" valueComputed=\"(select id from"
                                    + " localization where english = '" + lines[1].trim()
                                    + "' and description = 'test name' limit 1)\" />\n" + //
                                    "\t\t\t<column name=\"reporting_name_localization_id\""
                                    + " valueComputed=\"(select id from localization where english = '"
                                    + lines[2].trim() + "' and description = 'test reporting name' limit 1)\" />\n" + //
                                    "\t\t\t<column name=\"guid\" value=\"" + uuid + "\"/>\n" + //
                                    "\t\t</insert> \n" + //
                                    "\t\t<insert tableName=\"test_result\">\n" + //
                                    "\t\t\t<column name=\"id\" valueNumeric=\"nextval( 'test_result_seq' )\"/>\n" + //
                                    "\t\t\t<column name=\"test_id\"  valueNumeric=\" ( select id from"
                                    + " clinlims.test where description = '" + lines[1].trim() + "' ) \" />\n" + //
                                    "\t\t\t<column name=\"tst_rslt_type\" value=\"R\" />\n" + //
                                    "\t\t\t<column name=\"lastupdated\" valueComputed=\"${now}\"/>\n" + //
                                    "\t\t\t<column name=\"is_active\" value=\"t\"/>\n" + //
                                    "\t\t\t<column name=\"sort_order\" valueNumeric=\"0\" />\n" + //
                                    "\t\t</insert>\n" + //
                                    "\t\t<insert schemaName=\"clinlims\" tableName=\"sampletype_test\">\n" + //
                                    "\t\t\t<column name=\"id\" valueNumeric=\" nextval( 'sample_type_test_seq' )"
                                    + " \"/>\n" + //
                                    "\t\t\t<column name=\"sample_type_id\" valueNumeric=\" ( select id from"
                                    + " clinlims.type_of_sample where description = 'Immunohistochemistry specimen'"
                                    + " ) \"/>\n" + //
                                    "\t\t\t<column name=\"test_id\"  valueNumeric=\" ( select id from"
                                    + " clinlims.test where description = '" + lines[1].trim() + "' ) \" />\n" + //
                                    "\t\t</insert>");
                        }
                        continue;
                    } else {
                        reader.reset();
                    }

                    String contents = reader.lines().map(line -> line + "\n").collect(Collectors.joining());
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.registerModule(new Hibernate5JakartaModule());
                    mapper.setSerializationInclusion(Include.NON_NULL);

                    SimpleModule module = new SimpleModule();
                    module.addDeserializer(Questionnaire.class, new QuestionnaireDeserializer());
                    mapper.registerModule(module);
                    EditProgramForm form = mapper.readValue(contents, EditProgramForm.class);

                    Questionnaire questionnaire = form.getAdditionalOrderEntryQuestions();

                    Program program = form.getProgram();
                    program.setManuallyChanged(false);
                    if (!GenericValidator.isBlankOrNull(program.getCode())) {
                        Optional<Program> dbProgram = programService.getMatch("code", form.getProgram().getCode());
                        if (dbProgram.isPresent()) {
                            if (dbProgram.get().getManuallyChanged()) {
                                continue;
                            }
                            program.setId(dbProgram.get().getId());
                            program.setLastupdated(dbProgram.get().getLastupdated());
                        }
                    }
                    if (program.getQuestionnaireUUID() == null) {
                        program.setQuestionnaireUUID(UUID.randomUUID());
                    }
                    if (questionnaire == null) {
                        questionnaire = new Questionnaire();
                    }
                    if (StringUtils.isNotBlank(form.getTestSectionName())) {
                        Optional<TestSection> testSection = testSectionService.getMatch("testSectionName",
                                form.getTestSectionName());
                        if (testSection.isPresent()) {
                            program.setTestSection(testSection.get());
                        } else {
                            program.setTestSection(null);
                        }
                    }
                    program = programService.save(program);
                    questionnaire.setId(program.getQuestionnaireUUID().toString());
                    fhirPersistanceService.updateFhirResourceInFhirStore(questionnaire);
                    DisplayListService.getInstance().refreshList(ListType.PROGRAM);

                    // }

                } catch (IOException | FhirLocalPersistingException e) {
                    LogEvent.logError(e);
                }
            }
        }
    }
}
