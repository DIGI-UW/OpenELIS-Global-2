import React, { useState, useRef } from "react";

import {
  Camera,
  Upload,
  TrashCan,
  Checkmark,
  Close,
} from "@carbon/icons-react";

const PatientPhotoModal = ({
  isOpen,
  onClose,
  onSave,
  existingPhoto = null,
}) => {
  const [activeTab, setActiveTab] = useState("upload");
  const [preview, setPreview] = useState(existingPhoto);
  const [dragActive, setDragActive] = useState(false);
  const [cameraStream, setCameraStream] = useState(null);
  const [error, setError] = useState(null);

  const fileInputRef = useRef(null);
  const videoRef = useRef(null);
  const canvasRef = useRef(null);

  const handleFileSelect = (file) => {
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setError("Veuillez sélectionner une image valide");
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setError("Image trop grande (max 5MB)");
      return;
    }

    setError(null);
    const reader = new FileReader();
    reader.onloadend = () => {
      setPreview(reader.result);
    };
    reader.readAsDataURL(file);
  };

  // Drag and drop
  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileSelect(e.dataTransfer.files[0]);
    }
  };

  //  Camera
  const startCamera = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { width: 640, height: 480 },
      });
      setCameraStream(stream);
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
      setError(null);
    } catch (err) {
      setError("Impossible d'accéder à la caméra. Vérifiez les permissions.");
      console.error(err);
    }
  };

  const stopCamera = () => {
    if (cameraStream) {
      cameraStream.getTracks().forEach((track) => track.stop());
      setCameraStream(null);
    }
  };

  const capturePhoto = () => {
    if (videoRef.current && canvasRef.current) {
      const canvas = canvasRef.current;
      const video = videoRef.current;

      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;

      const ctx = canvas.getContext("2d");
      ctx.drawImage(video, 0, 0);

      const imageData = canvas.toDataURL("image/jpeg", 0.8);
      setPreview(imageData);
      stopCamera();
    }
  };

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setError(null);

    if (tab === "camera") {
      startCamera();
    } else {
      stopCamera();
    }
  };

  // save and close
  const handleSave = () => {
    if (preview) {
      onSave(preview);
      handleClose();
    }
  };

  const handleClose = () => {
    stopCamera();
    setPreview(existingPhoto);
    setError(null);
    setActiveTab("upload");
    onClose();
  };

  React.useEffect(() => {
    return () => stopCamera();
  }, []);

  if (!isOpen) return null;

  return (
    <div className="cds--modal is-visible" role="dialog">
      <div className="cds--modal-container">
        <div className="cds--modal-header">
          <h3 className="cds--modal-header__heading">Photo du patient</h3>
          <button
            className="cds--modal-close"
            type="button"
            onClick={handleClose}
            aria-label="Fermer"
          >
            <Close size={20} />
          </button>
        </div>

        {/* Tabs */}
        <div className="cds--tabs">
          <div className="cds--tabs__nav">
            <button
              className={`cds--tabs__nav-item ${activeTab === "upload" ? "cds--tabs__nav-item--selected" : ""}`}
              type="button"
              onClick={() => handleTabChange("upload")}
            >
              <Upload size={16} />
              <span className="cds--tabs__nav-item-label">Importer</span>
            </button>
            <button
              className={`cds--tabs__nav-item ${activeTab === "camera" ? "cds--tabs__nav-item--selected" : ""}`}
              type="button"
              onClick={() => handleTabChange("camera")}
            >
              <Camera size={16} />
              <span className="cds--tabs__nav-item-label">Prendre photo</span>
            </button>
          </div>
        </div>

        <div className="cds--modal-content">
          {error && (
            <div className="cds--inline-notification cds--inline-notification--error">
              <div className="cds--inline-notification__details">
                <div className="cds--inline-notification__text-wrapper">
                  <p className="cds--inline-notification__subtitle">{error}</p>
                </div>
              </div>
            </div>
          )}

          {/* Tab Upload */}
          {activeTab === "upload" && (
            <div className="photo-upload-tab">
              {!preview ? (
                <div
                  className={`photo-dropzone ${dragActive ? "photo-dropzone--active" : ""}`}
                  onDragEnter={handleDrag}
                  onDragLeave={handleDrag}
                  onDragOver={handleDrag}
                  onDrop={handleDrop}
                  onClick={() => fileInputRef.current?.click()}
                >
                  <Upload size={48} />
                  <p className="photo-dropzone__title">Glissez une image ici</p>
                  <p className="photo-dropzone__subtitle">
                    ou cliquez pour parcourir
                  </p>
                  <p className="photo-dropzone__info">PNG, JPG jusqu'à 5MB</p>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={(e) => handleFileSelect(e.target.files[0])}
                    style={{ display: "none" }}
                  />
                </div>
              ) : (
                <div className="photo-preview">
                  <img
                    src={preview}
                    alt="Preview"
                    className="photo-preview__image"
                  />
                  <button
                    className="cds--btn cds--btn--danger cds--btn--sm"
                    type="button"
                    onClick={() => setPreview(null)}
                  >
                    <TrashCan size={16} />
                    Supprimer
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Tab Camera */}
          {activeTab === "camera" && (
            <div className="photo-camera-tab">
              {!preview ? (
                <div className="camera-wrapper">
                  <video
                    ref={videoRef}
                    autoPlay
                    playsInline
                    className="camera-video"
                  />
                  <canvas ref={canvasRef} style={{ display: "none" }} />
                  {cameraStream && (
                    <button
                      className="cds--btn cds--btn--primary"
                      type="button"
                      onClick={capturePhoto}
                    >
                      <Camera size={20} />
                      Capturer
                    </button>
                  )}
                </div>
              ) : (
                <div className="photo-preview">
                  <img
                    src={preview}
                    alt="Preview"
                    className="photo-preview__image"
                  />
                  <button
                    className="cds--btn cds--btn--secondary cds--btn--sm"
                    type="button"
                    onClick={() => {
                      setPreview(null);
                      startCamera();
                    }}
                  >
                    <TrashCan size={16} />
                    Reprendre
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="cds--modal-footer">
          <button
            className="cds--btn cds--btn--secondary"
            type="button"
            onClick={handleClose}
          >
            Annuler
          </button>
          <button
            className="cds--btn cds--btn--primary"
            type="button"
            onClick={handleSave}
            disabled={!preview}
          >
            <Checkmark size={16} />
            Enregistrer
          </button>
        </div>
      </div>
    </div>
  );
};

const PatientPhotoField = ({ value, onChange, label = "Photo du patient" }) => {
  const [isModalOpen, setIsModalOpen] = useState(false);

  const handlePhotoSave = (photoBase64) => {
    onChange(photoBase64);
  };

  return (
    <div className="cds--form-item">
      <label className="cds--label">{label}</label>

      <div
        className="patient-photo-preview"
        onClick={() => setIsModalOpen(true)}
      >
        {value ? (
          <img
            src={value}
            alt="Patient"
            className="patient-photo-preview__image"
          />
        ) : (
          <div className="patient-photo-preview__placeholder">
            <Camera size={32} />
            <span>Cliquer pour ajouter</span>
          </div>
        )}
      </div>

      <PatientPhotoModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSave={handlePhotoSave}
        existingPhoto={value}
      />

      <style>{`
        /* Styles Carbon Design System */
        .cds--modal {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: rgba(22, 22, 22, 0.5);
          display: none;
          align-items: center;
          justify-content: center;
          z-index: 9000;
        }

        .cds--modal.is-visible {
          display: flex;
        }

        .cds--modal-container {
          background: #fff;
          width: 100%;
          max-width: 600px;
          max-height: 90vh;
          display: flex;
          flex-direction: column;
          box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
        }

        .cds--modal-header {
          padding: 1rem;
          border-bottom: 1px solid #e0e0e0;
          display: flex;
          justify-content: space-between;
          align-items: center;
        }

        .cds--modal-header__heading {
          margin: 0;
          font-size: 1.25rem;
          font-weight: 400;
          color: #161616;
        }

        .cds--modal-close {
          background: transparent;
          border: none;
          cursor: pointer;
          padding: 0.5rem;
          display: flex;
          align-items: center;
          color: #161616;
        }

        .cds--modal-close:hover {
          background: #e0e0e0;
        }

        .cds--tabs {
          border-bottom: 1px solid #e0e0e0;
        }

        .cds--tabs__nav {
          display: flex;
          background: #f4f4f4;
        }

        .cds--tabs__nav-item {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 0.5rem;
          padding: 0.75rem 1rem;
          background: transparent;
          border: none;
          border-bottom: 2px solid transparent;
          cursor: pointer;
          color: #525252;
          font-size: 0.875rem;
        }

        .cds--tabs__nav-item:hover {
          background: #e0e0e0;
        }

        .cds--tabs__nav-item--selected {
          background: #fff;
          color: #0f62fe;
          border-bottom-color: #0f62fe;
        }

        .cds--tabs__nav-item-label {
          font-weight: 400;
        }

        .cds--modal-content {
          padding: 1rem;
          overflow-y: auto;
          flex: 1;
        }

        .cds--inline-notification {
          display: flex;
          padding: 0.75rem 1rem;
          margin-bottom: 1rem;
        }

        .cds--inline-notification--error {
          background: #fff1f1;
          border-left: 3px solid #da1e28;
        }

        .cds--inline-notification__details {
          flex: 1;
        }

        .cds--inline-notification__subtitle {
          margin: 0;
          font-size: 0.875rem;
          color: #161616;
        }

        .photo-dropzone {
          border: 2px dashed #8d8d8d;
          padding: 3rem 2rem;
          text-align: center;
          cursor: pointer;
          background: #f4f4f4;
          transition: all 0.11s;
        }

        .photo-dropzone:hover {
          border-color: #0f62fe;
          background: #e8f4ff;
        }

        .photo-dropzone--active {
          border-color: #0f62fe;
          background: #d0e2ff;
        }

        .photo-dropzone__title {
          font-size: 1rem;
          color: #161616;
          margin: 1rem 0 0.5rem;
        }

        .photo-dropzone__subtitle {
          font-size: 0.875rem;
          color: #525252;
          margin: 0;
        }

        .photo-dropzone__info {
          font-size: 0.75rem;
          color: #8d8d8d;
          margin: 0.5rem 0 0;
        }

        .photo-preview {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 1rem;
        }

        .photo-preview__image {
          max-width: 100%;
          max-height: 400px;
          object-fit: contain;
          border: 1px solid #e0e0e0;
        }

        .camera-wrapper {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 1rem;
        }

        .camera-video {
          width: 100%;
          max-height: 400px;
          background: #000;
        }

        .cds--modal-footer {
          display: flex;
          justify-content: flex-end;
          gap: 0.5rem;
          padding: 1rem;
          border-top: 1px solid #e0e0e0;
        }

        .cds--btn {
          display: inline-flex;
          align-items: center;
          gap: 0.5rem;
          padding: 0.875rem 1rem;
          border: none;
          cursor: pointer;
          font-size: 0.875rem;
          font-weight: 400;
          min-height: 3rem;
          transition: background 0.11s;
        }

        .cds--btn--primary {
          background: #0f62fe;
          color: #fff;
        }

        .cds--btn--primary:hover:not(:disabled) {
          background: #0353e9;
        }

        .cds--btn--primary:disabled {
          background: #c6c6c6;
          cursor: not-allowed;
        }

        .cds--btn--secondary {
          background: transparent;
          color: #0f62fe;
          border: 1px solid #0f62fe;
        }

        .cds--btn--secondary:hover {
          background: #e8f4ff;
        }

        .cds--btn--danger {
          background: #da1e28;
          color: #fff;
        }

        .cds--btn--danger:hover {
          background: #ba1b23;
        }

        .cds--btn--sm {
          min-height: 2rem;
          padding: 0.5rem 1rem;
        }

        /* Composant principal */
        .cds--form-item {
          margin-bottom: 1rem;
        }

        .cds--label {
          display: block;
          font-size: 0.75rem;
          font-weight: 400;
          color: #161616;
          margin-bottom: 0.5rem;
        }

        .patient-photo-preview {
          width: 150px;
          height: 150px;
          border: 1px solid #8d8d8d;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          background: #f4f4f4;
          transition: all 0.11s;
          overflow: hidden;
        }

        .patient-photo-preview:hover {
          border-color: #0f62fe;
          background: #e8f4ff;
        }

        .patient-photo-preview__image {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        .patient-photo-preview__placeholder {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 0.5rem;
          color: #525252;
          font-size: 0.75rem;
        }
      `}</style>
    </div>
  );
};

// Exemple d'utilisation dans un formulaire patient
const PatientFormExample = () => {
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    dateOfBirth: "",
    gender: "",
    photo: null, // La photo sera stockée ici en Base64
  });

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Envoi de toutes les données y compris la photo
    console.log("Données du formulaire:", formData);

    // Exemple d'envoi API
    // const response = await fetch('/rest/patient', {
    //   method: 'POST',
    //   headers: { 'Content-Type': 'application/json' },
    //   body: JSON.stringify(formData)
    // });

    alert("Formulaire soumis ! Vérifiez la console pour voir les données.");
  };

  return (
    <div style={{ padding: "2rem", maxWidth: "800px", margin: "0 auto" }}>
      <h2 style={{ marginBottom: "1.5rem", fontWeight: "400" }}>
        Enregistrement du Patient
      </h2>

      <form onSubmit={handleSubmit}>
        <div className="cds--form-item">
          <label className="cds--label">Prénom</label>
          <input
            type="text"
            className="cds--text-input"
            value={formData.firstName}
            onChange={(e) =>
              setFormData({ ...formData, firstName: e.target.value })
            }
            required
          />
        </div>

        <div className="cds--form-item">
          <label className="cds--label">Nom</label>
          <input
            type="text"
            className="cds--text-input"
            value={formData.lastName}
            onChange={(e) =>
              setFormData({ ...formData, lastName: e.target.value })
            }
            required
          />
        </div>

        <PatientPhotoField
          value={formData.photo}
          onChange={(photo) => setFormData({ ...formData, photo })}
          label="Photo du patient"
        />

        <div style={{ marginTop: "2rem" }}>
          <button type="submit" className="cds--btn cds--btn--primary">
            Enregistrer le patient
          </button>
        </div>
      </form>

      <style>{`
        .cds--text-input {
          width: 100%;
          padding: 0.6875rem 1rem;
          font-size: 0.875rem;
          border: none;
          border-bottom: 1px solid #8d8d8d;
          background: #f4f4f4;
          transition: all 0.11s;
        }

        .cds--text-input:focus {
          outline: 2px solid #0f62fe;
          outline-offset: -2px;
        }
      `}</style>
    </div>
  );
};

export default PatientFormExample;
