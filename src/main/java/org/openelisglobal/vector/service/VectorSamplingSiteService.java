package org.openelisglobal.vector.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;
import org.springframework.security.access.prepost.PreAuthorize;

@CrudPrivileges(write = "PRIV_SAMPLE_TYPE_MANAGE")
/*
 * The sampling sites, species and trap types offered on environmental and
 * vector order entry are catalogue data: the VIEW gates below also accept
 * PRIV_CATALOGUE_VIEW. Creating and editing them keeps PRIV_SAMPLE_TYPE_MANAGE.
 */
public interface VectorSamplingSiteService extends BaseObjectService<VectorSamplingSite, Integer> {

    @PreAuthorize("hasAnyAuthority('PRIV_SAMPLE_TYPE_VIEW','PRIV_CATALOGUE_VIEW')")
    List<VectorSamplingSite> getByType(String type);

    @PreAuthorize("hasAnyAuthority('PRIV_SAMPLE_TYPE_VIEW','PRIV_CATALOGUE_VIEW')")
    List<VectorSamplingSite> getActive();

    @PreAuthorize("hasAnyAuthority('PRIV_SAMPLE_TYPE_VIEW','PRIV_CATALOGUE_VIEW')")
    VectorSamplingSite getByCode(String code);

    @PreAuthorize("hasAnyAuthority('PRIV_SAMPLE_TYPE_VIEW','PRIV_CATALOGUE_VIEW')")
    List<VectorSamplingSite> search(String searchTerm);

    @PreAuthorize("hasAuthority('PRIV_SAMPLE_TYPE_MANAGE')")
    VectorSamplingSite patchUpdate(Integer id, VectorSamplingSite patch, String sysUserId);

    /**
     * The sampling site an environmental or vector order names, created when the
     * order introduces a new one. Returns the site id, or {@code siteId} unchanged
     * when nothing could be resolved.
     *
     * <p>
     * Gated on PRIV_ORDER_CREATE as well as PRIV_SAMPLE_TYPE_MANAGE. Registering a
     * new surveillance site from the order form ("Add new site") is part of placing
     * the order, and the person placing it holds order:create, not the
     * catalogue-management privilege behind the inherited {@code insert}. Until
     * this method existed the form offered the action and the save then failed on
     * that inherited gate: a 403 for every non-administrator, unlogged.
     *
     * <p>
     * The semantics are the ones order entry always had. A site named by id has its
     * name, code and type synced from the form; a site named only by code is
     * reused; otherwise a new active LOCAL site is inserted.
     */
    @PreAuthorize("hasAnyAuthority('PRIV_SAMPLE_TYPE_MANAGE','PRIV_ORDER_CREATE')")
    String resolveOrCreateForOrder(String siteId, String siteName, String siteCode, String siteType, String sysUserId);
}
