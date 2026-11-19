package org.openelisglobal.microbiology.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;

public interface MicroOrganismDAO extends BaseDAO<MicroOrganism, String> {
    List<MicroOrganism> getActiveOrganisms();

    List<String> getLabGroupNames();

    List<MicroOrganism> getByIds(List<String> ids);

    /**
     * Active organisms the text finds: by part of the name, by the start of the
     * WHONET code, or by an older name or replaced code in WHONET's table. An exact
     * code leads, then the organisms WHONET marks as common, then names that start
     * with the text, then by name. Blank text matches every organism.
     */
    List<MicroOrganismMatch> searchActive(String text, int limit);

    List<String> getActiveOrganismGroups();

    Optional<MicroOrganism> findByDisplayNameIgnoreCase(String displayName);

    Optional<MicroOrganism> findByWhonetCodeIgnoreCase(String whonetCode);

    Optional<MicroOrganism> findByReference(String source, String code);

    List<MicroOrganism> getByDefaultAstPanelId(String panelId);

    long countWorkflowReferences(String organismId);

    List<MicroOrganism> search(String q, String status, String category, String sort, int offset, int limit);

    long countSearch(String q, String status, String category);
}
