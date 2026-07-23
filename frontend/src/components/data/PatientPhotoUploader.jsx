import React, { useState, useRef } from "react";
import { ArrowRight } from "@carbon/icons-react";

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

  // Camera
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

  const handleSave = async () => {
    if (preview) {
      await onSave(preview);
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
    <div className="modal-overlay">
      <div className="modal-container">
        {/* Header */}
        <div className="modal-header">
          <button onClick={handleClose} className="close-btn">
            <ArrowRight size={20} />
          </button>
        </div>

        {/* Tabs */}
        <div className="tabs-container">
          <button
            className={`tab ${activeTab === "upload" ? "active" : ""}`}
            onClick={() => handleTabChange("upload")}
          >
            <ArrowRight size={18} />
            <span>Importer</span>
          </button>
          <button
            className={`tab ${activeTab === "camera" ? "active" : ""}`}
            onClick={() => handleTabChange("camera")}
          >
            <ArrowRight size={18} />
            <span>Prendre photo</span>
          </button>
        </div>

        {/* Content */}
        <div className="modal-content">
          {error && (
            <div className="error-banner">
              <ArrowRight size={16} />
              <span>{error}</span>
            </div>
          )}

          {/* Tab Upload */}
          {activeTab === "upload" && (
            <div className="upload-tab">
              {!preview ? (
                <div
                  className={`dropzone ${dragActive ? "active" : ""}`}
                  onDragEnter={handleDrag}
                  onDragLeave={handleDrag}
                  onDragOver={handleDrag}
                  onDrop={handleDrop}
                  onClick={() => fileInputRef.current?.click()}
                >
                  <ArrowRight size={48} className="upload-icon" />
                  <p className="dropzone-title">Glissez une image ici</p>
                  <p className="dropzone-subtitle">ou cliquez pour parcourir</p>
                  <p className="dropzone-info">PNG, JPG jusqu'à 5MB</p>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={(e) => handleFileSelect(e.target.files[0])}
                    style={{ display: "none" }}
                  />
                </div>
              ) : (
                <div className="preview-container">
                  <img src={preview} alt="Preview" className="preview-image" />
                  <button
                    className="remove-btn"
                    onClick={() => setPreview(null)}
                  >
                    <ArrowRight size={18} />
                    Supprimer
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Tab Camera */}
          {activeTab === "camera" && (
            <div className="camera-tab">
              {!preview ? (
                <div className="camera-container">
                  <video
                    ref={videoRef}
                    autoPlay
                    playsInline
                    className="video-feed"
                  />
                  <canvas ref={canvasRef} style={{ display: "none" }} />
                  {cameraStream && (
                    <button className="capture-btn" onClick={capturePhoto}>
                      <ArrowRight size={24} />
                      Capturer
                    </button>
                  )}
                </div>
              ) : (
                <div className="preview-container">
                  <img src={preview} alt="Preview" className="preview-image" />
                  <button
                    className="remove-btn"
                    onClick={() => {
                      setPreview(null);
                      startCamera();
                    }}
                  >
                    <ArrowRight size={18} />
                    Reprendre
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={handleClose}>
            Annuler
          </button>
          <button
            className="btn btn-primary"
            onClick={handleSave}
            disabled={!preview}
          >
            <ArrowRight size={18} />
            Enregistrer
          </button>
        </div>
      </div>

      <style>{`
        .modal-overlay {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: rgba(0, 0, 0, 0.5);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 9999;
          padding: 1rem;
        }

        .modal-container {
          background: white;
          border-radius: 4px;
          width: 100%;
          max-width: 600px;
          max-height: 90vh;
          display: flex;
          flex-direction: column;
          box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
        }

        .modal-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 1rem 1.5rem;
          border-bottom: 1px solid #e0e0e0;
        }

        .modal-title {
          margin: 0;
          font-size: 1.25rem;
          font-weight: 600;
          color: #161616;
        }

        .close-btn {
          background: none;
          border: none;
          cursor: pointer;
          padding: 0.25rem;
          display: flex;
          align-items: center;
          color: #525252;
          transition: color 0.2s;
        }

        .close-btn:hover {
          color: #161616;
        }

        .tabs-container {
          display: flex;
          border-bottom: 1px solid #e0e0e0;
          background: #f4f4f4;
        }

        .tab {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 0.5rem;
          padding: 0.875rem 1rem;
          background: none;
          border: none;
          cursor: pointer;
          color: #525252;
          font-size: 0.875rem;
          font-weight: 500;
          transition: all 0.2s;
          border-bottom: 2px solid transparent;
        }

        .tab:hover {
          background: #e8e8e8;
        }

        .tab.active {
          background: white;
          color: #0f62fe;
          border-bottom-color: #0f62fe;
        }

        .modal-content {
          padding: 1.5rem;
          overflow-y: auto;
          flex: 1;
        }

        .error-banner {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          padding: 0.75rem 1rem;
          background: #fff1f1;
          border-left: 3px solid #da1e28;
          color: #da1e28;
          margin-bottom: 1rem;
          font-size: 0.875rem;
        }

        .dropzone {
          border: 2px dashed #8d8d8d;
          border-radius: 4px;
          padding: 3rem 2rem;
          text-align: center;
          cursor: pointer;
          transition: all 0.2s;
          background: #f4f4f4;
        }

        .dropzone:hover {
          border-color: #0f62fe;
          background: #e8f4ff;
        }

        .dropzone.active {
          border-color: #0f62fe;
          background: #d0e2ff;
        }

        .upload-icon {
          color: #8d8d8d;
          margin-bottom: 1rem;
        }

        .dropzone-title {
          font-size: 1rem;
          font-weight: 500;
          color: #161616;
          margin: 0.5rem 0;
        }

        .dropzone-subtitle {
          font-size: 0.875rem;
          color: #525252;
          margin: 0.25rem 0;
        }

        .dropzone-info {
          font-size: 0.75rem;
          color: #8d8d8d;
          margin: 0.5rem 0 0;
        }

        .preview-container {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 1rem;
        }

        .preview-image {
          max-width: 100%;
          max-height: 400px;
          border-radius: 4px;
          border: 1px solid #e0e0e0;
          object-fit: contain;
        }

        .camera-container {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 1rem;
        }

        .video-feed {
          width: 100%;
          max-height: 400px;
          border-radius: 4px;
          background: #000;
        }

        .capture-btn,
        .remove-btn {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          padding: 0.75rem 1.5rem;
          border: none;
          border-radius: 4px;
          font-size: 0.875rem;
          font-weight: 500;
          cursor: pointer;
          transition: background 0.2s;
        }

        .capture-btn {
          background: #0f62fe;
          color: white;
        }

        .capture-btn:hover {
          background: #0353e9;
        }

        .remove-btn {
          background: #f4f4f4;
          color: #da1e28;
        }

        .remove-btn:hover {
          background: #fff1f1;
        }

        .modal-footer {
          display: flex;
          justify-content: flex-end;
          gap: 0.75rem;
          padding: 1rem 1.5rem;
          border-top: 1px solid #e0e0e0;
          background: #f4f4f4;
        }

        .btn {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          padding: 0.75rem 1.5rem;
          border: none;
          border-radius: 4px;
          font-size: 0.875rem;
          font-weight: 500;
          cursor: pointer;
          transition: background 0.2s;
        }

        .btn-secondary {
          background: white;
          color: #161616;
          border: 1px solid #8d8d8d;
        }

        .btn-secondary:hover {
          background: #e8e8e8;
        }

        .btn-primary {
          background: #0f62fe;
          color: white;
        }

        .btn-primary:hover:not(:disabled) {
          background: #0353e9;
        }

        .btn-primary:disabled {
          background: #c6c6c6;
          cursor: not-allowed;
        }
      `}</style>
    </div>
  );
};

// Composant principal avec preview
const PatientPhotoUploader = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [patientPhoto, setPatientPhoto] = useState(null);
  const [saving, setSaving] = useState(false);

  const handleSavePhoto = async (photoBase64) => {
    setSaving(true);

    // Simulation de sauvegarde - remplacer par votre API
    try {
      // const patientId = 'votre-patient-id';
      // const response = await fetch(`/rest/patient/${patientId}/photo`, {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify({ photo: photoBase64 })
      // });
      //
      // if (!response.ok) throw new Error('Erreur sauvegarde');

      // Simulation
      await new Promise((resolve) => setTimeout(resolve, 1000));

      setPatientPhoto(photoBase64);
      alert("Photo sauvegardée avec succès!");
    } catch (error) {
      console.error("Erreur:", error);
      alert("Erreur lors de la sauvegarde");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ maxWidth: "800px", margin: "0 0" }}>
      <div
        style={{
          background: "white",
          padding: "1.5rem",
          borderRadius: "4px",
        }}
      >
        <label
          style={{
            display: "block",
            marginBottom: "0.5rem",
            fontSize: "0.875rem",
            fontWeight: "500",
            color: "#161616",
          }}
        >
          Photo du patient
        </label>

        <div
          onClick={() => setIsModalOpen(true)}
          style={{
            width: "150px",
            height: "150px",
            border: "2px dashed #8d8d8d",
            borderRadius: "4px",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            cursor: "pointer",
            background: patientPhoto ? "transparent" : "#f4f4f4",
            transition: "all 0.2s",
            overflow: "hidden",
          }}
          onMouseEnter={(e) => {
            if (!patientPhoto) {
              e.currentTarget.style.borderColor = "#0f62fe";
              e.currentTarget.style.background = "#e8f4ff";
            }
          }}
          onMouseLeave={(e) => {
            if (!patientPhoto) {
              e.currentTarget.style.borderColor = "#8d8d8d";
              e.currentTarget.style.background = "#f4f4f4";
            }
          }}
        >
          {patientPhoto ? (
            <img
              src={patientPhoto}
              alt="Patient"
              style={{
                width: "100%",
                height: "100%",
                objectFit: "cover",
              }}
            />
          ) : (
            <div style={{ textAlign: "center", color: "#8d8d8d" }}>
              <ArrowRight size={32} />
              <p style={{ fontSize: "0.75rem", marginTop: "0.5rem" }}>
                Cliquer pour ajouter
              </p>
            </div>
          )}
        </div>
      </div>

      <PatientPhotoModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSave={handleSavePhoto}
        existingPhoto={patientPhoto}
      />

      {saving && (
        <div
          style={{
            position: "fixed",
            top: "1rem",
            right: "1rem",
            background: "#0f62fe",
            color: "white",
            padding: "1rem",
            borderRadius: "4px",
            boxShadow: "0 2px 6px rgba(0,0,0,0.2)",
          }}
        >
          Sauvegarde en cours...
        </div>
      )}
    </div>
  );
};

export default PatientPhotoUploader;
