package org.openelisglobal.payment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.payment.service.OrderPricingServiceImpl;
import org.openelisglobal.payment.service.PriceQuote;

public class OrderPricingLinesTest {

    private static org.openelisglobal.test.valueholder.Test test(String id, String name, Long paise) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(id);
        // With no localization, Test.getName() falls back to the description.
        test.setDescription(name);
        test.setPricePaise(paise);
        return test;
    }

    private static Panel panel(String id, String name, Long paise) {
        Panel panel = new Panel();
        panel.setId(id);
        panel.setPanelName(name);
        panel.setPricePaise(paise);
        return panel;
    }

    private static Analysis analysis(org.openelisglobal.test.valueholder.Test test, Panel panel) {
        Analysis analysis = new Analysis();
        analysis.setTest(test);
        analysis.setPanel(panel);
        return analysis;
    }

    private static PriceQuote quote(Analysis... analyses) {
        return PriceQuote.of(OrderPricingServiceImpl.linesFor(List.of(analyses)));
    }

    @Test
    public void standaloneTestsUseTheirOwnPrices() {
        PriceQuote quote = quote(analysis(test("1", "Hb", 10000L), null), analysis(test("2", "WBC", 5000L), null));

        assertEquals(15000L, quote.totalPaise());
        assertTrue(quote.isComplete());
    }

    @Test
    public void pricedPanelIsChargedOnceAndItsTestsPricesAreIgnored() {
        Panel bilan = panel("p1", "Bilan Biochimique", 40000L);
        PriceQuote quote = quote(analysis(test("1", "Urea", null), bilan),
                analysis(test("2", "Creatinine", 9900L), bilan), analysis(test("3", "Glucose", null), bilan));

        assertEquals(40000L, quote.totalPaise());
        assertTrue(quote.isComplete());
    }

    @Test
    public void panelAndStandaloneTestAreBothCharged() {
        Panel bilan = panel("p1", "Bilan Biochimique", 40000L);
        PriceQuote quote = quote(analysis(test("1", "Urea", null), bilan), analysis(test("9", "HIV", 25000L), null));

        assertEquals(65000L, quote.totalPaise());
    }

    @Test
    public void unpricedPanelFallsBackToItsTestsPrices() {
        Panel unpriced = panel("p2", "Lipid Profile", null);
        PriceQuote quote = quote(analysis(test("4", "HDL", 3000L), unpriced),
                analysis(test("5", "LDL", null), unpriced));

        assertFalse(quote.isComplete());
        assertEquals(List.of("LDL"), quote.unpricedTestNames());
    }

    @Test
    public void twoPricedPanelsAreEachChargedOnce() {
        Panel a = panel("p1", "Bilan Biochimique", 40000L);
        Panel b = panel("p3", "Hepatic Panel", 30000L);
        PriceQuote quote = quote(analysis(test("1", "Urea", null), a), analysis(test("6", "ALT", null), b),
                analysis(test("7", "AST", null), b));

        assertEquals(70000L, quote.totalPaise());
    }
}
