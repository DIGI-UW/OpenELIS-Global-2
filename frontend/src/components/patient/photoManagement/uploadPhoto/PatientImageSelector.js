import React, { useState } from "react";
import { UserAvatar, View } from "@carbon/icons-react";
import { Modal } from "@carbon/react";
import ImagePreviewModal from "./ImagePreviewModal";
import "./PatientImageSelector.css";
import { useIntl } from "react-intl";

const PatientImageSelector = ({
  value = null,
  onChange,
  label = "",
  required = false,
  disabled = false,
}) => {
  const intl = useIntl();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);

  const handleImageSelect = (imageData) => {
    onChange(imageData);
  };

  return (
    <div className="patient-image-selector">
      <label className="image-selector-label">
        {label}
        {required && <span className="required-indicator"> *</span>}
      </label>

      <div className="image-selector-content">
        <div
          className="image-display"
          onClick={() => !disabled && setIsModalOpen(true)}
          style={disabled ? { opacity: 0.5, cursor: "not-allowed" } : {}}
        >
          {value ? (
            <div className="image-with-overlay">
              <img src={value} alt="Patient photo" className="patient-image" />
              <div className="image-overlay">
                <span className="overlay-text">
                  {" "}
                  {intl.formatMessage({ id: "patient.photo.retake" })}
                </span>
              </div>
              <button
                type="button"
                className="patient-photo-view-btn"
                onClick={(e) => {
                  e.stopPropagation();
                  setIsViewModalOpen(true);
                }}
                title={intl.formatMessage({ id: "patient.photo.view" })}
                aria-label={intl.formatMessage({ id: "patient.photo.view" })}
              >
                <View size={16} />
              </button>
            </div>
          ) : (
            <div className="image-placeholder">
              <UserAvatar size={48} />
              <span className="placeholder-text">
                {" "}
                {intl.formatMessage({ id: "patient.photo.add" })}
              </span>
            </div>
          )}
        </div>
      </div>

      <ImagePreviewModal
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onImageSelect={handleImageSelect}
        currentImage={value}
      />

      <Modal
        open={isViewModalOpen}
        onRequestClose={() => setIsViewModalOpen(false)}
        modalHeading={intl.formatMessage({ id: "patient.photo.view" })}
        passiveModal
        size="lg"
      >
        {value && (
          <div className="patient-photo-view-container">
            <img
              src={value}
              alt={intl.formatMessage({ id: "patient.photo.preview.alt" })}
              className="patient-photo-view-image"
            />
          </div>
        )}
      </Modal>
    </div>
  );
};

export default PatientImageSelector;
