import React, { useEffect, useState } from "react";
import {
  DatePicker,
  DatePickerInput,
  Dropdown,
  FilterableMultiSelect,
  InlineNotification,
  Modal,
  Stack,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  toLocalIsoDate,
} from "../../utils/Utils";

/**
 * Enroll tests under an accrediting body (OGC-686, D.2). The API enrolls one test
 * per call, so a multi-select fans out to N POSTs; one rejection fails that test
 * and leaves the rest of the batch alone.
 *
 * Each rejection carries its reason in {"error": ...} — "This test is already
 * accredited by that body" (FRS AC-8), an unknown test, an unknown body. Report
 * that reason against the test it belongs to: a bare failure count tells the QA
 * lead nothing they can act on, and cannot tell a duplicate from a 500.
 */
const EnrollTestsModal = ({ open, onClose, bodies, onSaved }) => {
  const intl = useIntl();

  const [tests, setTests] = useState([]);
  const [selectedBody, setSelectedBody] = useState(null);
  const [selectedTests, setSelectedTests] = useState([]);
  const [effectiveFrom, setEffectiveFrom] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    setError(null);
    setSelectedBody(null);
    setSelectedTests([]);
    setEffectiveFrom("");
    getFromOpenElisServer("/rest/displayList/ALL_TESTS", (data) =>
      setTests(
        Array.isArray(data)
          ? data.map((t) => ({ id: String(t.id), text: t.value }))
          : [],
      ),
    );
  }, [open]);

  const handleSubmit = () => {
    if (!selectedBody || selectedTests.length === 0) {
      setError(
        intl.formatMessage({ id: "qa.qms.accreditation.enroll.required" }),
      );
      return;
    }
    setSaving(true);
    setError(null);
    const total = selectedTests.length;
    const generic = intl.formatMessage({
      id: "qa.qms.accreditation.enroll.failed",
    });
    const failures = [];
    let done = 0;

    const settle = (test, reason) => {
      if (reason) {
        failures.push({ test: test.text, reason });
      }
      done += 1;
      if (done < total) {
        return;
      }
      setSaving(false);
      onSaved();
      if (failures.length === 0) {
        onClose();
        return;
      }
      // One test selected: the reason is the whole story. Several: name each
      // failing test, or the QA lead has to guess which of them bounced.
      setError(
        total === 1
          ? failures[0].reason
          : failures.map((f) => `${f.test}: ${f.reason}`).join("; "),
      );
    };

    selectedTests.forEach((test) => {
      postToOpenElisServerFullResponse(
        "/rest/accreditation/enrollments",
        JSON.stringify({
          testId: test.id,
          accreditingBodyId: selectedBody.id,
          effectiveFrom: effectiveFrom || null,
        }),
        (response) => {
          if (response && response.ok) {
            settle(test, null);
            return;
          }
          if (!response) {
            settle(test, generic);
            return;
          }
          response
            .json()
            .then((data) => settle(test, data?.error || generic))
            .catch(() => settle(test, generic));
        },
      );
    });
  };

  const activeBodies = bodies.filter((b) => b.active);

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      onRequestSubmit={handleSubmit}
      modalHeading={intl.formatMessage({
        id: "qa.qms.accreditation.enroll.modal.title",
      })}
      primaryButtonText={intl.formatMessage({ id: "button.save" })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      primaryButtonDisabled={saving}
      size="md"
    >
      <Stack gap={5}>
        <Dropdown
          id="enroll-body"
          titleText={intl.formatMessage({
            id: "qa.qms.accreditation.enroll.field.body",
          })}
          label=""
          items={activeBodies}
          itemToString={(item) => (item ? `${item.code} — ${item.name}` : "")}
          selectedItem={selectedBody}
          onChange={({ selectedItem }) => setSelectedBody(selectedItem)}
        />
        <FilterableMultiSelect
          id="enroll-tests"
          titleText={intl.formatMessage({
            id: "qa.qms.accreditation.enroll.field.tests",
          })}
          items={tests}
          itemToString={(item) => (item ? item.text : "")}
          selectedItems={selectedTests}
          onChange={({ selectedItems }) => setSelectedTests(selectedItems)}
          placeholder={intl.formatMessage({
            id: "qa.qms.accreditation.enroll.field.tests",
          })}
        />
        <DatePicker
          datePickerType="single"
          dateFormat="Y-m-d"
          value={effectiveFrom ? [effectiveFrom] : []}
          onChange={(dates) =>
            setEffectiveFrom(dates.length ? toLocalIsoDate(dates[0]) : "")
          }
        >
          <DatePickerInput
            id="enroll-effective-from"
            labelText={intl.formatMessage({
              id: "qa.qms.accreditation.enroll.field.effectiveFrom",
            })}
            placeholder="yyyy-mm-dd"
          />
        </DatePicker>

        {error && (
          <InlineNotification
            kind="error"
            lowContrast
            onCloseButtonClick={() => setError(null)}
            title={intl.formatMessage({ id: "error.title" })}
            subtitle={error}
          />
        )}
      </Stack>
    </Modal>
  );
};

export default EnrollTestsModal;
