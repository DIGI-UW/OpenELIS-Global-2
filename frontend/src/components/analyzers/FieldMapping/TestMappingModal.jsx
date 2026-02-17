/**
 * TestMappingModal Component
 *
 * Modal for testing field mappings with sample ASTM messages
 *
 * Per FR-007 specification:
 * - Medium size ComposedModal (~600-700px)
 * - Analyzer info section (read-only)
 * - Sample ASTM message input with character counter
 * - Preview options checkboxes
 * - Result display sections (parsed fields, applied mappings, entity preview, warnings/errors)
 * - Action buttons (Close, Test Another, Save as Test Case)
 */

import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextArea,
  Checkbox,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  InlineNotification,
  Loading,
  CodeSnippet,
  CodeSnippetSkeleton,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import * as analyzerService from "../../../services/analyzerService";
import "./TestMappingModal.css";

const MAX_MESSAGE_SIZE = 10240; // 10KB

const TestMappingModal = ({
  open,
  onClose,
  analyzerId,
  analyzerName,
  analyzerType,
  activeMappingsCount = 0,
}) => {
  const intl = useIntl();
  const [astmMessage, setAstmMessage] = useState("");
  const [includeDetailedParsing, setIncludeDetailedParsing] = useState(false);
  const [validateAllMappings, setValidateAllMappings] = useState(false);
  const [loading, setLoading] = useState(false);
  const [previewResult, setPreviewResult] = useState(null);
  const [error, setError] = useState(null);

  // Reset state when modal opens/closes
  useEffect(() => {
    if (!open) {
      setAstmMessage("");
      setIncludeDetailedParsing(false);
      setValidateAllMappings(false);
      setPreviewResult(null);
      setError(null);
      setLoading(false);
    }
  }, [open]);

  const handleClose = () => {
    // Remove focus from any button before closing to prevent aria-hidden warning
    if (document.activeElement && document.activeElement.blur) {
      document.activeElement.blur();
    }
    onClose && onClose();
  };

  const handlePreview = () => {
    // Validate message
    if (!astmMessage || astmMessage.trim().length === 0) {
      setError(
        intl.formatMessage({
          id: "analyzer.testMapping.validation.messageRequired",
        }),
      );
      return;
    }

    if (astmMessage.length > MAX_MESSAGE_SIZE) {
      setError(
        intl.formatMessage({
          id: "analyzer.testMapping.validation.messageTooLarge",
        }),
      );
      return;
    }

    setError(null);
    setLoading(true);
    setPreviewResult(null);

    const previewData = {
      astmMessage: astmMessage.trim(),
      includeDetailedParsing,
      validateAllMappings,
    };

    analyzerService.previewMapping(
      analyzerId,
      previewData,
      (response, extraParams) => {
        setLoading(false);
        if (response.error) {
          setError(
            response.error ||
              intl.formatMessage({
                id: "analyzer.testMapping.validation.invalidFormat",
              }),
          );
        } else {
          setPreviewResult(response);
        }
      },
      null,
    );
  };

  const handleTestAnother = () => {
    setAstmMessage("");
    setIncludeDetailedParsing(false);
    setValidateAllMappings(false);
    setPreviewResult(null);
    setError(null);
  };

  const characterCount = astmMessage.length;
  const characterCountPercent = (characterCount / MAX_MESSAGE_SIZE) * 100;
  const isNearLimit = characterCountPercent >= 90;

  // Table headers for parsed fields
  const parsedFieldsHeaders = [
    {
      key: "fieldName",
      header: intl.formatMessage({
        id: "analyzer.testMapping.parsedFields.fieldName",
      }),
    },
    {
      key: "astmRef",
      header: intl.formatMessage({
        id: "analyzer.testMapping.parsedFields.astmRef",
      }),
    },
    {
      key: "rawValue",
      header: intl.formatMessage({
        id: "analyzer.testMapping.parsedFields.rawValue",
      }),
    },
    {
      key: "mappedTo",
      header: intl.formatMessage({
        id: "analyzer.testMapping.parsedFields.mappedTo",
      }),
    },
    {
      key: "interpretation",
      header: intl.formatMessage({
        id: "analyzer.testMapping.parsedFields.interpretation",
      }),
    },
  ];

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      aria-label={intl.formatMessage({ id: "analyzer.testMapping.title" })}
      data-testid="test-mapping-modal"
      preventCloseOnClickOutside={false}
      size="md"
    >
      <ModalHeader
        title={<FormattedMessage id="analyzer.testMapping.title" />}
        label={<FormattedMessage id="analyzer.testMapping.subtitle" />}
      />
      <ModalBody className="test-mapping-modal-body">
        {/* Analyzer Information Section */}
        <div
          className="test-mapping-analyzer-info"
          data-testid="test-mapping-analyzer-info"
        >
          <h3>
            <FormattedMessage id="analyzer.testMapping.analyzerInfo" />
          </h3>
          <div className="test-mapping-info-row">
            <span className="test-mapping-info-label">
              <FormattedMessage id="analyzer.testMapping.analyzerName" />:
            </span>
            <span>{analyzerName || "-"}</span>
          </div>
          <div className="test-mapping-info-row">
            <span className="test-mapping-info-label">
              <FormattedMessage id="analyzer.testMapping.analyzerType" />:
            </span>
            <span>{analyzerType || "-"}</span>
          </div>
          <div className="test-mapping-info-row">
            <span className="test-mapping-info-label">
              <FormattedMessage
                id="analyzer.testMapping.activeMappings"
                values={{ count: activeMappingsCount }}
              />
              :
            </span>
            <span>{activeMappingsCount}</span>
          </div>
        </div>

        {/* Sample Message Input Section */}
        <div
          className="test-mapping-input-section"
          data-testid="test-mapping-input-section"
        >
          <TextArea
            id="astm-message-input"
            labelText={
              <FormattedMessage id="analyzer.testMapping.messageInput.required" />
            }
            helperText={
              <FormattedMessage id="analyzer.testMapping.messageInput.helper" />
            }
            placeholder={intl.formatMessage({
              id: "analyzer.testMapping.messageInput.placeholder",
            })}
            value={astmMessage}
            onChange={(e) => setAstmMessage(e.target.value)}
            rows={8}
            maxLength={MAX_MESSAGE_SIZE}
            invalid={isNearLimit}
            invalidText={
              isNearLimit
                ? intl.formatMessage(
                    { id: "analyzer.testMapping.messageInput.counter" },
                    { current: characterCount, max: MAX_MESSAGE_SIZE },
                  )
                : ""
            }
            data-testid="test-mapping-message-input"
          />
          <div className="test-mapping-character-counter">
            <span className={isNearLimit ? "test-mapping-counter-warning" : ""}>
              {intl.formatMessage(
                { id: "analyzer.testMapping.messageInput.counter" },
                { current: characterCount, max: MAX_MESSAGE_SIZE },
              )}
            </span>
          </div>
        </div>

        {/* Preview Options Section */}
        <div
          className="test-mapping-options-section"
          data-testid="test-mapping-options-section"
        >
          <h3>
            <FormattedMessage id="analyzer.testMapping.previewOptions" />
          </h3>
          <Checkbox
            id="detailed-parsing-option"
            labelText={
              <FormattedMessage id="analyzer.testMapping.previewOptions.detailedParsing" />
            }
            checked={includeDetailedParsing}
            onChange={(checked) => setIncludeDetailedParsing(checked)}
            data-testid="test-mapping-option-detailed"
          />
          <Checkbox
            id="validate-all-option"
            labelText={
              <FormattedMessage id="analyzer.testMapping.previewOptions.validateAll" />
            }
            checked={validateAllMappings}
            onChange={(checked) => setValidateAllMappings(checked)}
            data-testid="test-mapping-option-validate"
          />
        </div>

        {/* Error Display */}
        {error && (
          <InlineNotification
            kind="error"
            title={error}
            lowContrast
            hideCloseButton
            data-testid="test-mapping-error"
          />
        )}

        {/* Loading State */}
        {loading && (
          <div
            className="test-mapping-loading"
            data-testid="test-mapping-loading"
          >
            <Loading
              description={intl.formatMessage({
                id: "analyzer.testMapping.loading",
              })}
              withOverlay={false}
            />
          </div>
        )}

        {/* Result Display Section */}
        {previewResult && !loading && (
          <div
            className="test-mapping-results"
            data-testid="test-mapping-results"
          >
            <h3>
              <FormattedMessage id="analyzer.testMapping.results" />
            </h3>

            {/* Parsed Fields Table */}
            {previewResult.parsedFields &&
              previewResult.parsedFields.length > 0 && (
                <div
                  className="test-mapping-section"
                  data-testid="test-mapping-parsed-fields"
                >
                  <h4>
                    <FormattedMessage id="analyzer.testMapping.parsedFields" />
                  </h4>
                  <TableContainer>
                    <Table>
                      <TableHead>
                        <TableRow>
                          {parsedFieldsHeaders.map((header) => (
                            <TableHeader key={header.key}>
                              {header.header}
                            </TableHeader>
                          ))}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {previewResult.parsedFields.map((field, index) => (
                          <TableRow key={index}>
                            <TableCell>{field.fieldName || "-"}</TableCell>
                            <TableCell>{field.astmRef || "-"}</TableCell>
                            <TableCell>{field.rawValue || "-"}</TableCell>
                            <TableCell>{field.mappedTo || "-"}</TableCell>
                            <TableCell>{field.interpretation || "-"}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </div>
              )}

            {/* Applied Mappings Section */}
            {previewResult.appliedMappings &&
              previewResult.appliedMappings.length > 0 && (
                <div
                  className="test-mapping-section"
                  data-testid="test-mapping-applied-mappings"
                >
                  <h4>
                    <FormattedMessage id="analyzer.testMapping.appliedMappings" />
                  </h4>
                  <ul>
                    {previewResult.appliedMappings.map((mapping, index) => (
                      <li key={index}>
                        {mapping.analyzerFieldName} →{" "}
                        {mapping.openelisFieldType} ({mapping.openelisFieldId}):{" "}
                        {mapping.mappedValue}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

            {/* Entity Preview Section */}
            {previewResult.entityPreview && (
              <div
                className="test-mapping-section"
                data-testid="test-mapping-entity-preview"
              >
                <h4>
                  <FormattedMessage id="analyzer.testMapping.entityPreview" />
                </h4>
                <CodeSnippet type="multi" feedback="Copied to clipboard">
                  {JSON.stringify(previewResult.entityPreview, null, 2)}
                </CodeSnippet>
              </div>
            )}

            {/* Warnings Section */}
            {previewResult.warnings && previewResult.warnings.length > 0 && (
              <div
                className="test-mapping-section"
                data-testid="test-mapping-warnings"
              >
                <h4>
                  <FormattedMessage id="analyzer.testMapping.warnings" />
                </h4>
                {previewResult.warnings.map((warning, index) => (
                  <InlineNotification
                    key={index}
                    kind="warning"
                    title={warning}
                    lowContrast
                    hideCloseButton
                  />
                ))}
              </div>
            )}

            {/* Errors Section */}
            {previewResult.errors && previewResult.errors.length > 0 && (
              <div
                className="test-mapping-section"
                data-testid="test-mapping-errors"
              >
                <h4>
                  <FormattedMessage id="analyzer.testMapping.errors" />
                </h4>
                {previewResult.errors.map((error, index) => (
                  <InlineNotification
                    key={index}
                    kind="error"
                    title={error}
                    lowContrast
                    hideCloseButton
                  />
                ))}
              </div>
            )}
          </div>
        )}

        {/* No Results Message */}
        {!previewResult && !loading && !error && (
          <div
            className="test-mapping-no-results"
            data-testid="test-mapping-no-results"
          >
            <FormattedMessage id="analyzer.testMapping.noResults" />
          </div>
        )}
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={handleClose}
          data-testid="test-mapping-close"
        >
          <FormattedMessage id="button.close" defaultMessage="Close" />
        </Button>
        {previewResult && (
          <Button
            kind="ghost"
            onClick={handleTestAnother}
            data-testid="test-mapping-test-another"
          >
            <FormattedMessage id="analyzer.testMapping.testAnother" />
          </Button>
        )}
        <Button
          kind="primary"
          onClick={handlePreview}
          disabled={loading || !astmMessage || astmMessage.trim().length === 0}
          data-testid="test-mapping-preview-button"
        >
          <FormattedMessage id="analyzer.testMapping.submit" />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default TestMappingModal;
