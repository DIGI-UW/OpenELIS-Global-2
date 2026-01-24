import React, { useState, useEffect } from "react";
import { useIntl } from "react-intl";
import {
  Button,
  Loading,
  InlineNotification,
  TextInput,
} from "@carbon/react";
import { DocumentPdf, View, Reset } from "@carbon/icons-react";
import FieldSelector from "./FieldSelector";
import FilterBuilder from "./FilterBuilder";
import ReportPreview from "./ReportPreview";
import { getFromOpenElisServer } from "../../utils/Utils";
import config from "../../../config.json";
import "../../Style.css";

const AdHocReportBuilder = () => {
  const intl = useIntl();

  const [availableFields, setAvailableFields] = useState({
    patientFields: [],
    sampleFields: [],
  });
  const [fieldsLoading, setFieldsLoading] = useState(true);
  const [fieldsError, setFieldsError] = useState(null);

  const [selectedFields, setSelectedFields] = useState([]);
  const [filters, setFilters] = useState([]);
  const [reportTitle, setReportTitle] = useState("");

  const [previewData, setPreviewData] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);

  const [pdfLoading, setPdfLoading] = useState(false);
  const [pdfError, setPdfError] = useState(null);

  useEffect(() => {
    loadAvailableFields();
  }, []);

  const loadAvailableFields = () => {
    setFieldsLoading(true);
    setFieldsError(null);

    getFromOpenElisServer("/rest/adhoc-report/fields", (response) => {
      if (response) {
        setAvailableFields({
          patientFields: response.patientFields || [],
          sampleFields: response.sampleFields || [],
        });
      } else {
        setFieldsError(intl.formatMessage({ id: "adhoc.error.loadingFields" }));
      }
      setFieldsLoading(false);
    });
  };

  const getAllFields = () => {
    return [...availableFields.patientFields, ...availableFields.sampleFields];
  };

  const handleFieldToggle = (fieldId, isChecked) => {
    if (isChecked) {
      setSelectedFields([...selectedFields, fieldId]);
    } else {
      setSelectedFields(selectedFields.filter((f) => f !== fieldId));
    }
    setPreviewData(null);
  };

  const handleSelectAll = (entity, fieldIds) => {
    const newSelected = [...new Set([...selectedFields, ...fieldIds])];
    setSelectedFields(newSelected);
    setPreviewData(null);
  };

  const handleDeselectAll = (entity, fieldIds) => {
    setSelectedFields(selectedFields.filter((f) => !fieldIds.includes(f)));
    setPreviewData(null);
  };

  const handleFiltersChange = (newFilters) => {
    setFilters(newFilters);
    setPreviewData(null);
  };

  const buildReportDefinition = () => {
    return {
      selectedFields: selectedFields,
      filters: filters
        .filter((f) => f.fieldId)
        .map((f) => ({
          fieldId: f.fieldId,
          operator: f.operator,
          value: f.value,
          valueTo: f.valueTo || null,
        })),
      reportTitle:
        reportTitle || intl.formatMessage({ id: "adhoc.report.default.title" }),
      limit: pageSize,
      offset: (currentPage - 1) * pageSize,
    };
  };

  const handlePreview = () => {
    if (selectedFields.length === 0) {
      setPreviewError(
        intl.formatMessage({ id: "adhoc.error.noFieldsSelected" }),
      );
      return;
    }

    setPreviewLoading(true);
    setPreviewError(null);

    const definition = buildReportDefinition();

    fetch(config.serverBaseUrl + "/rest/adhoc-report/preview", {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: JSON.stringify(definition),
    })
      .then((response) => {
        if (!response.ok) {
          return response.json().then((err) => {
            throw new Error(err.message || "Preview failed");
          });
        }
        return response.json();
      })
      .then((data) => {
        setPreviewLoading(false);
        setPreviewData(data);
        setPreviewError(null);
      })
      .catch((error) => {
        setPreviewLoading(false);
        setPreviewError(
          error.message || intl.formatMessage({ id: "adhoc.error.preview" }),
        );
        setPreviewData(null);
      });
  };

  const handleGeneratePdf = () => {
    if (selectedFields.length === 0) {
      setPdfError(intl.formatMessage({ id: "adhoc.error.noFieldsSelected" }));
      return;
    }

    setPdfLoading(true);
    setPdfError(null);

    const definition = buildReportDefinition();
    definition.limit = 10000;
    definition.offset = 0;

    fetch(config.serverBaseUrl + "/rest/adhoc-report/generate-pdf", {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: JSON.stringify(definition),
    })
      .then((response) => {
        if (!response.ok) {
          return response
            .json()
            .then((err) => {
              throw new Error(err.message || "PDF generation failed");
            })
            .catch(() => {
              throw new Error(intl.formatMessage({ id: "adhoc.error.pdf" }));
            });
        }
        return response.blob();
      })
      .then((blob) => {
        setPdfLoading(false);
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = `${reportTitle || "adhoc-report"}_${new Date().toISOString().slice(0, 10)}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      })
      .catch((error) => {
        setPdfLoading(false);
        setPdfError(
          error.message || intl.formatMessage({ id: "adhoc.error.pdf" }),
        );
      });
  };

  const handleReset = () => {
    setSelectedFields([]);
    setFilters([]);
    setReportTitle("");
    setPreviewData(null);
    setPreviewError(null);
    setPdfError(null);
    setCurrentPage(1);
  };

  const handlePageChange = (page) => {
    setCurrentPage(page);
    setTimeout(() => handlePreview(), 0);
  };

  const handlePageSizeChange = (size) => {
    setPageSize(size);
    setCurrentPage(1);
    setTimeout(() => handlePreview(), 0);
  };

  if (fieldsLoading) {
    return (
      <div className="adhoc-report-container">
        <div className="adhoc-loading">
          <Loading
            description={intl.formatMessage({ id: "adhoc.loading" })}
            withOverlay={false}
          />
        </div>
      </div>
    );
  }

  if (fieldsError) {
    return (
      <div className="adhoc-report-container">
        <div className="adhoc-error">
          <h3>{intl.formatMessage({ id: "adhoc.error.title" })}</h3>
          <p>{fieldsError}</p>
          <Button kind="primary" onClick={loadAvailableFields}>
            {intl.formatMessage({ id: "adhoc.error.retry" })}
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="adhoc-report-container">
      <div className="adhoc-report-header">
        <h1>{intl.formatMessage({ id: "adhoc.report.title" })}</h1>
        <p>{intl.formatMessage({ id: "adhoc.report.description" })}</p>
      </div>

      <div className="adhoc-step">
        <TextInput
          id="report-title"
          labelText={intl.formatMessage({ id: "adhoc.report.titleLabel" })}
          placeholder={intl.formatMessage({
            id: "adhoc.report.titlePlaceholder",
          })}
          value={reportTitle}
          onChange={(e) => setReportTitle(e.target.value)}
        />
      </div>

      <div className="adhoc-steps">
        <div className="adhoc-step">
          <FieldSelector
            patientFields={availableFields.patientFields}
            sampleFields={availableFields.sampleFields}
            selectedFields={selectedFields}
            onFieldToggle={handleFieldToggle}
            onSelectAll={handleSelectAll}
            onDeselectAll={handleDeselectAll}
          />
        </div>

        <div className="adhoc-step">
          <FilterBuilder
            availableFields={getAllFields()}
            filters={filters}
            onFiltersChange={handleFiltersChange}
          />
        </div>

        <div className="adhoc-report-actions">
          <Button
            kind="primary"
            renderIcon={View}
            onClick={handlePreview}
            disabled={selectedFields.length === 0 || previewLoading}
          >
            {previewLoading
              ? intl.formatMessage({ id: "adhoc.button.previewing" })
              : intl.formatMessage({ id: "adhoc.button.preview" })}
          </Button>

          <Button
            kind="secondary"
            renderIcon={DocumentPdf}
            onClick={handleGeneratePdf}
            disabled={selectedFields.length === 0 || pdfLoading}
          >
            {pdfLoading
              ? intl.formatMessage({ id: "adhoc.button.generating" })
              : intl.formatMessage({ id: "adhoc.button.generatePdf" })}
          </Button>

          <Button kind="ghost" renderIcon={Reset} onClick={handleReset}>
            {intl.formatMessage({ id: "adhoc.button.reset" })}
          </Button>
        </div>

        {(previewError || pdfError) && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "adhoc.error.title" })}
            subtitle={previewError || pdfError}
            onCloseButtonClick={() => {
              setPreviewError(null);
              setPdfError(null);
            }}
          />
        )}

        <div className="adhoc-step">
          <ReportPreview
            reportData={previewData}
            isLoading={previewLoading}
            error={previewError}
            currentPage={currentPage}
            pageSize={pageSize}
            onPageChange={handlePageChange}
            onPageSizeChange={handlePageSizeChange}
          />
        </div>
      </div>
    </div>
  );
};

export default AdHocReportBuilder;
