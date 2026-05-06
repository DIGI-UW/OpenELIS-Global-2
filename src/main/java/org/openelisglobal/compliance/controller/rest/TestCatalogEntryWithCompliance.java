package org.openelisglobal.compliance.controller.rest;

import lombok.Getter;
import lombok.Setter;

/**
 * {@link TestCatalogEntry} extended with per-test compliance counts. Backs the
 * Tab 2 overview table — assembling on the server avoids a brittle client-side
 * "match by id across two arrays" join.
 */
@Setter
@Getter
public class TestCatalogEntryWithCompliance extends TestCatalogEntry {

    private int thresholdCount;
    private int standardCount;

}
