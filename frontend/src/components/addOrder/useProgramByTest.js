import { useEffect, useRef, useState } from "react";
import { getFromOpenElisServer } from "../utils/Utils";
import { convertQuestionnaireToResponse } from "./OrderEntryAdditionalQuestions";
import { triggeredProgramCodes } from "./programTriggers";

const ROUTINE_PROGRAM_CODE = "ROUTINE";

const sameId = (a, b) => String(a) === String(b);

/**
 * Attaches the questionnaire of a test-triggered program to the order while a
 * trigger test is selected, and removes it again when the test is unticked.
 * Which tests trigger which program is in programTriggers.js (by LOINC or test
 * name, never by database id).
 *
 * It only acts while the order is on the default (Routine) program or on a
 * program this hook attached itself, so a program the user chose in the
 * Program Selection step is never overridden. When the tests trigger several
 * programs, the first in programTriggers.js wins; if the attached program stops
 * matching (its test was unticked), the hook moves to the next match or clears
 * it, so a stale questionnaire never stays on the order.
 *
 * Returns true while a program was attached by this hook, so the caller can
 * render the questions on a later step.
 */
export const useProgramByTest = ({
  samples,
  orderFormValues,
  setOrderFormValues,
  domain,
}) => {
  const [programs, setPrograms] = useState([]);
  const [triggered, setTriggered] = useState(false);
  const autoProgramId = useRef(null);
  const pendingProgramId = useRef(null);
  const currentProgramId = orderFormValues?.sampleOrderItems?.programId;

  useEffect(() => {
    getFromOpenElisServer(
      `/rest/user-programs?domain=${encodeURIComponent(domain)}`,
      (list) => setPrograms(Array.isArray(list) ? list : []),
    );
  }, [domain]);

  useEffect(() => {
    if (!programs.length) {
      return;
    }
    const tests = (samples || []).flatMap((sample) => sample.tests || []);
    // Highest-priority triggered program that this user can actually use.
    const target = triggeredProgramCodes(tests)
      .map((code) => programs.find((p) => p.code?.toUpperCase() === code))
      .find(Boolean);
    const routine = programs.find(
      (p) => p.code?.toUpperCase() === ROUTINE_PROGRAM_CODE,
    );
    const usingDefault =
      !currentProgramId || (routine && sameId(currentProgramId, routine.id));
    const attachedByHook =
      autoProgramId.current && sameId(currentProgramId, autoProgramId.current);

    if (target && (usingDefault || attachedByHook)) {
      const targetId = String(target.id);
      if (attachedByHook && sameId(autoProgramId.current, targetId)) {
        return; // already attached
      }
      if (pendingProgramId.current === targetId) {
        return;
      }
      pendingProgramId.current = targetId;
      getFromOpenElisServer(
        `/rest/program/${targetId}/questionnaire`,
        (res) => {
          // The test may have been unticked while the request was in flight.
          if (pendingProgramId.current !== targetId) {
            return;
          }
          pendingProgramId.current = null;
          if (!res || !("item" in res)) {
            return;
          }
          autoProgramId.current = targetId;
          setTriggered(true);
          setOrderFormValues((prev) => ({
            ...prev,
            sampleOrderItems: {
              ...prev.sampleOrderItems,
              programId: targetId,
              questionnaire: res,
              additionalQuestions: convertQuestionnaireToResponse(res),
            },
          }));
        },
      );
    } else if (!target) {
      pendingProgramId.current = null;
      if (attachedByHook) {
        autoProgramId.current = null;
        setTriggered(false);
        setOrderFormValues((prev) => ({
          ...prev,
          sampleOrderItems: {
            ...prev.sampleOrderItems,
            programId: routine ? String(routine.id) : "",
            questionnaire: null,
            additionalQuestions: null,
          },
        }));
      }
    }
  }, [samples, programs, currentProgramId]);

  return triggered;
};
