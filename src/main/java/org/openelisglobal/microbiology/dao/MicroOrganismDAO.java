package org.openelisglobal.microbiology.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;

public interface MicroOrganismDAO extends BaseDAO<MicroOrganism, String> {
    List<MicroOrganism> getActiveOrganisms();

    List<MicroOrganism> getByIds(List<String> ids);

    /**
     * Active organisms whose name, WHONET name or WHONET code matches the text. An
     * exact WHONET code leads, then the most common in human infections, then those
     * WHONET has a code for. Blank text matches every organism.
     */
    List<MicroOrganism> searchActive(String text, int limit);

    List<String> getActiveOrganismGroups();

    Optional<MicroOrganism> findByDisplayNameIgnoreCase(String displayName);

    Optional<MicroOrganism> findByWhonetCodeIgnoreCase(String whonetCode);

    Optional<MicroOrganism> findByReference(String source, String code);

    List<MicroOrganism> getByDefaultAstPanelId(String panelId);

    long countWorkflowReferences(String organismId);

    List<MicroOrganism> search(String q, String status, String category, String sort, int offset, int limit);

    long countSearch(String q, String status, String category);
}
