/**
 * CopyMappingsModal Component
 *
 * Modal for copying field mappings from one analyzer to another
 *
 * Per FR-006 specification:
 * - Small size ComposedModal (~400-480px)
 * - Source analyzer section (read-only)
 * - Target analyzer dropdown (searchable, filters to analyzers with active mappings)
 * - Mapping summary section
 * - Warning note
 * - Confirmation dialog before copy
 * - Success/error notifications
 */

import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Dropdown,
  InlineNotification,
  Loading,
} from "@carbon/react";
import { Copy } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import * as analyzerService from "../../../services/analyzerService";
import "./CopyMappingsModal.css";

const CopyMappingsModal = ({
  open,
  onClose,
  sourceAnalyzerId,
  sourceAnalyzerName,
  sourceAnalyzerType,
  onSuccess,
}) => {
  const intl = useIntl();
  const [targetAnalyzerId, setTargetAnalyzerId] = useState("");
  const [availableAnalyzers, setAvailableAnalyzers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [copying, setCopying] = useState(false);
  const [error, setError] = useState(null);
  const [mappingCount, setMappingCount] = useState(0);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [copyResult, setCopyResult] = useState(null);

  // Load available analyzers (excluding source) when modal opens
  useEffect(() => {
    if (open) {
      setLoading(true);
      setError(null);
      setTargetAnalyzerId("");
      setMappingCount(0);
      setShowConfirmation(false);
      setCopyResult(null);

      // Load all analyzers
      analyzerService.getAnalyzers({}, (data) => {
        const list =
          data && Array.isArray(data.analyzers) ? data.analyzers : [];
        // Filter out source analyzer
        const filtered = list.filter((a) => a.id !== sourceAnalyzerId);
        setAvailableAnalyzers(filtered);
        setLoading(false);
      });
    }
  }, [open, sourceAnalyzerId]);

  // Load mapping count when target analyzer is selected
  useEffect(() => {
    if (open && targetAnalyzerId && sourceAnalyzerId) {
      // Get mappings count from source analyzer
      analyzerService.getMappings(sourceAnalyzerId, (mappings) => {
        if (Array.isArray(mappings)) {
          const activeMappings = mappings.filter((m) => m.isActive !== false);
          setMappingCount(activeMappings.length);
        } else {
          setMappingCount(0);
        }
      });
    }
  }, [open, targetAnalyzerId, sourceAnalyzerId]);

  const handleClose = () => {
    // Remove focus from any button before closing to prevent aria-hidden warning
    if (document.activeElement && document.activeElement.blur) {
      document.activeElement.blur();
    }
    onClose && onClose();
  };

  const handleCopy = () => {
    if (!targetAnalyzerId) {
      setError(
        intl.formatMessage({ id: "analyzer.copyMappings.target.required" }),
      );
      return;
    }

    if (mappingCount === 0) {
      setError(
        intl.formatMessage({
          id: "analyzer.copyMappings.error.noSourceMappings",
        }),
      );
      return;
    }

    // Show confirmation dialog
    setShowConfirmation(true);
  };

  const handleConfirmCopy = () => {
    setShowConfirmation(false);
    setError(null);
    setCopying(true);

    const copyData = {
      sourceAnalyzerId: sourceAnalyzerId,
      overwriteExisting: true,
      skipIncompatible: true,
    };

    analyzerService.copyMappings(
      targetAnalyzerId,
      copyData,
      (response, extraParams) => {
        setCopying(false);
        if (response.error) {
          setError(
            response.error ||
              intl.formatMessage({ id: "analyzer.copyMappings.error" }),
          );
        } else {
          setCopyResult(response);
          if (typeof onSuccess === "function") {
            onSuccess(response, targetAnalyzerId);
          }
        }
      },
      null,
    );
  };

  const handleCancelConfirmation = () => {
    setShowConfirmation(false);
  };

  const handleViewTarget = () => {
    handleClose();
    if (targetAnalyzerId) {
      window.location.href = `/analyzers/${targetAnalyzerId}/mappings`;
    }
  };

  // Prepare analyzer dropdown items
  const analyzerItems = availableAnalyzers.map((analyzer) => ({
    id: analyzer.id,
    text: analyzer.name || analyzer.id,
    analyzer: analyzer,
  }));

  const selectedAnalyzer = analyzerItems.find(
    (item) => item.id === targetAnalyzerId,
  );

  return (
    <>
      <ComposedModal
        open={open && !showConfirmation && !copyResult}
        onClose={handleClose}
        aria-label={intl.formatMessage({ id: "analyzer.copyMappings.title" })}
        data-testid="copy-mappings-modal"
        preventCloseOnClickOutside={false}
        size="sm"
      >
        <ModalHeader
          title={<FormattedMessage id="analyzer.copyMappings.title" />}
          label={
            <FormattedMessage
              id="analyzer.copyMappings.subtitle"
              values={{
                source: sourceAnalyzerName || sourceAnalyzerId,
                target:
                  selectedAnalyzer?.text ||
                  intl.formatMessage({
                    id: "analyzer.copyMappings.target.placeholder",
                  }),
              }}
            />
          }
        />
        <ModalBody className="copy-mappings-modal-body">
          {/* Source Analyzer Section */}
          <div
            className="copy-mappings-source-section"
            data-testid="copy-mappings-source-section"
          >
            <h3>
              <FormattedMessage id="analyzer.copyMappings.source" />
            </h3>
            <div className="copy-mappings-info-row">
              <span className="copy-mappings-info-label">
                <FormattedMessage id="analyzer.copyMappings.source.name" />:
              </span>
              <span>{sourceAnalyzerName || sourceAnalyzerId}</span>
            </div>
            <div className="copy-mappings-info-row">
              <span className="copy-mappings-info-label">
                <FormattedMessage id="analyzer.copyMappings.source.type" />:
              </span>
              <span>{sourceAnalyzerType || "-"}</span>
            </div>
          </div>

          {/* Target Analyzer Section */}
          <div
            className="copy-mappings-target-section"
            data-testid="copy-mappings-target-section"
          >
            <Dropdown
              id="target-analyzer-dropdown"
              titleText={
                <FormattedMessage id="analyzer.copyMappings.target.required" />
              }
              label={intl.formatMessage({
                id: "analyzer.copyMappings.target.placeholder",
              })}
              items={analyzerItems}
              selectedItem={selectedAnalyzer || null}
              onChange={({ selectedItem }) => {
                setTargetAnalyzerId(selectedItem ? selectedItem.id : "");
                setError(null);
              }}
              itemToString={(item) => (item ? item.text : "")}
              data-testid="copy-mappings-target-dropdown"
            />
          </div>

          {/* Mapping Summary Section */}
          {targetAnalyzerId && mappingCount > 0 && (
            <div
              className="copy-mappings-summary-section"
              data-testid="copy-mappings-summary-section"
            >
              <h3>
                <FormattedMessage id="analyzer.copyMappings.summary" />
              </h3>
              <p>
                <FormattedMessage
                  id="analyzer.copyMappings.summary.count"
                  values={{ count: mappingCount }}
                />
              </p>
            </div>
          )}

          {/* Warning Note */}
          <div
            className="copy-mappings-warning-section"
            data-testid="copy-mappings-warning-section"
          >
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({
                id: "analyzer.copyMappings.warning",
              })}
              lowContrast
              hideCloseButton
            />
          </div>

          {/* Error Display */}
          {error && (
            <InlineNotification
              kind="error"
              title={error}
              lowContrast
              hideCloseButton
              data-testid="copy-mappings-error"
            />
          )}

          {/* Loading State */}
          {loading && (
            <div
              className="copy-mappings-loading"
              data-testid="copy-mappings-loading"
            >
              <Loading
                description={intl.formatMessage({
                  id: "analyzer.copyMappings.loading",
                })}
                withOverlay={false}
              />
            </div>
          )}
        </ModalBody>
        <ModalFooter>
          <Button
            kind="secondary"
            onClick={handleClose}
            data-testid="copy-mappings-cancel"
          >
            <FormattedMessage id="button.cancel" defaultMessage="Cancel" />
          </Button>
          <Button
            kind="primary"
            renderIcon={Copy}
            onClick={handleCopy}
            disabled={!targetAnalyzerId || loading || copying}
            data-testid="copy-mappings-copy-button"
          >
            <FormattedMessage id="analyzer.copyMappings.copy" />
          </Button>
        </ModalFooter>
      </ComposedModal>

      {/* Confirmation Modal */}
      <ComposedModal
        open={showConfirmation}
        onClose={handleCancelConfirmation}
        aria-label={intl.formatMessage({
          id: "analyzer.copyMappings.confirmation.title",
        })}
        data-testid="copy-mappings-confirmation-modal"
        preventCloseOnClickOutside={false}
        size="sm"
      >
        <ModalHeader
          title={
            <FormattedMessage id="analyzer.copyMappings.confirmation.title" />
          }
        />
        <ModalBody>
          <p>
            <FormattedMessage
              id="analyzer.copyMappings.confirmation.message"
              values={{ count: mappingCount }}
            />
          </p>
        </ModalBody>
        <ModalFooter>
          <Button
            kind="secondary"
            onClick={handleCancelConfirmation}
            data-testid="copy-mappings-confirm-cancel"
          >
            <FormattedMessage id="button.cancel" defaultMessage="Cancel" />
          </Button>
          <Button
            kind="danger"
            renderIcon={Copy}
            onClick={handleConfirmCopy}
            disabled={copying}
            data-testid="copy-mappings-confirm-button"
          >
            <FormattedMessage id="analyzer.copyMappings.copy" />
          </Button>
        </ModalFooter>
      </ComposedModal>

      {/* Success/Error Result Modal */}
      {copyResult && (
        <ComposedModal
          open={!!copyResult}
          onClose={handleClose}
          aria-label={intl.formatMessage({ id: "analyzer.copyMappings.title" })}
          data-testid="copy-mappings-result-modal"
          preventCloseOnClickOutside={false}
          size="sm"
        >
          <ModalHeader
            title={<FormattedMessage id="analyzer.copyMappings.title" />}
          />
          <ModalBody>
            {copyResult.error ? (
              <InlineNotification
                kind="error"
                title={
                  copyResult.error ||
                  intl.formatMessage({ id: "analyzer.copyMappings.error" })
                }
                lowContrast
                hideCloseButton
                data-testid="copy-mappings-result-error"
              />
            ) : (
              <>
                <InlineNotification
                  kind="success"
                  title={
                    <FormattedMessage
                      id="analyzer.copyMappings.success"
                      values={{ count: copyResult.copiedCount || 0 }}
                    />
                  }
                  lowContrast
                  hideCloseButton
                  data-testid="copy-mappings-result-success"
                />
                {copyResult.warnings && copyResult.warnings.length > 0 && (
                  <div
                    className="copy-mappings-warnings"
                    data-testid="copy-mappings-result-warnings"
                  >
                    <h4>
                      <FormattedMessage
                        id="analyzer.copyMappings.warnings"
                        defaultMessage="Warnings"
                      />
                    </h4>
                    <ul>
                      {copyResult.warnings.map((warning, index) => (
                        <li key={index}>{warning}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </>
            )}
          </ModalBody>
          <ModalFooter>
            {copyResult.error ? (
              <Button
                kind="secondary"
                onClick={handleClose}
                data-testid="copy-mappings-result-close"
              >
                <FormattedMessage id="button.close" defaultMessage="Close" />
              </Button>
            ) : (
              <>
                <Button
                  kind="secondary"
                  onClick={handleClose}
                  data-testid="copy-mappings-result-close"
                >
                  <FormattedMessage id="button.close" defaultMessage="Close" />
                </Button>
                <Button
                  kind="primary"
                  onClick={handleViewTarget}
                  data-testid="copy-mappings-view-target"
                >
                  <FormattedMessage id="analyzer.copyMappings.success.viewTarget" />
                </Button>
              </>
            )}
          </ModalFooter>
        </ComposedModal>
      )}
    </>
  );
};

export default CopyMappingsModal;
