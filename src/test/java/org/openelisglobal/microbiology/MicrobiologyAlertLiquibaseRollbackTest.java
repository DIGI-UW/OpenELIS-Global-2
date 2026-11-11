package org.openelisglobal.microbiology;

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class MicrobiologyAlertLiquibaseRollbackTest {

    private static final String ENTITY_REF = "src/main/resources/liquibase/3.5.x.x/057-alert-entity-ref.xml";
    private static final String TYPE_UNION = "src/main/resources/liquibase/3.5.x.x/058-alert-type-union.xml";

    @Test
    public void rollbackRemovesStringKeyedAlertsBeforeRestoringNumericIdConstraint() throws Exception {
        String changeLog = Files.readString(Path.of(TYPE_UNION));
        int cleanup = changeLog.indexOf("DELETE FROM clinlims.alert");
        int restoreConstraint = changeLog.indexOf("ADD CONSTRAINT chk_alert_type", cleanup);

        assertTrue("Rollback must remove string-keyed alerts", cleanup >= 0);
        assertTrue("Alert cleanup must happen before the numeric-only constraint is restored",
                cleanup < restoreConstraint);
    }

    /**
     * 057 is applied on deployed databases, so Liquibase validates its checksum on
     * every start. Corrections belong in a later changeset instead.
     */
    @Test
    public void theAppliedEntityRefChangesetCarriesNoLaterCorrections() throws Exception {
        String applied = Files.readString(Path.of(ENTITY_REF));

        assertTrue("The referral alert types belong to the later union changeset",
                !applied.contains("REFERRAL_CRITICAL_RESULT"));
        assertTrue("The rollback cleanup belongs to the later union changeset",
                !applied.contains("DELETE FROM clinlims.alert"));
    }

    @Test
    public void theUnionChangesetKeepsEveryAlertTypeInUse() throws Exception {
        String union = Files.readString(Path.of(TYPE_UNION));
        int constraint = union.indexOf("ADD CONSTRAINT chk_alert_type");
        String forward = union.substring(constraint, union.indexOf("</sql>", constraint));

        for (String alertType : new String[] { "MICROBIOLOGY_CRITICAL", "REQUIRED_BY_DEADLINE",
                "REFERRAL_CRITICAL_RESULT", "REFERRAL_REJECTED", "CRITICAL_RESULT", "CRITICAL_UNACKNOWLEDGED" }) {
            assertTrue(alertType + " must stay permitted", forward.contains(alertType));
        }
    }
}
