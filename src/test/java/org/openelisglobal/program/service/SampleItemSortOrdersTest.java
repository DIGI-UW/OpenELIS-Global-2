package org.openelisglobal.program.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

public class SampleItemSortOrdersTest {

    private static SampleItem itemWithSortOrder(String sortOrder) {
        SampleItem item = new SampleItem();
        item.setSortOrder(sortOrder);
        return item;
    }

    @Test
    public void of_withUnorderedItems_shouldReturnSortOrdersNumericallyAscending() {
        List<SampleItem> items = Arrays.asList(itemWithSortOrder("10"), itemWithSortOrder("2"), itemWithSortOrder("1"));

        assertEquals(Arrays.asList("1", "2", "10"), SampleItemSortOrders.of(items));
    }

    @Test
    public void of_withBlankOrMissingSortOrder_shouldSkipThoseItems() {
        List<SampleItem> items = Arrays.asList(itemWithSortOrder("1"), itemWithSortOrder(null), itemWithSortOrder("  "),
                itemWithSortOrder("2"));

        assertEquals(Arrays.asList("1", "2"), SampleItemSortOrders.of(items));
    }

    @Test
    public void of_withNonNumericSortOrder_shouldKeepItAfterTheNumericOnes() {
        List<SampleItem> items = Arrays.asList(itemWithSortOrder("x"), itemWithSortOrder("2"), itemWithSortOrder("1"));

        assertEquals(Arrays.asList("1", "2", "x"), SampleItemSortOrders.of(items));
    }

    @Test
    public void of_withNullOrEmptyList_shouldReturnEmptyList() {
        assertTrue(SampleItemSortOrders.of(null).isEmpty());
        assertTrue(SampleItemSortOrders.of(Collections.emptyList()).isEmpty());
    }
}
