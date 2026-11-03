package org.openelisglobal.spi;

/**
 * Marker for every extension point OpenELIS publishes. An extension point is an
 * interface in this artifact that extends this marker; a module implements it
 * in a class listed under {@code backend.extensions} in its manifest, and the
 * core consults the implementations through its extension registry at the
 * documented seam.
 *
 * <p>
 * Implementations are Spring beans of the module's own context: they may inject
 * the module's beans and any core service. Value types that cross an extension
 * point are defined in this artifact, never core entities.
 */
public interface ExtensionPoint {
}
