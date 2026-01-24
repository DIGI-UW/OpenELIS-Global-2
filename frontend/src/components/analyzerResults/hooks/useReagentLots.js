import { useState, useCallback } from "react";

/**
 * Custom hook for reagent lot selection with FIFO validation.
 * Implements strict FIFO mode configuration (MAJ-004).
 *
 * @param {boolean} strictFifoMode - If true, blocks non-FIFO lot selections
 * @returns {Object} Reagent lot selection state and methods
 */
export const useReagentLots = (strictFifoMode = false) => {
  const [selectedReagentLots, setSelectedReagentLots] = useState({});
  const [availableReagentLots, setAvailableReagentLots] = useState({});
  const [fifoWarnings, setFifoWarnings] = useState({});
  const [showReagentModal, setShowReagentModal] = useState(false);
  const [fifoOverrideApproved, setFifoOverrideApproved] = useState({});

  /**
   * Check if a lot is the FIFO lot (oldest/first received)
   */
  const isFifoLot = useCallback(
    (reagentId, lotNumber) => {
      const lots = availableReagentLots[reagentId];
      if (!lots || lots.length === 0) return true;
      return lots[0].lotNumber === lotNumber;
    },
    [availableReagentLots],
  );

  /**
   * Get the FIFO lot for a reagent
   */
  const getFifoLot = useCallback(
    (reagentId) => {
      const lots = availableReagentLots[reagentId];
      return lots?.[0] || null;
    },
    [availableReagentLots],
  );

  /**
   * Handle reagent lot change with strict FIFO mode support (MAJ-004).
   * In strict mode, non-FIFO selections are blocked unless supervisor override is approved.
   *
   * @param {string} reagentId - The reagent ID
   * @param {string} lotNumber - The selected lot number
   * @param {boolean} checked - Whether the lot is being selected
   * @returns {Object} Result with success status and any warnings
   */
  const handleReagentLotChange = useCallback(
    (reagentId, lotNumber, checked) => {
      if (!checked) {
        setSelectedReagentLots((prev) => {
          const updated = { ...prev };
          delete updated[reagentId];
          return updated;
        });
        setFifoWarnings((prev) => {
          const updated = { ...prev };
          delete updated[reagentId];
          return updated;
        });
        return { success: true };
      }

      const isFirstLot = isFifoLot(reagentId, lotNumber);
      const fifoLot = getFifoLot(reagentId);

      if (!isFirstLot) {
        if (strictFifoMode && !fifoOverrideApproved[reagentId]) {
          setFifoWarnings((prev) => ({
            ...prev,
            [reagentId]: {
              message: `FIFO violation: Oldest lot (${fifoLot?.lotNumber}) must be used first`,
              requiresOverride: true,
              blockedLot: lotNumber,
            },
          }));
          return {
            success: false,
            blocked: true,
            message: `Strict FIFO mode: Cannot select lot ${lotNumber}. Use oldest lot ${fifoLot?.lotNumber} first.`,
          };
        }

        setFifoWarnings((prev) => ({
          ...prev,
          [reagentId]: {
            message: `Not FIFO - Older lot available (${fifoLot?.lotNumber})`,
            requiresOverride: false,
          },
        }));
      } else {
        setFifoWarnings((prev) => {
          const updated = { ...prev };
          delete updated[reagentId];
          return updated;
        });
      }

      setSelectedReagentLots((prev) => ({ ...prev, [reagentId]: lotNumber }));
      return { success: true };
    },
    [isFifoLot, getFifoLot, strictFifoMode, fifoOverrideApproved],
  );

  /**
   * Approve FIFO override for a specific reagent (supervisor action)
   */
  const approveFifoOverride = useCallback((reagentId, approverUserId) => {
    setFifoOverrideApproved((prev) => ({
      ...prev,
      [reagentId]: {
        approved: true,
        approvedBy: approverUserId,
        approvedAt: new Date().toISOString(),
      },
    }));
    setFifoWarnings((prev) => {
      const updated = { ...prev };
      if (updated[reagentId]) {
        updated[reagentId] = {
          ...updated[reagentId],
          requiresOverride: false,
          overrideApproved: true,
        };
      }
      return updated;
    });
  }, []);

  /**
   * Reset all FIFO overrides
   */
  const resetFifoOverrides = useCallback(() => {
    setFifoOverrideApproved({});
  }, []);

  /**
   * Check if any reagent has a FIFO warning
   */
  const hasFifoWarnings = useCallback(() => {
    return Object.keys(fifoWarnings).length > 0;
  }, [fifoWarnings]);

  /**
   * Check if any reagent is blocked due to strict FIFO
   */
  const hasBlockedSelections = useCallback(() => {
    return Object.values(fifoWarnings).some((w) => w.requiresOverride);
  }, [fifoWarnings]);

  /**
   * Clear all FIFO warnings
   */
  const clearFifoWarnings = useCallback(() => {
    setFifoWarnings({});
  }, []);

  return {
    selectedReagentLots,
    availableReagentLots,
    fifoWarnings,
    showReagentModal,
    fifoOverrideApproved,
    setSelectedReagentLots,
    setAvailableReagentLots,
    setShowReagentModal,
    setFifoWarnings,
    handleReagentLotChange,
    isFifoLot,
    getFifoLot,
    approveFifoOverride,
    resetFifoOverrides,
    hasFifoWarnings,
    hasBlockedSelections,
    clearFifoWarnings,
  };
};

export default useReagentLots;
