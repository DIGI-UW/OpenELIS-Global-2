import React, { useState } from "react";
import { createPortal } from "react-dom";
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
}: {
  value?: string | null;
  onChange: (imageData: string) => void;
  label?: string;
  required?: boolean;
  disabled?: boolean;
}) => {
  const intl = useIntl();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);

  const handleImageSelect = (imageData: string) => {
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
          // View mode opens the read-only viewer; edit mode opens the picker.
          onClick={() =>
            disabled ? setIsViewModalOpen(true) : setIsModalOpen(true)
          }
          style={disabled ? { cursor: "zoom-in" } : {}}
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

      {/* Portalled out of the form: the patient form wraps its fields in a
          disabled fieldset in view mode, which would otherwise disable the
          dialog's own Close and Cancel buttons along with them. */}
      {createPortal(
        <>
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
        </>,
        document.body,
      )}
    </div>
  );
};

export default PatientImageSelector;
