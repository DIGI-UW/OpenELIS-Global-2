import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  InlineNotification,
  Select,
  SelectItem,
  TextInput,
  Tile,
} from "@carbon/react";
import { Checkmark, Renew } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * PharmaceuticalQualityCheckPage - Page 2 of the Pharmaceuticals workflow.
 * Performs initial QC and marks samples as ready for downstream processing.
 */
function PharmaceuticalQualityCheckPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const [qcForm, setQcForm] = useState({
    qcResult: "",
    qcRemarks: "",
  });

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    if (String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    setSuccessMessage(null);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              status: sample.pageStatus || sample.status || "PENDING",
              qcResult: sample.data?.qcResult,
              qcRemarks: sample.data?.qcRemarks,
            }));
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  const applyQcToSelected = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.qc.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    const data = {};
    if (qcForm.qcResult) data.qcResult = qcForm.qcResult;
    if (qcForm.qcRemarks) data.qcRemarks = qcForm.qcRemarks;

    if (Object.keys(data).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.qc.error.noData",
          defaultMessage: "Select a QC result or add remarks before applying.",
        }),
      );
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        data,
      }),
      (status) => {
        if (status === 200) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.page.pharma.qc.applied",
                defaultMessage: "Applied QC values to {count} sample(s).",
              },
              { count: selectedSampleIds.length },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.pharma.qc.error.apply",
              defaultMessage: "Failed to apply QC values. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    qcForm,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  const markQcComplete = useCallback(() => {
    if (selectedSampleIds.length === 0) return;
    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.page.pharma.qc.completed",
                defaultMessage: "Marked {count} sample(s) as QC Complete.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.pharma.error.status",
              defaultMessage: "Failed to update status. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const pendingCount = samples.filter((s) => s.status !== "COMPLETED").length;

  return (
    <div className="pharma-quality-check-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.pharma.qc.title"
            defaultMessage="Raw Sample Quality Check (QC)"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.pharma.qc.description"
            defaultMessage="Verify container integrity, label readability, appearance, and environmental deviations. Record QC result and remarks, then mark samples as QC complete."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.qc.completedCount"
                  defaultMessage="QC Complete"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.qc.pendingCount"
                  defaultMessage="Pending QC"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        {selectedSampleIds.length > 0 && (
          <>
            <Button
              kind="secondary"
              size="sm"
              renderIcon={Renew}
              onClick={applyQcToSelected}
            >
              <FormattedMessage
                id="notebook.page.pharma.qc.apply"
                defaultMessage="Apply QC values ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="primary"
              size="sm"
              renderIcon={Checkmark}
              onClick={markQcComplete}
            >
              <FormattedMessage
                id="notebook.page.pharma.qc.markComplete"
                defaultMessage="Mark QC Complete"
              />
            </Button>
          </>
        )}
      </div>

      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          hideCloseButton
          lowContrast
        />
      )}

      <Grid fullWidth condensed className="form-grid">
        <Column lg={6} md={4} sm={4}>
          <Select
            id="qcResult"
            labelText={intl.formatMessage({
              id: "notebook.page.pharma.qc.field.result",
              defaultMessage: "QC Result",
            })}
            value={qcForm.qcResult}
            onChange={(e) =>
              setQcForm((prev) => ({ ...prev, qcResult: e.target.value }))
            }
          >
            <SelectItem value="" text={intl.formatMessage({ id: "label.select", defaultMessage: "Select" })} />
            <SelectItem value="Pass" text="Pass" />
            <SelectItem value="Fail" text="Fail" />
            <SelectItem value="Pass with remarks" text="Pass with remarks" />
          </Select>
        </Column>
        <Column lg={10} md={6} sm={4}>
          <TextInput
            id="qcRemarks"
            labelText={intl.formatMessage({
              id: "notebook.page.pharma.qc.field.remarks",
              defaultMessage: "QC Remarks",
            })}
            value={qcForm.qcRemarks}
            onChange={(e) =>
              setQcForm((prev) => ({ ...prev, qcRemarks: e.target.value }))
            }
            placeholder={intl.formatMessage({
              id: "notebook.page.pharma.qc.placeholder",
              defaultMessage: "Condition on arrival, label readability, deviations...",
            })}
          />
        </Column>
      </Grid>

      <div className="sample-grid-container">
        <SampleGrid
          samples={samples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            { key: "accessionNumber", header: "Accession Number" },
            { key: "externalId", header: "Sample ID" },
            { key: "qcResult", header: "QC Result" },
            { key: "qcRemarks", header: "Remarks" },
            { key: "status", header: "Status" },
          ]}
        />
      </div>

      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.pharma.qc.empty"
              defaultMessage="No samples available yet. Complete Sample Creation first, then perform QC."
            />
          </p>
        </div>
      )}
    </div>
  );
}

export default PharmaceuticalQualityCheckPage;

