import React, { useState, useRef } from "react";
import {
  Modal,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Button,
} from "@carbon/react";
import { Camera, CloudUpload } from "@carbon/icons-react";
import "./ImagePreviewModal.css";
import { useIntl } from "react-intl";

const ImagePreviewModal = ({
  open,
  onClose,
  onImageSelect,
  currentImage = null,
}) => {
  const intl = useIntl();

  const [selectedTab, setSelectedTab] = useState(0);
  const [previewUrl, setPreviewUrl] = useState(currentImage);
  const [isCameraActive, setIsCameraActive] = useState(false);

  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);

  React.useEffect(() => {
    if (videoRef.current && streamRef.current && isCameraActive) {
      videoRef.current.srcObject = streamRef.current;
    }
  }, [isCameraActive]);

  React.useEffect(() => {
    return () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => track.stop());
      }
    };
  }, []);

  const fileInputRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);

  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (file && file.type.startsWith("image/")) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewUrl(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  //  drag and drop
  const handleDragOver = (event) => {
    event.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (event) => {
    event.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (event) => {
    event.preventDefault();
    setIsDragging(false);

    const file = event.dataTransfer.files[0];
    if (file && file.type.startsWith("image/")) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewUrl(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleClickUpload = () => {
    fileInputRef.current?.click();
  };

  // start la caméra
  const startCamera = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: "user",
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
        audio: false,
      });

      streamRef.current = stream;

      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        // Attendre que la vidéo soit prête
        await videoRef.current.play();
      }

      setIsCameraActive(true);
    } catch (error) {
      console.error("Erreur d'accès à la caméra:", error);
      alert("permission denied");
    }
  };

  // Stop  caméra
  const stopCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
      setIsCameraActive(false);
    }
  };

  // Take photo
  const capturePhoto = () => {
    if (canvasRef.current && videoRef.current) {
      const canvas = canvasRef.current;
      const video = videoRef.current;

      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;

      const context = canvas.getContext("2d");
      context.drawImage(video, 0, 0, canvas.width, canvas.height);

      const imageData = canvas.toDataURL("image/jpeg", 0.8);
      setPreviewUrl(imageData);
      stopCamera();
    }
  };

  const handleConfirm = () => {
    if (previewUrl) {
      onImageSelect(previewUrl);
      handleClose();
    }
  };

  const handleClose = () => {
    stopCamera();
    setPreviewUrl(currentImage);
    setSelectedTab(0);
    onClose();
  };

  const handleTabChange = (index) => {
    if (index !== 1) {
      stopCamera();
    }
    setSelectedTab(index);
  };

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading="Sélectionner une photo du patient"
      primaryButtonText="Confirmer"
      secondaryButtonText="Annuler"
      onRequestSubmit={handleConfirm}
      size="lg"
      preventCloseOnClickOutside
    >
      <Tabs
        selectedIndex={selectedTab}
        onChange={({ selectedIndex }) => handleTabChange(selectedIndex)}
      >
        <TabList aria-label="Options de sélection d'image">
          <Tab renderIcon={CloudUpload}>
            {intl.formatMessage({ id: "patient.photo.import" })}
          </Tab>
          <Tab renderIcon={Camera}>
            {intl.formatMessage({ id: "patient.photo.take" })}
          </Tab>
        </TabList>

        <TabPanels>
          {/* Tab 1: Upload / Drag and Drop */}
          <TabPanel>
            <div className="image-upload-panel">
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/jpg"
                onChange={handleFileChange}
                style={{ display: "none" }}
              />

              {!previewUrl ? (
                <div
                  className={`dropzone ${isDragging ? "dropzone-active" : ""}`}
                  onDragOver={handleDragOver}
                  onDragLeave={handleDragLeave}
                  onDrop={handleDrop}
                  onClick={handleClickUpload}
                >
                  <CloudUpload size={64} className="dropzone-icon" />
                  <p className="dropzone-title">
                    {intl.formatMessage({ id: "patient.photo.dragndrop" })}
                  </p>
                  <p className="dropzone-subtitle">
                    {intl.formatMessage({ id: "patient.photo.browse" })}
                  </p>
                  <p className="dropzone-formats">
                    Formats : JPG, PNG (max 1MB)
                  </p>
                </div>
              ) : (
                <div className="image-preview-container">
                  <p className="preview-label">Aperçu:</p>
                  <img
                    src={previewUrl}
                    alt="Aperçu de l'image patient"
                    className="image-preview"
                  />
                  <Button
                    kind="tertiary"
                    size="sm"
                    onClick={() => setPreviewUrl(null)}
                    style={{ marginTop: "1rem" }}
                  >
                    Changer l'image
                  </Button>
                </div>
              )}
            </div>
          </TabPanel>

          {/* Tab 2: Capture photo avec caméra */}
          <TabPanel>
            <div className="camera-panel">
              {!isCameraActive && !previewUrl ? (
                <div className="camera-start-container">
                  <div className="camera-placeholder">
                    <Camera size={64} />
                    <p>
                      {intl.formatMessage({
                        id: "patient.photo.active.camera",
                      })}
                    </p>
                  </div>
                  <Button
                    onClick={startCamera}
                    renderIcon={Camera}
                    className="camera-button"
                  >
                    {intl.formatMessage({ id: "patient.photo.start.camera" })}
                  </Button>
                </div>
              ) : !isCameraActive && previewUrl ? (
                <div className="camera-preview-container">
                  <div className="image-preview-container">
                    <p className="preview-label">Photo capturée:</p>
                    <img
                      src={previewUrl}
                      alt="Photo capturée du patient"
                      className="image-preview"
                    />
                  </div>
                  <Button
                    kind="primary"
                    onClick={startCamera}
                    renderIcon={Camera}
                    style={{ marginTop: "1rem" }}
                  >
                    {intl.formatMessage({ id: "patient.photo.retake" })}
                  </Button>
                </div>
              ) : (
                <div className="camera-active-container">
                  <div className="camera-wrapper">
                    <video
                      ref={videoRef}
                      autoPlay
                      playsInline
                      className="camera-video"
                    />
                    <div className="capture-button-overlay">
                      <button
                        className="capture-circle-button"
                        onClick={capturePhoto}
                        aria-label="Capturer la photo"
                      >
                        <div className="capture-circle-inner"></div>
                      </button>
                    </div>
                  </div>
                  <canvas ref={canvasRef} style={{ display: "none" }} />
                </div>
              )}
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>
    </Modal>
  );
};

export default ImagePreviewModal;
