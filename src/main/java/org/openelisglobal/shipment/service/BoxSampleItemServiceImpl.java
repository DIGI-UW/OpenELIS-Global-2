package org.openelisglobal.shipment.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.shipment.dao.BoxSampleItemDAO;
import org.openelisglobal.shipment.dao.ShippingBoxDAO;
import org.openelisglobal.shipment.dto.SampleItemDTO;
import org.openelisglobal.shipment.valueholder.BoxSampleItem;
import org.openelisglobal.shipment.valueholder.ReceptionStatus;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for box-sample item association operations.
 *
 * This service uses SampleItem (not Sample) as the correct granularity for
 * shipment operations.
 */
@Service
@Transactional
public class BoxSampleItemServiceImpl implements BoxSampleItemService {

    private static final Logger logger = LoggerFactory.getLogger(BoxSampleItemServiceImpl.class);

    @Autowired
    private BoxSampleItemDAO boxSampleItemDAO;

    @Autowired
    private ShippingBoxDAO shippingBoxDAO;

    @Autowired
    private org.openelisglobal.sampleitem.dao.SampleItemDAO sampleItemDAO;

    @Autowired
    private org.openelisglobal.referral.dao.ReferralDAO referralDAO;

    @Autowired
    private UnassignedSampleItemService unassignedSampleItemService;

