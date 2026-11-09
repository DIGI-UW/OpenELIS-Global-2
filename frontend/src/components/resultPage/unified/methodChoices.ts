import { useEffect, useState } from "react";
import { getFromOpenElisServer } from "../../utils/Utils";

export interface MethodChoice {
  id: string;
  value: string;
}

const choicesByTest = new Map<string, MethodChoice[]>();

/** Forgets what was loaded, so a catalog change is read again (tests use it too). */
export const clearMethodChoices = (): void => choicesByTest.clear();

/**
 * OGC-1614 — the methods a result of this test may record: the test's linked
 * active methods, A to Z, or every active method when none is linked, as the
 * server answers for the test. Loaded once per test.
 */
export const useMethodChoices = (
  testId: string | undefined,
): MethodChoice[] | null => {
  const [choices, setChoices] = useState<MethodChoice[] | null>(
    testId ? choicesByTest.get(testId) || null : null,
  );
  useEffect(() => {
    if (!testId) {
      setChoices(null);
      return;
    }
    const known = choicesByTest.get(testId);
    if (known) {
      setChoices(known);
      return;
    }
    let current = true;
    getFromOpenElisServer(
      `/rest/methods-for-test/${encodeURIComponent(testId)}`,
      (response: { methods?: MethodChoice[] } | undefined) => {
        const loaded = Array.isArray(response?.methods)
          ? response!.methods!
          : [];
        choicesByTest.set(testId, loaded);
        if (current) {
          setChoices(loaded);
        }
      },
    );
    return () => {
      current = false;
    };
  }, [testId]);
  return choices;
};

/**
 * The options a row's Method list shows: the test's choices, plus the method
 * the row already records when it is not one of them, so a saved value never
 * disappears from its own list. Until the choices load, every method is offered.
 */
export const methodOptions = (
  choices: MethodChoice[] | null,
  allMethods: MethodChoice[],
  current: string | undefined,
): MethodChoice[] => {
  if (!choices) {
    return allMethods;
  }
  if (!current || choices.some((m) => m.id === current)) {
    return choices;
  }
  const recorded = allMethods.find((m) => m.id === current);
  return recorded ? [recorded, ...choices] : choices;
};
