package org.openelisglobal.analyzer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.Table;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingConfirmation;

/**
 * Tests run with {@code hbm2ddl.auto=update}, which silently creates any
 * sequence an entity names, so a generator pointing at a sequence Liquibase
 * never creates (or later drops) passes every database test and fails in
 * production. This checks the entity against the changeset that creates its
 * table.
 */
public class AnalyzerMappingSequenceTest {

    private static final Path CHANGESET = Path.of("src", "main", "resources", "liquibase", "3.5.x.x",
            "123-analyzer-mapping.xml");

    @Test
    public void eachMappingEntityDrawsIdsFromTheSequenceItsChangesetCreates() throws Exception {
        String changeset = Files.readString(CHANGESET);
        for (Class<?> entity : List.of(AnalyzerMapping.class, AnalyzerMappingConfirmation.class)) {
            String table = entity.getAnnotation(Table.class).name();
            String sequence = sequenceOf(entity);
            assertEquals(entity.getSimpleName(), table + "_seq", sequence);
            assertTrue(entity.getSimpleName() + " sequence is created with its table",
                    changeset.contains("sequenceName=\"" + sequence + "\""));
        }
    }

    private static String sequenceOf(Class<?> entity) throws Exception {
        GenericGenerator generator = entity.getDeclaredField("id").getAnnotation(GenericGenerator.class);
        for (Parameter parameter : generator.parameters()) {
            if ("sequence_name".equals(parameter.name())) {
                return parameter.value();
            }
        }
        throw new AssertionError(entity.getSimpleName() + " names no sequence");
    }
}
