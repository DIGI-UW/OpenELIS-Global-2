package org.openelisglobal.payment.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The price of an order: the sum of its distinct billable items' prices (tests,
 * or priced panels), plus any tests that have no price. An order with unpriced
 * tests is not complete and must not be billed, since that would under-charge
 * silently. {@code testCount} is the number of distinct billable items.
 */
public record PriceQuote(long totalPaise, int testCount, List<String> unpricedTestNames) {

    /**
     * One billable item on an order, with its price (null when unpriced). The id is
     * a test id, or "panel:" plus the panel id for a priced panel.
     */
    public record Line(String testId, String testName, Long pricePaise) {
    }

    public boolean isComplete() {
        return unpricedTestNames.isEmpty();
    }

    /** Each item counts once, even if it appears on several analyses. */
    public static PriceQuote of(List<Line> lines) {
        Map<String, Line> distinct = new LinkedHashMap<>();
        for (Line line : lines) {
            distinct.putIfAbsent(line.testId(), line);
        }
        long total = 0;
        List<String> unpriced = new ArrayList<>();
        for (Line line : distinct.values()) {
            if (line.pricePaise() == null) {
                unpriced.add(line.testName());
            } else {
                total += line.pricePaise();
            }
        }
        return new PriceQuote(total, distinct.size(), unpriced);
    }
}
