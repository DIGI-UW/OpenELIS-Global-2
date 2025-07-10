package org.openelisglobal.testresultsview;

import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testresultsview.service.ClientResultsViewInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class ClientResultsViewInfoServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ClientResultsViewInfoService clientResultsViewInfoService;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setJdbcTemplate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Before
    public void setUp() {
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS client_results_view_seq START WITH 1 INCREMENT BY 1");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS result (id numeric(10,0) NOT NULL,"
                + "analysis_id numeric(10,0), sort_order numeric, is_reportable character varying(1),"
                + "result_type character varying(1),value character varying(200), analyte_id numeric(10,0),"
                + "test_result_id numeric(10,0), lastupdated timestamp(6) without time zone,"
                + "min_normal double precision, max_normal double precision, parent_id numeric(10,0),"
                + "significant_digits numeric DEFAULT 0, \"grouping\" numeric DEFAULT 0);");

        jdbcTemplate.update("INSERT INTO result (id,sort_order,is_reportable,result_type) VALUES (1001, 1, 'Y','N');");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS client_results_view ("
                + "id INTEGER PRIMARY KEY DEFAULT nextval('client_results_view_seq'), password TEXT, "
                + "result_id VARCHAR(10), "
                + "CONSTRAINT fk_client_results_view FOREIGN KEY (result_id) REFERENCES result(id));");

        jdbcTemplate.update("INSERT INTO client_results_view (id, password, result_id ) "
                + "VALUES ( 7001, 'encrypted-password-string', 1001);");
    }

    @After
    public void cleaUp() {
        jdbcTemplate.execute("DROP TABLE result CASCADE ");
        jdbcTemplate.execute("DROP TABLE client_results_view CASCADE ");
        jdbcTemplate.execute("DROP SEQUENCE client_results_view_seq");
    }

    @Test
    public void getAll() {

    }
}
