import React from "react";
import { useLocation } from "react-router-dom";
import CaseViewShell from "../components/microbiology/CaseViewShell";
import MicrobiologyCaseView from "../components/microbiology/MicrobiologyCaseView";

const MicrobiologyPage = () => {
  const location = useLocation();
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
