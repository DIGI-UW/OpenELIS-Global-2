package org.openelisglobal.biorepository.service;

import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.biorepository.dao.BiorepositoryApprovedSampleTypeDAO;
import org.openelisglobal.biorepository.valueholder.BiorepositoryApprovedSampleType;
import org.openelisglobal.biorepository.valueholder.BiorepositoryApprovedSampleType.SampleCategory;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for BiorepositoryApprovedSampleType entity operations.
 */
@Service
public class BiorepositoryApprovedSampleTypeServiceImpl
        extends AuditableBaseObjectServiceImpl<BiorepositoryApprovedSampleType, Integer>
        implements BiorepositoryApprovedSampleTypeService {

    @Autowired
    protected BiorepositoryApprovedSampleTypeDAO baseObjectDAO;

    BiorepositoryApprovedSampleTypeServiceImpl() {
        super(BiorepositoryApprovedSampleType.class);
    }

    @Override
    protected BiorepositoryApprovedSampleTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BiorepositoryApprovedSampleType> getAllActive() {
        return baseObjectDAO.getAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BiorepositoryApprovedSampleType> getByCategory(SampleCategory category) {
        return baseObjectDAO.getByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isApprovedSampleType(String typeOfSampleId) {
        return baseObjectDAO.isApprovedSampleType(typeOfSampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isApprovedSampleType(TypeOfSample typeOfSample) {
        if (typeOfSample == null || typeOfSample.getId() == null) {
            return false;
        }
        return baseObjectDAO.isApprovedSampleType(typeOfSample.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public BiorepositoryApprovedSampleType getByTypeOfSampleId(String typeOfSampleId) {
        return baseObjectDAO.getByTypeOfSampleId(typeOfSampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActive() {
        return baseObjectDAO.countActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeOfSample> getApprovedTypeOfSamples() {
        return baseObjectDAO.getAllActive().stream().map(BiorepositoryApprovedSampleType::getTypeOfSample)
                .collect(Collectors.toList());
    }
}
