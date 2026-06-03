package org.openelisglobal.vector.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorPoolFanOutServiceImpl implements VectorPoolFanOutService {

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private VectorPoolService vectorPoolService;

    @Autowired
    private VectorPoolLabelService poolLabelService;

    @Autowired
    private AnalysisService analysisService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public List<SampleItem> fanOut(SampleItem original, List<Analysis> originalAnalyses, int poolCount,
            String sysUserId) {
        if (original == null || original.getId() == null) {
            return Collections.emptyList();
        }
        if (poolCount <= 1) {
            return Collections.emptyList();
        }
        Sample sample = original.getSample();
        if (sample == null || sample.getId() == null) {
            return Collections.emptyList();
        }
        String enteredStatusId = SpringContext.getBean(IStatusService.class).getStatusID(SampleStatus.Entered);

        // Sort_order has to be unique within the sample so specimen barcodes
        // (formatted accession.sortOrder) round-trip back to a single
        // sample_item. Multiple parents fanning out in the same order have
        // overlapping windows if we just use parent.sortOrder + 1, so take
        // MAX(sort_order) across the sample's existing items — which already
        // includes any siblings produced by an earlier fanOut call in this
        // transaction.
        int baseSortOrder = nextAvailableSortOrder(sample.getId(), original);
        List<SampleItem> siblings = new ArrayList<>(poolCount);
        for (int i = 0; i < poolCount; i++) {
            SampleItem sibling = new SampleItem();
            sibling.setSample(sample);
            sibling.setTypeOfSample(original.getTypeOfSample());
            sibling.setSourceOfSample(original.getSourceOfSample());
            sibling.setUnitOfMeasure(original.getUnitOfMeasure());
            sibling.setStatusId(enteredStatusId);
            sibling.setSortOrder(String.valueOf(baseSortOrder + i));
            sibling.setParentSampleItem(null);
            sibling.setQuantity(1.0);
            sibling.setSysUserId(sysUserId);
            sibling.setCollectionDate(original.getCollectionDate());
            sibling.setReceivedDate(original.getReceivedDate());
            sibling.setCollector(original.getCollector());
            sibling.setCollectionConditions(original.getCollectionConditions());
            sibling.setCollectionMethod(original.getCollectionMethod());
            sibling.setSampleTemperature(original.getSampleTemperature());
            sibling.setSpecimenOrigin(original.getSpecimenOrigin());
            sibling.setFhirUuid(UUID.randomUUID());
            sampleItemService.insert(sibling);
            siblings.add(sibling);
        }

        long existingIntakeCount = vectorPoolService.getBySampleId(sample.getId()).stream()
                .filter(p -> p.getParentPool() == null).count();
        String lotExternalId = poolLabelService.intakeLotLabel(sample.getAccessionNumber(),
                (int) (existingIntakeCount + 1));

        VectorPool pool = new VectorPool();
        pool.setSampleId(sample.getId());
        pool.setActive(Boolean.TRUE);
        pool.setExternalId(lotExternalId);
        VectorPool persistedPool = vectorPoolService.createPoolWithMembers(pool, siblings, sysUserId);
        String poolIdAsString = persistedPool.getId() != null ? String.valueOf(persistedPool.getId()) : null;

        if (poolIdAsString != null) {
            // Re-FK every analysis currently pointing at the parent — both the
            // ones the caller passed in and any stragglers we discover via
            // lookup. The DB delete below would fail analysis_sampitem_fk
            // otherwise, and we want fanOut to be robust to callers passing
            // a partial/empty analysis list.
            java.util.Set<String> rekeyedIds = new java.util.HashSet<>();
            if (originalAnalyses != null) {
                for (Analysis analysis : originalAnalyses) {
                    if (analysis == null || analysis.getId() == null) {
                        continue;
                    }
                    analysis.setSampleItem(null);
                    analysis.setVectorPoolId(poolIdAsString);
                    analysis.setSysUserId(sysUserId);
                    analysisService.update(analysis);
                    rekeyedIds.add(analysis.getId());
                }
            }
            for (Analysis analysis : analysisService.getAnalysesBySampleItem(original)) {
                if (analysis == null || analysis.getId() == null || rekeyedIds.contains(analysis.getId())) {
                    continue;
                }
                analysis.setSampleItem(null);
                analysis.setVectorPoolId(poolIdAsString);
                analysis.setSysUserId(sysUserId);
                analysisService.update(analysis);
            }
        }

        // Hard-delete the placeholder. Per the supervisor's review, sample_item
        // should only ever contain individual organisms — the pool grouping
        // lives only in vector_pool. Pre-clear any sample_item_barcode_info row
        // for the parent: some flows (e.g. re-submit, edit, or a path that
        // builds the barcode row before fan-out runs) leave an FK reference
        // pointing at the placeholder. That row is meaningless once the parent
        // disappears, so delete it instead of letting it block the FK.
        String parentId = original.getId();
        entityManager.createQuery("DELETE FROM SampleItemBarcodeInfo b WHERE b.sampleItem.id = :id")
                .setParameter("id", parentId).executeUpdate();
        original.setSysUserId(sysUserId);
        sampleItemService.delete(original);

        LogEvent.logInfo(this.getClass().getName(), "fanOut",
                "Vector pool fan-out: deleted parent SampleItem " + parentId + ", replaced with " + siblings.size()
                        + " siblings under pool " + persistedPool.getId() + " [" + lotExternalId + "]" + " (sample "
                        + sample.getId() + "); re-FK'd " + (originalAnalyses != null ? originalAnalyses.size() : 0)
                        + " analyses");
        return siblings;
    }

    private static int parseSortOrder(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int nextAvailableSortOrder(String sampleId, SampleItem original) {
        List<SampleItem> existing = sampleItemService.getSampleItemsBySampleId(sampleId);
        int max = 0;
        if (existing != null) {
            for (SampleItem si : existing) {
                int order = parseSortOrder(si.getSortOrder());
                if (order > max) {
                    max = order;
                }
            }
        }
        if (max <= 0) {
            max = parseSortOrder(original.getSortOrder());
        }
        return max + 1;
    }
}
