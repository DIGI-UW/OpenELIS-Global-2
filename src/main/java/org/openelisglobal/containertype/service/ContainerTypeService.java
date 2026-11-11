package org.openelisglobal.containertype.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.containertype.form.ContainerTypeForm;
import org.openelisglobal.containertype.form.ContainerTypeUsage;
import org.openelisglobal.containertype.form.ContainerTypeView;

/**
 * OGC-1611 FR-G1 to G3: the Container Types list, its edits and deactivation.
 */
public interface ContainerTypeService {

    List<String> DOMAINS = List.of("CLINICAL", "ENVIRONMENTAL", "VECTOR");

    /**
     * The container types matching the search (name, code, additive or material),
     * the domain and the category, A to Z; deactivated ones only when asked.
     */
    List<ContainerTypeView> list(String search, String domain, String category, boolean includeInactive);

    Optional<ContainerTypeView> get(Long id);

    /** Adds a container type, or edits it when an id is given. */
    ContainerTypeView save(Long id, ContainerTypeForm form, String sysUserId);

    Optional<ContainerTypeUsage> usage(Long id);

    Optional<ContainerTypeView> setActive(Long id, boolean active, String sysUserId);

    /**
     * Adds an Environmental container type for each environmental container
     * dictionary entry that has none (FR-G4); returns how many were added.
     */
    int importEnvironmentalContainers();
}
