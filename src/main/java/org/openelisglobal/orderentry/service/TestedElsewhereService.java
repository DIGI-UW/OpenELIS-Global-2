package org.openelisglobal.orderentry.service;

import java.util.List;
import java.util.Map;

/**
 * Tests on an order whose results another laboratory reported (FRS clinical
 * order entry v4, FR-B20). Every method takes the order by its lab number and
 * answers plain maps, compiled inside the transaction.
 */
public interface TestedElsewhereService {

    /**
     * The tests of the order marked tested elsewhere, each with {@code testId},
     * {@code performingLabId}, {@code performingLabName} and {@code reportedValue}.
     *
     * @throws OrderEntryRequestRefusedException when no order has that lab number
     */
    List<Map<String, Object>> forOrder(String labNumber);

    /**
     * Marks a test of the order tested elsewhere, or updates its laboratory and
     * value.
     *
     * @throws OrderEntryRequestRefusedException when the order, the test or the
     *                                           laboratory is unknown, the test is
     *                                           not on the order, or the value is
     *                                           longer than 255 characters
     */
    Map<String, Object> mark(String labNumber, String testId, String performingLabId, String reportedValue,
            String sysUserId);

    /**
     * Clears the mark.
     *
     * @return whether a mark was removed
     */
    boolean unmark(String labNumber, String testId);
}
