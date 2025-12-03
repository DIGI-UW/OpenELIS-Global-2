/**
 * StatisticsConfigModal Component
 *
 * Modal for configuring statistics calculation method for control lots
 * Task Reference: T054
 * Specification: FR-003, FR-004, FR-005, User Story 6
 *
 * Features:
 * - Select calculation method (Manufacturer Fixed, Rolling, Initial Runs)
 * - Configure rolling window size
 * - Configure initial runs required
 * - Enter manufacturer mean and SD
 */

import React, { useState } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  FormGroup,
  RadioButtonGroup,
  RadioButton,
  NumberInput,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import PropTypes from "prop-types";
import "./StatisticsConfigModal.css";

const StatisticsConfigModal = ({ open, config, onClose, onSave }) => {
  const intl = useIntl();

  // Local state for configuration
  const [localConfig, setLocalConfig] = useState({
    calculationMethod: config?.calculationMethod || "MANUFACTURER_FIXED",
    rollingWindowSize: config?.rollingWindowSize || 20,
    initialRunsRequired: config?.initialRunsRequired || 20,
    mean: config?.mean || "",
    standardDeviation: config?.standardDeviation || "",
  });

  // Handle calculation method change
  const handleMethodChange = (method) => {
    setLocalConfig((prev) => ({
      ...prev,
      calculationMethod: method,
    }));
  };

  // Handle numeric input change
  const handleNumericChange = (field, value) => {
    setLocalConfig((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Handle save
  const handleSave = () => {
    onSave({
      ...localConfig,
      mean: localConfig.mean ? parseFloat(localConfig.mean) : null,
      standardDeviation: localConfig.standardDeviation ? parseFloat(localConfig.standardDeviation) : null,
    });
  };

  // Validate form
  const isValid = () => {
    if (localConfig.calculationMethod === "MANUFACTURER_FIXED") {
      return localConfig.mean && localConfig.standardDeviation;
    }
    if (localConfig.calculationMethod === "ROLLING") {
      return localConfig.rollingWindowSize >= 10 && localConfig.rollingWindowSize <= 100;
    }
    if (localConfig.calculationMethod === "INITIAL_RUNS") {
      return localConfig.initialRunsRequired >= 10 && localConfig.initialRunsRequired <= 50;
    }
    return true;
  };

  return (
    <ComposedModal
      open={open}
      onClose={onClose}
      size="md"
      data-testid="statistics-config-modal"
    >
      <ModalHeader
        title={intl.formatMessage({ id: "qc.controlLot.statistics.config.title" })}
        label={intl.formatMessage({ id: "qc.controlLot.statistics.config.label" })}
        data-testid="statistics-config-modal-header"
      />
      <ModalBody data-testid="statistics-config-modal-body">
        {/* Calculation Method Selection (FR-003) */}
        <FormGroup legendText={intl.formatMessage({ id: "qc.controlLot.statistics.method" })}>
          <RadioButtonGroup
            name="calculation-method"
            valueSelected={localConfig.calculationMethod}
            onChange={handleMethodChange}
            orientation="vertical"
            data-testid="statistics-method-radio-group"
          >
            <RadioButton
              id="method-manufacturer"
              value="MANUFACTURER_FIXED"
              labelText={
                <div className="method-option">
                  <span className="method-option-label">
                    {intl.formatMessage({ id: "qc.controlLot.statistics.method.manufacturer_fixed" })}
                  </span>
                  <span className="method-option-description">
                    {intl.formatMessage({ id: "qc.controlLot.statistics.method.manufacturer_fixed.description" })}
                  </span>
                </div>
              }
              data-testid="statistics-method-manufacturer"
            />
            <RadioButton
              id="method-rolling"
              value="ROLLING"
              labelText={
                <div className="method-option">
                  <span className="method-option-label">
                    {intl.formatMessage({ id: "qc.controlLot.statistics.method.rolling" })}
                  </span>
                  <span className="method-option-description">
                    {intl.formatMessage({ id: "qc.controlLot.statistics.method.rolling.description" })}
                  </span>
                </div>
              }
              data-testid="statistics-method-rolling"
            />
            <RadioButton
              id="method-initial-runs"
              value="INITIAL_RUNS"
              labelText={
                <div className="method-option">
                  <span className="method-option-label">
                    {intl.formatMessage({ id: "qc.controlLot.statistics.method.initial_runs" })}
                  </span>
                  <span className="method-option-description">
                    {intl.formatMessage({ id: "qc.controlLot.statistics.method.initial_runs.description" })}
                  </span>
                </div>
              }
              data-testid="statistics-method-initial-runs"
            />
          </RadioButtonGroup>
        </FormGroup>

        {/* Manufacturer Fixed Options (FR-004) */}
        {localConfig.calculationMethod === "MANUFACTURER_FIXED" && (
          <div className="statistics-config-options" data-testid="statistics-manufacturer-options">
            <FormGroup legendText={intl.formatMessage({ id: "qc.controlLot.statistics.manufacturerValues" })}>
              <div className="statistics-config-inputs">
                <TextInput
                  id="manufacturer-mean"
                  labelText={intl.formatMessage({ id: "qc.controlLot.statistics.mean" })}
                  placeholder="0.00"
                  value={localConfig.mean}
                  onChange={(e) => handleNumericChange("mean", e.target.value)}
                  type="number"
                  step="0.01"
                  data-testid="statistics-mean-input"
                />
                <TextInput
                  id="manufacturer-sd"
                  labelText={intl.formatMessage({ id: "qc.controlLot.statistics.sd" })}
                  placeholder="0.00"
                  value={localConfig.standardDeviation}
                  onChange={(e) => handleNumericChange("standardDeviation", e.target.value)}
                  type="number"
                  step="0.01"
                  min="0"
                  data-testid="statistics-sd-input"
                />
              </div>
            </FormGroup>
          </div>
        )}

        {/* Rolling Window Options (FR-005) */}
        {localConfig.calculationMethod === "ROLLING" && (
          <div className="statistics-config-options" data-testid="statistics-rolling-options">
            <FormGroup legendText={intl.formatMessage({ id: "qc.controlLot.statistics.rollingConfig" })}>
              <NumberInput
                id="rolling-window-size"
                label={intl.formatMessage({ id: "qc.controlLot.statistics.windowSize" })}
                helperText={intl.formatMessage({ id: "qc.controlLot.statistics.windowSize.helper" })}
                min={10}
                max={100}
                step={5}
                value={localConfig.rollingWindowSize}
                onChange={(e, { value }) => handleNumericChange("rollingWindowSize", value)}
                data-testid="statistics-window-size-input"
              />
            </FormGroup>
          </div>
        )}

        {/* Initial Runs Options (FR-005) */}
        {localConfig.calculationMethod === "INITIAL_RUNS" && (
          <div className="statistics-config-options" data-testid="statistics-initial-runs-options">
            <FormGroup legendText={intl.formatMessage({ id: "qc.controlLot.statistics.initialRunsConfig" })}>
              <NumberInput
                id="initial-runs-required"
                label={intl.formatMessage({ id: "qc.controlLot.statistics.initialRunsRequired" })}
                helperText={intl.formatMessage({ id: "qc.controlLot.statistics.initialRunsRequired.helper" })}
                min={10}
                max={50}
                step={5}
                value={localConfig.initialRunsRequired}
                onChange={(e, { value }) => handleNumericChange("initialRunsRequired", value)}
                data-testid="statistics-initial-runs-input"
              />
            </FormGroup>
          </div>
        )}
      </ModalBody>
      <ModalFooter data-testid="statistics-config-modal-footer">
        <Button kind="secondary" onClick={onClose} data-testid="statistics-config-cancel-button">
          {intl.formatMessage({ id: "button.cancel" })}
        </Button>
        <Button
          kind="primary"
          onClick={handleSave}
          disabled={!isValid()}
          data-testid="statistics-config-save-button"
        >
          {intl.formatMessage({ id: "button.save" })}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

StatisticsConfigModal.propTypes = {
  open: PropTypes.bool.isRequired,
  config: PropTypes.shape({
    calculationMethod: PropTypes.string,
    rollingWindowSize: PropTypes.number,
    initialRunsRequired: PropTypes.number,
    mean: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    standardDeviation: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  }),
  onClose: PropTypes.func.isRequired,
  onSave: PropTypes.func.isRequired,
};

StatisticsConfigModal.defaultProps = {
  config: {
    calculationMethod: "MANUFACTURER_FIXED",
    rollingWindowSize: 20,
    initialRunsRequired: 20,
    mean: null,
    standardDeviation: null,
  },
};

export default StatisticsConfigModal;
