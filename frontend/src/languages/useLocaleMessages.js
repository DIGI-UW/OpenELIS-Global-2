import { useEffect, useRef, useState } from "react";
import { loadMessagesForLocale, resolveMessagesForLocale } from "./index";

/**
 * The locale and messages the app renders with. Starts from what is in memory
 * for the initial code (English at once, a stored locale once its chunk is
 * here) and downloads what is missing; `switchLocale` downloads the new
 * locale's bundle before swapping it in, so the page never shows the old
 * language's strings under the new locale while it is on its way.
 *
 * Every download carries the number of the request that started it and only
 * the newest request may apply: a user who picks French while its chunk is
 * slow and then English stays in English when the French chunk lands, and a
 * download still out when the component unmounts changes nothing.
 */
export default function useLocaleMessages(initialCode) {
  const [state, setState] = useState(() =>
    resolveMessagesForLocale(initialCode),
  );
  const requests = useRef(0);

  useEffect(() => {
    if (state.ready) {
      return undefined;
    }
    requests.current += 1;
    const request = requests.current;
    loadMessagesForLocale(state.code).then((loaded) => {
      if (request === requests.current) {
        setState(loaded);
      }
    });
    return () => {
      requests.current += 1;
    };
  }, [state.code, state.ready]);

  /**
   * Switches to a locale in any spelling. Resolves with the applied state, or
   * null when a later switch or an unmount overtook this one.
   */
  const switchLocale = (code) => {
    requests.current += 1;
    const request = requests.current;
    return loadMessagesForLocale(code).then((loaded) => {
      if (request !== requests.current) {
        return null;
      }
      setState(loaded);
      return loaded;
    });
  };

  return { ...state, switchLocale };
}
