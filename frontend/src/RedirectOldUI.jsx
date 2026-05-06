import React, { useEffect } from "react";
import config from "./config.json";

function RedirectOldUI() {
  useEffect(() => {
    window.location.href =
      config.serverBaseUrl + window.location.pathname + window.location.search;
  }, []);

  return <></>;
}

export default RedirectOldUI;
