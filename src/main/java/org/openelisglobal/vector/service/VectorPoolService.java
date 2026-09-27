package org.openelisglobal.vector.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.vector.valueholder.VectorPool;

public interface VectorPoolService extends BaseObjectService<VectorPool, Integer> {

    /**
     * Null-safe pool lookup — returns empty rather than throwing
     * {@code ObjectNotFoundException} when the id is not found. Use this instead of
     * {@link #get(Integer)} inside {@code @Transactional} methods that need to
     * handle missing pools gracefully without poisoning the surrounding
     * transaction.
     */
    Optional<VectorPool> findById(Integer id);

    /** Pool's {@code sampleId} must be set before calling. */
    VectorPool createPoolWithMembers(VectorPool pool, List<SampleItem> members, String sysUserId);

    List<VectorPool> getBySampleId(String sampleId);

    /** Direct sub-pools only (not transitive). */
    List<VectorPool> getByParentPoolId(Integer parentPoolId);

    /**
     * Returns the SampleItems that constitute this pool (the N organisms in a pool
     * of N), ordered by sortOrder. Read path used by display layers that need to
     * render a pool-anchored analysis with a representative organism (sample type,
     * sort order, accession).
     */
    List<SampleItem> getMembersByPoolId(Integer poolId);

    /**
     * Count-only variant of {@link #getMembersByPoolId(Integer)} for display paths
     * that just need the pool size (e.g., result rows showing "Pool of N"). Avoids
     * hydrating SampleItem entities for every pool-anchored row.
     */
    int countMembersByPoolId(Integer poolId);

    /**
     * Returns the first non-voided pool member by sortOrder, or empty if the pool
     * has none. Used by display paths that need a representative SampleItem from a
     * pool (e.g., to render sample type / accession on a pool-anchored row). Backed
     * by a {@code setMaxResults(1)} JPQL so large pools don't hydrate every member
     * just to pick one.
     */
    Optional<SampleItem> getFirstNonVoidedMemberByPoolId(Integer poolId);

    /**
     * Returns the intake pool (parentPool IS NULL) that contains the given
     * SampleItem, or null if this SampleItem has no pool membership. Used by the
     * result display layer to attach pool metadata to SampleItem-anchored analyses
     * that were copied from a pool by confirmResultForAllMembers.
     */
    VectorPool getIntakePoolBySampleItemId(String sampleItemId);
}
