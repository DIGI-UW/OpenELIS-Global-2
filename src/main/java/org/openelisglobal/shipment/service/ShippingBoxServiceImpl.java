package org.openelisglobal.shipment.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.shipment.dao.BoxSampleDAO;
import org.openelisglobal.shipment.dao.ShippingBoxDAO;
import org.openelisglobal.shipment.valueholder.BoxState;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for shipping box operations Per Constitution V.4: Services
 * compile all DTOs within transaction to prevent LazyInitializationException
 */
@Service
@Transactional
public class ShippingBoxServiceImpl implements ShippingBoxService {

    private static final Logger logger = LoggerFactory.getLogger(ShippingBoxServiceImpl.class);

    @Autowired
    private ShippingBoxDAO shippingBoxDAO;

    @Autowired
    private BoxSampleDAO boxSampleDAO;

    @Override
    @Transactional(readOnly = true)
    public List<ShippingBox> getAllActiveBoxes() {
        try {
            List<ShippingBox> boxes = shippingBoxDAO.findAllActive();
            // Initialize lazy-loaded associations
            boxes.forEach(this::initializeLazyAssociations);
            return boxes;
        } catch (Exception e) {
            logger.error("Error getting all active shipping boxes", e);
            throw new LIMSRuntimeException("Error getting all active shipping boxes", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingBox getBoxById(Integer id) {
        try {
            ShippingBox box = shippingBoxDAO.get(id).orElse(null);
            initializeLazyAssociations(box);
            return box;
        } catch (Exception e) {
            logger.error("Error getting shipping box by ID", e);
            throw new LIMSRuntimeException("Error getting shipping box by ID", e);
        }
    }

    /**
     * Initialize lazy-loaded associations to prevent LazyInitializationException
     */
    private void initializeLazyAssociations(ShippingBox box) {
        if (box != null) {
            // Force initialization of destinationFacility
            if (box.getDestinationFacility() != null) {
                box.getDestinationFacility().getOrganizationName();
            }
            // Force initialization of createdBy
            if (box.getCreatedBy() != null) {
                box.getCreatedBy().getNameForDisplay();
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingBox getBoxByBoxId(String boxId) {
        try {
            ShippingBox box = shippingBoxDAO.findByBoxId(boxId);
            initializeLazyAssociations(box);
            return box;
        } catch (Exception e) {
            logger.error("Error getting shipping box by boxId", e);
            throw new LIMSRuntimeException("Error getting shipping box by boxId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingBox getBoxByFhirUuid(UUID fhirUuid) {
        try {
            ShippingBox box = shippingBoxDAO.findByFhirUuid(fhirUuid);
            initializeLazyAssociations(box);
            return box;
        } catch (Exception e) {
            logger.error("Error getting shipping box by FHIR UUID", e);
            throw new LIMSRuntimeException("Error getting shipping box by FHIR UUID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingBox> getBoxesByState(BoxState state) {
        try {
            List<ShippingBox> boxes = shippingBoxDAO.findByState(state);
            boxes.forEach(this::initializeLazyAssociations);
            return boxes;
        } catch (Exception e) {
            logger.error("Error getting shipping boxes by state", e);
            throw new LIMSRuntimeException("Error getting shipping boxes by state", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingBox> getBoxesByDestinationFacility(Integer facilityId) {
        try {
            List<ShippingBox> boxes = shippingBoxDAO.findByDestinationFacilityId(facilityId);
            boxes.forEach(this::initializeLazyAssociations);
            return boxes;
        } catch (Exception e) {
            logger.error("Error getting shipping boxes by destination facility", e);
            throw new LIMSRuntimeException("Error getting shipping boxes by destination facility", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingBox> getBoxesByCreatedDateRange(Timestamp startDate, Timestamp endDate) {
        try {
            List<ShippingBox> boxes = shippingBoxDAO.findByCreatedDateRange(startDate, endDate);
            boxes.forEach(this::initializeLazyAssociations);
            return boxes;
        } catch (Exception e) {
            logger.error("Error getting shipping boxes by created date range", e);
            throw new LIMSRuntimeException("Error getting shipping boxes by created date range", e);
        }
    }

    @Override
    public ShippingBox createBox(ShippingBox box) {
        try {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            box.setCreatedDate(now);
            box.setLastupdated(now);
            box.setState(BoxState.DRAFT);
            box.setArchived(false);

            Integer id = shippingBoxDAO.insert(box);
            logger.info("Created shipping box with ID: {}", id);
            return shippingBoxDAO.get(id).orElse(null);
        } catch (Exception e) {
            logger.error("Error creating shipping box", e);
            throw new LIMSRuntimeException("Error creating shipping box", e);
        }
    }

    @Override
    public ShippingBox updateBox(ShippingBox box) {
        try {
            box.setLastupdated(new Timestamp(System.currentTimeMillis()));
            shippingBoxDAO.update(box);
            logger.info("Updated shipping box with ID: {}", box.getId());
            return box;
        } catch (Exception e) {
            logger.error("Error updating shipping box", e);
            throw new LIMSRuntimeException("Error updating shipping box", e);
        }
    }

    @Override
    public void deleteBox(Integer id) {
        try {
            ShippingBox box = shippingBoxDAO.get(id).orElse(null);
            if (box != null) {
                box.setArchived(true);
                box.setArchivedDate(new Timestamp(System.currentTimeMillis()));
                shippingBoxDAO.update(box);
                logger.info("Archived shipping box with ID: {}", id);
            }
        } catch (Exception e) {
            logger.error("Error deleting shipping box", e);
            throw new LIMSRuntimeException("Error deleting shipping box", e);
        }
    }

    @Override
    public ShippingBox changeBoxState(Integer id, BoxState newState) {
        try {
            ShippingBox box = shippingBoxDAO.get(id)
                    .orElseThrow(() -> new IllegalArgumentException("Box not found with ID: " + id));

            box.setState(newState);
            box.setLastupdated(new Timestamp(System.currentTimeMillis()));

            // Update date fields based on state
            Timestamp now = new Timestamp(System.currentTimeMillis());
            if (newState == BoxState.SENT) {
                box.setSentDate(now);
            } else if (newState == BoxState.RECEIVED) {
                box.setReceivedDate(now);
            } else if (newState == BoxState.RECONCILED) {
                box.setReconciledDate(now);
            }

            shippingBoxDAO.update(box);
            logger.info("Changed box {} state to {}", id, newState);
            return box;
        } catch (Exception e) {
            logger.error("Error changing box state", e);
            throw new LIMSRuntimeException("Error changing box state", e);
        }
    }

    @Override
    public ShippingBox markReadyToSend(Integer id) {
        try {
            ShippingBox box = shippingBoxDAO.get(id)
                    .orElseThrow(() -> new IllegalArgumentException("Box not found with ID: " + id));

            // Validate box has at least one sample
            int sampleCount = boxSampleDAO.countByShippingBoxId(id);
            if (sampleCount == 0) {
                throw new IllegalStateException("Cannot mark empty box as ready to send");
            }

            return changeBoxState(id, BoxState.READY_TO_SEND);
        } catch (Exception e) {
            logger.error("Error marking box as ready to send", e);
            throw new LIMSRuntimeException("Error marking box as ready to send", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBoxesForDashboard() {
        try {
            List<ShippingBox> boxes = shippingBoxDAO.findAllActive();
            List<Map<String, Object>> result = new ArrayList<>();

            // Compile all DTOs within transaction
            for (ShippingBox box : boxes) {
                Map<String, Object> boxData = new HashMap<>();
                boxData.put("id", box.getId());
                boxData.put("boxId", box.getBoxId());
                boxData.put("state", box.getState().name());
                boxData.put("createdDate", box.getCreatedDate());
                boxData.put("sampleCount", boxSampleDAO.countByShippingBoxId(box.getId()));

                // Eagerly fetch destination facility name within transaction
                if (box.getDestinationFacility() != null) {
                    boxData.put("destinationFacilityName", box.getDestinationFacility().getOrganizationName());
                }

                result.add(boxData);
            }

            return result;
        } catch (Exception e) {
            logger.error("Error getting boxes for dashboard", e);
            throw new LIMSRuntimeException("Error getting boxes for dashboard", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countBoxesByState(BoxState state) {
        try {
            return shippingBoxDAO.countByState(state);
        } catch (Exception e) {
            logger.error("Error counting boxes by state", e);
            throw new LIMSRuntimeException("Error counting boxes by state", e);
        }
    }
}
