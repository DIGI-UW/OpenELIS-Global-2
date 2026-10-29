import React from "react";
import useContainerScanner from "../components/microbiology/useContainerScanner";
import workspaceService from "../components/microbiology/CaseWorkspaceService";
import { useLocation } from "react-router-dom";
import CaseViewShell from "../components/microbiology/CaseViewShell";
import MicrobiologyCaseView from "../components/microbiology/MicrobiologyCaseView";

const MicrobiologyPage = () => {
  const location = useLocation();
  useContainerScanner(workspaceService);
  const query = new URLSearchParams(location.search);
  return query.get("view") === "workbench" ||
    query.has("section") ||
    ["cultures", "ast"].includes(query.get("grain")) ? (
    <MicrobiologyCaseView />
  ) : (
    <CaseViewShell />
  );
};
export default MicrobiologyPage;
