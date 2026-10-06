package org.openelisglobal.orderentry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-1424 (FR-B28): after the 3.6.x.x/010 migration the order-level Payment
 * status options carry readable English and French names, not the raw codes the
 * generic localization backfill copied in.
 */
public class PaymentStatusLabelsMigrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void everyPaymentStatusOptionHasAReadableEnglishAndAFrenchName() {
        List<Map<String, Object>> rows = new JdbcTemplate(dataSource)
                .queryForList("SELECT d.dict_entry, (SELECT v.value FROM clinlims.localization_value v"
                        + " WHERE v.localization_id = d.name_localization_id AND v.locale = 'en') AS en,"
                        + " (SELECT v.value FROM clinlims.localization_value v"
                        + " WHERE v.localization_id = d.name_localization_id AND v.locale = 'fr') AS fr"
                        + " FROM clinlims.dictionary d JOIN clinlims.dictionary_category c"
                        + " ON c.id = d.dictionary_category_id"
                        + " WHERE c.name = 'patientPayment' AND d.name_localization_id IS NOT NULL");

        assertTrue("the patientPayment options are seeded", rows.size() >= 4);
        for (Map<String, Object> row : rows) {
            String code = String.valueOf(row.get("dict_entry"));
            assertTrue(code + " has a readable English name, not its code",
                    row.get("en") != null && !code.equals(row.get("en")));
            assertTrue(code + " has a French name", row.get("fr") != null);
        }
        assertEquals("Normal cash payment", rows.stream().filter(r -> "normalCash".equals(r.get("dict_entry")))
                .map(r -> r.get("en")).findFirst().orElse(null));
    }
}
