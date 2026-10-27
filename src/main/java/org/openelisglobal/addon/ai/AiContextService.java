package org.openelisglobal.addon.ai;

import java.util.Map;
import org.openelisglobal.addon.security.AddonToken;

/**
 * What an add-on with the {@code ai.context} scope can learn about the orders
 * of a UI token: a de-identified, structured context for decision support, and
 * de-identification of free text (for example a question the user typed).
 * Nothing returned names a patient, a lab number or a date.
 */
public interface AiContextService {

    /** At most this many orders become cases. */
    int MAX_CASES = 30;

    /** One case per order (the validation page). */
    String VIEW_CASES = "cases";
    /**
     * One patient: the newest day of the orders is current, the rest is history.
     */
    String VIEW_PATIENT = "patient";

    /**
     * The de-identified context of the orders in the token.
     *
     * @param view {@link #VIEW_CASES} or {@link #VIEW_PATIENT}
     */
    Map<String, Object> context(AddonToken.Claims claims, String view);

    /**
     * {@code text} without the identifiers of the patients of the token's orders.
     */
    String deidentify(AddonToken.Claims claims, String text);
}
