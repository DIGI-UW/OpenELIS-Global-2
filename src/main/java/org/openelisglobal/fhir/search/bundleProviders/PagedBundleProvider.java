package org.openelisglobal.fhir.search.bundleProviders;

import ca.uhn.fhir.rest.api.server.IBundleProvider;

/**
 * A search result that can be told which page of itself it is serving.
 *
 * <p>
 * Once a request carries {@code _offset}, HAPI hands the whole range to the
 * bundle provider and returns whatever it gets back, so the provider - not HAPI
 * - decides what a page contains. A result that cannot be told this serves the
 * entire set for every page, and the {@code next} link it advertises never
 * advances.
 */
public interface PagedBundleProvider extends IBundleProvider {

    /**
     * @param offset zero-based offset requested with {@code _offset}, null when the
     *               client sent none
     * @param count  page size requested with {@code _count}, null when the client
     *               sent none
     */
    void setCurrentPage(Integer offset, Integer count);
}
