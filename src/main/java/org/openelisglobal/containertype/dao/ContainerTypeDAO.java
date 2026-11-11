package org.openelisglobal.containertype.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.containertype.valueholder.ContainerType;

public interface ContainerTypeDAO extends BaseDAO<ContainerType, Long> {

    Optional<ContainerType> findByCode(String code);

    /** How many samples record this container by name (FR-G3). */
    int countSamplesUsing(String containerName);

    /**
     * Adds an Environmental container type for each entry of the environmental
     * container dictionary that has none yet (FR-G4); returns how many it added.
     */
    int importEnvironmentalDictionary();
}
