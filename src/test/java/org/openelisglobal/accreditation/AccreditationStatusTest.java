package org.openelisglobal.accreditation;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import org.junit.Test;
import org.openelisglobal.accreditation.valueholder.AccreditationStatus;

/**
 * The status an accrediting body derives from its active flag and its expiry
 * date. A pure function of its three arguments, so no database and no Spring
 * context — the integration tests assert that the services report this status,
 * not how the boundary is drawn.
 */
public class AccreditationStatusTest {

    @Test
    public void theExpiringWindowIsInclusiveAtBothEnds() {
        LocalDate today = LocalDate.now();

        assertEquals(AccreditationStatus.EXPIRING,
                AccreditationStatus.of(true, today.plusDays(AccreditationStatus.EXPIRING_WINDOW_DAYS), today));
        assertEquals(AccreditationStatus.ACTIVE,
                AccreditationStatus.of(true, today.plusDays(AccreditationStatus.EXPIRING_WINDOW_DAYS + 1), today));
        // Expiring today is still valid for reporting, not yet expired.
        assertEquals(AccreditationStatus.EXPIRING, AccreditationStatus.of(true, today, today));
    }
}
