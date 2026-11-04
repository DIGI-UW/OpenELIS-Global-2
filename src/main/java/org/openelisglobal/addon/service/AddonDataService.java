package org.openelisglobal.addon.service;

import java.util.Map;
import java.util.Optional;
import org.openelisglobal.addon.security.AddonToken;

/**
 * What the add-on API reads about one order. Only codes and results; the
 * patient view has sex and date of birth, never names or identifiers (those
 * come from {@link AddonQueryService}, under {@code patient.identity}).
 */
public interface AddonDataService {

    /**
     * Whether these credentials may read this order: a user token only for the lab
     * numbers it carries, the service key for any order with {@code orders.search},
     * otherwise only for orders the add-on was sent an event about (within the
     * event retention period).
     */
    boolean mayRead(AddonToken.Claims claims, String accessionNumber);

    Optional<Map<String, Object>> order(String accessionNumber);

    Optional<Map<String, Object>> results(String accessionNumber);

    Optional<Map<String, Object>> patient(String accessionNumber);
}
