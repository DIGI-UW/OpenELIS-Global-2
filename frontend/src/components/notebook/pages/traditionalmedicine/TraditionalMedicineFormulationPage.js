import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Tag,
} from "@carbon/react";
import { Checkmark, Edit, Add } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineFormulationPage - Page 8 of the Traditional Medicine workflow.
 * Create final traditional/modern medicine products including capsules, tinctures,
 * ointments, teas, and other formulations.
 */
function TraditionalMedicineFormulationPage({
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

  // Bulk Apply Modal State
  const [bulkModalOpen, setBulkModalOpen] = useState(false);
  const [bulkFormData, setBulkFormData] = useState({
    productType: "",
    productName: "",
    batchId: "",
    formulationDate: "",
    preparedBy: "",
    ingredients: "",
    dosageForm: "",
    strength: "",
    strengthUnit: "",
    quantity: "",
    quantityUnit: "",
    shelfLife: "",
    storageInstructions: "",
    qualityCheckPassed: false,
    formulationNotes: "",
  });

  // Product type options
  const productTypes = [
    {
      value: "",
      label: intl.formatMessage({
        id: "common.select",
        defaultMessage: "Select...",
      }),
    },
    { value: "Capsule", label: "Capsule" },
    { value: "Tablet", label: "Tablet" },
    { value: "Tincture", label: "Tincture" },
    { value: "Ointment", label: "Ointment/Cream" },
    { value: "Tea", label: "Tea/Infusion" },
    { value: "Powder", label: "Powder" },
    { value: "Syrup", label: "Syrup" },
    { value: "Oil", label: "Essential Oil/Extract Oil" },
    { value: "Decoction", label: "Decoction" },
    { value: "Poultice", label: "Poultice" },
    { value: "Suppository", label: "Suppository" },
    { value: "Lotion", label: "Lotion" },
    { value: "Other", label: "Other" },
  ];

  // Dosage form options
  const dosageForms = [
    {
      value: "",
      label: intl.formatMessage({
        id: "common.select",
        defaultMessage: "Select...",
      }),
    },
    { value: "Oral", label: "Oral" },
    { value: "Topical", label: "Topical" },
    { value: "Sublingual", label: "Sublingual" },
    { value: "Inhalation", label: "Inhalation" },
    { value: "Rectal", label: "Rectal" },
    { value: "Nasal", label: "Nasal" },
    { value: "Other", label: "Other" },
  ];

  // Strength unit options
  const strengthUnits = [
    {
      value: "",
      label: intl.formatMessage({
        id: "common.select",
        defaultMessage: "Select...",
      }),
    },
    { value: "mg", label: "mg (milligrams)" },
    { value: "g", label: "g (grams)" },
    { value: "ml", label: "ml (milliliters)" },
    { value: "%", label: "% (percentage)" },
    { value: "IU", label: "IU (International Units)" },
    { value: "mcg", label: "mcg (micrograms)" },
  ];

  // Quantity unit options
  const quantityUnits = [
    {
      value: "",
      label: intl.formatMessage({
        id: "common.select",
        defaultMessage: "Select...",
      }),
    },
    { value: "capsules", label: "Capsules" },
    { value: "tablets", label: "Tablets" },
    { value: "ml", label: "ml (milliliters)" },
    { value: "L", label: "L (liters)" },
    { value: "g", label: "g (grams)" },
    { value: "kg", label: "kg (kilograms)" },
    { value: "sachets", label: "Sachets" },
    { value: "units", label: "Units" },
  ];

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
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
              localName: sample.data?.localName,
              extractId: sample.data?.extractId,
              // Formulation fields
              productType: sample.data?.productType,
              productName: sample.data?.productName,
              batchId: sample.data?.batchId,
              formulationDate: sample.data?.formulationDate,
              preparedBy: sample.data?.preparedBy,
              ingredients: sample.data?.ingredients,
              dosageForm: sample.data?.dosageForm,
              strength: sample.data?.strength,
              strengthUnit: sample.data?.strengthUnit,
              quantity: sample.data?.quantity,
              quantityUnit: sample.data?.quantityUnit,
              shelfLife: sample.data?.shelfLife,
              storageInstructions: sample.data?.storageInstructions,
              qualityCheckPassed: sample.data?.qualityCheckPassed,
              formulationNotes: sample.data?.formulationNotes,
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

  // Open bulk apply modal
  const openBulkModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    // Generate a default batch ID
    const today = new Date().toISOString().slice(0, 10).replace(/-/g, "");
    const defaultBatchId = `BATCH-${today}-${Math.random().toString(36).substr(2, 4).toUpperCase()}`;

    setBulkFormData({
      productType: "",
      productName: "",
      batchId: defaultBatchId,
      formulationDate: new Date().toISOString().slice(0, 10),
      preparedBy: "",
      ingredients: "",
      dosageForm: "",
      strength: "",
      strengthUnit: "",
      quantity: "",
      quantityUnit: "",
      shelfLife: "",
      storageInstructions: "",
      qualityCheckPassed: false,
      formulationNotes: "",
    });
    setBulkModalOpen(true);
  }, [selectedSampleIds, intl]);

  // Apply bulk formulation data
  const applyBulkFormulation = useCallback(() => {
    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Validate required fields
    if (!bulkFormData.productType) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noProductType",
          defaultMessage: "Please select a product type.",
        }),
      );
      return;
    }

    if (!bulkFormData.batchId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noBatchId",
          defaultMessage: "Please enter a batch ID.",
        }),
      );
      return;
    }

    setError(null);
    setSuccessMessage(null);

    const requestData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      data: {
        productType: bulkFormData.productType,
        productName: bulkFormData.productName || null,
        batchId: bulkFormData.batchId,
        formulationDate: bulkFormData.formulationDate || null,
        preparedBy: bulkFormData.preparedBy || null,
        ingredients: bulkFormData.ingredients || null,
        dosageForm: bulkFormData.dosageForm || null,
        strength: bulkFormData.strength
          ? parseFloat(bulkFormData.strength)
          : null,
        strengthUnit: bulkFormData.strengthUnit || null,
        quantity: bulkFormData.quantity
          ? parseInt(bulkFormData.quantity, 10)
          : null,
        quantityUnit: bulkFormData.quantityUnit || null,
        shelfLife: bulkFormData.shelfLife || null,
        storageInstructions: bulkFormData.storageInstructions || null,
        qualityCheckPassed: bulkFormData.qualityCheckPassed,
        formulationNotes: bulkFormData.formulationNotes || null,
      },
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify(requestData),
      (status) => {
        if (status === 200) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.page.tradmed.success.formulationApplied",
                defaultMessage:
                  "Applied formulation data to {count} sample(s).",
              },
              { count: selectedSampleIds.length },
            ),
          );
          setBulkModalOpen(false);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.tradmed.error.bulkApply",
              defaultMessage:
                "Failed to apply formulation data. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    hasRealPageId,
    bulkFormData,
    selectedSampleIds,
    intl,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Mark as formulated (product created)
  const markAsFormulated = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Validate selected samples have formulation data
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const incompleteCount = selectedSamples.filter(
      (s) => !s.productType || !s.batchId,
    ).length;
    if (incompleteCount > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.tradmed.error.incompleteFormulation",
            defaultMessage:
              "{count} sample(s) missing formulation data. Apply formulation details first.",
          },
          { count: incompleteCount },
        ),
      );
      return;
    }

    setError(null);
    setSuccessMessage(null);

    // Optimistic UI update
    setSamples((prevSamples) =>
      prevSamples.map((sample) =>
        selectedSampleIds.includes(sample.id)
          ? { ...sample, status: "COMPLETED" }
          : sample,
      ),
    );

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
                id: "notebook.page.tradmed.success.formulated",
                defaultMessage:
                  "Marked {count} sample(s) as Product Formulated.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          // Revert optimistic update on error
          loadPageSamples();
          setError(
            intl.formatMessage({
              id: "notebook.page.tradmed.error.status",
              defaultMessage: "Failed to update samples. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    samples,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  const pendingCount = samples.filter((s) => s.status === "PENDING").length;
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const inProgressCount = samples.filter(
    (s) => s.status === "IN_PROGRESS",
  ).length;

  // Render formulation info as tags
  const renderFormulationInfo = (sample) => {
    const tags = [];
    if (sample.productType) {
      tags.push(
        <Tag key="type" type="blue" size="sm">
          {sample.productType}
        </Tag>,
      );
    }
    if (sample.batchId) {
      tags.push(
        <Tag key="batch" type="gray" size="sm">
          {sample.batchId}
        </Tag>,
      );
    }
    if (sample.strength && sample.strengthUnit) {
      tags.push(
        <Tag key="strength" type="teal" size="sm">
          {sample.strength} {sample.strengthUnit}
        </Tag>,
      );
    }
    if (sample.qualityCheckPassed) {
      tags.push(
        <Tag key="qc" type="green" size="sm">
          QC Passed
        </Tag>,
      );
    }
    return tags.length > 0 ? tags : "-";
  };

  // Custom column renderer for formulation data
  const enhancedSamples = samples.map((sample) => ({
    ...sample,
    formulationInfo: renderFormulationInfo(sample),
  }));

  return (
    <div className="tradmed-formulation-page">
      {/* Page Header */}
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
            defaultMessage="Create final traditional/modern medicine products including capsules, tinctures, ointments, teas, and other formulations. Link ingredients to source extracts and assign batch IDs."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.pendingFormulation"
                  defaultMessage="Pending Formulation"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile in-progress">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.inProgress"
                  defaultMessage="In Progress"
                />
              </span>
              <span className="progress-value">{inProgressCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.formulated"
                  defaultMessage="Formulated"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        {selectedSampleIds.length > 0 && (
          <>
            <Button
              kind="tertiary"
              size="sm"
              renderIcon={Edit}
              onClick={openBulkModal}
            >
              <FormattedMessage
                id="notebook.page.tradmed.applyFormulation"
                defaultMessage="Apply Formulation Details ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="primary"
              size="sm"
              renderIcon={Checkmark}
              onClick={markAsFormulated}
            >
              <FormattedMessage
                id="notebook.page.tradmed.markFormulated"
                defaultMessage="Mark as Formulated ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
          </>
        )}
      </div>

      {/* Errors / Success */}
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

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          samples={enhancedSamples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            { key: "accessionNumber", header: "Accession Number" },
            { key: "externalId", header: "Sample ID" },
            { key: "localName", header: "Local Name" },
            { key: "productName", header: "Product Name" },
            { key: "formulationInfo", header: "Formulation Details" },
            { key: "preparedBy", header: "Prepared By" },
            { key: "status", header: "Status" },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.tradmed.formulation.empty"
              defaultMessage="No samples pending formulation. Complete product testing first."
            />
          </p>
        </div>
      )}

      {/* Bulk Apply Modal */}
      <Modal
        open={bulkModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.formulation.modal.title",
          defaultMessage: "Apply Formulation Details",
        })}
        primaryButtonText={intl.formatMessage({
          id: "common.apply",
          defaultMessage: "Apply",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setBulkModalOpen(false)}
        onRequestSubmit={applyBulkFormulation}
        size="lg"
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.page.tradmed.formulation.modal.description"
            defaultMessage="Apply formulation details to {count} selected sample(s). These samples will be linked to the same batch."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth narrow className="modal-form-grid">
          {/* Product Information Section */}
          <Column lg={8} md={4} sm={4}>
            <Select
              id="productType"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.productType",
                defaultMessage: "Product Type",
              })}
              value={bulkFormData.productType}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  productType: e.target.value,
                })
              }
            >
              {productTypes.map((opt) => (
                <SelectItem
                  key={opt.value}
                  value={opt.value}
                  text={opt.label}
                />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="productName"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.productName",
                defaultMessage: "Product Name",
              })}
              placeholder={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.productNamePlaceholder",
                defaultMessage: "e.g., Moringa Wellness Capsules",
              })}
              value={bulkFormData.productName}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  productName: e.target.value,
                })
              }
            />
          </Column>

          {/* Batch Information */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="batchId"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.batchId",
                defaultMessage: "Batch ID",
              })}
              value={bulkFormData.batchId}
              onChange={(e) =>
                setBulkFormData({ ...bulkFormData, batchId: e.target.value })
              }
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="formulationDate"
              type="date"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.formulationDate",
                defaultMessage: "Formulation Date",
              })}
              value={bulkFormData.formulationDate}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  formulationDate: e.target.value,
                })
              }
            />
          </Column>

          {/* Dosage Information */}
          <Column lg={8} md={4} sm={4}>
            <Select
              id="dosageForm"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.dosageForm",
                defaultMessage: "Dosage Form/Route",
              })}
              value={bulkFormData.dosageForm}
              onChange={(e) =>
                setBulkFormData({ ...bulkFormData, dosageForm: e.target.value })
              }
            >
              {dosageForms.map((opt) => (
                <SelectItem
                  key={opt.value}
                  value={opt.value}
                  text={opt.label}
                />
              ))}
            </Select>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="strength"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.strength",
                defaultMessage: "Strength",
              })}
              type="number"
              value={bulkFormData.strength}
              onChange={(e) =>
                setBulkFormData({ ...bulkFormData, strength: e.target.value })
              }
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Select
              id="strengthUnit"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.strengthUnit",
                defaultMessage: "Unit",
              })}
              value={bulkFormData.strengthUnit}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  strengthUnit: e.target.value,
                })
              }
            >
              {strengthUnits.map((opt) => (
                <SelectItem
                  key={opt.value}
                  value={opt.value}
                  text={opt.label}
                />
              ))}
            </Select>
          </Column>

          {/* Quantity */}
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="quantity"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.quantity",
                defaultMessage: "Quantity",
              })}
              type="number"
              value={bulkFormData.quantity}
              onChange={(e) =>
                setBulkFormData({ ...bulkFormData, quantity: e.target.value })
              }
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Select
              id="quantityUnit"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.quantityUnit",
                defaultMessage: "Unit",
              })}
              value={bulkFormData.quantityUnit}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  quantityUnit: e.target.value,
                })
              }
            >
              {quantityUnits.map((opt) => (
                <SelectItem
                  key={opt.value}
                  value={opt.value}
                  text={opt.label}
                />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="preparedBy"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.preparedBy",
                defaultMessage: "Prepared By",
              })}
              value={bulkFormData.preparedBy}
              onChange={(e) =>
                setBulkFormData({ ...bulkFormData, preparedBy: e.target.value })
              }
            />
          </Column>

          {/* Ingredients */}
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="ingredients"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.ingredients",
                defaultMessage: "Ingredients/Components",
              })}
              placeholder={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.ingredientsPlaceholder",
                defaultMessage:
                  "List ingredients and their amounts, linked to source extract IDs where applicable",
              })}
              value={bulkFormData.ingredients}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  ingredients: e.target.value,
                })
              }
              rows={3}
            />
          </Column>

          {/* Storage & Shelf Life */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="shelfLife"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.shelfLife",
                defaultMessage: "Shelf Life",
              })}
              placeholder={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.shelfLifePlaceholder",
                defaultMessage: "e.g., 24 months",
              })}
              value={bulkFormData.shelfLife}
              onChange={(e) =>
                setBulkFormData({ ...bulkFormData, shelfLife: e.target.value })
              }
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="storageInstructions"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.storageInstructions",
                defaultMessage: "Storage Instructions",
              })}
              placeholder={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.storageInstructionsPlaceholder",
                defaultMessage: "e.g., Store below 25°C in a dry place",
              })}
              value={bulkFormData.storageInstructions}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  storageInstructions: e.target.value,
                })
              }
            />
          </Column>

          {/* Notes */}
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="formulationNotes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.notes",
                defaultMessage: "Formulation Notes",
              })}
              value={bulkFormData.formulationNotes}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  formulationNotes: e.target.value,
                })
              }
              rows={2}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineFormulationPage;