    @Override
    @Transactional(readOnly = true)
    public BoxSampleItem getBoxSampleItemById(Integer id) {
        try {
            BoxSampleItem boxSampleItem = boxSampleItemDAO.get(id).orElse(null);
            if (boxSampleItem != null) {
                initializeAssociations(boxSampleItem);
            }
            return boxSampleItem;
        } catch (Exception e) {
            logger.error("Error getting box sample item by ID", e);
            throw new LIMSRuntimeException("Error getting box sample item by ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoxSampleItem> getBoxSampleItemsByShippingBoxId(Integer shippingBoxId) {
        try {
            List<BoxSampleItem> boxSampleItems = boxSampleItemDAO.findByShippingBoxId(shippingBoxId);
            for (BoxSampleItem boxSampleItem : boxSampleItems) {
                initializeAssociations(boxSampleItem);
            }
            return boxSampleItems;
        } catch (Exception e) {
            logger.error("Error getting box sample items by shipping box ID", e);
            throw new LIMSRuntimeException("Error getting box sample items by shipping box ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleItemDTO> getBoxSampleItemDTOsByShippingBoxId(Integer shippingBoxId) {
        try {
            List<BoxSampleItem> boxSampleItems = boxSampleItemDAO.findByShippingBoxId(shippingBoxId);
            List<SampleItemDTO> dtos = new ArrayList<>();

            for (BoxSampleItem boxSampleItem : boxSampleItems) {
                initializeAssociations(boxSampleItem);

                SampleItem sampleItem = boxSampleItem.getSampleItem();
                if (sampleItem != null) {
                    // Use UnassignedSampleItemService to get full DTO with referrals
                    SampleItemDTO dto = unassignedSampleItemService.getSampleItemById(sampleItem.getId());
                    if (dto != null) {
                        // Mark as assigned to this box
                        dto.setAssignedBoxId(shippingBoxId);
                        if (boxSampleItem.getShippingBox() != null) {
                            dto.setAssignedBoxName(boxSampleItem.getShippingBox().getBoxId());
                        }
                        // Include BoxSampleItem ID for reception status updates
                        dto.setBoxSampleItemId(boxSampleItem.getId());
                        // Include reception data
                        if (boxSampleItem.getReceptionStatus() != null) {
                            dto.setReceptionStatus(boxSampleItem.getReceptionStatus().name());
                        }
                        dto.setReceptionNotes(boxSampleItem.getReceptionNotes());
                        dtos.add(dto);
                    }
                }
            }

            return dtos;
        } catch (Exception e) {
            logger.error("Error getting box sample item DTOs by shipping box ID", e);
            throw new LIMSRuntimeException("Error getting box sample item DTOs by shipping box ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BoxSampleItem getBoxSampleItemBySampleItemId(String sampleItemId) {
        try {
            BoxSampleItem boxSampleItem = boxSampleItemDAO.findBySampleItemId(sampleItemId);
            if (boxSampleItem != null) {
                initializeAssociations(boxSampleItem);
            }
            return boxSampleItem;
        } catch (Exception e) {
            logger.error("Error getting box sample item by sample item ID", e);
            throw new LIMSRuntimeException("Error getting box sample item by sample item ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoxSampleItem> getBoxSampleItemsByReceptionStatus(Integer shippingBoxId,
            ReceptionStatus receptionStatus) {
        try {
            List<BoxSampleItem> boxSampleItems = boxSampleItemDAO.findByShippingBoxIdAndReceptionStatus(shippingBoxId,
                    receptionStatus);
            for (BoxSampleItem boxSampleItem : boxSampleItems) {
                initializeAssociations(boxSampleItem);
            }
            return boxSampleItems;
        } catch (Exception e) {
            logger.error("Error getting box sample items by reception status", e);
            throw new LIMSRuntimeException("Error getting box sample items by reception status", e);
        }
    }

    /**
     * Initialize lazy loaded associations to prevent LazyInitializationException
     */
    private void initializeAssociations(BoxSampleItem boxSampleItem) {
        if (boxSampleItem.getSampleItem() != null) {
            SampleItem si = boxSampleItem.getSampleItem();
            si.getId(); // Force initialization
            if (si.getSample() != null) {
                si.getSample().getAccessionNumber(); // Force initialization
            }
            if (si.getTypeOfSample() != null) {
                si.getTypeOfSample().getDescription(); // Force initialization
            }
        }
        if (boxSampleItem.getShippingBox() != null) {
            boxSampleItem.getShippingBox().getId(); // Force initialization
        }
    }

    @Override
    public BoxSampleItem addSampleItemToBox(Integer shippingBoxId, String sampleItemId, Integer systemUserId) {
        try {
            // Check if sample item already in a box
            if (boxSampleItemDAO.existsBySampleItemId(sampleItemId)) {
                throw new IllegalStateException("Sample item is already assigned to a box");
            }

            // Get shipping box
            ShippingBox box = shippingBoxDAO.get(shippingBoxId).orElseThrow(
                    () -> new IllegalArgumentException("Shipping box not found with ID: " + shippingBoxId));

            // Validate capacity
            if (box.getCapacity() != null && box.getCapacity() > 0) {
                int currentCount = boxSampleItemDAO.countByShippingBoxId(shippingBoxId);
                if (currentCount >= box.getCapacity()) {
                    throw new IllegalStateException(
                            "Box is at capacity (" + box.getCapacity() + "). Cannot add more samples.");
                }
            }

            // Get existing sample item from database
            SampleItem sampleItem = sampleItemDAO.get(sampleItemId)
                    .orElseThrow(() -> new IllegalArgumentException("Sample item not found with ID: " + sampleItemId));

            // Create box sample item association
            BoxSampleItem boxSampleItem = new BoxSampleItem();
            boxSampleItem.setShippingBox(box);
            boxSampleItem.setSampleItem(sampleItem);
            boxSampleItem.setSystemUserId(systemUserId);
            boxSampleItem.setAddedDate(new Timestamp(System.currentTimeMillis()));
            boxSampleItem.setReceptionStatus(ReceptionStatus.PENDING);
            boxSampleItem.setLastupdated(new Timestamp(System.currentTimeMillis()));

            Integer id = boxSampleItemDAO.insert(boxSampleItem);
            logger.info("Added sample item {} to box {}", sampleItemId, shippingBoxId);

            // Update actualSampleCount
            int newCount = boxSampleItemDAO.countByShippingBoxId(shippingBoxId);
            box.setActualSampleCount(newCount);
            box.setLastupdated(new Timestamp(System.currentTimeMillis()));
            shippingBoxDAO.update(box);

            // Assign all referrals for this sample item to this box
            Integer sampleItemIdInt = Integer.parseInt(sampleItemId);
            List<Referral> referrals = referralDAO.getReferralsBySampleItemId(sampleItemIdInt);
            for (Referral referral : referrals) {
                if (referral.getAssignedBox() == null) {
                    referral.setAssignedBox(box);
                    referral.setLastupdated(new Timestamp(System.currentTimeMillis()));
                    referralDAO.update(referral);
                    logger.info("Assigned referral {} to box {}", referral.getId(), shippingBoxId);
                }
            }

            // Reload to get fully initialized entity
            BoxSampleItem savedBoxSampleItem = boxSampleItemDAO.get(id).orElse(null);

            // Initialize lazy loaded associations within transaction
            if (savedBoxSampleItem != null) {
                initializeAssociations(savedBoxSampleItem);
            }

            return savedBoxSampleItem;
        } catch (Exception e) {
            logger.error("Error adding sample item to box", e);
            throw new LIMSRuntimeException("Error adding sample item to box", e);
        }
    }

    @Override
    public void removeSampleItemFromBox(Integer boxSampleItemId, Integer systemUserId) {
        try {
            BoxSampleItem boxSampleItem = boxSampleItemDAO.get(boxSampleItemId).orElse(null);
            if (boxSampleItem == null) {
                throw new IllegalArgumentException("Box sample item not found with ID: " + boxSampleItemId);
            }

            // Get the sample item and shipping box before deletion
            SampleItem sampleItem = boxSampleItem.getSampleItem();
            ShippingBox shippingBox = boxSampleItem.getShippingBox();

            // Unassign all referrals for this sample item that are assigned to this box
            if (sampleItem != null && shippingBox != null) {
                List<Referral> referrals = referralDAO.getReferralsBySampleItemId(Integer.parseInt(sampleItem.getId()));

                for (Referral referral : referrals) {
                    // Check if this referral is assigned to this box
                    if (referral.getAssignedBox() != null
                            && referral.getAssignedBox().getId().equals(shippingBox.getId())) {
                        // Unassign the box from this referral
                        referral.setAssignedBox(null);
                        referral.setLastupdated(new Timestamp(System.currentTimeMillis()));
                        if (systemUserId != null) {
                            referral.setSysUserId(String.valueOf(systemUserId));
                        }
                        referralDAO.update(referral);
                        logger.info("Unassigned referral {} from box {} by user {}", referral.getId(),
                                shippingBox.getId(), systemUserId);
                    }
                }
            }

            // Now delete the box sample item
            Integer boxId = shippingBox != null ? shippingBox.getId() : null;
            boxSampleItemDAO.delete(boxSampleItem);
            logger.info("Removed box sample item with ID: {} by user: {}", boxSampleItemId, systemUserId);

            // Update actualSampleCount
            if (boxId != null && shippingBox != null) {
                int newCount = boxSampleItemDAO.countByShippingBoxId(boxId);
                shippingBox.setActualSampleCount(newCount);
                shippingBox.setLastupdated(new Timestamp(System.currentTimeMillis()));
                shippingBoxDAO.update(shippingBox);
            }
        } catch (IllegalArgumentException e) {
            logger.error("Box sample item not found", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error removing sample item from box", e);
            throw new LIMSRuntimeException("Error removing sample item from box", e);
        }
    }

    @Override
    public BoxSampleItem updateReceptionStatus(Integer boxSampleItemId, ReceptionStatus receptionStatus, String notes,
            Integer systemUserId) {
        try {
            BoxSampleItem boxSampleItem = boxSampleItemDAO.get(boxSampleItemId).orElseThrow(
                    () -> new IllegalArgumentException("Box sample item not found with ID: " + boxSampleItemId));

            boxSampleItem.setReceptionStatus(receptionStatus);
            boxSampleItem.setReceptionNotes(notes);
            boxSampleItem.setLastupdated(new Timestamp(System.currentTimeMillis()));
            if (systemUserId != null) {
                boxSampleItem.setSystemUserId(systemUserId);
            }

            boxSampleItemDAO.update(boxSampleItem);
            logger.info("Updated reception status for box sample item {} to {} by user {}", boxSampleItemId,
                    receptionStatus, systemUserId);

            return boxSampleItem;
        } catch (Exception e) {
            logger.error("Error updating reception status", e);
            throw new LIMSRuntimeException("Error updating reception status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSampleItemInBox(String sampleItemId) {
        try {
            return boxSampleItemDAO.existsBySampleItemId(sampleItemId);
        } catch (Exception e) {
            logger.error("Error checking if sample item is in box", e);
            throw new LIMSRuntimeException("Error checking if sample item is in box", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countSampleItemsInBox(Integer shippingBoxId) {
        try {
            return boxSampleItemDAO.countByShippingBoxId(shippingBoxId);
        } catch (Exception e) {
            logger.error("Error counting sample items in box", e);
            throw new LIMSRuntimeException("Error counting sample items in box", e);
        }
    }
}
