package org.openelisglobal.resultvalidation;

import org.springframework.jdbc.core.JdbcTemplate;

final class QcHoldFixture {

    private QcHoldFixture() {
    }

    static void holdByAFailedControl(JdbcTemplate jdbcTemplate, String analysisId) {
        jdbcTemplate.update("INSERT INTO clinlims.nc_event (id, nce_number, trigger_source_type) VALUES (98001,"
                + " 'NCE-HOLD-1', 'QC_BENCH_CONTROL')");
        jdbcTemplate.update("INSERT INTO clinlims.nce_specimen (id, nce_id, analysis_id) VALUES (98001, 98001, ?)",
                Integer.valueOf(analysisId));
    }
}
