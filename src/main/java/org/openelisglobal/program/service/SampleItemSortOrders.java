package org.openelisglobal.program.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * The sort orders of a sample's items, in the form LabelMakerServlet addresses
 * them ({@code labNo.<sortOrder>}). The case views send these to the client so
 * that a specimen label can be requested for one sample item instead of for
 * every item on the accession.
 */
public final class SampleItemSortOrders {

    private SampleItemSortOrders() {
    }

    public static List<String> of(List<SampleItem> sampleItems) {
        if (sampleItems == null) {
            return Collections.emptyList();
        }
        return sampleItems.stream().map(SampleItem::getSortOrder).filter(StringUtils::isNotBlank)
                .sorted(Comparator.comparing(SampleItemSortOrders::isNumeric).reversed()
                        .thenComparing(Comparator.comparingInt(SampleItemSortOrders::numericValue))
                        .thenComparing(Comparator.naturalOrder()))
                .collect(Collectors.toList());
    }

    private static boolean isNumeric(String sortOrder) {
        return StringUtils.isNumeric(sortOrder);
    }

    private static int numericValue(String sortOrder) {
        return isNumeric(sortOrder) ? Integer.parseInt(sortOrder) : Integer.MAX_VALUE;
    }
}
