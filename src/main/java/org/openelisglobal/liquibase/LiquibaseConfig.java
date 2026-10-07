package org.openelisglobal.liquibase;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Map;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class LiquibaseConfig {

    @Autowired
    private DataSource dataSource;

    @Value("${spring.liquibase.contexts:default}")
    private String contexts;

    @Autowired
    private Environment environment;

    @Bean("liquibase")
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:liquibase/base-changelog.xml");
        liquibase.setDataSource(dataSource);
        liquibase.setContexts(contexts);
        liquibase.setChangeLogParameters(cutoverParameters());
        return liquibase;
    }

    private Map<String, String> cutoverParameters() {
        String mappingFile = environment.getProperty("amr.cutover.mappingFile", "").trim();
        String actorId = environment.getProperty("amr.cutover.actorId", "").trim();
        String at = environment.getProperty("amr.cutover.at", "").trim();
        if (mappingFile.isEmpty() && actorId.isEmpty() && at.isEmpty()) {
            return Map.of();
        }
        if (mappingFile.isEmpty() || actorId.isEmpty() || at.isEmpty()) {
            throw new IllegalArgumentException("AMR cutover requires mappingFile, actorId and at together");
        }
        // These values are substituted into migration SQL; accept only their declared
        // formats.
        if (!actorId.matches("[0-9]+")) {
            throw new IllegalArgumentException("amr.cutover.actorId must be a numeric user ID");
        }
        try {
            LocalDateTime.parse(at,
                    DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("amr.cutover.at must be a valid timestamp in yyyy-MM-dd HH:mm:ss format",
                    e);
        }
        return Map.of("amr.cutover.mappingFile", mappingFile, "amr.cutover.actorId", actorId, "amr.cutover.at", at);
    }
}
