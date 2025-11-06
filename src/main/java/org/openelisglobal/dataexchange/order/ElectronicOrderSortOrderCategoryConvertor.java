package org.openelisglobal.dataexchange.order;

import java.beans.PropertyEditorSupport;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;

public class ElectronicOrderSortOrderCategoryConvertor extends PropertyEditorSupport {

    @Override
    public void setAsText(final String text) {
        setValue(ElectronicOrder.SortOrder.fromString(text));
    }
}
