package org.openelisglobal.microbiology;

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class MicrobiologyAlertLiquibaseRollbackTest {

    @Test
    public void rollbackRemovesStringKeyedAlertsBeforeRestoringNumericIdConstraint() throws Exception {
        String changeLog = Files.readString(Path.of("src/main/resources/liquibase/3.5.x.x/057-alert-entity-ref.xml"));

        int cleanup = changeLog.indexOf("DELETE FROM clinlims.alert");
        int restoreConstraint = changeLog.indexOf("<addNotNullConstraint");

        assertTrue("Rollback must remove string-keyed alerts", cleanup >= 0);
        assertTrue("Alert cleanup must happen before restoring alert_entity_id NOT NULL", cleanup < restoreConstraint);
    }
}
