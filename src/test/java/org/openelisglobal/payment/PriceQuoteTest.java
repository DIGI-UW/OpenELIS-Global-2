package org.openelisglobal.payment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.payment.service.PriceQuote;

public class PriceQuoteTest {

    @Test
    public void sumsEachTestOnceEvenWhenItAppearsTwice() {
        PriceQuote quote = PriceQuote.of(List.of(new PriceQuote.Line("t1", "Hb", 100L),
                new PriceQuote.Line("t2", "CBC", 250L), new PriceQuote.Line("t1", "Hb", 100L)));

        assertEquals(350L, quote.totalPaise());
        assertEquals(2, quote.testCount());
        assertTrue(quote.isComplete());
    }

    @Test
    public void unpricedTestIsReportedAndMakesTheQuoteIncomplete() {
        PriceQuote quote = PriceQuote
                .of(List.of(new PriceQuote.Line("t1", "Hb", 100L), new PriceQuote.Line("t3", "Culture", null)));

        assertEquals(100L, quote.totalPaise());
        assertEquals(List.of("Culture"), quote.unpricedTestNames());
        assertFalse(quote.isComplete());
    }

    @Test
    public void emptyOrderIsZeroAndComplete() {
        PriceQuote quote = PriceQuote.of(List.of());

        assertEquals(0L, quote.totalPaise());
        assertEquals(0, quote.testCount());
        assertTrue(quote.isComplete());
    }
}
