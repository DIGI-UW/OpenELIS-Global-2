import { useEffect, useRef, useState } from "react";
import { getFromOpenElisServer } from "../utils/Utils";
import { convertQuestionnaireToResponse } from "./OrderEntryAdditionalQuestions";

/**
 * Programs that attach themselves when a given test is ordered.
 *
 * Key:   program code (Admin > Programs > Programme Code).
 * Value: ids of the tests that trigger it. Tick a test in Add Sample and open
 *        the browser console (F12): this hook logs the selected test ids.
 */
export const TEST_TRIGGERED_PROGRAMS = {
  HIV_VL: ["37"],
  // DNA_PCR: ["REPLACE_WITH_DNA_PCR_TEST_ID"],
  // TB_EXAM: ["REPLACE_WITH_TB_TEST_ID"],
};

const ROUTINE_PROGRAM_CODE = "ROUTINE";

const idOf = (test) =>
  String(test && typeof test === "object" ? test.id : test);
const sameId = (a, b) => String(a) === String(b);

/**
 * Attaches the questionnaire of a test-triggered program to the order while a
 * trigger test is selected, and removes it again when the test is unticked.
 * It only acts while the order is still on the default (Routine) program, so a
 * program the user chose in the Program Selection step is never overridden.
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
    const selected = new Set(
      (samples || []).flatMap((sample) => (sample.tests || []).map(idOf)),
    );
    console.debug("[useProgramByTest] selected test ids:", [...selected]);

    const code = Object.keys(TEST_TRIGGERED_PROGRAMS).find((c) =>
      TEST_TRIGGERED_PROGRAMS[c].some((id) => selected.has(String(id))),
    );
    const target =
      code && programs.find((p) => p.code?.toUpperCase() === code);
    const routine = programs.find(
      (p) => p.code?.toUpperCase() === ROUTINE_PROGRAM_CODE,
    );
    const usingDefault =
      !currentProgramId || (routine && sameId(currentProgramId, routine.id));

    if (target && usingDefault) {
      const targetId = String(target.id);
      if (pendingProgramId.current === targetId) {
        return;
      }
      pendingProgramId.current = targetId;
      getFromOpenElisServer(`/rest/program/${targetId}/questionnaire`, (res) => {
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
      });
    } else if (!target) {
      pendingProgramId.current = null;
      if (
        autoProgramId.current &&
        sameId(currentProgramId, autoProgramId.current)
      ) {
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

export default useProgramByTest;