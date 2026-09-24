package org.openelisglobal.dataexchange.service.order;

import java.util.List;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;

/**
 * Applies the user's lab-unit assignment to a list of incoming electronic
 * orders, the way Results and Validation apply it to their rows.
 */
public interface ElectronicOrderLabUnitScope {

    /**
     * The orders the user may see: those ordering a test in one of the user's
     * Results lab units, and those that cannot be tied to any OpenELIS test, since
     * nothing places such an order in a lab unit. A user holding every lab unit
     * gets the list back unchanged.
     */
    List<ElectronicOrder> restrictToUserLabUnits(List<ElectronicOrder> orders, String systemUserId);
}
