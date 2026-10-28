package org.openelisglobal.dashboard.management.surveillance;

import java.util.List;
import org.openelisglobal.dashboard.management.ScopeRange;

public interface SurveillanceDAO {

    /** id, code, display name, display order of active programs, in card order. */
    List<Object[]> getActivePrograms();

    /**
     * program id, metric key, label, display order, one row per active metric key.
     */
    List<Object[]> getPositivityMetrics();

    /**
     * program id, metric key (null on the program total), "C" or "P" for the
     * current or prior period, tested, positive.
     */
    List<Object[]> countPositivity(ScopeRange scope);

    /**
     * program id, metric key, label, organism codes, organism group, antibiotic
     * codes, display order.
     */
    List<Object[]> getResistanceMetrics();

    /** id, WHONET code, organism group of active organisms. */
    List<Object[]> getActiveOrganisms();

    /** id, WHONET code of active antibiotics. */
    List<Object[]> getActiveAntibiotics();
}
