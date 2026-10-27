package org.openelisglobal.dashboard.management.equipment;

import java.time.LocalDate;
import java.util.List;

public interface EquipmentDAO {

    /**
     * Rows of {analyzer id, name, samples received in scope, last activity, most
     * frequent test section id}, one per active analyzer.
     */
    List<Object[]> getActivity(LocalDate from, LocalDate to);
}
