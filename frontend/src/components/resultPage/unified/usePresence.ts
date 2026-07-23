import { useEffect, useRef, useState } from "react";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";

/**
 * OGC-1020 (R1) — FR-O3 soft presence ("In review by X").
 *
 * Sends a heartbeat every HEARTBEAT_MS naming the analysis this session has
 * open in Edit (or null), and receives the map of analyses OTHER users are
 * editing. Advisory only — never blocks anything. Presence is session-bound
 * server-side (in-memory TTL registry, never persisted), so a closed tab or
 * auto-logout clears it within seconds.
 */

const HEARTBEAT_MS = 10_000;

export function usePresence(
  editingAnalysisId: string | null,
  visibleAnalysisIds: string[],
): Record<string, string> {
  const [presence, setPresence] = useState<Record<string, string>>({});
  const editingRef = useRef<string | null>(editingAnalysisId);
  const visibleRef = useRef<string[]>(visibleAnalysisIds);
  editingRef.current = editingAnalysisId;
  visibleRef.current = visibleAnalysisIds;

  useEffect(() => {
    let cancelled = false;

    const beat = () => {
      postToOpenElisServerJsonResponse(
        "/rest/results-entry/presence",
        JSON.stringify({
          analysisId: editingRef.current,
          visibleAnalysisIds: visibleRef.current,
        }),
        (response: Record<string, string> | undefined) => {
          if (!cancelled && response && typeof response === "object") {
            setPresence(response);
          }
        },
      );
    };

    beat();
    const timer = setInterval(beat, HEARTBEAT_MS);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, []);

  // An immediate extra beat when the edited row or the visible worklist
  // changes keeps colleagues' indicators fresh without waiting a full
  // interval (the mount beat fires before rows load, so without this a
  // fresh page wouldn't show "In review by" until the next interval).
  const visibleKey = visibleAnalysisIds.join(",");
  useEffect(() => {
    postToOpenElisServerJsonResponse(
      "/rest/results-entry/presence",
      JSON.stringify({
        analysisId: editingAnalysisId,
        visibleAnalysisIds: visibleRef.current,
      }),
      (response: Record<string, string> | undefined) => {
        if (response && typeof response === "object") {
          setPresence(response);
        }
      },
    );
  }, [editingAnalysisId, visibleKey]);

  return presence;
}
