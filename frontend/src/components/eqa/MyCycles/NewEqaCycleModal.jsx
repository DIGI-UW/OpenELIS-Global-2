import React, { useEffect, useState } from "react";
import {
  InlineNotification,
  Modal,
  Select,
  SelectItem,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { createMyCycle, fetchMyPrograms } from "./cyclesApi";

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

  useEffect(() => {
    let mounted = true;
    fetchMyPrograms((programs) => {
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
      <Select
        id="new-cycle-scheme"
        labelText={t("eqa.cycle.new.scheme", "Scheme (from My EQA Schemes)")}
        helperText={
          myPrograms.length === 0
            ? t(
                "eqa.cycle.new.noPrograms",
                "Enroll in a scheme under My EQA Schemes first.",
              )
            : undefined
        }
        value={newCycle.schemeName}
        disabled={Boolean(schemeName)}
        onChange={(e) =>
          setNewCycle({ ...newCycle, schemeName: e.target.value })
        }
      >
        <SelectItem value="" text="" />
        {schemeName &&
          !myPrograms.some((prog) => prog.programName === schemeName) && (
            <SelectItem value={schemeName} text={schemeName} />
          )}
        {myPrograms.map((prog) => (
          <SelectItem
            key={prog.id}
            value={prog.programName}
            text={prog.programName}
          />
        ))}
      </Select>
      <TextInput
        id="new-cycle-name"
        labelText={t("eqa.cycle.new.name", "Cycle name")}
        value={newCycle.cycleName}
        onChange={(e) =>
          setNewCycle({ ...newCycle, cycleName: e.target.value })
        }
        style={{ marginTop: "1rem" }}
      />
      <TextInput
        id="new-cycle-distribution"
        type="date"
        labelText={t("eqa.cycle.new.distributionDate", "Distribution date")}
        value={newCycle.distributionDate}
        onChange={(e) =>
          setNewCycle({ ...newCycle, distributionDate: e.target.value })
        }
        style={{ marginTop: "1rem" }}
      />
      <TextInput
        id="new-cycle-deadline"
        type="date"
        labelText={t("eqa.cycle.new.deadline", "Submission deadline")}
        value={newCycle.submissionDeadline}
        onChange={(e) =>
          setNewCycle({ ...newCycle, submissionDeadline: e.target.value })
        }
        style={{ marginTop: "1rem" }}
      />
    </Modal>
  );
};

export default NewEqaCycleModal;
