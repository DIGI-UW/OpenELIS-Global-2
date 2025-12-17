import React, { useState, useCallback, useEffect } from "react";
import {
  Modal,
  NumberInput,
  TextArea,
  ComboBox,
  FormLabel,
  Stack,
  Dropdown,
  InlineNotification,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryManagementAPI, InventoryLotAPI } from "./InventoryService";
import { getFromOpenElisServer } from "../utils/Utils";

const RecordUsageModal = ({ open, onClose, onSave, lot, item }) => {
  const intl = useIntl();

  const [formData, setFormData] = useState({
    quantityUsed: 1,
    testResultId: "",
    notes: "",
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [showExpirationWarning, setShowExpirationWarning] = useState(false);
  const [overrideConfirmed, setOverrideConfirmed] = useState(false);

  // FEFO mode state (when item is provided instead of lot)
  const [availableLots, setAvailableLots] = useState([]);
  const [selectedLot, setSelectedLot] = useState(null);
  const [loadingLots, setLoadingLots] = useState(false);
  const [fefoRecommendation, setFefoRecommendation] = useState(null);

  // Determine mode: lot mode (existing) vs item mode (FEFO)
  const isItemMode = !!item && !lot;
  const activeLot = isItemMode ? selectedLot : lot;

  // Fetch FEFO-sorted lots when in item mode
  useEffect(() => {
    if (!isItemMode || !item || !open) {
      return;
    }

    const fetchAvailableLots = async () => {
      setLoadingLots(true);
      setError(null);

      try {
        const lots = await InventoryLotAPI.getAvailableByItem(item.id);

        if (lots && lots.length > 0) {
          setAvailableLots(lots);
          // Auto-select first lot (FEFO - earliest expiring)
          setSelectedLot(lots[0]);
          setFefoRecommendation(lots[0]);
        } else {
          setAvailableLots([]);
          setSelectedLot(null);
          setFefoRecommendation(null);
          setError(intl.formatMessage({ id: "usage.fefo.noAvailableLots" }));
        }
      } catch (err) {
        console.error("Error fetching available lots:", err);
        setError(
          err.message || intl.formatMessage({ id: "usage.fefo.fetchError" }),
        );
        setAvailableLots([]);
        setSelectedLot(null);
      } finally {
        setLoadingLots(false);
      }
    };

    fetchAvailableLots();
  }, [isItemMode, item, open, intl]);

  const handleChange = (field, value) => {
    setFormData((prev) => {
      if (prev[field] === value) {
        return prev;
      }
      return { ...prev, [field]: value };
    });
    setError(null);
  };

  const isLotExpired = (lot) => {
    if (!lot || !lot.expirationDate) return false;
    const expDate = new Date(lot.expirationDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return expDate < today;
  };

  const searchAccessionNumbers = useCallback((query) => {
    if (!query || query.length < 2) {
      setSearchResults([]);
      return;
    }

    setSearchLoading(true);
    getFromOpenElisServer(
      `/rest/samples/search?accessionNumber=${encodeURIComponent(query)}&includeTests=true`,
      (response) => {
        setSearchLoading(false);
        if (response && response.samples) {
          const items = response.samples.map((sample) => ({
            id: sample.accessionNumber,
            text: `${sample.accessionNumber} - ${sample.patientName || "Unknown"}`,
          }));
          setSearchResults(items);
        } else {
          setSearchResults([]);
        }
      },
    );
  }, []);

  const validate = () => {
    if (!formData.quantityUsed || formData.quantityUsed <= 0) {
      setError("Quantity must be greater than 0");
      return false;
    }

    if (!activeLot) {
      setError(intl.formatMessage({ id: "usage.error.noLotSelected" }));
      return false;
    }

    if (
      activeLot &&
      activeLot.currentQuantity &&
      formData.quantityUsed > activeLot.currentQuantity
    ) {
      setError(
        `Cannot use ${formData.quantityUsed} units. Only ${activeLot.currentQuantity} units available.`,
      );
      return false;
    }

    return true;
  };

  const handleSubmit = async () => {
    if (!validate()) return;

    // Check if lot is expired and user hasn't confirmed override
    if (isLotExpired(activeLot) && !overrideConfirmed) {
      setShowExpirationWarning(true);
      return;
    }

    setSaving(true);
    setError(null);

    try {
      await InventoryManagementAPI.consume({
        itemId: String(activeLot.inventoryItem.id),
        quantity: formData.quantityUsed,
        testResultId: formData.testResultId || null,
        analysisId: null,
        overrideExpiration: overrideConfirmed,
      });

      setFormData({
        quantityUsed: 1,
        testResultId: "",
        notes: "",
      });
      setOverrideConfirmed(false);

      onSave();
    } catch (err) {
      console.error("Error recording usage:", err);
      setError(err.message || "Error recording usage");
    } finally {
      setSaving(false);
    }
  };

  const handleConfirmOverride = () => {
    setShowExpirationWarning(false);
    setOverrideConfirmed(true);
    // After setting override flag, submit the form
    setTimeout(() => {
      handleSubmit();
    }, 0);
  };

  const handleCancelOverride = () => {
    setShowExpirationWarning(false);
    setOverrideConfirmed(false);
  };

  const handleCancel = () => {
    setFormData({
      quantityUsed: 1,
      testResultId: "",
      notes: "",
    });
    setError(null);
    setOverrideConfirmed(false);
    setShowExpirationWarning(false);
    setSelectedLot(null);
    setAvailableLots([]);
    setFefoRecommendation(null);
    onClose();
  };

  const handleLotChange = ({ selectedItem }) => {
    setSelectedLot(selectedItem);
    setError(null);
  };

  const formatLotDropdownItem = (lot) => {
    if (!lot) return "";
    const expDate = lot.expirationDate
      ? new Date(lot.expirationDate).toLocaleDateString()
      : "No expiration";
    return `${lot.lotNumber} - Expires: ${expDate} - Qty: ${lot.currentQuantity} ${lot.inventoryItem?.units || "units"}`;
  };

  // Modal can be opened in two modes:
  // 1. Lot mode: lot prop is provided (existing behavior)
  // 2. Item mode: item prop is provided (new FEFO behavior)
  if (!lot && !item) return null;

  return (
    <>
      <Modal
        open={open}
        onRequestClose={handleCancel}
        onRequestSubmit={handleSubmit}
        modalHeading={intl.formatMessage({ id: "usage.record.title" })}
        primaryButtonText={intl.formatMessage({ id: "button.record" })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        primaryButtonDisabled={saving || loadingLots}
        size="sm"
      >
        <Stack gap={6}>
          {/* FEFO recommendation notification (item mode only) */}
          {isItemMode && fefoRecommendation && selectedLot && (
            <InlineNotification
              kind={
                selectedLot.id === fefoRecommendation.id ? "info" : "warning"
              }
              title={
                selectedLot.id === fefoRecommendation.id
                  ? intl.formatMessage({ id: "usage.fefo.recommended" })
                  : intl.formatMessage({ id: "usage.fefo.notRecommended" })
              }
              subtitle={
                selectedLot.id === fefoRecommendation.id
                  ? intl.formatMessage(
                      { id: "usage.fefo.recommended.message" },
                      {
                        lotNumber: fefoRecommendation.lotNumber,
                        expirationDate: fefoRecommendation.expirationDate
                          ? new Date(
                              fefoRecommendation.expirationDate,
                            ).toLocaleDateString()
                          : "N/A",
                      },
                    )
                  : intl.formatMessage(
                      { id: "usage.fefo.notRecommended.message" },
                      {
                        recommendedLot: fefoRecommendation.lotNumber,
                      },
                    )
              }
              lowContrast
              hideCloseButton
            />
          )}

          {/* Loading state for item mode */}
          {isItemMode && loadingLots && (
            <div style={{ textAlign: "center", padding: "1rem" }}>
              <Loading
                description={intl.formatMessage({
                  id: "usage.fefo.loading",
                })}
                withOverlay={false}
                small
              />
            </div>
          )}

          {/* Item mode: Lot selection dropdown with FEFO */}
          {isItemMode && !loadingLots && (
            <>
              <div>
                <FormLabel>
                  <FormattedMessage id="catalog.item.name" />
                </FormLabel>
                <p>
                  <strong>{item.name}</strong>
                </p>
              </div>

              <Dropdown
                id="lot-selection"
                titleText={intl.formatMessage({ id: "usage.selectLot" })}
                label={
                  selectedLot
                    ? formatLotDropdownItem(selectedLot)
                    : intl.formatMessage({ id: "usage.selectLot.placeholder" })
                }
                items={availableLots}
                itemToString={formatLotDropdownItem}
                onChange={handleLotChange}
                selectedItem={selectedLot}
                disabled={availableLots.length === 0}
              />
            </>
          )}

          {/* Lot mode: Display fixed lot (existing behavior) */}
          {!isItemMode && lot && (
            <>
              <div>
                <FormLabel>
                  <FormattedMessage id="lot.number" />
                </FormLabel>
                <p>
                  <strong>{lot.lotNumber}</strong>
                </p>
              </div>

              <div>
                <FormLabel>
                  <FormattedMessage id="lot.currentQuantity" />
                </FormLabel>
                <p>
                  <strong>
                    {lot.currentQuantity} {lot.inventoryItem?.units || "units"}
                  </strong>
                </p>
              </div>
            </>
          )}

          {/* Show current quantity for selected lot in item mode */}
          {isItemMode && selectedLot && (
            <div>
              <FormLabel>
                <FormattedMessage id="lot.currentQuantity" />
              </FormLabel>
              <p>
                <strong>
                  {selectedLot.currentQuantity}{" "}
                  {selectedLot.inventoryItem?.units || "units"}
                </strong>
              </p>
            </div>
          )}

          <NumberInput
            id="quantityUsed"
            label={intl.formatMessage({ id: "usage.quantityUsed" })}
            min={1}
            max={activeLot?.currentQuantity}
            value={formData.quantityUsed}
            onChange={(e, { value }) => handleChange("quantityUsed", value)}
            invalidText={error}
            invalid={!!error}
            helperText={intl.formatMessage({ id: "usage.quantityUsed.helper" })}
            disabled={!activeLot}
          />

          <ComboBox
            id="testResultId"
            titleText={intl.formatMessage({ id: "usage.testResultId" })}
            placeholder={intl.formatMessage({
              id: "usage.testResultId.placeholder",
            })}
            items={searchResults}
            itemToString={(item) => (item ? item.text : "")}
            onInputChange={(query) => searchAccessionNumbers(query)}
            onChange={({ selectedItem }) => {
              handleChange("testResultId", selectedItem ? selectedItem.id : "");
            }}
            helperText="Start typing an accession number to search, or type manually"
          />

          <TextArea
            id="notes"
            labelText={intl.formatMessage({ id: "usage.notes" })}
            value={formData.notes}
            onChange={(e) => handleChange("notes", e.target.value)}
            placeholder={intl.formatMessage({ id: "usage.notes.placeholder" })}
            rows={3}
          />

          {error && (
            <div className="error-message" style={{ color: "#da1e28" }}>
              {error}
            </div>
          )}
        </Stack>
      </Modal>

      <Modal
        open={showExpirationWarning}
        onRequestClose={handleCancelOverride}
        onRequestSubmit={handleConfirmOverride}
        modalHeading={intl.formatMessage({
          id: "usage.expiration.warning.title",
        })}
        primaryButtonText={intl.formatMessage({ id: "button.continue" })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        danger
        size="xs"
      >
        <p>
          <FormattedMessage
            id="usage.expiration.warning.message"
            values={{
              lotNumber: activeLot?.lotNumber || "N/A",
              expirationDate: activeLot?.expirationDate
                ? new Date(activeLot.expirationDate).toLocaleDateString()
                : "N/A",
            }}
          />
        </p>
      </Modal>
    </>
  );
};

export default RecordUsageModal;
