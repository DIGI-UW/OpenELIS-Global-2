package org.openelisglobal.containertype.form;

import java.util.List;

/**
 * What uses a container type, for the deactivation warning (FR-G3). Tests name
 * expected containers from P2 on; until then the list is empty.
 */
public record ContainerTypeUsage(List<String> tests, int samples) {
}
