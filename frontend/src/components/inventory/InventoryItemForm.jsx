import React, {
  useState,
  useEffect,
  useContext,
  useCallback,
  useRef,
} from "react";
import {
  Modal,
  TextInput,
  NumberInput,
  Checkbox,
  TextArea,
  Stack,
  Button,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import { InventoryItemAPI } from "./InventoryService";

// Same rule as the server's CodeGenerator.toCode; it does not truncate, the
// server does, so a code that grows on upper-casing (e.g. ß to SS) is cut there.
const toCode = (value) =>
  value
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");

// One definition of an empty form, used both to seed it and to reset it. Two
// copies of this object drifted apart the moment a field was added to one.
const EMPTY_FORM = {
  code: "",
  name: "",
  tags: [],
  category: "",
  manufacturer: "",
  catalogNumber: "",
  upc: "",
  units: "",
  lowStockThreshold: 0,
  expirationAlertDays: "",
  trackLots: false,
  stabilityAfterOpening: "",
  storageRequirements: "",
  compatibleAnalyzers: "",
  testsPerKit: "",
  leadTimeDays: "",
};

const InventoryItemForm = ({
  open,
  onClose,
  onSave,
  item = null,
  observedLeadTime = null,
  initialUpc = null,
}) => {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const notify = useCallback(
    ({ kind, title, subtitle }) => {
      setNotificationVisible(true);
      addNotification({
        kind,
        title,
        subtitle,
      });
    },
    [addNotification, setNotificationVisible],
  );
  const isEdit = !!item;

  // Form state
  const [formData, setFormData] = useState(
    initialUpc ? { ...EMPTY_FORM, upc: initialUpc } : EMPTY_FORM,
  );

  const [saving, setSaving] = useState(false);
  const normalizedCode = toCode(formData.code);
  const isMountedRef = useRef(true);
  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const [error, setError] = useState(null);
  const [tagSuggestions, setTagSuggestions] = useState([]);
  const [tagDraft, setTagDraft] = useState("");

  // Suggestions are every tag already in use, so a lab converges on its own vocabulary
  // instead of each item inventing one.
  useEffect(() => {
    if (!open) return;
    const loadTags = async () => {
      try {
        const tags = await InventoryItemAPI.getTags();
        // The dialog can close while this is in flight.
        if (!isMountedRef.current) return;
        setTagSuggestions(tags);
      } catch (err) {
        // A suggestion list that fails to load costs nothing: a tag can still be typed.
        console.error("Error loading tags:", err);
      }
    };
    loadTags();
  }, [open]);

  // Load item data if editing, reset if adding new
  useEffect(() => {
    if (item) {
      setFormData({
        code: item.code || "",
        name: item.name || "",
        tags: item.tags || [],
        category: item.category || "",
        manufacturer: item.manufacturer || "",
        catalogNumber: item.catalogNumber || "",
        upc: item.upc || "",
        units: item.units || "",
        lowStockThreshold: item.lowStockThreshold || 0,
        expirationAlertDays: item.expirationAlertDays ?? "",
        trackLots: item.trackLots === "Y",
        stabilityAfterOpening: item.stabilityAfterOpening ?? "",
        storageRequirements: item.storageRequirements || "",
        compatibleAnalyzers: item.compatibleAnalyzers || "",
        testsPerKit: item.testsPerKit ?? "",
        leadTimeDays: item.leadTimeDays ?? "",
      });
    } else {
      // A scan that matched nothing arrives here with its code, so the item
      // being defined starts out already carrying the barcode that found it.
      setFormData(initialUpc ? { ...EMPTY_FORM, upc: initialUpc } : EMPTY_FORM);
    }
  }, [item, open, initialUpc]);

  // Handle input changes
  const handleChange = (field, value) => {
    // Convert empty string or NaN to 0 for numeric fields
    // leadTimeDays, stabilityAfterOpening and testsPerKit are deliberately absent:
    // each is optional, and blank has to survive as blank rather than becoming a
    // zero the server then rejects.
    const numericFields = ["lowStockThreshold"];

    let processedValue = value;
    if (numericFields.includes(field)) {
      if (value === "" || value === null || value === undefined) {
        processedValue = 0;
      } else if (isNaN(value)) {
        processedValue = 0;
      }
    }

    setFormData((prev) => {
      // Prevent unnecessary state updates if value hasn't changed
      if (prev[field] === processedValue) {
        return prev;
      }
      return { ...prev, [field]: processedValue };
    });
    setError(null);
  };

  /** An unfilled optional number is absent, not zero. */
  const optionalNumber = (value) =>
    value === "" || value == null || Number(value) === 0 ? null : Number(value);

  // Tags are compared without case or spacing so a second spelling of one a lab
  // already uses cannot be added twice. The backend applies the same rule, and is
  // what settles which spelling is kept.
  const tagKey = (tag) => tag.trim().replace(/\s+/g, " ").toLowerCase();

  const hasTag = (tag) =>
    formData.tags.some((applied) => tagKey(applied) === tagKey(tag ?? ""));

  const addTag = (tag) => {
    const trimmed = (tag ?? "").trim().replace(/\s+/g, " ");
    if (!trimmed || hasTag(trimmed)) return;
    setFormData((prev) => ({ ...prev, tags: [...prev.tags, trimmed] }));
    setTagDraft("");
    setError(null);
  };

  const removeTag = (tag) => {
    setFormData((prev) => ({
      ...prev,
      tags: prev.tags.filter((applied) => applied !== tag),
    }));
  };

  // Validate form
  const validate = () => {
    if (!formData.name?.trim()) {
      setError(intl.formatMessage({ id: "catalog.item.error.nameRequired" }));
      return false;
    }

    // The three rules that used to live here demanded a field per item type — stability
    // for a reagent, analyzers for a cartridge, tests-per-kit for an RDT. A tag carries no
    // behaviour, so there is nothing left to key them on, and every one of those fields is
    // now offered to every item and optional on all of them.
    return true;
  };

  // Handle save
  const handleSave = async () => {
    if (!validate()) return;

    // A tag typed but not confirmed with Enter is still a tag the user asked
    // for. Dropping it silently on save is the kind of loss nobody notices
    // until the item cannot be found by it.
    const typedTags = tagDraft.trim()
      ? [...formData.tags, tagDraft.trim()]
      : formData.tags;

    setSaving(true);
    setError(null);

    try {
      // Build sanitized data with only type-relevant fields
      const sanitizedData = {
        name: formData.name,
        tags: typedTags,
        category: formData.category,
        manufacturer: formData.manufacturer,
        units: formData.units,
        lowStockThreshold: Number(formData.lowStockThreshold) || 0,
        // Blank means "not entered", which is not the same as zero: resolveLeadTime
        // only treats a positive value as set, and null is what lets the learned
        // figure take over.
        leadTimeDays:
          formData.leadTimeDays === "" || formData.leadTimeDays == null
            ? null
            : Number(formData.leadTimeDays),
      };

      // Sent for every item now. They used to ride on the item type, so an item that was
      // not a reagent could not record a storage requirement even when it had one.
      // Blank stays null rather than becoming 0: both numbers carry a minimum of 1 on
      // the entity, so a zero is a 400 rather than an empty field.
      sanitizedData.stabilityAfterOpening = optionalNumber(
        formData.stabilityAfterOpening,
      );
      sanitizedData.storageRequirements = formData.storageRequirements;
      sanitizedData.compatibleAnalyzers = formData.compatibleAnalyzers;
      sanitizedData.testsPerKit = optionalNumber(formData.testsPerKit);
      sanitizedData.catalogNumber = formData.catalogNumber;
      sanitizedData.upc = formData.upc ? formData.upc.trim() : null;
      sanitizedData.expirationAlertDays = optionalNumber(
        formData.expirationAlertDays,
      );
      // The column is the module's Y/N convention, not a boolean.
      sanitizedData.trackLots = formData.trackLots ? "Y" : "N";

      if (isEdit) {
        await InventoryItemAPI.update(item.id, sanitizedData);
      } else {
        // Never sent on update: lot numbers embed it (generateLotNumber).
        sanitizedData.code = toCode(formData.code) || null;
        await InventoryItemAPI.create(sanitizedData);
      }
      if (!isMountedRef.current) return;
      setSaving(false);
      onSave();
    } catch (err) {
      console.error("Error saving item:", err);
      // errorCode is an en.json id; message is the raw backend string.
      const errorMessage = err.errorCode
        ? intl.formatMessage({ id: err.errorCode }, err.params)
        : err.message ||
          intl.formatMessage({ id: "catalog.item.error.saveGeneric" });
      if (!isMountedRef.current) return;
      setError(errorMessage);
      setSaving(false);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: errorMessage,
      });
    }
  };

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      onRequestSubmit={handleSave}
      modalHeading={intl.formatMessage({
        // Not "catalog item": the catalog is gone, and the old wording read as
        // though this were where stock is added.
        id: isEdit
          ? "inventory.item.form.title.edit"
          : "inventory.item.form.title.add",
      })}
      primaryButtonText={intl.formatMessage({ id: "button.save" })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      primaryButtonDisabled={saving}
      size="lg"
    >
      {/* Said plainly and first: this screen defines a kind of thing. Adding
          more of one that already exists is Receive stock, and the two have
          been confused often enough to be worth one sentence. */}
      <p className="inventory-item-derived">
        <FormattedMessage id="catalog.item.form.help" />
      </p>
      <Stack gap={5}>
        {error && (
          <div style={{ color: "red", marginBottom: "1rem" }}>{error}</div>
        )}

        <TextInput
          id="name"
          labelText={<FormattedMessage id="catalog.item.name" />}
          value={formData.name}
          onChange={(e) => handleChange("name", e.target.value)}
          required
        />

        <TextInput
          id="code"
          labelText={
            <FormattedMessage id="catalog.item.code" defaultMessage="Code" />
          }
          value={formData.code}
          disabled={isEdit}
          placeholder={
            isEdit
              ? ""
              : intl.formatMessage({
                  id: "catalog.item.code.placeholder",
                  defaultMessage: "Leave blank to auto-generate from name",
                })
          }
          helperText={
            isEdit
              ? intl.formatMessage({
                  id: "catalog.item.code.locked",
                  defaultMessage:
                    "Code is locked once saved so integrations and existing references keep working.",
                })
              : normalizedCode && normalizedCode !== formData.code
                ? intl.formatMessage(
                    {
                      id: "catalog.item.code.preview",
                      defaultMessage: "Will be saved as {code}",
                    },
                    { code: normalizedCode },
                  )
                : intl.formatMessage({
                    id: "catalog.item.code.hint",
                    defaultMessage:
                      "Stable identifier used by integrations. Leave blank and we'll generate one from the name, like PAR-500MG-001.",
                  })
          }
          maxLength={64}
          onChange={(e) => handleChange("code", e.target.value)}
        />

        {/* A text input with a native datalist rather than Carbon's ComboBox.
            ComboBox wraps Downshift, which owns the input's value, so clearing
            the field after adding a tag meant remounting the control — and
            remounting it destroyed the focused element mid-keystroke, which
            closed the editor and opened whatever modal caught the focus next.
            A controlled input clears by setting state, and the browser's own
            datalist gives the same suggestion list with none of that. */}
        <div className="inventory-item-tags">
          <TextInput
            id="itemTags"
            list="inventory-tag-suggestions"
            labelText={<FormattedMessage id="inventory.item.tags" />}
            helperText={intl.formatMessage({ id: "inventory.item.tags.help" })}
            placeholder={intl.formatMessage({ id: "inventory.item.tags.add" })}
            value={tagDraft}
            onChange={(e) => setTagDraft(e.target.value)}
            onKeyDown={(event) => {
              if (event.key !== "Enter") return;
              event.preventDefault();
              event.stopPropagation();
              addTag(tagDraft);
            }}
          />
          <datalist id="inventory-tag-suggestions">
            {tagSuggestions
              .filter((tag) => !hasTag(tag))
              .map((tag) => (
                <option key={tag} value={tag} />
              ))}
          </datalist>
          {formData.tags.length > 0 && (
            <div className="inventory-item-tags__chips">
              {formData.tags.map((tag) => (
                <Tag
                  key={tag}
                  type="cool-gray"
                  filter
                  onClose={() => removeTag(tag)}
                  title={intl.formatMessage(
                    { id: "inventory.item.tags.remove" },
                    { tag },
                  )}
                >
                  {tag}
                </Tag>
              ))}
            </div>
          )}
        </div>

        <div className="inventory-item-pair">
          <TextInput
            id="category"
            labelText={<FormattedMessage id="catalog.item.category" />}
            value={formData.category}
            onChange={(e) => handleChange("category", e.target.value)}
          />
          <TextInput
            id="manufacturer"
            labelText={<FormattedMessage id="catalog.item.manufacturer" />}
            value={formData.manufacturer}
            onChange={(e) => handleChange("manufacturer", e.target.value)}
          />
        </div>

        <div className="inventory-item-pair">
          <TextInput
            id="catalogNumber"
            labelText={<FormattedMessage id="catalog.item.catalogNumber" />}
            value={formData.catalogNumber}
            onChange={(e) => handleChange("catalogNumber", e.target.value)}
          />
          <TextInput
            id="upc"
            labelText={<FormattedMessage id="catalog.item.upc" />}
            helperText={intl.formatMessage({ id: "catalog.item.upc.help" })}
            value={formData.upc}
            onChange={(e) => handleChange("upc", e.target.value)}
          />
          <TextInput
            id="units"
            labelText={<FormattedMessage id="catalog.item.units" />}
            value={formData.units}
            onChange={(e) => handleChange("units", e.target.value)}
            placeholder="e.g., mL, tests, kits"
          />
        </div>

        <Checkbox
          id="trackLots"
          labelText={intl.formatMessage({ id: "catalog.item.trackLots" })}
          helperText={intl.formatMessage({ id: "catalog.item.trackLots.help" })}
          checked={formData.trackLots}
          onChange={(_, { checked }) => handleChange("trackLots", checked)}
        />

        <NumberInput
          id="lowStockThreshold"
          label={<FormattedMessage id="catalog.item.lowStockThreshold" />}
          value={formData.lowStockThreshold ?? 0}
          onChange={(e, { value }) =>
            handleChange("lowStockThreshold", value ?? 0)
          }
          min={0}
          max={999999}
        />

        <NumberInput
          id="leadTimeDays"
          label={<FormattedMessage id="inventory.item.leadTime" />}
          helperText={intl.formatMessage({
            id: "inventory.item.leadTime.help",
          })}
          value={formData.leadTimeDays}
          onChange={(e, { value }) => handleChange("leadTimeDays", value)}
          min={0}
          max={999}
          allowEmpty
        />

        {observedLeadTime != null &&
          Number(formData.leadTimeDays) !== observedLeadTime && (
            <div className="inventory-item-suggestion">
              <span>
                <FormattedMessage
                  id="inventory.item.leadTime.observedSuggest"
                  values={{ days: observedLeadTime }}
                />
              </span>
              <Button
                kind="ghost"
                size="sm"
                onClick={() => handleChange("leadTimeDays", observedLeadTime)}
              >
                <FormattedMessage id="inventory.item.leadTime.useObserved" />
              </Button>
            </div>
          )}

        <NumberInput
          id="expirationAlertDays"
          label={<FormattedMessage id="catalog.item.expirationAlertDays" />}
          value={formData.expirationAlertDays}
          onChange={(e, { value }) =>
            handleChange("expirationAlertDays", value)
          }
          min={0}
          max={365}
          allowEmpty
        />

        <NumberInput
          id="stabilityAfterOpening"
          label={<FormattedMessage id="catalog.item.stabilityAfterOpening" />}
          value={formData.stabilityAfterOpening}
          onChange={(e, { value }) =>
            handleChange("stabilityAfterOpening", value)
          }
          min={0}
          max={365}
          allowEmpty
        />

        <TextArea
          id="storageRequirements"
          labelText={<FormattedMessage id="catalog.item.storageRequirements" />}
          value={formData.storageRequirements}
          onChange={(e) => handleChange("storageRequirements", e.target.value)}
          placeholder="e.g., Store at 2-8°C, protect from light"
        />

        <TextInput
          id="compatibleAnalyzers"
          labelText={<FormattedMessage id="catalog.item.compatibleAnalyzers" />}
          value={formData.compatibleAnalyzers}
          onChange={(e) => handleChange("compatibleAnalyzers", e.target.value)}
          placeholder="e.g., GeneXpert, Cobas"
        />

        <NumberInput
          id="testsPerKit"
          label={<FormattedMessage id="catalog.item.testsPerKit" />}
          value={formData.testsPerKit}
          onChange={(e, { value }) => handleChange("testsPerKit", value)}
          min={0}
          max={1000}
          allowEmpty
        />

        <p className="inventory-item-derived">
          <FormattedMessage id="inventory.item.autoConsume.help" />
        </p>
      </Stack>
    </Modal>
  );
};

export default InventoryItemForm;
