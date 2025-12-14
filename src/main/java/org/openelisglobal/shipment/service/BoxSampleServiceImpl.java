package org.openelisglobal.shipment.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.shipment.dao.BoxSampleDAO;
import org.openelisglobal.shipment.dao.ShippingBoxDAO;
import org.openelisglobal.shipment.valueholder.BoxSample;
import org.openelisglobal.shipment.valueholder.ReceptionStatus;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for box-sample association operations
 */
@Service
@Transactional
public class BoxSampleServiceImpl implements BoxSampleService {

    private static final Logger logger = LoggerFactory.getLogger(BoxSampleServiceImpl.class);

    @Autowired
    private BoxSampleDAO boxSampleDAO;

    @Autowired
    private ShippingBoxDAO shippingBoxDAO;

    @Autowired
    private org.openelisglobal.sample.dao.SampleDAO sampleDAO;

    @Override
    @Transactional(readOnly = true)
    public BoxSample getBoxSampleById(Integer id) {
        try {
            BoxSample boxSample = boxSampleDAO.get(id).orElse(null);
            if (boxSample != null) {
                initializeAssociations(boxSample);
            }
            return boxSample;
        } catch (Exception e) {
            logger.error("Error getting box sample by ID", e);
            throw new LIMSRuntimeException("Error getting box sample by ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoxSample> getBoxSamplesByShippingBoxId(Integer shippingBoxId) {
        try {
            List<BoxSample> boxSamples = boxSampleDAO.findByShippingBoxId(shippingBoxId);
            for (BoxSample boxSample : boxSamples) {
                initializeAssociations(boxSample);
            }
            return boxSamples;
        } catch (Exception e) {
            logger.error("Error getting box samples by shipping box ID", e);
            throw new LIMSRuntimeException("Error getting box samples by shipping box ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BoxSample getBoxSampleBySampleId(Integer sampleId) {
        try {
            BoxSample boxSample = boxSampleDAO.findBySampleId(sampleId);
            if (boxSample != null) {
                initializeAssociations(boxSample);
            }
            return boxSample;
        } catch (Exception e) {
            logger.error("Error getting box sample by sample ID", e);
            throw new LIMSRuntimeException("Error getting box sample by sample ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoxSample> getBoxSamplesByReceptionStatus(Integer shippingBoxId, ReceptionStatus receptionStatus) {
        try {
            List<BoxSample> boxSamples = boxSampleDAO.findByShippingBoxIdAndReceptionStatus(shippingBoxId,
                    receptionStatus);
            for (BoxSample boxSample : boxSamples) {
                initializeAssociations(boxSample);
            }
            return boxSamples;
        } catch (Exception e) {
            logger.error("Error getting box samples by reception status", e);
            throw new LIMSRuntimeException("Error getting box samples by reception status", e);
        }
    }

    /**
     * Initialize lazy loaded associations to prevent LazyInitializationException
     */
    private void initializeAssociations(BoxSample boxSample) {
        if (boxSample.getSample() != null) {
            boxSample.getSample().getAccessionNumber(); // Force initialization
        }
        if (boxSample.getShippingBox() != null) {
            boxSample.getShippingBox().getId(); // Force initialization
        }
    }

    @Override
    public BoxSample addSampleToBox(Integer shippingBoxId, Integer sampleId, Integer systemUserId) {
        try {
            // Check if sample already in a box
            if (boxSampleDAO.existsBySampleId(sampleId)) {
                throw new IllegalStateException("Sample is already assigned to a box");
            }

            // Get shipping box
            ShippingBox box = shippingBoxDAO.get(shippingBoxId).orElseThrow(
                    () -> new IllegalArgumentException("Shipping box not found with ID: " + shippingBoxId));

            // Get existing sample from database
            Sample sample = sampleDAO.get(sampleId.toString()).orElseThrow(
                    () -> new IllegalArgumentException("Sample not found with ID: " + sampleId));

            // Create box sample association
            BoxSample boxSample = new BoxSample();
            boxSample.setShippingBox(box);
            boxSample.setSample(sample);
            boxSample.setSystemUserId(systemUserId);

            boxSample.setAddedDate(new Timestamp(System.currentTimeMillis()));
            boxSample.setReceptionStatus(ReceptionStatus.PENDING);
            boxSample.setLastupdated(new Timestamp(System.currentTimeMillis()));

            Integer id = boxSampleDAO.insert(boxSample);
            logger.info("Added sample {} to box {}", sampleId, shippingBoxId);

            // Reload to get fully initialized entity with eager loaded associations
            BoxSample savedBoxSample = boxSampleDAO.get(id).orElse(null);

            // Initialize lazy loaded associations within transaction
            if (savedBoxSample != null) {
                initializeAssociations(savedBoxSample);
            }

            return savedBoxSample;
        } catch (Exception e) {
            logger.error("Error adding sample to box", e);
            throw new LIMSRuntimeException("Error adding sample to box", e);
        }
    }

    @Override
    public void removeSampleFromBox(Integer boxSampleId) {
        try {
            BoxSample boxSample = boxSampleDAO.get(boxSampleId).orElse(null);
            if (boxSample != null) {
                boxSampleDAO.delete(boxSample);
                logger.info("Removed box sample with ID: {}", boxSampleId);
            }
        } catch (Exception e) {
            logger.error("Error removing sample from box", e);
            throw new LIMSRuntimeException("Error removing sample from box", e);
        }
    }

    @Override
    public BoxSample updateReceptionStatus(Integer boxSampleId, ReceptionStatus receptionStatus, String notes) {
        try {
            BoxSample boxSample = boxSampleDAO.get(boxSampleId)
                    .orElseThrow(() -> new IllegalArgumentException("Box sample not found with ID: " + boxSampleId));

            boxSample.setReceptionStatus(receptionStatus);
            boxSample.setReceptionNotes(notes);
            boxSample.setLastupdated(new Timestamp(System.currentTimeMillis()));

            boxSampleDAO.update(boxSample);
            logger.info("Updated reception status for box sample {} to {}", boxSampleId, receptionStatus);

            return boxSample;
        } catch (Exception e) {
            logger.error("Error updating reception status", e);
            throw new LIMSRuntimeException("Error updating reception status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSampleInBox(Integer sampleId) {
        try {
            return boxSampleDAO.existsBySampleId(sampleId);
        } catch (Exception e) {
            logger.error("Error checking if sample is in box", e);
            throw new LIMSRuntimeException("Error checking if sample is in box", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countSamplesInBox(Integer shippingBoxId) {
        try {
            return boxSampleDAO.countByShippingBoxId(shippingBoxId);
        } catch (Exception e) {
            logger.error("Error counting samples in box", e);
            throw new LIMSRuntimeException("Error counting samples in box", e);
        }
    }
}
