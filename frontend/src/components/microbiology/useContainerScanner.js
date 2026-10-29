import { useEffect } from "react";
import { useHistory } from "react-router-dom";
import { MICROBIOLOGY_CASE_PATH } from "./MicrobiologyRoutes";

export default function useContainerScanner(service, onError) {
  const history = useHistory();
  useEffect(() => {
    let buffer = "";
    let last = 0;
    let live = true;
    const key = (event) => {
      if (event.ctrlKey || event.altKey || event.metaKey || event.isComposing)
        return;
      const now = performance.now();
      if (now - last > 80) buffer = "";
      last = now;
      if (event.key === "Enter") {
        const code = buffer;
        buffer = "";
        if (!/^(PLT|ISO|AST)-[a-f\d-]{36}-[a-f\d-]{36}$/i.test(code)) return;
        event.preventDefault();
        service
          .resolveContainer(code)
          .then((target) => {
            if (live)
              history.push(
                `${MICROBIOLOGY_CASE_PATH}/${encodeURIComponent(target.caseId)}?scope=${target.scope}&row=${encodeURIComponent(target.rowId)}`,
              );
          })
          .catch(() => {
            if (live) onError?.();
          });
      } else if (event.key.length === 1)
        buffer = (buffer + event.key).slice(-90);
    };
    window.addEventListener("keydown", key);
    return () => {
      live = false;
      window.removeEventListener("keydown", key);
    };
  }, [service, history, onError]);
}
