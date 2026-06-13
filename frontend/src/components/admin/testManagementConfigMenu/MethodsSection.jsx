import React, { useCallback, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  ComboBox,
  DatePicker,
  DatePickerInput,
  InlineNotification,
  Modal,
  RadioButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TextInput,
  Toggle,
} from "@carbon/react";
import { Add, TrashCan } from "@carbon/icons-react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  deleteFromOpenElisServer,
  patchToOpenElisServerJsonResponse,
} from "../../utils/Utils";

const CODE_REGEX = /^[A-Z0-9]{3,10}$/;

export default function MethodsSection({ testId }) {
  const intl = useIntl();

  const [links, setLinks] = useState([]);
  const [allMethods, setAllMethods] = useState([]);
  const [allTests, setAllTests] = useState([]);
  const [notification, setNotification] = useState(null);

  // Link Method modal state
  const [linkModalOpen, setLinkModalOpen] = useState(false);
  const [linkMethodId, setLinkMethodId] = useState("");
  const [linkEffectiveDate, setLinkEffectiveDate] = useState("");
  const [linkIsDefault, setLinkIsDefault] = useState(false);

  // Inline create state
  const [showInlineCreate, setShowInlineCreate] = useState(false);
  const [createNameEn, setCreateNameEn] = useState("");
  const [createNameFr, setCreateNameFr] = useState("");
  const [createCode, setCreateCode] = useState("");
  const [createEffectiveDate, setCreateEffectiveDate] = useState("");
  const [createIsDefault, setCreateIsDefault] = useState(false);
  const [codeError, setCodeError] = useState("");

  // Copy from test state
  const [copyTestId, setCopyTestId] = useState("");

  const loadLinks = useCallback(() => {
    if (!testId) return;
    getFromOpenElisServer(`/rest/test/${testId}/methods`, (res) => {
      setLinks(Array.isArray(res) ? res : []);
    });
  }, [testId]);

  const loadAllMethods = useCallback(() => {
    getFromOpenElisServer("/rest/displayList/METHODS", (res) => {
      setAllMethods(Array.isArray(res) ? res : []);
    });
  }, []);

  useEffect(() => {
    loadLinks();
    loadAllMethods();
    getFromOpenElisServer("/rest/test-list", (res) => {
      setAllTests(Array.isArray(res) ? res : []);
    });
  }, [loadLinks, loadAllMethods]);

  const linkedMethodIds = new Set(links.map((l) => l.methodId));
  const availableMethods = allMethods.filter((m) => !linkedMethodIds.has(m.id));
  const otherTests = allTests.filter((t) => t.id !== testId);

  const notify = (kind, titleKey) => {
    setNotification({ kind, titleKey });
    setTimeout(() => setNotification(null), 4000);
  };

  // ── Link existing method ──────────────────────────────────────────────────

  const handleLinkSubmit = () => {
    if (!linkMethodId || !linkEffectiveDate) return;
    postToOpenElisServerJsonResponse(
      `/rest/test/${testId}/methods`,
      JSON.stringify({
        methodId: linkMethodId,
        isDefault: linkIsDefault,
        effectiveDate: linkEffectiveDate,
      }),
      (res) => {
        if (res) {
          setLinkModalOpen(false);
          setLinkMethodId("");
          setLinkEffectiveDate("");
          setLinkIsDefault(false);
          loadLinks();
          notify("success", "admin.testCatalog.methods.btn.linkMethod");
        }
      },
    );
  };

  // ── Inline create & link ──────────────────────────────────────────────────

  const validateCode = (val) => {
    const upper = val.toUpperCase();
    if (!CODE_REGEX.test(upper)) {
      setCodeError(
        intl.formatMessage({
          id: "admin.testCatalog.methods.error.codeFormat",
        }),
      );
      return false;
    }
    setCodeError("");
    return true;
  };

  const handleInlineCreate = () => {
    if (!createNameEn || !createNameFr || !createEffectiveDate) return;
    if (!validateCode(createCode)) return;
    postToOpenElisServerJsonResponse(
      `/rest/test/${testId}/methods/inline-create`,
      JSON.stringify({
        nameEnglish: createNameEn,
        nameFrench: createNameFr,
        code: createCode.toUpperCase(),
        isDefault: createIsDefault,
        effectiveDate: createEffectiveDate,
      }),
      (res) => {
        if (res) {
          setShowInlineCreate(false);
          setCreateNameEn("");
          setCreateNameFr("");
          setCreateCode("");
          setCreateEffectiveDate("");
          setCreateIsDefault(false);
          loadLinks();
          loadAllMethods();
          notify("success", "admin.testCatalog.methods.btn.createMethod");
        }
      },
    );
  };

  // ── Default toggle ────────────────────────────────────────────────────────

  const handleSetDefault = (linkId) => {
    const link = links.find((l) => l.id === linkId);
    if (!link) return;
    patchToOpenElisServerJsonResponse(
      `/rest/test/${testId}/methods/${linkId}`,
      JSON.stringify({ isDefault: true, effectiveDate: link.effectiveDate }),
      () => loadLinks(),
    );
  };

  // ── Remove link ───────────────────────────────────────────────────────────

  const handleRemove = (linkId) => {
    deleteFromOpenElisServer(`/rest/test/${testId}/methods/${linkId}`, () => {
      loadLinks();
    });
  };

  // ── Copy from test ────────────────────────────────────────────────────────

  const handleCopyFromTest = () => {
    if (!copyTestId) return;
    postToOpenElisServerJsonResponse(
      `/rest/test/${testId}/methods/copyFrom/${copyTestId}`,
      JSON.stringify({}),
      () => {
        setCopyTestId("");
        loadLinks();
        notify("success", "admin.testCatalog.methods.btn.copyFromTest");
      },
    );
  };

  // ─────────────────────────────────────────────────────────────────────────

  return (
    <div className="methods-section" style={{ marginTop: "1rem" }}>
      <h5>
        <FormattedMessage id="admin.testCatalog.methods.title" />
      </h5>

      {notification && (
        <InlineNotification
          kind={notification.kind}
          title={intl.formatMessage({ id: notification.titleKey })}
          onClose={() => setNotification(null)}
          style={{ marginBottom: "0.5rem" }}
        />
      )}

      {/* Linked methods table */}
      <TableContainer>
        <Table size="sm" useZebraStyles>
          <TableHead>
            <TableRow>
              <TableHeader>
                <FormattedMessage id="admin.testCatalog.methods.column.name" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="admin.testCatalog.methods.column.code" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="admin.testCatalog.methods.column.default" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="admin.testCatalog.methods.column.effectiveDate" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="admin.testCatalog.methods.column.actions" />
              </TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {links.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5}>
                  <FormattedMessage id="admin.testCatalog.methods.empty" />
                </TableCell>
              </TableRow>
            ) : (
              links.map((link) => (
                <TableRow key={link.id}>
                  <TableCell>{link.methodName}</TableCell>
                  <TableCell>{link.methodCode || "—"}</TableCell>
                  <TableCell>
                    <RadioButton
                      id={`default-${link.id}`}
                      labelText=""
                      checked={link.isDefault}
                      onChange={() => handleSetDefault(link.id)}
                    />
                  </TableCell>
                  <TableCell>{link.effectiveDate}</TableCell>
                  <TableCell>
                    <Button
                      kind="ghost"
                      size="sm"
                      renderIcon={TrashCan}
                      iconDescription={intl.formatMessage({
                        id: "admin.testCatalog.methods.action.remove",
                      })}
                      hasIconOnly
                      onClick={() => handleRemove(link.id)}
                    />
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Action buttons */}
      <div
        style={{
          display: "flex",
          gap: "0.5rem",
          marginTop: "0.75rem",
          flexWrap: "wrap",
        }}
      >
        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Add}
          onClick={() => setLinkModalOpen(true)}
          disabled={availableMethods.length === 0}
        >
          <FormattedMessage id="admin.testCatalog.methods.btn.linkMethod" />
        </Button>
        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Add}
          onClick={() => setShowInlineCreate((v) => !v)}
        >
          <FormattedMessage id="admin.testCatalog.methods.btn.createMethod" />
        </Button>
      </div>

      {/* Inline create form */}
      {showInlineCreate && (
        <div
          style={{
            border: "1px solid #e0e0e0",
            borderRadius: "4px",
            padding: "1rem",
            marginTop: "0.75rem",
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: "0.75rem",
          }}
        >
          <TextInput
            id="inline-name-en"
            labelText={intl.formatMessage({
              id: "admin.testCatalog.methods.inline.nameEnglish",
            })}
            value={createNameEn}
            onChange={(e) => setCreateNameEn(e.target.value)}
          />
          <TextInput
            id="inline-name-fr"
            labelText={intl.formatMessage({
              id: "admin.testCatalog.methods.inline.nameFrench",
            })}
            value={createNameFr}
            onChange={(e) => setCreateNameFr(e.target.value)}
          />
          <TextInput
            id="inline-code"
            labelText={intl.formatMessage({
              id: "admin.testCatalog.methods.inline.code",
            })}
            helperText={intl.formatMessage({
              id: "admin.testCatalog.methods.inline.codeHint",
            })}
            invalid={!!codeError}
            invalidText={codeError}
            value={createCode}
            onChange={(e) => {
              setCreateCode(e.target.value.toUpperCase());
              if (codeError) validateCode(e.target.value);
            }}
          />
          <DatePicker
            dateFormat="Y-m-d"
            datePickerType="single"
            onChange={([date]) => {
              if (date) {
                const d = new Date(date);
                setCreateEffectiveDate(
                  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`,
                );
              }
            }}
          >
            <DatePickerInput
              id="inline-effective-date"
              placeholder="YYYY-MM-DD"
              labelText={intl.formatMessage({
                id: "admin.testCatalog.methods.effectiveDate.label",
              })}
            />
          </DatePicker>
          <div
            style={{
              display: "flex",
              alignItems: "flex-end",
              gap: "1rem",
              gridColumn: "span 2",
            }}
          >
            <Toggle
              id="inline-default"
              labelText={intl.formatMessage({
                id: "admin.testCatalog.methods.default.label",
              })}
              toggled={createIsDefault}
              onToggle={(v) => setCreateIsDefault(v)}
              size="sm"
            />
            <Button
              kind="primary"
              size="sm"
              onClick={handleInlineCreate}
              disabled={
                !createNameEn ||
                !createNameFr ||
                !createCode ||
                !createEffectiveDate
              }
            >
              <FormattedMessage id="admin.testCatalog.methods.inline.createAndLink" />
            </Button>
            <Button
              kind="ghost"
              size="sm"
              onClick={() => setShowInlineCreate(false)}
            >
              <FormattedMessage id="label.button.cancel" />
            </Button>
          </div>
        </div>
      )}

      {/* Copy from Test */}
      <div
        style={{
          display: "flex",
          gap: "0.5rem",
          alignItems: "flex-end",
          marginTop: "1rem",
        }}
      >
        <ComboBox
          id="copy-from-test"
          titleText={intl.formatMessage({
            id: "admin.testCatalog.methods.copyFromTest.label",
          })}
          placeholder={intl.formatMessage({
            id: "admin.testCatalog.methods.copyFromTest.placeholder",
          })}
          items={otherTests}
          itemToString={(item) => (item ? item.value : "")}
          onChange={({ selectedItem }) => setCopyTestId(selectedItem?.id || "")}
          style={{ minWidth: "260px" }}
        />
        <Button
          kind="secondary"
          size="sm"
          onClick={handleCopyFromTest}
          disabled={!copyTestId}
        >
          <FormattedMessage id="admin.testCatalog.methods.btn.copyFromTest" />
        </Button>
      </div>

      {/* Link Method modal */}
      <Modal
        open={linkModalOpen}
        modalHeading={intl.formatMessage({
          id: "admin.testCatalog.methods.modal.link.title",
        })}
        primaryButtonText={intl.formatMessage({
          id: "admin.testCatalog.methods.btn.linkMethod",
        })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        primaryButtonDisabled={!linkMethodId || !linkEffectiveDate}
        onRequestSubmit={handleLinkSubmit}
        onRequestClose={() => setLinkModalOpen(false)}
        onSecondarySubmit={() => setLinkModalOpen(false)}
      >
        {availableMethods.length === 0 ? (
          <p>
            <FormattedMessage id="admin.testCatalog.methods.modal.link.noAvailable" />
          </p>
        ) : (
          <div
            style={{ display: "flex", flexDirection: "column", gap: "1rem" }}
          >
            <ComboBox
              id="link-method-select"
              titleText={intl.formatMessage({
                id: "admin.testCatalog.methods.modal.link.selectPlaceholder",
              })}
              items={availableMethods}
              itemToString={(item) => (item ? item.value : "")}
              onChange={({ selectedItem }) =>
                setLinkMethodId(selectedItem?.id || "")
              }
            />
            <DatePicker
              dateFormat="Y-m-d"
              datePickerType="single"
              onChange={([date]) => {
                if (date) {
                  const d = new Date(date);
                  setLinkEffectiveDate(
                    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`,
                  );
                }
              }}
            >
              <DatePickerInput
                id="link-effective-date"
                placeholder="YYYY-MM-DD"
                labelText={intl.formatMessage({
                  id: "admin.testCatalog.methods.effectiveDate.label",
                })}
              />
            </DatePicker>
            <Toggle
              id="link-default"
              labelText={intl.formatMessage({
                id: "admin.testCatalog.methods.default.label",
              })}
              toggled={linkIsDefault}
              onToggle={(v) => setLinkIsDefault(v)}
              size="sm"
            />
          </div>
        )}
      </Modal>
    </div>
  );
}
