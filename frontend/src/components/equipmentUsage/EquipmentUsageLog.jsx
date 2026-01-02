import { useState, useEffect, useContext, useCallback } from "react";
import { Button, Grid, Column, Loading } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import CartridgeUsageAPI from "./EquipmentUsageService";
import ChooseEquipmentModal from "./modals/ChooseEquipment";
import "./EquipmentUsage.css";

/**
 * EquipmentUsageLog Component
 *
 * Simplified form for recording equipment usage in the MNTD laboratory.
 * Features:
 * - Equipment selection (filtered to CARTRIDGE type from inventory)
 * - Equipment details display (name, serial number, department)
 * - Submit button to record usage without reducing inventory
 * - Calls onSubmitSuccess callback with API response for display in dashboard
 */
const EquipmentUsageLog = ({ onSubmitSuccess }) => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle, message }) => {
      setNotificationVisible(true);
      addNotification({
        kind,
        title,
        subtitle,
        message,
      });
    },
    [addNotification, setNotificationVisible],
  );

  // Equipment Selection State
  const [selectedEquipment, setSelectedEquipment] = useState(null);
  const [equipment, setEquipment] = useState([]);
  const [loadingEquipment, setLoadingEquipment] = useState(true);
  const [equipmentError, setEquipmentError] = useState(null);

  // Modal State
  const [showChooseEquipmentModal, setShowChooseEquipmentModal] =
    useState(false);

  // Form State
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Load Equipment (Cartridges) on Mount
  useEffect(() => {
    const fetchEquipment = async () => {
      setLoadingEquipment(true);
      setEquipmentError(null);
      try {
        // Fetch cartridges from inventory
        CartridgeUsageAPI.getCartridges(
          (data) => {
            if (data && Array.isArray(data)) {
              setEquipment(data);
            } else {
              setEquipmentError(
                intl.formatMessage({ id: "equipment.error.loadFailed" }),
              );
            }
            setLoadingEquipment(false);
          },
          (error) => {
            console.error("Error loading equipment:", error);
            setEquipmentError(
              intl.formatMessage({ id: "equipment.error.loadFailed" }),
            );
            setLoadingEquipment(false);
          },
        );
      } catch (error) {
        console.error("Error fetching equipment:", error);
        setEquipmentError(
          intl.formatMessage({ id: "equipment.error.loadFailed" }),
        );
        setLoadingEquipment(false);
      }
    };

    fetchEquipment();
  }, [intl]);

  // Handle Equipment Selection
  const handleSelectEquipment = (equipment) => {
    setSelectedEquipment(equipment);
    setShowChooseEquipmentModal(false);
  };

  // Submit to Server (Record Equipment Usage - without inventory deduction)
  const handleSubmit = () => {
    if (!selectedEquipment) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({
          id: "equipment.usage.error.selectEquipment",
        }),
      });
      return;
    }

    setIsSubmitting(true);
    try {
      // Get the first available lot for this equipment
      CartridgeUsageAPI.getAvailableLots(
        selectedEquipment.id,
        (lots) => {
          if (!lots || lots.length === 0) {
            notify({
              kind: NotificationKinds.error,
              title: intl.formatMessage({ id: "notification.error" }),
              message: intl.formatMessage({
                id: "equipment.usage.error.noLotsAvailable",
              }),
            });
            setIsSubmitting(false);
            return;
          }

          // Use the first available lot
          const lot = lots[0];
          const usageRequest = {
            itemId: selectedEquipment.id,
            lotId: lot.id,
            quantity: 1, // Equipment usage counts as 1 unit per submission
            labUnitId: userSessionDetails?.labUnit || "",
          };

          // Record equipment usage (without reducing inventory)
          CartridgeUsageAPI.recordEquipmentUsage(
            usageRequest,
            (response) => {
              console.log("=== USAGE RECORDED CALLBACK ===", response);
              if (response.ok) {
                response
                  .json()
                  .then((data) => {
                    console.log("Equipment usage recorded:", data);

                    notify({
                      kind: NotificationKinds.success,
                      title: intl.formatMessage({ id: "notification.success" }),
                      message: intl.formatMessage({
                        id: "equipment.usage.message.recordedSuccess",
                      }),
                    });

                    // Reset form
                    setSelectedEquipment(null);

                    // Call parent callback to display response in dashboard
                    if (onSubmitSuccess) {
                      onSubmitSuccess(data);
                    }

                    setIsSubmitting(false);
                  })
                  .catch((error) => {
                    console.error("Error parsing response:", error);
                    notify({
                      kind: NotificationKinds.error,
                      title: intl.formatMessage({ id: "notification.error" }),
                      message: "Failed to process response",
                    });
                    setIsSubmitting(false);
                  });
              } else {
                console.error("Response not OK:", response.status);
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({ id: "notification.error" }),
                  message: intl.formatMessage({
                    id: "equipment.usage.error.submitFailed",
                  }),
                });
                setIsSubmitting(false);
              }
            },
            (error) => {
              console.error("Error submitting usage:", error);
              notify({
                kind: NotificationKinds.error,
                title: intl.formatMessage({ id: "notification.error" }),
                message: intl.formatMessage({
                  id: "equipment.usage.error.submitFailed",
                }),
              });
              setIsSubmitting(false);
            },
          );
        },
        (error) => {
          console.error("Error loading lots:", error);
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.error" }),
            message: "Failed to load available lots",
          });
          setIsSubmitting(false);
        },
      );
    } catch (error) {
      console.error("Error submitting usage:", error);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({
          id: "equipment.usage.error.submitFailed",
        }),
      });
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <AlertDialog />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="equipmentUsageContainer">
            {/* Equipment Selection Section */}
            {loadingEquipment ? (
              <Loading description="Loading equipment..." />
            ) : (
              <div className="equipmentSelectionSection">
                <h3>
                  <FormattedMessage id="equipment.usage.selectedEquipment" />
                </h3>
                {selectedEquipment ? (
                  <div className="equipmentListSection">
                    <div className="equipmentItem">
                      <div className="equipmentItemContent">
                        <span className="equipmentName">
                          {selectedEquipment.name}
                        </span>
                        <span className="equipmentSerial">
                          {selectedEquipment.catalogNumber || "No serial"}
                        </span>
                        <Button
                          kind="ghost"
                          size="sm"
                          className="removeEquipmentBtn"
                          onClick={() => handleSelectEquipment(null)}
                        >
                          <FormattedMessage id="common.remove" />
                        </Button>
                      </div>
                    </div>
                  </div>
                ) : (
                  <Button
                    kind="primary"
                    size="sm"
                    onClick={() => setShowChooseEquipmentModal(true)}
                  >
                    <FormattedMessage id="equipment.usage.chooseEquipment" />
                  </Button>
                )}
              </div>
            )}

            {/* Equipment Details Section */}
            {selectedEquipment && (
              <div className="equipmentDetailsSection">
                <div className="detailsRow">
                  <div className="detailField">
                    <label>
                      <FormattedMessage id="equipment.name" />
                    </label>
                    <input
                      type="text"
                      value={selectedEquipment.name}
                      readOnly
                      className="detailsInput"
                    />
                  </div>
                  <div className="detailField">
                    <label>
                      <FormattedMessage id="equipment.serialNumber" />
                    </label>
                    <input
                      type="text"
                      value={selectedEquipment.catalogNumber || ""}
                      readOnly
                      className="detailsInput"
                    />
                  </div>
                  <div className="detailField">
                    <label>
                      <FormattedMessage id="equipment.department" />
                    </label>
                    <input
                      type="text"
                      value="MNTD"
                      readOnly
                      className="detailsInput"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Action Buttons */}
            <div className="equipmentUsageActionsBottom">
              <Button
                kind="primary"
                size="sm"
                onClick={handleSubmit}
                disabled={!selectedEquipment || isSubmitting}
              >
                {isSubmitting ? (
                  <FormattedMessage
                    id="common.submitting"
                    defaultMessage="Submitting..."
                  />
                ) : (
                  <FormattedMessage id="equipment.usage.submit" />
                )}
              </Button>
            </div>
          </div>
        </Column>
      </Grid>

      {/* Choose Equipment Modal */}
      <ChooseEquipmentModal
        open={showChooseEquipmentModal}
        onClose={() => setShowChooseEquipmentModal(false)}
        equipment={equipment}
        onSelectEquipment={handleSelectEquipment}
      />
    </>
  );
};

export default EquipmentUsageLog;
