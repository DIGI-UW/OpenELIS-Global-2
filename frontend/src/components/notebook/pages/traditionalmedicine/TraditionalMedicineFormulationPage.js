import React, { useState, useEffect, useRef, useCallback, useMemo, useContext } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  Modal,
  Dropdown,
  TextInput,
  TextArea,
  Loading,
} from "@carbon/react";
import {
  Renew,
  CheckmarkFilled,
  Edit,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import NotificationContext, { NotificationKinds } from "../../layout/Layout";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineFormulationPage - Page 7 of the Traditional Medicine workflow.
 *
 * SRS Requirements - STAGE 8: Formulation
 * - Formulation types (capsules, tinctures, ointments, teas, syrups)
 * - Batch number, manufacturing date
 * - QC results (stability, microbial, heavy metals)
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 */
function TraditionalMedicineFormulationPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } = useContext(NotificationContext);
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [formulationModalOpen, setFormulationModalOpen] = useState(false);
  const [isApplying, setIsApplying] = useState(false);

  const [formulationType, setFormulationType] = useState(null);
  const [batchNumber, setBatchNumber] = useState("");
  const [manufacturingDate, setManufacturingDate] = useState("");
  const [ingredients, setIngredients] = useState("");
  const [qcNotes, setQcNotes] = useState("");

  const formulationOptions = [
    { id: "capsule", label: "Capsules" },
    { id: "tincture", label: "Tinctures" },
    { id: "ointment", label: "Ointments/Creams" },
    { id: "tea", label: "Teas" },
    { id: "syrup", label: "Syrups" },
    { id: "other", label: "Other" },
  ];

  // Notification callback
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          setSamples(
            response && Array.isArray(response)
              ? response.map((s) => ({
                  id: String(s.id || s.sampleItemId),
                  externalId: s.externalId,
                  accessionNumber: s.accessionNumber,
                  status: s.pageStatus || s.status || "PENDING",
                  localName: s.data?.localName,
                  formulationType: s.data?.formulationType,
                  batchNumber: s.data?.batchNumber,
                }))
              : [],
          );
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, loadPageSamples]);

  const resetForm = useCallback(() => {
    setFormulationType(null);
    setBatchNumber("");
    setManufacturingDate("");
    setIngredients("");
    setQcNotes("");
  }, []);

  const openModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetForm();
    setFormulationModalOpen(true);
  }, [selectedSampleIds, intl, resetForm, notify]);

  const applyFormulation = useCallback(() => {
    if (!formulationType) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.formulation.error.typeRequired",
          defaultMessage: "Please select formulation type.",
        }),
      });
      return;
    }

    if (!batchNumber) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.formulation.error.batchRequired",
          defaultMessage: "Please enter batch number.",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplying(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/formulation`,
      JSON.stringify({
        sampleIds,
        formulationType: formulationType.id,
        formulationTypeLabel: formulationType.label,
        batchNumber,
        manufacturingDate,
        ingredients,
        qcNotes,
      }),
      (response) => {
        setIsApplying(false);
        if (response?.success) {
          // Update sample status using bulk endpoint after formulation
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds,
              status: "IN_PROGRESS",
            }),
            (statusCode) => {
              if (statusCode === 200) {
                notify({
                  kind: NotificationKinds.success,
                  title: response.message ||
                    intl.formatMessage(
                      {
                        id: "notebook.page.tradmed.formulation.success",
                        defaultMessage: "Recorded formulation for {count} sample(s).",
                      },
                      { count: response.updatedCount || selectedSampleIds.length },
                    ),
                });
                setFormulationModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.page.tradmed.error.statusUpdate",
                    defaultMessage: "Formulation recorded but failed to update sample status.",
                  }),
                });
              }
            }
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "Operation failed",
          });
        }
      },
    );
  }, [
    formulationType,
    batchNumber,
    manufacturingDate,
    ingredients,
    qcNotes,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  const pendingSamples = useMemo(
    () => samples.filter((s) => !s.formulationType),
    [samples],
  );
  const formulatedSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.formulationType && s.status === "COMPLETED",
      ),
    [samples],
  );

  return (
    <div className="tradmed-formulation-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.formulation.title"
            defaultMessage="Formulation of Medical Product"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.formulation.description"
            defaultMessage="Record formulation details, batch numbers, ingredients, and QC results for final medical product."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.formulation.pending"
                  defaultMessage="Awaiting Formulation"
                />
              </span>
              <span className="progress-value">{pendingSamples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.formulation.formulated"
                  defaultMessage="Formulated"
                />
              </span>
              <span className="progress-value">{formulatedSamples.length}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={openModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.tradmed.formulation.recordFormulation"
            defaultMessage="Record Formulation ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
          disabled={loading}
        >
          <FormattedMessage
            id="notebook.page.tradmed.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>


      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.formulation.pending.title"
              defaultMessage="Samples Awaiting Formulation"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {pendingSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.formulation.pending.empty"
                  defaultMessage="No samples awaiting formulation."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="pending-formulation"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "localName", header: "Local Name" },
              ]}
            />
          )}
        </div>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.formulation.formulated.title"
              defaultMessage="Formulated Products"
            />
            <Tag type="green" size="sm" className="count-tag">
              {formulatedSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && formulatedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.formulation.formulated.empty"
                  defaultMessage="No formulated products yet."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="formulated-samples"
              samples={formulatedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "formulationType", header: "Type" },
                { key: "batchNumber", header: "Batch #" },
              ]}
            />
          )}
        </div>
      </div>

      <Modal
        open={formulationModalOpen}
        onRequestClose={() => setFormulationModalOpen(false)}
        onRequestSubmit={applyFormulation}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.formulation.modal.title",
          defaultMessage: "Record Formulation Details",
        })}
        primaryButtonText={
          isApplying
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.record",
                defaultMessage: "Record Formulation",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplying}
        size="md"
      >
        {isApplying && <Loading withOverlay={false} small />}

        <Grid fullWidth narrow>
          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="formulation-type"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.type",
                defaultMessage: "Formulation Type *",
              })}
              label="Select..."
              items={formulationOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={formulationType}
              onChange={({ selectedItem }) => setFormulationType(selectedItem)}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="batch-number"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.batchNumber",
                defaultMessage: "Batch Number *",
              })}
              value={batchNumber}
              onChange={(e) => setBatchNumber(e.target.value)}
              placeholder="e.g., FORM-2025-001"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="manufacturing-date"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.mfgDate",
                defaultMessage: "Manufacturing Date",
              })}
              type="date"
              value={manufacturingDate}
              onChange={(e) => setManufacturingDate(e.target.value)}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="ingredients"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.ingredients",
                defaultMessage: "Ingredients & Concentrations",
              })}
              value={ingredients}
              onChange={(e) => setIngredients(e.target.value)}
              rows={2}
              placeholder="List all ingredients with concentrations/quantities"
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="qc-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.qcNotes",
                defaultMessage: "QC Results & Notes",
              })}
              value={qcNotes}
              onChange={(e) => setQcNotes(e.target.value)}
              rows={3}
              placeholder="Stability testing, microbial testing, heavy metals, etc."
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineFormulationPage;
