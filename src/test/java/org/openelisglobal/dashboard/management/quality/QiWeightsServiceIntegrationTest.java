package org.openelisglobal.dashboard.management.quality;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

public class QiWeightsServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QiWeightsService qiWeightsService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        tearDown();
    }

    @After
    public void tearDown() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table = (SELECT id"
                + " FROM clinlims.reference_tables WHERE LOWER(name) = 'qi_weights')");
        jdbcTemplate.update("DELETE FROM clinlims.qi_weights");
    }

    @Test
    public void withNoRowTheFrsDefaultsApply() {
        assertEquals(QiWeightsServiceImpl.DEFAULTS, qiWeightsService.getWeights());
    }

    @Test
    public void savedWeightsRoundTripAndStayOneRow() {
        WeightsView first = new WeightsView(new BigDecimal("0.40"), new BigDecimal("0.30"), new BigDecimal("0.20"),
                new BigDecimal("0.10"));
        WeightsView second = new WeightsView(new BigDecimal("0.25"), new BigDecimal("0.25"), new BigDecimal("0.25"),
                new BigDecimal("0.25"));

        qiWeightsService.saveWeights(first, "1");
        assertEquals(first, qiWeightsService.getWeights());
        qiWeightsService.saveWeights(second, "1");

        assertEquals(second, qiWeightsService.getWeights());
        assertEquals(Integer.valueOf(1),
                jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.qi_weights", Integer.class));
        assertEquals(List.of("I", "U"),
                jdbcTemplate.queryForList(
                        "SELECT h.activity FROM clinlims.history h JOIN clinlims.reference_tables r"
                                + " ON r.id = h.reference_table WHERE LOWER(r.name) = 'qi_weights' ORDER BY h.id",
                        String.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void weightsThatDoNotSumToOneAreNotSaved() {
        qiWeightsService.saveWeights(new WeightsView(new BigDecimal("0.50"), new BigDecimal("0.50"),
                new BigDecimal("0.50"), new BigDecimal("0.50")), "1");
    }
}
