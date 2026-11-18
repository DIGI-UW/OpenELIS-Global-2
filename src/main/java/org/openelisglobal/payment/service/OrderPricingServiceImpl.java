package org.openelisglobal.payment.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prices an order. A test ordered as part of a priced panel is covered by the
 * panel price, charged once per panel; its own price is ignored. A test ordered
 * on its own, or from a panel with no price, is charged at its own price.
 */
@Service
public class OrderPricingServiceImpl implements OrderPricingService {

    /** Keeps panel lines apart from test lines, which are keyed by test id. */
    private static final String PANEL_KEY_PREFIX = "panel:";

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    @Override
    @Transactional(readOnly = true)
    public PriceQuote quoteSample(String sampleId) {
        String canceled = statusService.getStatusID(StatusService.AnalysisStatus.Canceled);
        List<Analysis> analyses = analysisService.getAnalysesBySampleIdExcludedByStatusId(sampleId, Set.of(canceled));
        return PriceQuote.of(linesFor(analyses));
    }

    /**
     * One line per priced panel and per test outside a priced panel.
     * {@link PriceQuote#of} keeps each key once, so a panel with several tests is
     * charged once. Must run inside a transaction: it reads each analysis's test
     * and panel.
     */
    public static List<PriceQuote.Line> linesFor(List<Analysis> analyses) {
        List<PriceQuote.Line> lines = new ArrayList<>();
        for (Analysis analysis : analyses) {
            Panel panel = analysis.getPanel();
            if (panel != null && panel.getPricePaise() != null) {
                lines.add(new PriceQuote.Line(PANEL_KEY_PREFIX + panel.getId(), panel.getPanelName(),
                        panel.getPricePaise()));
                continue;
            }
            Test test = analysis.getTest();
            if (test != null) {
                lines.add(new PriceQuote.Line(test.getId(), test.getName(), test.getPricePaise()));
            }
        }
        return lines;
    }
}
