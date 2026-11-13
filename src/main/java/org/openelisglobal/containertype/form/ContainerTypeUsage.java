package org.openelisglobal.containertype.form;

import java.util.List;

/**
 * What uses a container type, for the deactivation warning (FR-G3): the tests
 * expecting it (FR-G5) and the samples taken in it.
 */
public record ContainerTypeUsage(List<String> tests, int samples) {
}
