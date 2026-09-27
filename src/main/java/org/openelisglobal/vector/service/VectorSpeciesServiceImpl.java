package org.openelisglobal.vector.service;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.vector.dao.VectorSpeciesDAO;
import org.openelisglobal.vector.valueholder.VectorSpecies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VectorSpeciesServiceImpl extends AuditableBaseObjectServiceImpl<VectorSpecies, Integer>
        implements VectorSpeciesService {

    @Autowired
    protected VectorSpeciesDAO baseObjectDAO;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @Autowired
    private DictionaryService dictionaryService;

    public VectorSpeciesServiceImpl() {
        super(VectorSpecies.class);
    }

    @Override
    protected VectorSpeciesDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorSpecies> getBySampleTypeId(String sampleTypeId) {
        List<VectorSpecies> list = getBaseObjectDAO().getBySampleTypeId(sampleTypeId);
        list.forEach(this::enrich);
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dictionary> getLifecycleStagesBySampleTypeId(String sampleTypeId) {
        List<VectorSpecies> species = getBaseObjectDAO().getBySampleTypeId(sampleTypeId);
        List<Dictionary> stages = new ArrayList<>();
        for (VectorSpecies s : species) {
            if (s.getLifecycleCategoryId() != null) {
                List<Dictionary> entries = dictionaryService
                        .getDictionaryEntriesByCategoryId(String.valueOf(s.getLifecycleCategoryId()));
                for (Dictionary d : entries) {
                    if ("Y".equalsIgnoreCase(d.getIsActive()) && !stages.contains(d)) {
                        stages.add(d);
                    }
                }
            }
        }
        return stages;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorSpecies> getAll() {
        List<VectorSpecies> list = super.getAll();
        list.forEach(this::enrich);
        return list;
    }

    @Override
    @Transactional
    public Integer create(VectorSpecies species, String sampleTypeId, String sysUserId) {
        if (sampleTypeId != null) {
            species.setSampleTypeId(Long.valueOf(sampleTypeId));
        }
        species.setSysUserId(sysUserId);
        return getBaseObjectDAO().insert(species);
    }

    @Override
    @Transactional
    public VectorSpecies patchUpdate(Integer id, VectorSpecies patch, String sampleTypeId, String sysUserId) {
        VectorSpecies existing = getBaseObjectDAO().get(id)
                .orElseThrow(() -> new ObjectNotFoundException(id, VectorSpecies.class.getName()));
        existing.setGenus(patch.getGenus());
        existing.setSpecies(patch.getSpecies());
        existing.setSubspecies(patch.getSubspecies());
        existing.setPathogenCategoryId(patch.getPathogenCategoryId());
        existing.setLifecycleCategoryId(patch.getLifecycleCategoryId());
        if (patch.getActive() != null) {
            existing.setActive(patch.getActive());
        }
        if (sampleTypeId != null) {
            existing.setSampleTypeId(Long.valueOf(sampleTypeId));
        }
        existing.setSysUserId(sysUserId);
        VectorSpecies updated = getBaseObjectDAO().update(existing);
        enrich(updated);
        return updated;
    }

    private void enrich(VectorSpecies s) {
        if (s.getSampleTypeId() != null) {
            TypeOfSample tos = typeOfSampleService.getTypeOfSampleById(String.valueOf(s.getSampleTypeId()));
            s.setSampleType(tos);
        }
        if (s.getPathogenCategoryId() != null) {
            DictionaryCategory cat = dictionaryCategoryService.get(String.valueOf(s.getPathogenCategoryId()));
            s.setPathogenCategory(cat);
        }
        if (s.getLifecycleCategoryId() != null) {
            DictionaryCategory cat = dictionaryCategoryService.get(String.valueOf(s.getLifecycleCategoryId()));
            s.setLifecycleCategory(cat);
        }
    }
}
