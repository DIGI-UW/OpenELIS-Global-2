import React, { useEffect, useState } from "react";
import {
  Button,
  InlineLoading,
  InlineNotification,
  Select,
  SelectItem,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { requestFailed, serverMessage } from "../../../utils/requestOutcome";
import { labNow } from "../../../utils/labClock";

interface LotOption {
  id: number;
  lotNumber: string;
  expirationDate?: string | number | null;
  currentQuantity?: number | null;
}

interface ReagentOption {
  id: number;
  name: string;
  units?: string | null;
  quantityPerTest?: number | null;
  lots: LotOption[];
}

/** FR-G5: a lot holding less than one test uses gives the run's results nothing. */
const isShort = (reagent: ReagentOption, lot: LotOption) =>
  reagent.quantityPerTest != null &&
  lot.currentQuantity != null &&
  lot.currentQuantity < reagent.quantityPerTest;

interface RunLotOptions {
  reagents: ReagentOption[];
  currentLotId?: number | null;
  lastUsedLotId?: number | null;
}

interface RunLotPickerProps {
  runId: number;
  onSaved: (header: unknown) => void;
}

const EXPIRING_SOON_DAYS = 30;

const pickValue = (reagentId: number, lotId: number) => `${reagentId}:${lotId}`;

/**
 * OGC-1200 FR-E3: the reagent lot of a workplan run, chosen on the run from the
 * usable lots of the reagents linked to its tests. Nothing is pre-selected (no
 * first-expiry auto-pick); "Use last-used lot" picks the lot recorded on the
 * last other run, and a lot expiring within a month is flagged. Expired and
 * used-up lots are not offered.
 */
const RunLotPicker: React.FC<RunLotPickerProps> = ({ runId, onSaved }) => {
  const intl = useIntl();
  const [options, setOptions] = useState<RunLotOptions | null>(null);
  const [picked, setPicked] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getFromOpenElisServer(
      `/rest/result-runs/${runId}/lot-options`,
      (body?: RunLotOptions) =>
        setOptions(
          body && Array.isArray(body.reagents)
            ? body
            : { reagents: [], currentLotId: null, lastUsedLotId: null },
        ),
    );
  }, [runId]);

  if (!options) {
    return <InlineLoading />;
  }
  if (!options.reagents.length) {
    return (
      <p className="runsChipNote" data-testid="run-lot-none">
        <FormattedMessage id="run.lot.none" />
      </p>
    );
  }

  const soon = labNow().getTime() + EXPIRING_SOON_DAYS * 24 * 60 * 60 * 1000;
  const label = (reagent: ReagentOption, lot: LotOption) => {
    const text = lot.expirationDate
      ? intl.formatMessage(
          { id: "run.lot.option" },
          {
            reagent: reagent.name,
            lot: lot.lotNumber,
            expires: intl.formatDate(new Date(lot.expirationDate), {
              year: "numeric",
              month: "short",
              day: "2-digit",
            }),
          },
        )
      : intl.formatMessage(
          { id: "run.lot.optionNoExpiry" },
          { reagent: reagent.name, lot: lot.lotNumber },
        );
    const dated =
      lot.expirationDate && new Date(lot.expirationDate).getTime() <= soon
        ? intl.formatMessage({ id: "run.lot.expiringSoon" }, { option: text })
        : text;
    return isShort(reagent, lot)
      ? intl.formatMessage(
          { id: "run.lot.short" },
          {
            option: dated,
            left: lot.currentQuantity,
            units: reagent.units ?? "",
          },
        )
      : dated;
  };
  const pickedShort = options.reagents.some((reagent) =>
    reagent.lots.some(
      (lot) =>
        pickValue(reagent.id, lot.id) === picked && isShort(reagent, lot),
    ),
  );
  const lastUsed = options.reagents.flatMap((reagent) =>
    reagent.lots
      .filter((lot) => lot.id === options.lastUsedLotId)
      .map((lot) => pickValue(reagent.id, lot.id)),
  )[0];

  const save = () => {
    const [reagentId, reagentLotId] = picked.split(":").map(Number);
    setBusy(true);
    setError(null);
    putToOpenElisServerJsonResponse(
      `/rest/result-runs/${runId}/reagent-lot`,
      JSON.stringify({ reagentId, reagentLotId }),
      (response: { id?: number; message?: string; status?: number }) => {
        setBusy(false);
        if (requestFailed(response) || !response?.id) {
          setError(
            serverMessage(response) ||
              intl.formatMessage({ id: "run.lot.failed" }),
          );
          return;
        }
        setPicked("");
        onSaved(response);
      },
    );
  };

  return (
    <div className="runLotPicker" data-testid="run-lot-picker">
      <Select
        id={`run-${runId}-lot`}
        size="sm"
        labelText={intl.formatMessage({ id: "run.lot.choose" })}
        value={picked}
        onChange={(event: React.ChangeEvent<HTMLSelectElement>) => {
          const value = event.target.value;
          setPicked(value);
        }}
      >
        <SelectItem
          value=""
          text={intl.formatMessage({ id: "run.lot.placeholder" })}
        />
        {options.reagents.flatMap((reagent) =>
          reagent.lots.map((lot) => (
            <SelectItem
              key={pickValue(reagent.id, lot.id)}
              value={pickValue(reagent.id, lot.id)}
              text={label(reagent, lot)}
            />
          )),
        )}
      </Select>
      {pickedShort && (
        <p className="unifiedFieldHint" data-testid="run-lot-short">
          <FormattedMessage id="run.lot.shortHint" />
        </p>
      )}
      <div className="runLotActions">
        {lastUsed && (
          <Button
            size="sm"
            kind="ghost"
            onClick={() => setPicked(lastUsed)}
            data-testid="run-lot-last-used"
          >
            <FormattedMessage id="run.lot.useLast" />
          </Button>
        )}
        <Button
          size="sm"
          kind="tertiary"
          disabled={!picked || busy}
          onClick={save}
          data-testid="run-lot-save"
        >
          <FormattedMessage id="run.lot.save" />
        </Button>
      </div>
      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={error}
          data-testid="run-lot-error"
        />
      )}
    </div>
  );
};

export default RunLotPicker;
