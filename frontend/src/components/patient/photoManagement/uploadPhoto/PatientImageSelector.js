import React, { useState } from "react";
import { UserAvatar } from "@carbon/icons-react";
import ImagePreviewModal from "./ImagePreviewModal";
import "./PatientImageSelector.css";
import { useIntl } from "react-intl";

const PatientImageSelector = ({
  value = null,
  onChange,
  label = "",
  required = false,
}) => {
  const intl = useIntl();
  const [isModalOpen, setIsModalOpen] = useState(false);

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
        <div className="image-display" onClick={() => setIsModalOpen(true)}>
          {value ? (
            <div className="image-with-overlay">
              <img src={value} alt="Patient photo" className="patient-image" />
              <div className="image-overlay">
                <span className="overlay-text">
                  {" "}
                  {intl.formatMessage({ id: "patient.photo.retake" })}
                </span>
              </div>
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
    </div>
  );
};

export default PatientImageSelector;
