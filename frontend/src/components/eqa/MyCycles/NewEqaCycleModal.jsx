import React, { useContext, useEffect, useState } from "react";
import { InlineNotification, Modal, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";
import CustomDatePicker from "../../common/CustomDatePicker";
import { ConfigurationContext } from "../../layout/Layout";
import { displayDateToIso } from "../../utils/Utils";
import { createMyCycle, fetchEnrollmentsOpenForCycles } from "./cyclesApi";
import SearchableSelect from "../../common/SearchableSelect";

const EMPTY_NEW_CYCLE = {
  schemeName: "",
  cycleName: "",
  distributionDate: "",
  submissionDeadline: "",
};

/**
 * "New EQA cycle": a participant records a cycle for a provider that does not
 * send consignments to this OpenELIS. Consignments from an OpenELIS provider
 * open their cycle on import, so this is the fallback. My Cycles opens it on
 * its own; order entry opens it for the scheme the sample belongs to
 * (OGC-1483), which is then fixed.
 */
const NewEqaCycleModal = ({ schemeName = "", onClose, onCreated }) => {
  const intl = useIntl();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);
  const [newCycle, setNewCycle] = useState({
    ...EMPTY_NEW_CYCLE,
    schemeName,
  });
  const [myPrograms, setMyPrograms] = useState([]);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const { configurationProperties = {} } =
    useContext(ConfigurationContext) || {};

  const setNewCycleDate = (field) => (date) =>
    setNewCycle((prev) => ({
      ...prev,
      [field]: displayDateToIso(
        date,
        configurationProperties.DEFAULT_DATE_LOCALE,
      ),
    }));

  useEffect(() => {
    let mounted = true;
    fetchEnrollmentsOpenForCycles((programs) => {
      if (mounted) setMyPrograms(programs);
    });
    return () => {
      mounted = false;
    };
  }, []);

  const handleCreate = () => {
    setSaving(true);
    setError(null);
    createMyCycle(newCycle, (result) => {
      setSaving(false);
      if (!result.ok) {
        setError(
          result.error ||
            t("eqa.cycle.new.error", "Could not create the cycle"),
        );
        return;
      }
      onCreated?.(result.cycle);
    });
  };

  return (
    <Modal
      open
      size="sm"
      modalHeading={t("eqa.cycle.new.heading", "New EQA cycle")}
      primaryButtonText={t("eqa.cycle.new.create", "Create cycle")}
      secondaryButtonText={t("eqa.queue.cancel", "Cancel")}
      primaryButtonDisabled={
        saving ||
        !newCycle.schemeName ||
        !newCycle.cycleName.trim() ||
        !newCycle.submissionDeadline
      }
      onRequestClose={onClose}
      onSecondarySubmit={onClose}
      onRequestSubmit={handleCreate}
    >
      <p style={{ color: "#525252", marginBottom: "1rem" }}>
        {t(
          "eqa.cycle.new.help",
          'For a provider that does not send consignments to this OpenELIS: record the cycle the panel belongs to so it appears here with "Receive panel". Consignments from an OpenELIS provider open their cycle on import.',
        )}
      </p>
      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={error}
          style={{ marginBottom: "1rem" }}
        />
      )}
      <SearchableSelect
        id="new-cycle-scheme"
        labelText={t("eqa.cycle.new.scheme", "Scheme (from My EQA Schemes)")}
        helperText={
          myPrograms.length === 0
            ? t(
                "eqa.cycle.new.noUsableEnrollments",
                "No active enrollment here matches a scheme on this instance. Enrollments are managed under My EQA Schemes, schemes under EQA Management, Schemes.",
              )
            : undefined
        }
        options={[
          ...(schemeName &&
          !myPrograms.some((prog) => prog.programName === schemeName)
            ? [{ id: schemeName, value: schemeName }]
            : []),
          ...myPrograms.map((prog) => ({
            id: prog.programName,
            value: prog.programName,
          })),
        ]}
        value={newCycle.schemeName}
        disabled={Boolean(schemeName)}
        onChange={(name) =>
          setNewCycle((current) => ({ ...current, schemeName: name }))
        }
      />
      <TextInput
        id="new-cycle-name"
        labelText={t("eqa.cycle.new.name", "Cycle name")}
        value={newCycle.cycleName}
        onChange={(e) =>
          setNewCycle({ ...newCycle, cycleName: e.target.value })
        }
        style={{ marginTop: "1rem" }}
      />
      <div style={{ marginTop: "1rem" }}>
        <CustomDatePicker
          id="new-cycle-distribution"
          labelText={t("eqa.cycle.new.distributionDate", "Distribution date")}
          onChange={setNewCycleDate("distributionDate")}
        />
      </div>
      <div style={{ marginTop: "1rem" }}>
        <CustomDatePicker
          id="new-cycle-deadline"
          labelText={t("eqa.cycle.new.deadline", "Submission deadline")}
          onChange={setNewCycleDate("submissionDeadline")}
        />
      </div>
    </Modal>
  );
};

export default NewEqaCycleModal;
